package org.tinyradius.server;


import org.tinyradius.packet.RadiusRequest;
import org.tinyradius.packet.RadiusResponse;
import org.tinyradius.util.RadiusEndpoint;

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
