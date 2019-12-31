package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPackets;
import org.tinyradius.server.RequestCtx;

import static org.tinyradius.packet.PacketType.ACCOUNTING_RESPONSE;

/**
 * A reference implementation of AccountingRequest handler that responds to all Accounting-Request
 * with standard Accounting-Response.
 */
public class SimpleAccountingHandler extends RequestHandler {

    @Override
    protected Class<AccountingRequest> acceptedPacketType() {
        return AccountingRequest.class;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
        final RadiusPacket request = msg.getRequest();

        RadiusPacket answer = RadiusPackets.create(request.getDictionary(), ACCOUNTING_RESPONSE, request.getIdentifier());
        request.getAttributes(33).forEach(answer::addAttribute);

        ctx.writeAndFlush(msg.withResponse(answer));
    }
}
