package org.tinyradius.proxy.handler;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.handler.RequestHandler;
import org.tinyradius.util.Lifecycle;

public interface LifecycleRequestHandler<T extends RadiusPacket> extends RequestHandler<T>, Lifecycle {
}
