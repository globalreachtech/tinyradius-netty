package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.client.handler.ProxyStateClientHandler;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPackets;
import org.tinyradius.util.RadiusEndpoint;


/**
 * RadiusServer handler that proxies packets to destination.
 * <p>
 * Depends on RadiusClient to proxy packets.
 * <p>
 * RadiusClient port should be set to proxy port, which will be used to communicate
 * with upstream servers. RadiusClient should also use a variant of {@link ProxyStateClientHandler}
 * which matches requests/responses by adding a custom Proxy-State attribute.
 * <p>
 * This implementation expects {@link #getProxyServer(RadiusPacket, RadiusEndpoint)} to lookup
 * endpoint to forward requests to.
 */
public abstract class ProxyHandler extends SimpleChannelInboundHandler<RequestContext> {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    private final RadiusClient radiusClient;

    public ProxyHandler(RadiusClient radiusClient) {
        this.radiusClient = radiusClient;
    }

    /**
     * Proxies the given packet to the server given in the proxy connection.
     * Stores the proxy connection object in the cache with a key that
     * is added to the packet in the "Proxy-State" attribute.
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestContext msg) throws Exception {

        final RadiusPacket request = msg.getRequest();

        RadiusEndpoint clientEndpoint = new RadiusEndpoint(msg.getRemoteAddress(), msg.getSecret());
        RadiusEndpoint serverEndpoint = getProxyServer(request, clientEndpoint);

        if (serverEndpoint == null) {
            logger.info("Server not found for client proxy request, ignoring");
            return;
        }

        logger.info("Proxy packet to " + serverEndpoint.getAddress());

        radiusClient.communicate(request, serverEndpoint)
                .addListener((Future<RadiusPacket> f) -> {
                    if (f.isSuccess() && f.getNow() != null)
                        ctx.writeAndFlush(msg.withResponse(handleServerResponse(request.getDictionary(), f.getNow())));
                });
    }

    /**
     * Sends an answer to a proxied packet back to the original host.
     * Retrieves the RadiusProxyConnection object from the cache employing
     * the Proxy-State attribute.
     *
     * @param dictionary dictionary for new packet to use
     * @param packet     response received from server
     * @return packet to send back to client
     */
    protected RadiusPacket handleServerResponse(Dictionary dictionary, RadiusPacket packet) {
        return RadiusPackets.create(dictionary, packet.getType(), packet.getIdentifier(), packet.getAttributes());
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
}
