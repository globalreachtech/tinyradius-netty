package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.client.RadiusClient;
import org.tinyradius.io.client.handler.PromiseAdapter;
import org.tinyradius.io.server.RequestCtx;

import java.util.Optional;


/**
 * RadiusServer handler that proxies packets to destination.
 * <p>
 * RadiusClient port should be set to proxy port, which will be used to communicate
 * with upstream servers. RadiusClient should also use a variant of {@link PromiseAdapter}
 * which matches requests/responses by adding a custom Proxy-State attribute.
 */
public abstract class ProxyHandler extends SimpleChannelInboundHandler<RequestCtx> {

    private static final Logger log = LogManager.getLogger(ProxyHandler.class);
    private final RadiusClient radiusClient;

    /**
     * Constructs a {@code ProxyHandler} with the specified {@link RadiusClient}.
     *
     * @param radiusClient the RadiusClient to use for proxying requests
     */
    protected ProxyHandler(RadiusClient radiusClient) {
        this.radiusClient = radiusClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
        var request = msg.getRequest();

        var clientEndpoint = msg.getEndpoint();
        var serverEndpoint = getOriginServer(request, clientEndpoint);

        if (serverEndpoint.isEmpty()) {
            log.info("Server not found for client proxy request, ignoring");
            return;
        }

        log.debug("Proxying packet to {}", serverEndpoint.get().address());

        radiusClient.communicate(request, serverEndpoint.get()).addListener(f -> {
            var packet = (RadiusResponse) f.getNow();
            if (f.isSuccess() && packet != null) {
                var response = RadiusResponse.create(
                        request.getDictionary(), packet.getType(), packet.getId(), packet.getAuthenticator(), packet.getAttributes());
                ctx.writeAndFlush(msg.withResponse(response));
            }
        });
    }

    /**
     * @param request        the request in question
     * @param clientEndpoint the client endpoint the request originated from
     *                       (containing the address, port number and shared secret)
     * @return RadiusEndpoint to proxy request to
     */
    @NonNull
    protected abstract Optional<RadiusEndpoint> getOriginServer(@NonNull RadiusRequest request, @NonNull RadiusEndpoint clientEndpoint);
}
