package org.tinyradius.io.server;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;

/**
 * Context object for an incoming RADIUS server request.
 * <p>
 * Holds the incoming request packet and the remote endpoint (address and shared secret)
 * from which the request was received. This context is passed through the
 * Netty channel pipeline for processing the request.
 * <p>
 * Use {@link #withResponse(RadiusResponse)} to create a {@link ResponseCtx}
 * for sending a response back to the client.
 */
@Getter
public class RequestCtx {

    private final RadiusRequest request;
    private final RadiusEndpoint endpoint;

    public RequestCtx(@NonNull RadiusRequest request, @NonNull RadiusEndpoint endpoint) {
        this.request = request;
        this.endpoint = endpoint;
    }

    @NonNull
    public ResponseCtx withResponse(@NonNull RadiusResponse response) {
        return new ResponseCtx(request, endpoint, response);
    }

    @Override
    public String toString() {
        return "RequestCtx{" +
                "packet=" + getRequest() +
                ", endpoint=" + getEndpoint() +
                "}";
    }
}
