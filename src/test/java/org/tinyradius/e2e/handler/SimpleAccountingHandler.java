package org.tinyradius.e2e.handler;

import io.netty.channel.ChannelHandlerContext;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.packet.request.AccountingRequest;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.handler.RequestHandler;

import static org.tinyradius.core.attribute.rfc.Rfc2865.PROXY_STATE;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_RESPONSE;

public class SimpleAccountingHandler extends RequestHandler {

    @Override
    protected Class<? extends RadiusRequest> acceptedPacketType() {
        return AccountingRequest.class;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) throws RadiusPacketException {
        final RadiusRequest request = msg.getRequest();
        final RadiusResponse answer = RadiusResponse.create(
                request.getDictionary(), ACCOUNTING_RESPONSE, request.getId(), null, request.getAttributes(PROXY_STATE));

        ctx.writeAndFlush(msg.withResponse(answer));
    }
}
