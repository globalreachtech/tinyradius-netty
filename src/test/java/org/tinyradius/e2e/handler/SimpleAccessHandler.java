package org.tinyradius.e2e.handler;

import io.netty.channel.ChannelHandlerContext;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.handler.RequestHandler;

import java.util.Map;

import static org.tinyradius.core.attribute.AttributeTypes.PROXY_STATE;
import static org.tinyradius.core.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.core.packet.PacketType.ACCESS_REJECT;

/**
 * Simple access handler for testing purposes.
 */
public class SimpleAccessHandler extends RequestHandler {

    private final Map<String, String> credentials;

    /**
     * Constructs a {@code SimpleAccessHandler} with the given credentials.
     *
     * @param credentials the map of username to password for authentication
     */
    public SimpleAccessHandler(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    @Override
    protected @NonNull Class<? extends RadiusRequest> acceptedPacketType() {
        return AccessRequest.class;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) throws RadiusPacketException {
        AccessRequestPap request = (AccessRequestPap) msg.getRequest();

        String user = request.getUsername().orElse(null);
        String pass = request.getPassword().orElse(null);
        byte type = user != null && pass != null &&
                credentials.get(user).equals(pass) ? ACCESS_ACCEPT : ACCESS_REJECT;

        RadiusResponse answer = RadiusResponse.create(request.getDictionary(), type, request.getId(), null, request.getAttributes(PROXY_STATE));

        ctx.writeAndFlush(msg.withResponse(answer));
    }
}
