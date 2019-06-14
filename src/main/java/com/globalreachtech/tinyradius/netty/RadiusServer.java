package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.attribute.RadiusAttribute;
import com.globalreachtech.tinyradius.dictionary.DefaultDictionary;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.AccessRequest;
import com.globalreachtech.tinyradius.packet.AccountingRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.PromiseCombiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.*;
import static java.util.Objects.requireNonNull;

/**
 * Implements a simple Radius server. This class must be subclassed to
 * provide an implementation for getSharedSecret() and getUserPassword().
 * If the server supports accounting, it must override
 * accountingRequestReceived().
 */
public abstract class RadiusServer<T extends DatagramChannel> {

    private static Log logger = LogFactory.getLog(RadiusServer.class);

    final ChannelFactory<T> factory;
    final EventLoopGroup eventLoopGroup;
    final EventExecutorGroup eventExecutorGroup;
    private final Dictionary dictionary;
    final PacketManager packetManager;
    final int authPort;
    final int acctPort;

    private InetAddress listenAddress = null;
    private T authSocket = null;
    private T acctSocket = null;

    private Future<Void> startStatus = null;

    public RadiusServer(EventLoopGroup eventLoopGroup, EventExecutorGroup eventExecutorGroup, ChannelFactory<T> factory) {
        this(DefaultDictionary.INSTANCE, eventLoopGroup, eventExecutorGroup, factory);
    }

    public RadiusServer(Dictionary dictionary, EventLoopGroup eventLoopGroup, EventExecutorGroup eventExecutorGroup, ChannelFactory<T> factory) {
        this(dictionary, eventLoopGroup, eventExecutorGroup, factory, new ServerPacketManager(new HashedWheelTimer(), 30000), 1812, 1813);
    }

    public RadiusServer(Dictionary dictionary,
                        EventLoopGroup eventLoopGroup,
                        EventExecutorGroup eventExecutorGroup,
                        ChannelFactory<T> factory,
                        PacketManager packetManager,
                        int authPort, int acctPort) {
        this.dictionary = requireNonNull(dictionary, "dictionary cannot be null");
        this.eventLoopGroup = requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.eventExecutorGroup = requireNonNull(eventExecutorGroup, "eventExecutorGroup cannot be null");
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.packetManager = packetManager;
        this.authPort = validPort(authPort);
        this.acctPort = validPort(acctPort);
    }

    /**
     * Returns the shared secret used to communicate with the client with the
     * passed IP address or null if the client is not allowed at this server.
     *
     * @param client IP address and port number of client
     * @return shared secret or null
     */
    public abstract String getSharedSecret(InetSocketAddress client);

    /**
     * Returns the password of the passed user. Either this
     * method or accessRequestReceived() should be overriden.
     *
     * @param userName user name
     * @return plain-text password or null if user unknown
     */
    public abstract String getUserPassword(String userName);

    /**
     * Constructs an answer for an Access-Request packet. This
     * method should be overridden.
     *
     * @param accessRequest Radius clientRequest packet
     * @param client        address of Radius client
     * @return clientResponse packet or null if no packet shall be sent
     * @throws RadiusException malformed clientRequest packet; if this
     *                         exception is thrown, no answer will be sent
     */
    public RadiusPacket accessRequestReceived(AccessRequest accessRequest, InetSocketAddress client) throws RadiusException {
        String plaintext = getUserPassword(accessRequest.getUserName());
        int type = plaintext != null && accessRequest.verifyPassword(plaintext) ? ACCESS_ACCEPT : ACCESS_REJECT;

        RadiusPacket answer = new RadiusPacket(type, accessRequest.getPacketIdentifier());
        answer.setDictionary(dictionary);
        copyProxyState(accessRequest, answer);
        return answer;
    }

