package org.tinyradius.io.server;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;

@Getter
public class ResponseCtx extends RequestCtx {

    private final RadiusResponse response;

    public ResponseCtx(@NonNull RadiusRequest packet, @NonNull RadiusEndpoint endpoint, @NonNull RadiusResponse response) {
        super(packet, endpoint);
        this.response = response;
    }
}
