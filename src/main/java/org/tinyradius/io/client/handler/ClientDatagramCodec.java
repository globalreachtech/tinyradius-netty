package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.client.PendingRequestCtx;

import java.net.InetSocketAddress;
import java.util.List;

import static org.tinyradius.core.packet.response.RadiusResponse.fromDatagram;

/**
 * Datagram codec for sending requests and receiving responses.
 * <p>
 * Only manages datagram conversion, does not call encodeRequest() / decodeResponse().
 */
@Log4j2
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class ClientDatagramCodec extends MessageToMessageCodec<DatagramPacket, PendingRequestCtx> {

    private final Dictionary dictionary;

    @Override
    protected void encode(ChannelHandlerContext ctx, PendingRequestCtx msg, List<Object> out) {
        log.debug("Sending packet to {} - {}", msg.getEndpoint().getAddress(), msg.getRequest());

        final DatagramPacket datagramPacket = new DatagramPacket(
                msg.getRequest().toByteBuf(),
                msg.getEndpoint().getAddress(),
                (InetSocketAddress) ctx.channel().localAddress());

        out.add(datagramPacket);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
        final InetSocketAddress remoteAddress = msg.sender();

        if (remoteAddress == null) {
            log.warn("Ignoring response, remoteAddress is null");
            return;
        }

        try {
            RadiusResponse response = fromDatagram(dictionary, msg);
            log.debug("Received packet from {} - {}", remoteAddress, response);

            out.add(response);
        } catch (RadiusPacketException e) {
            log.warn("Could not deserialize packet: {}", e.getMessage());
        }
    }
}
