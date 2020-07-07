package org.tinyradius.io.server;


import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;

public class ResponseCtx extends RequestCtx {

    private final RadiusResponse response;

    public ResponseCtx(RadiusRequest packet, RadiusEndpoint endpoint, RadiusResponse response) {
        super(packet, endpoint);
        this.response = response;
    }

    public RadiusResponse getResponse() {
        return response;
    }
}
