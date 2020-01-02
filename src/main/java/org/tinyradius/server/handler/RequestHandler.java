package org.tinyradius.server.handler;

import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;

public abstract class RequestHandler extends SimpleChannelInboundHandler<RequestCtx> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected abstract Class<? extends RadiusPacket> acceptedPacketType();

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        final boolean acceptInboundMessage = super.acceptInboundMessage(msg);
        if (!acceptInboundMessage)
            return false;

        final RadiusPacket request = ((RequestCtx) msg).getRequest();

        if (acceptedPacketType().isInstance(request)) {
            return true;
        } else {
            logger.debug("{} does not accept <{}>", getClass().getSimpleName(), request.getClass().getSimpleName());
            return false;
        }
    }

}
