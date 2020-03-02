package org.tinyradius.client;

import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.RadiusRequest;
import org.tinyradius.packet.RadiusResponse;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusEndpoint;

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
