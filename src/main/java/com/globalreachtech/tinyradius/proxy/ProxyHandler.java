package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.attribute.RadiusAttribute;
import com.globalreachtech.tinyradius.client.RadiusClient;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.server.BaseHandler;
import com.globalreachtech.tinyradius.server.RadiusServer;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import com.sun.xml.internal.ws.Closeable;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public abstract class ProxyHandler extends BaseHandler implements Closeable {

    private static Log logger = LogFactory.getLog(ProxyHandler.class);

    private final RadiusProxy.ConnectionManager connectionManager;
    private final RadiusClient<?> radiusClient;

    protected ProxyHandler(Dictionary dictionary,
                           RadiusServer.Deduplicator deduplicator,
                           RadiusProxy.ConnectionManager connectionManager,
                           RadiusClient<?> radiusClient,
                           Timer timer) {
        super(dictionary, deduplicator, timer);
        this.connectionManager = connectionManager;
        this.radiusClient = radiusClient;
    }

    /**
     * Stops the proxy listener and closes the socket.
     */
    @Override
    public void close() {
        logger.info("stopping Radius proxy listener");
        radiusClient.close();
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

    @Override
    protected Promise<RadiusPacket> handlePacket(Channel channel, InetSocketAddress remoteAddress, RadiusPacket request) {

        String sharedSecret = getSharedSecret(remoteAddress);
        // handle auth/acct packet
        RadiusEndpoint clientEndpoint = new RadiusEndpoint(remoteAddress, sharedSecret);
        RadiusEndpoint serverEndpoint = getProxyServer(request, clientEndpoint);

        if (serverEndpoint == null)
            logger.info("server not found for request, ignoring");

        RadiusProxyConnection proxyConnection = new RadiusProxyConnection(serverEndpoint, clientEndpoint, request, channel);

        logger.info("proxy packet to " + proxyConnection.getServerEndpoint().getEndpointAddress());
        return proxyPacket(request, proxyConnection);
    }

    /**
     * Sends an answer to a proxied packet back to the original host.
     * Retrieves the RadiusProxyConnection object from the cache employing
     * the Proxy-State attribute.
     *
     * @param packet packet to be sent back
     */
    protected void handleServerResponse(RadiusPacket packet) throws RadiusException, IOException {
        // retrieve my Proxy-State attribute (the last)
        List<RadiusAttribute> proxyStates = packet.getAttributes(33);
        if (proxyStates == null || proxyStates.size() == 0)
            throw new RadiusException("proxy packet without Proxy-State attribute");
        RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);

        // retrieve proxy connection from cache
        String state = new String(proxyState.getAttributeData());
        RadiusProxyConnection proxyConnection = connectionManager.removeProxyConnection(state);
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

        DatagramPacket datagram = makeDatagramPacket(answer, clientEndpoint.getSharedSecret(), clientEndpoint.getEndpointAddress(), proxyConnection.getRequestPacket());

        // send back using correct socket
        proxyConnection.getRequestChannel().writeAndFlush(datagram);
    }

    /**
     * Proxies the given packet to the server given in the proxy connection.
     * Stores the proxy connection object in the cache with a key that
     * is added to the packet in the "Proxy-State" attribute.
     *
     * @param packet          the packet to proxy
     * @param proxyConnection the RadiusProxyConnection for this packet
     */
    protected Promise<RadiusPacket> proxyPacket(RadiusPacket packet, RadiusProxyConnection proxyConnection) {
        // add Proxy-State attribute
        String proxyIndexStr = connectionManager.nextProxyIndex();
        packet.addAttribute(new RadiusAttribute(33, proxyIndexStr.getBytes()));

        // store RadiusProxyConnection object
        connectionManager.putProxyConnection(proxyIndexStr, proxyConnection);

        final RadiusEndpoint endpoint = proxyConnection.getServerEndpoint();

        // save clientRequest authenticator (will be calculated new)
        byte[] auth = packet.getAuthenticator();

        // send new packet (with new authenticator)
        radiusClient.communicate(packet, endpoint, 3)
                .addListener((Future<RadiusPacket> f) -> handleServerResponse(f.getNow()));

        // restore original authenticator
        packet.setAuthenticator(auth);

        return null;
        //todo
    }
}
