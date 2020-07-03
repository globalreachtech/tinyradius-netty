package org.tinyradius.server.handler;

import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.server.RequestCtx;

public abstract class RequestHandler extends SimpleChannelInboundHandler<RequestCtx> {

    private final Logger logger = LogManager.getLogger();

    /**
     * @return RadiusRequest subclass type that this handler can accept
     */
    protected abstract Class<? extends RadiusRequest> acceptedPacketType();

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        final boolean acceptInboundMessage = super.acceptInboundMessage(msg);
        if (!acceptInboundMessage)
            return false;

        final RadiusRequest request = ((RequestCtx) msg).getRequest();

        if (acceptedPacketType().isInstance(request)) {
            return true;
        } else {
            logger.debug("Ignoring {} received - handler only accepts {} packets",
                    request.getClass().getSimpleName(), acceptedPacketType().getSimpleName());
            return false;
        }
    }

}
