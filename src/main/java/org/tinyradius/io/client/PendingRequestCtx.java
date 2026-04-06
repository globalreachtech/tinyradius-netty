package org.tinyradius.io.client;

import io.netty.util.concurrent.Promise;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.server.RequestCtx;

/**
 * Wrapper that holds a promise to be resolved when response is received.
 */
@Getter
public class PendingRequestCtx extends RequestCtx {

    private final @NonNull Promise<RadiusResponse> response;

    public PendingRequestCtx(@NonNull RadiusRequest packet, @NonNull RadiusEndpoint endpoint, @NonNull Promise<RadiusResponse> response) {
        super(packet, endpoint);
        this.response = response;
    }

    @Override
    public String toString() {
        return "PendingRequestCtx{" +
                "packet=" + getRequest() +
                ", endpoint=" + getEndpoint() +
                "}";
    }
}
