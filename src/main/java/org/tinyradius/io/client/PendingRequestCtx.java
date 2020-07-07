package org.tinyradius.io.client;

import io.netty.util.concurrent.Promise;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.RadiusEndpoint;

/**
 * Wrapper that holds a promise to be resolved when response is received.
 */
public class PendingRequestCtx extends RequestCtx {

    private final Promise<RadiusResponse> response;

    public PendingRequestCtx(RadiusRequest packet, RadiusEndpoint endpoint, Promise<RadiusResponse> response) {
        super(packet, endpoint);
        this.response = response;
    }

    public Promise<RadiusResponse> getResponse() {
        return response;
    }
}
