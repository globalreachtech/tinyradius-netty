package org.tinyradius.io.server;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;

/**
 * Context object for a RADIUS server response.
 * <p>
 * Holds the original request, the remote endpoint, and the response
 * to be sent back to the client. This context is passed through the
 * Netty channel pipeline for encoding and sending the response.
 */
@Getter
public class ResponseCtx extends RequestCtx {

    private final RadiusResponse response;

    public ResponseCtx(@NonNull RadiusRequest packet, @NonNull RadiusEndpoint endpoint, @NonNull RadiusResponse response) {
        super(packet, endpoint);
        this.response = response;
    }
}