    /**
     * Constructs an answer for an Accounting-Request packet. This method
     * should be overridden.
     *
     * @param accountingRequest Radius clientRequest packet
     * @return clientResponse packet or null if no packet shall be sent
     * @throws RadiusException malformed clientRequest packet; if this
     *                         exception is thrown, no answer will be sent
     */
    public RadiusPacket accountingRequestReceived(AccountingRequest accountingRequest) {
        RadiusPacket answer = new RadiusPacket(ACCOUNTING_RESPONSE, accountingRequest.getPacketIdentifier());
        copyProxyState(accountingRequest, answer);
        return answer;
    }

    /**
     * Starts the Radius server.
     */
    public Future<Void> start() {
        if (this.startStatus != null)
            return this.startStatus;

        final DefaultPromise<Void> status = new DefaultPromise<>(eventLoopGroup.next());

        final PromiseCombiner promiseCombiner = new PromiseCombiner(eventLoopGroup.next());
        promiseCombiner.addAll(listenAuth(), listenAcct());
        promiseCombiner.finish(status);

        this.startStatus = status;
        return status;
    }

    /**
     * Stops the server and closes the sockets.
     */
    public void stop() {
        logger.info("stopping Radius server");
        if (authSocket != null)
            authSocket.close();
        if (acctSocket != null)
            acctSocket.close();
    }

