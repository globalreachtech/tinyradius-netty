package org.tinyradius.client;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.client.handler.ClientHandler;
import org.tinyradius.client.retry.RetryStrategy;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPacketEncoder;
import org.tinyradius.util.Lifecycle;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static java.lang.Byte.toUnsignedInt;
import static java.util.Objects.requireNonNull;

/**
 * This object represents a simple Radius client which communicates with
 * a specified Radius server. You can use a single instance of this object
 * to authenticate or account different users with the same Radius server
 * as long as you authenticate/account one user after the other.
 * <p>
 * This object is thread safe, but requires a packet manager to avoid confusion with the mapping of request
 * and result packets.
 */
public class RadiusClient implements Lifecycle {

    private static final Logger logger = LoggerFactory.getLogger(RadiusClient.class);

    private final ChannelFactory<? extends DatagramChannel> factory;
    private final EventLoopGroup eventLoopGroup;
    private final ClientHandler clientHandler;
    private final RetryStrategy retryStrategy;
    private final InetAddress listenAddress;
    private final int port;

    private DatagramChannel channel = null;

    private ChannelFuture channelFuture;

    /**
     * @param eventLoopGroup for both channel IO and processing
     * @param factory        to create new Channel
     * @param clientHandler  to log outgoing packets and handle incoming packets/responses
     * @param listenAddress  local address to bind to, will be wildcard address if null
     * @param port           port to bind to, or set to 0 to let system choose
     */
    public RadiusClient(EventLoopGroup eventLoopGroup,
                        ChannelFactory<? extends DatagramChannel> factory,
                        ClientHandler clientHandler,
                        RetryStrategy retryStrategy,
                        InetAddress listenAddress,
                        int port) {
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.eventLoopGroup = requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.clientHandler = clientHandler;
        this.retryStrategy = retryStrategy;
        this.listenAddress = listenAddress;
        this.port = port;
    }

    /**
     * Registers the channel and binds to address.
     * <p>
     * Also run implicitly if {@link #communicate(RadiusPacket, RadiusEndpoint)} is called.
     *
     * @return channelFuture of started channel socket
     */
    @Override
    public Future<Void> start() {
        if (this.channelFuture != null)
            return this.channelFuture;

        if (channel == null)
            channel = factory.newChannel();

        return channelFuture = listen(channel, new InetSocketAddress(listenAddress, port))
                .addListener(f -> logger.info("RadiusClient started"));
    }

    /**
     * Closes channel socket
     */
    public Future<Void> stop() {
        if (channel != null)
            return channel.close();
        return eventLoopGroup.next().newSucceededFuture(null);
    }

    /**
     * @param channel       to listen on
     * @param listenAddress the address to bind to
     * @return channel that resolves after it is bound to address and registered with eventLoopGroup
     */
    private ChannelFuture listen(final DatagramChannel channel, final InetSocketAddress listenAddress) {
        channel.pipeline().addLast(clientHandler);

        final ChannelPromise promise = channel.newPromise();

        eventLoopGroup.register(channel)
                .addListener(f -> channel.bind(listenAddress)
                        .addListener(g -> promise.trySuccess()));

        // todo error handling
        return promise;
    }

    public Future<RadiusPacket> communicate(RadiusPacket originalPacket, RadiusEndpoint endpoint) {
        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        final RadiusPacket request = clientHandler.prepareRequest(originalPacket, endpoint, promise);

        try {
            final DatagramPacket datagram = RadiusPacketEncoder.toDatagram(request, endpoint.getAddress());

            start().addListener(s -> {
                if (!s.isSuccess()) {
                    promise.tryFailure(s.cause());
                    return;
                }

                logger.info("Preparing send: {}", request);
                send(datagram, 1, promise);

                promise.addListener(f -> {
                    if (f.isSuccess())
                        logger.info("Response received, packet: {}", f.getNow());
                    else
                        logger.error("{}", f.cause().getMessage());

                    datagram.release();
                    if (datagram.refCnt() != 0)
                        logger.error("buffer leak? datagram refCnt should be 0, actual: {}", datagram.refCnt());
                });
            });

        } catch (RadiusException e) {
            promise.tryFailure(e);
        }

        return promise;
    }

    private void send(DatagramPacket datagram, int attempt, Promise<RadiusPacket> requestPromise) {
        logger.info("Attempt {}, sending packet {} to {}", attempt, toUnsignedInt(datagram.content().getByte(1)), datagram.recipient());
        if (logger.isDebugEnabled())
            logger.debug("\n" + ByteBufUtil.prettyHexDump(datagram.content()));

        // inc refCnt so msg isn't released after sending and can reuse for retries
        channel.writeAndFlush(datagram.retain());

        retryStrategy.scheduleRetry(() -> send(datagram, attempt + 1, requestPromise), attempt, requestPromise);
    }
}