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


    /**
     * Schedule a retry in the future.
     * <p>
     * When retry is due to run, should also check if promise isDone() before running.
     * <p>
     * Implemented here instead of RadiusClient so custom scheduling / retry backoff
     * can be used depending on implementation, and actual retry can be deferred. Scheduling
     * and logic should be implemented here, while RadiusClient only deals with IO.
     *
     * @param retry runnable to invoke to retry
     * @param attempt current attempt count
     * @param promise request promise that resolves when a reponse is received
     */
    public abstract void scheduleRetry(Runnable retry, int attempt, Promise<RadiusPacket> promise);
}
