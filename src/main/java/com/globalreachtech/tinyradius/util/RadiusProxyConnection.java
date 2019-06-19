package com.globalreachtech.tinyradius.util;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;

/**
 * This class stores information about a proxied packet.
 * It contains two RadiusEndpoint objects representing the Radius client
 * and server, the port number the proxied packet arrived
 * at originally and the proxied packet itself.
 */
public class RadiusProxyConnection {

    /**
     * Creates a RadiusProxyConnection object.
     * @param radiusServer server endpoint
     * @param radiusClient client endpoint
     * @param port port the proxied packet arrived at originally
     */
    public RadiusProxyConnection(RadiusEndpoint radiusServer, RadiusEndpoint radiusClient, RadiusPacket packet, int port) {
        this.radiusServer = radiusServer;
        this.radiusClient = radiusClient;
        this.packet = packet;
        this.port = port;
    }

    public RadiusEndpoint getRadiusClient() {
        return radiusClient;
    }

    public RadiusEndpoint getRadiusServer() {
        return radiusServer;
    }

    /**
     * Returns the proxied packet.
     * @return packet
     */
    public RadiusPacket getPacket() {
        return packet;
    }

    /**
     * Returns the port number the proxied packet arrived at
     * originally.
     * @return port number
     */
    public int getPort() {
        return port;
    }


    private RadiusEndpoint radiusServer;
    private RadiusEndpoint radiusClient;
    private int port;
    private RadiusPacket packet;

}
