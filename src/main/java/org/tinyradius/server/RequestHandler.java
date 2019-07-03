package org.tinyradius.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;

public interface RequestHandler<T extends RadiusPacket> {

    /**
     * Handles the received Radius packet and constructs a response. Filters/Deduplicators
     * can also implement this and wrap around underlying handlers.
     *
     * @param dictionary
     * @param channel       socket which received packet
     * @param request       the packet
     * @param remoteAddress remote address the packet was sent by
     * @param sharedSecret  shared secret associated with remoteAddress
     * @return Promise of RadiusPacket or null for no response. Uses Promise instead Future,
     * to allow requests to be timed out or cancelled by the caller
     */
    Promise<RadiusPacket> handlePacket(Dictionary dictionary, Channel channel, T request, InetSocketAddress remoteAddress, String sharedSecret);
}
