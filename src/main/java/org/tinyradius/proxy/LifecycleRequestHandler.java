package org.tinyradius.proxy;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestHandler;
import org.tinyradius.util.Lifecycle;

public interface LifecycleRequestHandler<T extends RadiusPacket> extends RequestHandler<T>, Lifecycle {
}
