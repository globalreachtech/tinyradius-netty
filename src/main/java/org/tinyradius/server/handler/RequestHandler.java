package org.tinyradius.server.handler;

import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.server.RequestCtx;

public abstract class RequestHandler extends SimpleChannelInboundHandler<RequestCtx> {

    private final Logger logger = LogManager.getLogger();

    protected abstract Class<? extends BaseRadiusPacket> acceptedPacketType();

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        final boolean acceptInboundMessage = super.acceptInboundMessage(msg);
        if (!acceptInboundMessage)
            return false;

        final BaseRadiusPacket request = ((RequestCtx) msg).getRequest();

        if (acceptedPacketType().isInstance(request)) {
            return true;
        } else {
            logger.debug("{} does not accept <{}>", getClass().getSimpleName(), request.getClass().getSimpleName());
            return false;
        }
    }

}
