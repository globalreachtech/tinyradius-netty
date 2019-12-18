package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPackets;

import static org.tinyradius.packet.PacketType.ACCOUNTING_RESPONSE;

/**
 * A reference implementation of AccountingRequest handler that responds to all Accounting-Request
 * with standard Accounting-Response.
 */
public class AccountingHandler extends SimpleChannelInboundHandler<RequestContext> {

    private static final Logger logger = LoggerFactory.getLogger(AccountingHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestContext msg) {
        final RadiusPacket request = msg.getRequest();

        if (!(request instanceof AccountingRequest)) {
            logger.warn("{} does not accept {}", getClass().getSimpleName(), request.getClass().getSimpleName());
            return;
        }

        RadiusPacket answer = RadiusPackets.create(request.getDictionary(), ACCOUNTING_RESPONSE, request.getIdentifier());
        request.getAttributes(33)
                .forEach(answer::addAttribute);

        ctx.writeAndFlush(msg.withResponse(answer));
    }
}
