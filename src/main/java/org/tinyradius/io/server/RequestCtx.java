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

    /**
     * Creates a new RequestCtx.
     *
     * @param request  the incoming RADIUS request
     * @param endpoint the remote endpoint from which the request was received
     */
    public RequestCtx(@NonNull RadiusRequest request, @NonNull RadiusEndpoint endpoint) {
        this.request = request;
        this.endpoint = endpoint;
    }

    /**
     * Creates a {@link ResponseCtx} for sending a response back to the client.
     *
     * @param response the RADIUS response to send
     * @return a new ResponseCtx with the given response and matching request/endpoint
     */
    @NonNull
    public ResponseCtx withResponse(@NonNull RadiusResponse response) {
        return new ResponseCtx(request, endpoint, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "RequestCtx{" +
                "packet=" + getRequest() +
                ", endpoint=" + getEndpoint() +
                "}";
    }
}
