package org.tinyradius.io.server;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;

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
