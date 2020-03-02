package org.tinyradius.server;

import org.tinyradius.packet.RadiusRequest;
import org.tinyradius.packet.RadiusResponse;
import org.tinyradius.util.RadiusEndpoint;

public class RequestCtx {

    private final RadiusRequest request;
    private final RadiusEndpoint endpoint;

    public RequestCtx(RadiusRequest request, RadiusEndpoint endpoint) {
        this.request = request;
        this.endpoint = endpoint;
    }

    public RadiusRequest getRequest() {
        return request;
    }

    public RadiusEndpoint getEndpoint() {
        return endpoint;
    }

    public ResponseCtx withResponse(RadiusResponse response) {
        return new ResponseCtx(request, endpoint, response);
    }
}
