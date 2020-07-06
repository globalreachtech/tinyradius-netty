package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.client.PendingRequestCtx;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.util.List;

import static org.tinyradius.packet.response.RadiusResponse.fromDatagram;

/**
 * Datagram codec for sending requests and receiving responses.
 */
@ChannelHandler.Sharable
public class ClientPacketCodec extends MessageToMessageCodec<DatagramPacket, PendingRequestCtx> {

    private static final Logger logger = LogManager.getLogger();

    private final Dictionary dictionary;

    public ClientPacketCodec(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    protected DatagramPacket encodePacket(InetSocketAddress localAddress, PendingRequestCtx msg) {
        try {
            final RadiusRequest packet = msg.getRequest().encodeRequest(msg.getEndpoint().getSecret());
            final DatagramPacket datagramPacket = packet.toDatagram(
                    msg.getEndpoint().getAddress(), localAddress);
            logger.debug("Sending request to {}", msg.getEndpoint().getAddress());
            return datagramPacket;
        } catch (RadiusPacketException e) {
            logger.warn("Could not encode Radius packet: {}", e.getMessage());
            msg.getResponse().tryFailure(e);
            return null;
        }
    }

    protected RadiusResponse decodePacket(DatagramPacket msg) {
        InetSocketAddress remoteAddress = msg.sender();

        if (remoteAddress == null) {
            logger.warn("Ignoring response, remoteAddress is null");
            return null;
        }

        try {
            // can't decode/verify until we know corresponding request auth
            RadiusResponse response = fromDatagram(dictionary, msg);
            logger.debug("Received response from {} - {}", remoteAddress, response);
            return response;
        } catch (RadiusPacketException e) {
            logger.warn("Could not decode Radius packet: {}", e.getMessage());
            return null;
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, PendingRequestCtx msg, List<Object> out) {
        final DatagramPacket datagramPacket = encodePacket((InetSocketAddress) ctx.channel().localAddress(), msg);
        if (datagramPacket != null)
            out.add(datagramPacket);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
        final RadiusResponse radiusPacket = decodePacket(msg);
        if (radiusPacket != null)
            out.add(radiusPacket);
    }
}
