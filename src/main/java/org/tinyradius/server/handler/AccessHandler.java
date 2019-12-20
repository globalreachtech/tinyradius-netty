package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.PacketType;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPackets;
import org.tinyradius.server.RequestCtx;

import static org.tinyradius.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.packet.PacketType.ACCESS_REJECT;

/**
 * Reference implementation of AccessRequest handler that returns Access-Accept/Reject
 * depending on whether {@link #getUserPassword(String)} matches password in Access-Request.
 */
public abstract class AccessHandler extends SimpleChannelInboundHandler<RequestCtx> {

    private static final Logger logger = LoggerFactory.getLogger(AccessHandler.class);

    /**
     * Returns the password of the passed user.
     *
     * @param userName user name
     * @return plain-text password or null if user unknown
     */
    public abstract String getUserPassword(String userName);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {

        if (!(msg.getRequest() instanceof AccessRequest)) {
            logger.warn("{} does not accept {}", getClass().getSimpleName(), msg.getRequest().getClass().getSimpleName());
            return;
        }

        final AccessRequest request = (AccessRequest) msg.getRequest();

        logger.info("Received Access-Request:\n" + request);

        String password = getUserPassword(request.getUserName());
        int type = password != null && request.verifyPassword(password) ?
                ACCESS_ACCEPT : ACCESS_REJECT;

        RadiusPacket answer = RadiusPackets.create(request.getDictionary(), type, request.getIdentifier());
        request.getAttributes(33)
                .forEach(answer::addAttribute);

        if (type == PacketType.ACCESS_ACCEPT)
            answer.addAttribute("Reply-Message", "Welcome " + request.getUserName() + "!");
        logger.info("Answer:\n" + answer);

        ctx.writeAndFlush(msg.withResponse(answer));
    }
}
