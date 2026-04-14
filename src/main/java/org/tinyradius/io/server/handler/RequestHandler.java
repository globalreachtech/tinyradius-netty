package org.tinyradius.io.server.handler;

import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.io.server.RequestCtx;

/**
 * Base class for RADIUS server request handlers.
 * <p>
 * This class handles {@link RequestCtx} messages and filters them based on the
 * accepted packet type defined by {@link #acceptedPacketType()}.
 */
public abstract class RequestHandler extends SimpleChannelInboundHandler<RequestCtx> {

    private static final Logger log = LogManager.getLogger(RequestHandler.class);

    /**
     * @return RadiusRequest subclass type that this handler can accept
     */
    @NonNull
    protected abstract Class<? extends RadiusRequest> acceptedPacketType();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        var acceptInboundMessage = super.acceptInboundMessage(msg);
        if (!acceptInboundMessage)
            return false;

        var request = ((RequestCtx) msg).getRequest();

        if (acceptedPacketType().isInstance(request)) {
            return true;
        } else {
            log.debug("Ignoring {} received - handler only accepts {} packets",
                    request.getClass().getSimpleName(), acceptedPacketType().getSimpleName());
            return false;
        }
    }

}
