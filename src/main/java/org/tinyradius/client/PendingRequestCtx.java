package org.tinyradius.client;

import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusEndpoint;

/**
 * Wrapper that holds a promise to be resolved when response is received.
 */
public class PendingRequestCtx extends RequestCtx {

    private final Promise<BaseRadiusPacket> response;

    public PendingRequestCtx(BaseRadiusPacket packet, RadiusEndpoint endpoint, Promise<BaseRadiusPacket> response) {
        super(packet, endpoint);
        this.response = response;
    }

    public Promise<BaseRadiusPacket> getResponse() {
        return response;
    }
}
