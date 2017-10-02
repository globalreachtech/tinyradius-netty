package org.tinyradius.netty;

import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;

/**
 *
 */
public interface RadiusQueue {

    /**
     *
     * @param request
     * @param endpoint
     * @return
     */
    public RadiusRequestContext queue(RadiusPacket request, RadiusEndpoint endpoint);

    /**
     *
     * @param context
     * @return
     */
    public void dequeue(RadiusRequestContext context);

    /**
     *
     * @param response
     * @return
     */
    public RadiusRequestContext lookup(DatagramPacket response);
}