package org.tinyradius.netty;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

/**
 *
 */
public interface RadiusRequestContext {

    /**
     *
     * @return
     */
    public RadiusPacket request();

    /**
     *
     * @return
     */
    public RadiusPacket response();

    /**
     *
     */
    public long responseTime();

    /**
     *
     * @return
     */
    public RadiusEndpoint endpoint();
}
