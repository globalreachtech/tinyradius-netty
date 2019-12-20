package org.tinyradius.server;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

public class RequestCtx {

    private final RadiusPacket request;
    private final RadiusEndpoint endpoint;

    public RequestCtx(RadiusPacket request, RadiusEndpoint endpoint) {
        this.request = request;
        this.endpoint = endpoint;
    }

    public RadiusPacket getRequest() {
        return request;
    }

    public RadiusEndpoint getEndpoint() {
        return endpoint;
    }

    public ServerResponseCtx withResponse(RadiusPacket response) {
        return new ServerResponseCtx(request, endpoint, response);
    }
}
