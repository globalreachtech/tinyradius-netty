package org.tinyradius.server;

import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

public class ResponseCtx extends RequestCtx {

    private final BaseRadiusPacket response;

    public ResponseCtx(BaseRadiusPacket packet, RadiusEndpoint endpoint, BaseRadiusPacket response) {
        super(packet, endpoint);
        this.response = response;
    }

    public BaseRadiusPacket getResponse() {
        return response;
    }
}
