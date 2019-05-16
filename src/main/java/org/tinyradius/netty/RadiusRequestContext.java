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
    RadiusPacket request();

    /**
     *
     * @return
     */
    RadiusPacket response();

    /**
     *
     */
    long responseTime();

    /**
     *
     * @return
     */
    RadiusEndpoint endpoint();
}
