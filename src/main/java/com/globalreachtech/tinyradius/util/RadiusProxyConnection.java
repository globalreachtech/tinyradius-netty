package com.globalreachtech.tinyradius.util;

import com.globalreachtech.tinyradius.packet.RadiusPacket;

/**
 * This class stores information about a proxied packet.
 * It contains two RadiusEndpoint objects representing the Radius client
 * and server, the port number the proxied packet arrived
 * at originally and the proxied packet itself.
 */
public class RadiusProxyConnection {

    private final RadiusEndpoint serverEndpoint;
    private final RadiusEndpoint clientEndpoint;
    private final int port;
    private final RadiusPacket packet;

    /**
     * Creates a RadiusProxyConnection object.
     *
     * @param port port the proxied packet arrived at originally
     */
    public RadiusProxyConnection(RadiusEndpoint serverEndpoint, RadiusEndpoint clientEndpoint, RadiusPacket packet, int port) {
        this.serverEndpoint = serverEndpoint;
        this.clientEndpoint = clientEndpoint;
        this.packet = packet;
        this.port = port;
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
    public RadiusPacket getPacket() {
        return packet;
    }

    /**
     * Returns the port number the proxied packet arrived at originally.
     */
    public int getPort() {
        return port;
    }
}
