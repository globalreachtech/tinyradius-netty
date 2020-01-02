package org.tinyradius.server;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

public class ResponseCtx extends RequestCtx {

    private final RadiusPacket response;

    public ResponseCtx(RadiusPacket packet, RadiusEndpoint endpoint, RadiusPacket response) {
        super(packet, endpoint);
        this.response = response;
    }

    public RadiusPacket getResponse() {
        return response;
    }
}
