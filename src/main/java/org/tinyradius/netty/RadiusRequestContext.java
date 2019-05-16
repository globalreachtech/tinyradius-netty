package org.tinyradius.netty;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

public interface RadiusRequestContext {

    RadiusPacket request();

    RadiusPacket response();

    long responseTime();

    RadiusEndpoint endpoint();
}
