package org.tinyradius.server;

import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

public class RequestCtx {

    private final BaseRadiusPacket request;
    private final RadiusEndpoint endpoint;

    public RequestCtx(BaseRadiusPacket request, RadiusEndpoint endpoint) {
        this.request = request;
        this.endpoint = endpoint;
    }

    public BaseRadiusPacket getRequest() {
        return request;
    }

    public RadiusEndpoint getEndpoint() {
        return endpoint;
    }

    public ResponseCtx withResponse(BaseRadiusPacket response) {
        return new ResponseCtx(request, endpoint, response);
    }
}
