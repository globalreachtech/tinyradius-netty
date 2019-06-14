package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.attribute.RadiusAttribute;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.proxy.RadiusProxyConnection;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.MAX_PACKET_LENGTH;
import static io.netty.buffer.Unpooled.buffer;

/**
 * This class implements a Radius proxy that receives Radius packets
 * and forwards them to a Radius server.
 * You have to override the method getRadiusProxyConnection() which
 * identifies the Radius proxy connection a Radius packet belongs to.
 */
public abstract class RadiusProxy<T extends DatagramChannel> extends RadiusServer<T> {

    private static Log logger = LogFactory.getLog(RadiusProxy.class);

    /**
     * Index for Proxy-State attribute.
     */
    private AtomicInteger proxyIndex = new AtomicInteger(1);

    private final IProxyPacketManager proxyPacketManager;
    private final int proxyPort;
    private T proxySocket = null;

    private Future<Void> proxyStatus = null;

    public RadiusProxy(EventLoopGroup eventGroup, EventExecutorGroup eventExecutorGroup, ChannelFactory<T> factory, IProxyPacketManager proxyPacketManager) {
        super(eventGroup, eventExecutorGroup, factory);
        this.proxyPort = 1814;
        this.proxyPacketManager = proxyPacketManager;

    }

    public RadiusProxy(Dictionary dictionary, EventLoopGroup eventGroup, EventExecutorGroup eventExecutorGroup, ChannelFactory<T> factory, IProxyPacketManager proxyPacketManager) {
        super(dictionary, eventGroup, eventExecutorGroup, factory);
        this.proxyPort = 1814;
        this.proxyPacketManager = proxyPacketManager;

    }

    public RadiusProxy(Dictionary dictionary, EventLoopGroup loopGroup, EventExecutorGroup eventExecutorGroup, ChannelFactory<T> factory, IProxyPacketManager proxyPacketManager, int authPort, int acctPort, int proxyPort) {
        super(dictionary, loopGroup, eventExecutorGroup, factory, proxyPacketManager, authPort, acctPort);
        this.proxyPort = validPort(proxyPort);
        this.proxyPacketManager = proxyPacketManager;
    }

    /**
     * Starts the Radius proxy. Listens on the proxy port.
     */
    @Override
    public Future<Void> start() {
        if (this.proxyStatus != null)
            return this.proxyStatus;

        final Promise<Void> status = new DefaultPromise<>(eventLoopGroup.next());

        final PromiseCombiner promiseCombiner = new PromiseCombiner(eventLoopGroup.next());
        promiseCombiner.addAll(super.start(), listenProxy());
        promiseCombiner.finish(status);

        this.proxyStatus = status;
        return status;
    }

    /**
     * Stops the proxy and closes the socket.
     */
    @Override
    public void stop() {
        logger.info("stopping Radius proxy");
        if (proxySocket != null)
            proxySocket.close();
        super.stop();
    }

    protected ChannelFuture listenProxy() {
        logger.info("starting RadiusProxyListener on port " + proxyPort);
        return listen(getProxySocket(), new InetSocketAddress(getListenAddress(), proxyPort));
    }

    /**
     * This method must be implemented to return a RadiusEndpoint
     * if the given packet is to be proxied. The endpoint represents the
     * Radius server the packet should be proxied to.
     *
     * @param packet the packet in question
     * @param client the client endpoint the packet originated from
     *               (containing the address, port number and shared secret)
     * @return a RadiusEndpoint or null if the packet should not be
     * proxied
     */
    public abstract RadiusEndpoint getProxyServer(RadiusPacket packet, RadiusEndpoint client);

    /**
     * Returns a socket bound to the proxy port.
     *
     * @return socket
     */
    protected T getProxySocket() {
        if (proxySocket == null) {
            proxySocket = factory.newChannel();
        }
        return proxySocket;
    }

