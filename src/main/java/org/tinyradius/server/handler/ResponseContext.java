package org.tinyradius.server.handler;

import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;

public class ResponseContext extends RequestContext {

    private final RadiusPacket response;

    ResponseContext(RadiusPacket response, RadiusPacket request, InetSocketAddress localAddress, InetSocketAddress remoteAddress, String secret) {
        super(request, localAddress, remoteAddress, secret);
        this.response = response;
    }

    public RadiusPacket getResponse() {
        return response;
    }
}
