package org.tinyradius.netty;

import io.netty.util.concurrent.Future;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

/**
 *
 */
public interface RadiusQueueEntry {

    /**
     *
     * @return
     */
    public RadiusRequestContext context();
}