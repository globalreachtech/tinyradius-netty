package org.tinyradius.client;

import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusEndpoint;

/**
 * Wrapper that holds a promise to be resolved when response is received.
 */
public class RequestCtxWrapper extends RequestCtx {

    private final Promise<RadiusPacket> response;

    public RequestCtxWrapper(RadiusPacket packet, RadiusEndpoint endpoint, Promise<RadiusPacket> response) {
        super(packet, endpoint);
        this.response = response;
    }

    public Promise<RadiusPacket> getResponse() {
        return response;
    }
}
