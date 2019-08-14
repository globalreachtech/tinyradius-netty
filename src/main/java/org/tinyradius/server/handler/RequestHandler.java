package org.tinyradius.server.handler;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.Lifecycle;

import java.net.InetSocketAddress;

public interface RequestHandler<T extends RadiusPacket> extends Lifecycle {

    /**
     * Handles the received Radius packet and constructs a response. Filters/Deduplicators
     * can also implement this and wrap around underlying handlers.
     *
     * @param channel       socket which received packet
     * @param request       incoming packet, can be RadiusPacket or subclass
     * @param remoteAddress remote address the packet was sent by
     * @param sharedSecret  shared secret associated with remoteAddress
     * @return Promise of RadiusPacket or null for no response. Uses Promise instead Future,
     * so requests to be timed out or cancelled by the caller
     */
    Promise<RadiusPacket> handlePacket(Channel channel, T request, InetSocketAddress remoteAddress, String sharedSecret);
}
