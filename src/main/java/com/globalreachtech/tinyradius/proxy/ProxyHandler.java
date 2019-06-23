package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.client.RadiusClient;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.server.Deduplicator;
import com.globalreachtech.tinyradius.server.ServerHandler;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.net.InetSocketAddress;

public abstract class ProxyHandler extends ServerHandler implements Closeable {

    private static Log logger = LogFactory.getLog(ProxyHandler.class);

    private final RadiusClient<?> radiusClient;

    protected ProxyHandler(Dictionary dictionary, Deduplicator deduplicator, RadiusClient<?> radiusClient, Timer timer) {
        super(dictionary, deduplicator, timer);
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

    /**
     * Proxies the given packet to the server given in the proxy connection.
     * Stores the proxy connection object in the cache with a key that
     * is added to the packet in the "Proxy-State" attribute.
     */
    @Override
    protected Promise<RadiusPacket> handlePacket(Channel channel, InetSocketAddress remoteAddress, RadiusPacket packet) {
        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();

        String sharedSecret = getSharedSecret(remoteAddress);
        RadiusEndpoint clientEndpoint = new RadiusEndpoint(remoteAddress, sharedSecret);
        RadiusEndpoint serverEndpoint = getProxyServer(packet, clientEndpoint);

        if (serverEndpoint == null) {
            logger.info("server not found for client proxy request, ignoring");
            promise.tryFailure(new RadiusException("server not found for client proxy request"));
            return promise;
        }

        logger.info("proxy packet to " + serverEndpoint.getEndpointAddress());
        // save clientRequest authenticator (will be calculated new)
        byte[] auth = packet.getAuthenticator();

        // send new packet (with new authenticator)
        radiusClient.communicate(packet, serverEndpoint, 3)
                .addListener((Future<RadiusPacket> f) -> promise.trySuccess(
                        handleServerResponse(f.getNow())));

        // restore original authenticator
        packet.setAuthenticator(auth);
        return promise;
    }

    /**
     * Sends an answer to a proxied packet back to the original host.
     * Retrieves the RadiusProxyConnection object from the cache employing
     * the Proxy-State attribute.
     *
     * @param packet packet to be sent back
     */
    protected RadiusPacket handleServerResponse(RadiusPacket packet) {
        // retrieve clientEndpoint
        if (logger.isInfoEnabled())
            logger.info("received proxy packet: " + packet);

        // remove only own Proxy-State (last attribute)
        packet.removeLastAttribute(33);

        // re-encode answer packet with authenticator of the original packet
        return new RadiusPacket(packet.getPacketType(), packet.getPacketIdentifier(), packet.getAttributes());
    }
}
