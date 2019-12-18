package org.tinyradius.server.handler;

import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;

public class RequestContext {

    private final RadiusPacket request;
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;
    private final String secret;

    public RequestContext(RadiusPacket request, InetSocketAddress localAddress, InetSocketAddress remoteAddress, String secret) {
        this.request = request;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.secret = secret;
    }

    public RadiusPacket getRequest() {
        return request;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public String getSecret() {
        return secret;
    }

    public ResponseContext withResponse(RadiusPacket response) {
        return new ResponseContext(response, request, localAddress, remoteAddress, secret);
    }
}
