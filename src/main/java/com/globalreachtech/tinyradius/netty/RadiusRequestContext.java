package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;

public interface RadiusRequestContext {

    RadiusPacket request();

    RadiusPacket response();

    long responseTime();

    RadiusEndpoint endpoint();
}