    int validPort(int port) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("bad port number");
        return port;
    }

    /**
     * Returns the IP address the server listens on.
     * Returns null if listening on the wildcard address.
     *
     * @return listen address or null
     */
    public InetAddress getListenAddress() {
        return listenAddress;
    }

    /**
     * Sets the address the server listens on.
     * Must be called before start().
     * Defaults to null, meaning listen on every
     * local address (wildcard address).
     *
     * @param listenAddress listen address or null
     */
    public void setListenAddress(InetAddress listenAddress) {
        this.listenAddress = listenAddress;
    }

    /**
     * Copies all Proxy-State attributes from the clientRequest
     * packet to the clientResponse packet.
     *
     * @param request clientRequest packet
     * @param answer  clientResponse packet
     */
    protected void copyProxyState(RadiusPacket request, RadiusPacket answer) {
        List<RadiusAttribute> proxyStateAttrs = request.getAttributes(33);
        for (RadiusAttribute stateAttr : proxyStateAttrs) {
            answer.addAttribute(stateAttr);
        }
    }

    protected ChannelFuture listenAuth() {
        logger.info("starting RadiusAuthListener on port " + authPort);
        return listen(getAuthSocket(), new InetSocketAddress(listenAddress, authPort));
    }

    protected ChannelFuture listenAcct() {
        logger.info("starting RadiusAcctListener on port " + acctPort);
        return listen(getAcctSocket(), new InetSocketAddress(listenAddress, acctPort));
    }

    /**
     * @param channel       to listen on
     * @param listenAddress the address to bind to
     */
    protected ChannelFuture listen(final T channel, final InetSocketAddress listenAddress) {
        requireNonNull(channel, "channel cannot be null");
        requireNonNull(listenAddress, "listenAddress cannot be null");

        final ChannelPromise promise = channel.newPromise()
                .addListener(f -> channel.pipeline().addLast(new RadiusChannelHandler()));

        final PromiseCombiner promiseCombiner = new PromiseCombiner(eventLoopGroup.next());
        promiseCombiner.addAll(eventLoopGroup.register(channel), channel.bind(listenAddress));
        promiseCombiner.finish(promise);

        return promise;
    }

    /**
     * Handles the received Radius packet and constructs a clientResponse.
     *
     * @param localAddress  local address the packet was received on
     * @param remoteAddress remote address the packet was sent by
     * @param request       the packet
     * @return clientResponse packet or null for no clientResponse
     * @throws RadiusException
     */
    protected RadiusPacket handlePacket(InetSocketAddress localAddress, InetSocketAddress remoteAddress, RadiusPacket request, String sharedSecret) throws RadiusException, IOException {
        // check for duplicates
        if (!packetManager.isPacketDuplicate(request, remoteAddress)) {
            if (localAddress.getPort() == authPort) {
                // handle packets on auth port
                if (request instanceof AccessRequest)
                    return accessRequestReceived((AccessRequest) request, remoteAddress);
                else
                    logger.error("unknown Radius packet type: " + request.getPacketType());
            } else if (localAddress.getPort() == acctPort) {
                // handle packets on acct port
                if (request instanceof AccountingRequest)
                    return accountingRequestReceived((AccountingRequest) request);
                else
                    logger.error("unknown Radius packet type: " + request.getPacketType());
            }  // ignore packet on unknown port
        } else
            logger.info("ignore duplicate packet");

        return null;
    }

    protected T getAuthSocket() {
        if (authSocket == null) {
            authSocket = factory.newChannel();
        }
        return authSocket;
    }

    protected T getAcctSocket() {
        if (acctSocket == null) {
            acctSocket = factory.newChannel();
        }
        return acctSocket;
    }

    /**
     * Creates a Radius clientResponse datagram packet from a RadiusPacket to be send.
     *
     * @param packet  RadiusPacket
     * @param secret  shared secret to encode packet
     * @param address where to send the packet
     * @param request clientRequest packet
     * @return new datagram packet
     * @throws IOException
     */
    protected DatagramPacket makeDatagramPacket(RadiusPacket packet, String secret, InetSocketAddress address,
                                                RadiusPacket request) throws IOException, RadiusException {

        ByteBuf buf = Unpooled.buffer(MAX_PACKET_LENGTH, MAX_PACKET_LENGTH);

        ByteBufOutputStream bos = new ByteBufOutputStream(buf);
        packet.setDictionary(dictionary);
        packet.encodeResponsePacket(bos, secret, request);

        return new DatagramPacket(buf, address);
    }

    /**
     * Creates a RadiusPacket for a Radius clientRequest from a received
     * datagram packet.
     *
     * @param packet received datagram
     * @return RadiusPacket object
     * @throws RadiusException malformed packet
     * @throws IOException     communication error (after getRetryCount()
     *                         retries)
     */
    protected RadiusPacket makeRadiusPacket(DatagramPacket packet, String sharedSecret) throws IOException, RadiusException {
        ByteBufInputStream in = new ByteBufInputStream(packet.content());
        return RadiusPacket.decodeRequestPacket(dictionary, in, sharedSecret);
    }

    public interface PacketManager {

        /**
         * Checks whether the passed packet is a duplicate.
         * A packet is duplicate if another packet with the same identifier
         * has been sent from the same host in the last time.
         *
         * @param packet  packet in question
         * @param address client address
         * @return true if it is duplicate
         */
        boolean isPacketDuplicate(RadiusPacket packet, InetSocketAddress address);
    }

    private class RadiusChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            try {
                // check client
                InetSocketAddress localAddress = packet.recipient();
                InetSocketAddress remoteAddress = packet.sender();

                String secret = getSharedSecret(remoteAddress);
                if (secret == null) {
                    if (logger.isInfoEnabled())
                        logger.info("ignoring packet from unknown client " + remoteAddress + " received on local address " + localAddress);
                    return;
                }

                // parse packet
                RadiusPacket request = makeRadiusPacket(packet, secret);
                if (logger.isInfoEnabled())
                    logger.info("received packet from " + remoteAddress + " on local address " + localAddress + ": " + request);

                // handle packet
                logger.trace("about to call RadiusServer.handlePacket()");
                RadiusPacket response = handlePacket(localAddress, remoteAddress, request, secret);
                // send clientResponse
                if (response != null) {
                    response.setDictionary(dictionary);
                    if (logger.isInfoEnabled())
                        logger.info("send clientResponse: " + response);
                    DatagramPacket packetOut = makeDatagramPacket(response, secret, remoteAddress, request);
                    ctx.writeAndFlush(packetOut);
                } else {
                    logger.info("no clientResponse sent");
                }

            } catch (IOException ioe) {
                // error while reading/writing socket
                logger.error("communication error", ioe);
            } catch (RadiusException re) {
                // malformed packet
                logger.error("malformed Radius packet", re);
            }
        }
    }

}