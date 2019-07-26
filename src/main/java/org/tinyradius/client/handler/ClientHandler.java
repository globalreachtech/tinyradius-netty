package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

/**
 * ChannelInboundHandler used by RadiusClient
 */
public abstract class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Preprocess RadiusPackets before they are sent. May mutate outbound packets.
     * <p>
     * Save state info about outgoing requests so handler has context when handling replies
     * <p>
     * Ensure that you also return the encoded packet, typically with the shared secret available in RadiusEndpoint.
     *
     * @param packet   request to send
     * @param endpoint packet endpoint
     * @return promise of response which completes when server responds. Uses Promise instead Future,
     * to allow requests to be timed out or cancelled by the caller
     */
    public abstract RadiusPacket prepareRequest(RadiusPacket packet, RadiusEndpoint endpoint, Promise<RadiusPacket> promise);

    /**
     * Processes DatagramPacket. This does not swallow exceptions.
     *
     * @param datagramPacket datagram received
     * @throws RadiusException malformed packet
     */
    abstract void handleResponse(DatagramPacket datagramPacket) throws RadiusException;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagramPacket) {
        try {
            handleResponse(datagramPacket);
        } catch (Exception e) {
            logger.warn("DatagramPacket handle error: ", e);
        } finally {
            datagramPacket.release();
        }
    }
}
