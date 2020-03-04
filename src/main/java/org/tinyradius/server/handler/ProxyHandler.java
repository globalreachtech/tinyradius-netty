package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.client.handler.PromiseAdapter;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.util.RadiusPackets;
import org.tinyradius.packet.RadiusRequest;
import org.tinyradius.packet.RadiusResponse;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusEndpoint;

import java.util.Optional;


/**
 * RadiusServer handler that proxies packets to destination.
 * <p>
 * RadiusClient port should be set to proxy port, which will be used to communicate
 * with upstream servers. RadiusClient should also use a variant of {@link PromiseAdapter}
 * which matches requests/responses by adding a custom Proxy-State attribute.
 */
public abstract class ProxyHandler extends SimpleChannelInboundHandler<RequestCtx> {

    private static final Logger logger = LogManager.getLogger();

    private final RadiusClient radiusClient;

    public ProxyHandler(RadiusClient radiusClient) {
        this.radiusClient = radiusClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
        final RadiusRequest request = msg.getRequest();

        RadiusEndpoint clientEndpoint = msg.getEndpoint();
        Optional<RadiusEndpoint> serverEndpoint = getProxyServer(request, clientEndpoint);

        if (!serverEndpoint.isPresent()) {
            logger.info("Server not found for client proxy request, ignoring");
            return;
        }

        logger.debug("Proxying packet to {}", serverEndpoint.get().getAddress());

        radiusClient.communicate(request, serverEndpoint.get()).addListener(f -> {
            final RadiusResponse packet = (RadiusResponse) f.getNow();
            if (f.isSuccess() && packet != null) {
                final RadiusResponse response = RadiusPackets.createResponse(
                        request.getDictionary(), packet.getType(), packet.getIdentifier(), packet.getAuthenticator(), packet.getAttributes());
                ctx.writeAndFlush(msg.withResponse(response));
            }
        });
    }

    /**
     * This method must be implemented to return a RadiusEndpoint
     * if the given packet is to be proxied. The endpoint represents the
     * Radius server the packet should be proxied to.
     *
     * @param packet the packet in question
     * @param client the client endpoint the packet originated from
     *               (containing the address, port number and shared secret)
     * @return a Optional RadiusEndpoint to be proxied
     */
    protected abstract Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client);
}
