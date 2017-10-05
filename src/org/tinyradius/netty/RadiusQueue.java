package org.tinyradius.netty;

import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

/**
 *
 */
public interface RadiusQueue {

    /**
     *
     * @param request
     * @return
     */
    public RadiusQueueEntry queue(RadiusRequestContext request);

    /**
     *
     * @param context
     * @return
     */
    public void dequeue(RadiusQueueEntry context);

    /**
     *
     * @param response
     * @return
     */
    public RadiusQueueEntry lookup(DatagramPacket response);
}
