package org.tinyradius.io.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;

@Getter
@RequiredArgsConstructor
public class RequestCtx {

    private final RadiusRequest request;
    private final RadiusEndpoint endpoint;

    public ResponseCtx withResponse(RadiusResponse response) {
        return new ResponseCtx(request, endpoint, response);
    }

    public String toString() {
        return "RequestCtx{" +
                "packet=" + getRequest() +
                ", endpoint=" + getEndpoint() +
                "}";
    }
}
