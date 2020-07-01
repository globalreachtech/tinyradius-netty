package org.tinyradius.server.handler;

import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.server.RequestCtx;

public abstract class RequestHandler extends SimpleChannelInboundHandler<RequestCtx> {

    private final Logger logger = LogManager.getLogger();

    /**
     * @param request incoming RadiusRequest
     * @return true if RadiusRequest type is accepted by this handler
     */
    protected abstract boolean acceptRequestType(RadiusRequest request);

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        final boolean acceptInboundMessage = super.acceptInboundMessage(msg);
        if (!acceptInboundMessage)
            return false;

        final RadiusRequest request = ((RequestCtx) msg).getRequest();

        if (acceptRequestType(request)) {
            return true;
        } else {
            logger.debug("{} does not accept <{}>", getClass().getSimpleName(), request.getClass().getSimpleName());
            return false;
        }
    }

}
