package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.attribute.RadiusAttribute;
import com.globalreachtech.tinyradius.client.RadiusClient;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.server.RadiusServer;
import com.globalreachtech.tinyradius.util.RadiusProxyConnection;
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
 * <p>
 * You have to provide a packet manager that manages the proxy connection
 * a packet belongs to.
 * <p>
 * Note: this implementation does not make use of RadiusClient which also manages retries
 */
public abstract class RadiusProxy<T extends DatagramChannel> extends RadiusServer<T> {

    private static Log logger = LogFactory.getLog(RadiusProxy.class);

    /**
     * Index for Proxy-State attribute.
     */
    private AtomicInteger proxyIndex = new AtomicInteger(1);

    private final ProxyPacketManager proxyPacketManager;
    private final RadiusClient<T> radiusClient;
    private final int proxyPort;
    private T proxySocket = null;

    private Future<Void> proxyStatus = null;

    public RadiusProxy(EventLoopGroup eventLoopGroup, ChannelFactory<T> factory, ProxyPacketManager proxyPacketManager, RadiusClient<T> radiusClient) {
        super(eventLoopGroup, factory);
        this.proxyPort = 1814;
        this.proxyPacketManager = proxyPacketManager;
        this.radiusClient = radiusClient;
    }

    public RadiusProxy(Dictionary dictionary, EventLoopGroup eventLoopGroup, ChannelFactory<T> factory, ProxyPacketManager proxyPacketManager, RadiusClient<T> radiusClient) {
        super(dictionary, eventLoopGroup, factory);
        this.proxyPort = 1814;
        this.proxyPacketManager = proxyPacketManager;
        this.radiusClient = radiusClient;
    }

    public RadiusProxy(Dictionary dictionary, EventLoopGroup loopGroup, ChannelFactory<T> factory, ProxyPacketManager proxyPacketManager, RadiusClient<T> radiusClient, int authPort, int acctPort, int proxyPort) {
        super(dictionary, loopGroup, factory, proxyPacketManager, authPort, acctPort);
        this.radiusClient = radiusClient;
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

        final Promise<Void> status = eventLoopGroup.next().newPromise();

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
        // todo listen here or use radiusClient to handle server responses?

        // handle incoming proxy packet
        if (localAddress.getPort() == proxyPort) {
            handleUpstreamResponse(request);
            return null;
        }

        // handle auth/acct packet
        RadiusEndpoint clientEndpoint = new RadiusEndpoint(remoteAddress, sharedSecret);
        RadiusEndpoint serverEndpoint = getProxyServer(request, clientEndpoint);

        if (serverEndpoint == null)
            return super.handlePacket(localAddress, remoteAddress, request, sharedSecret);

        RadiusProxyConnection proxyConnection = new RadiusProxyConnection(serverEndpoint, clientEndpoint, request, localAddress.getPort());
        logger.info("proxy packet to " + proxyConnection.getServerEndpoint().getEndpointAddress());
        proxyPacket(request, proxyConnection);
        return null;
    }

    /**
     * Sends an answer to a proxied packet back to the original host.
     * Retrieves the RadiusProxyConnection object from the cache employing
     * the Proxy-State attribute.
     *
     * @param packet packet to be sent back
     */
    protected void handleUpstreamResponse(RadiusPacket packet) throws IOException, RadiusException {
        // retrieve my Proxy-State attribute (the last)
        List<RadiusAttribute> proxyStates = packet.getAttributes(33);
        if (proxyStates == null || proxyStates.size() == 0)
            throw new RadiusException("proxy packet without Proxy-State attribute");
        RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);

        // retrieve proxy connection from cache
        String state = new String(proxyState.getAttributeData());
        RadiusProxyConnection proxyConnection = proxyPacketManager.removeProxyConnection(state);
        if (proxyConnection == null) {
            logger.warn("received packet on proxy port without saved proxy connection - duplicate?");
            return;
        }

        // retrieve clientEndpoint
        RadiusEndpoint clientEndpoint = proxyConnection.getClientEndpoint();
        if (logger.isInfoEnabled()) {
            logger.info("received proxy packet: " + packet);
            logger.info("forward packet to " + clientEndpoint.getEndpointAddress().toString() + " with secret " + clientEndpoint.getSharedSecret());
        }

        // remove only own Proxy-State (last attribute)
        packet.removeLastAttribute(33);

        // re-encode answer packet with authenticator of the original packet
        RadiusPacket answer = new RadiusPacket(packet.getPacketType(), packet.getPacketIdentifier(), packet.getAttributes());

        DatagramPacket datagram = makeDatagramPacket(answer, clientEndpoint.getSharedSecret(), clientEndpoint.getEndpointAddress(), proxyConnection.getPacket());

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
        proxyPacketManager.putProxyConnection(proxyIndexStr, proxyConnection);

        final RadiusEndpoint serverEndpoint = proxyConnection.getServerEndpoint();

        // save clientRequest authenticator (will be calculated new)
        byte[] auth = packet.getAuthenticator();

        // encode new packet (with new authenticator)
        ByteBuf buf = buffer(MAX_PACKET_LENGTH, MAX_PACKET_LENGTH);
        packet.encodeRequestPacket(new ByteBufOutputStream(buf), serverEndpoint.getSharedSecret());

        // todo send with radiusClient
//        Future<RadiusPacket> communicate = radiusClient.communicate(packet, serverEndpoint, 3);

        DatagramPacket datagram = new DatagramPacket(buf, serverEndpoint.getEndpointAddress());

        // restore original authenticator
        packet.setAuthenticator(auth);

        getProxySocket().writeAndFlush(datagram);
    }

}
