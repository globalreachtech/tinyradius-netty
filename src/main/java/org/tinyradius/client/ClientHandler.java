package org.tinyradius.client;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

/**
 * ChannelInboundHandler used by RadiusClient
 */
public abstract class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    /**
     * Preprocess RadiusPackets before they are sent. May mutate outbound packets.
     * <p>
     * Save state info about outgoing requests so handler has context when handling replies
     *
     * @param packet   request to send
     * @param endpoint packet endpoint
     * @return promise of response which completes when server responds. Uses Promise instead Future,
     * to allow requests to be timed out or cancelled by the caller
     */
    public abstract RadiusPacket prepareRequest(RadiusPacket packet, RadiusEndpoint endpoint, Promise<RadiusPacket> promise);
}