    /**
     * Handles packets coming in on the proxy port. Decides whether
     * packets coming in on Auth/Acct ports should be proxied.
     */
    @Override
    protected RadiusPacket handlePacket(InetSocketAddress localAddress, InetSocketAddress remoteAddress, RadiusPacket request, String sharedSecret)
            throws RadiusException, IOException {
        // handle incoming proxy packet
        if (localAddress.getPort() == proxyPort) {
            upstreamResponseReceived(request);
            return null;
        }

        // handle auth/acct packet
        RadiusEndpoint radiusClient = new RadiusEndpoint(remoteAddress, sharedSecret);
        RadiusEndpoint radiusServer = getProxyServer(request, radiusClient);

        if (radiusServer == null)
            return super.handlePacket(localAddress, remoteAddress, request, sharedSecret);

        RadiusProxyConnection proxyConnection = new RadiusProxyConnection(radiusServer, radiusClient, request, localAddress.getPort());
        logger.info("proxy packet to " + proxyConnection.getRadiusServer().getEndpointAddress());
        proxyPacket(request, proxyConnection);
        return null;
    }

    /**
     * Sends an answer to a proxied packet back to the original host.
     * Retrieves the RadiusProxyConnection object from the cache employing
     * the Proxy-State attribute.
     *
     * @param packet packet to be sent back
     * @throws IOException
     */
    protected void upstreamResponseReceived(RadiusPacket packet) throws IOException, RadiusException {
        // retrieve my Proxy-State attribute (the last)
        List<RadiusAttribute> proxyStates = packet.getAttributes(33);
        if (proxyStates == null || proxyStates.size() == 0)
            throw new RadiusException("proxy packet without Proxy-State attribute");
        RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);

        // retrieve proxy connection from cache
        String state = new String(proxyState.getAttributeData());
        RadiusProxyConnection proxyConnection = proxyPacketManager.remove(state);
        if (proxyConnection == null) {
            logger.warn("received packet on proxy port without saved proxy connection - duplicate?");
            return;
        }

        // retrieve client
        RadiusEndpoint client = proxyConnection.getRadiusClient();
        if (logger.isInfoEnabled()) {
            logger.info("received proxy packet: " + packet);
            logger.info("forward packet to " + client.getEndpointAddress().toString() + " with secret " + client.getSharedSecret());
        }

        // remove only own Proxy-State (last attribute)
        packet.removeLastAttribute(33);

        // re-encode answer packet with authenticator of the original packet
        RadiusPacket answer = new RadiusPacket(packet.getPacketType(), packet.getPacketIdentifier(), packet.getAttributes());

        DatagramPacket datagram = makeDatagramPacket(answer, client.getSharedSecret(), client.getEndpointAddress(), proxyConnection.getPacket());

        // send back using correct socket
        T socket = proxyConnection.getPort() == authPort ? getAuthSocket() : getAcctSocket();
        socket.writeAndFlush(datagram);
    }

    /**
     * Proxies the given packet to the server given in the proxy connection.
     * Stores the proxy connection object in the cache with a key that
     * is added to the packet in the "Proxy-State" attribute.
     *
     * @param packet          the packet to proxy
     * @param proxyConnection the RadiusProxyConnection for this packet
     * @throws IOException
     */
    protected void proxyPacket(RadiusPacket packet, RadiusProxyConnection proxyConnection) throws IOException, RadiusException {
        // add Proxy-State attribute
        String proxyIndexStr = Integer.toString(proxyIndex.getAndIncrement());
        packet.addAttribute(new RadiusAttribute(33, proxyIndexStr.getBytes()));

        // store RadiusProxyConnection object
        proxyPacketManager.put(proxyIndexStr, proxyConnection);

        final RadiusEndpoint radiusServer = proxyConnection.getRadiusServer();

        // save clientRequest authenticator (will be calculated new)
        byte[] auth = packet.getAuthenticator();

        // encode new packet (with new authenticator)
        ByteBuf buf = buffer(MAX_PACKET_LENGTH, MAX_PACKET_LENGTH);
        ByteBufOutputStream bos = new ByteBufOutputStream(buf);
        packet.encodeRequestPacket(bos, radiusServer.getSharedSecret());

        DatagramPacket datagram = new DatagramPacket(buf, radiusServer.getEndpointAddress());

        // restore original authenticator
        packet.setAuthenticator(auth);

        getProxySocket().writeAndFlush(datagram);
    }

    public interface IProxyPacketManager extends RadiusServer.PacketManager {
        RadiusProxyConnection put(String proxyIndex, RadiusProxyConnection proxyConnection);

        RadiusProxyConnection remove(String proxyIndex);
    }
}
