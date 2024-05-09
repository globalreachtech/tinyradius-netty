package org.tinyradius.io.server.handler;

import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.log4j.Log4j2;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.io.server.RequestCtx;

@Log4j2
public abstract class RequestHandler extends SimpleChannelInboundHandler<RequestCtx> {

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
            log.debug("Ignoring {} received - handler only accepts {} packets",
                    request.getClass().getSimpleName(), acceptedPacketType().getSimpleName());
            return false;
        }
    }

}
