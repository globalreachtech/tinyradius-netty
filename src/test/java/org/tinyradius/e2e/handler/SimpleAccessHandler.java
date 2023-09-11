package org.tinyradius.e2e.handler;

import io.netty.channel.ChannelHandlerContext;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.handler.RequestHandler;

import static org.tinyradius.core.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.core.packet.PacketType.ACCESS_REJECT;
import static org.tinyradius.io.client.handler.PromiseAdapter.PROXY_STATE;

public class SimpleAccessHandler extends RequestHandler {

    private static final byte USER_NAME = 1;

    @Override
    protected Class<? extends RadiusRequest> acceptedPacketType() {
        return AccessRequest.class;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) throws RadiusPacketException {

        final AccessRequestPap request = (AccessRequestPap) msg.getRequest();

        final String password = request.getAttribute(USER_NAME).get().getValueString().equals("myUser") ? "myPassword" : null;
        final byte type = request.getPassword()
                .filter(p -> p.equals(password))
                .map(x -> ACCESS_ACCEPT)
                .orElse(ACCESS_REJECT);

        RadiusResponse answer = RadiusResponse.create(request.getDictionary(), type, request.getId(), null, request.filterAttributes(PROXY_STATE));

        ctx.writeAndFlush(msg.withResponse(answer));
    }
}
