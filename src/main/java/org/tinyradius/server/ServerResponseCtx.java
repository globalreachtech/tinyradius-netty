package org.tinyradius.server;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

public class ServerResponseCtx extends RequestCtx {

    private final RadiusPacket response;

    public ServerResponseCtx(RadiusPacket packet, RadiusEndpoint endpoint, RadiusPacket response) {
        super(packet, endpoint);
        this.response = response;
    }

    public RadiusPacket getResponse() {
        return response;
    }
}
