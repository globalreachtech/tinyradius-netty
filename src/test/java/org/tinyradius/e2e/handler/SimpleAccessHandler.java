package org.tinyradius.e2e.handler;

import io.netty.channel.ChannelHandlerContext;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.handler.RequestHandler;

import java.util.Map;

import static org.tinyradius.core.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.core.packet.PacketType.ACCESS_REJECT;
import static org.tinyradius.io.client.handler.PromiseAdapter.PROXY_STATE;

public class SimpleAccessHandler extends RequestHandler {

    private final Map<String, String> credentials;

    public SimpleAccessHandler(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    @Override
    protected Class<? extends RadiusRequest> acceptedPacketType() {
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
