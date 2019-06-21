package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import io.netty.channel.Channel;

/**
 * This class stores information about a proxied packet.
 * It contains two RadiusEndpoint objects representing the Radius client
 * and server, the channel the proxied packet arrived
 * at originally and the proxied packet itself.
 */
public class RadiusProxyConnection {

    private final RadiusEndpoint serverEndpoint;
    private final RadiusEndpoint clientEndpoint;
    private final RadiusPacket requestPacket;
    private final Channel requestChannel;

    /**
     * Creates a RadiusProxyConnection object.
     *
     */
    public RadiusProxyConnection(RadiusEndpoint serverEndpoint, RadiusEndpoint clientEndpoint, RadiusPacket requestPacket, Channel requestChannel) {
        this.serverEndpoint = serverEndpoint;
        this.clientEndpoint = clientEndpoint;
        this.requestPacket = requestPacket;
        this.requestChannel = requestChannel;
    }

    public RadiusEndpoint getClientEndpoint() {
        return clientEndpoint;
    }

    public RadiusEndpoint getServerEndpoint() {
        return serverEndpoint;
    }

    /**
     * Returns the proxied packet.
     */
    public RadiusPacket getRequestPacket() {
        return requestPacket;
    }

    public Channel getRequestChannel() {
        return requestChannel;
    }
}
