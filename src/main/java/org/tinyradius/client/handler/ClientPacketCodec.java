package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.util.List;

@ChannelHandler.Sharable
public class ClientPacketCodec extends MessageToMessageCodec<DatagramPacket, RequestCtx> {

    private static final Logger logger = LoggerFactory.getLogger(ClientPacketCodec.class);

    private final PacketEncoder packetEncoder;

    public ClientPacketCodec(PacketEncoder packetEncoder) {
        this.packetEncoder = packetEncoder;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RequestCtx msg, List<Object> out) {
        final RadiusPacket packet = msg.getRequest().encodeRequest(msg.getEndpoint().getSecret());
        try {
            final DatagramPacket datagramPacket = packetEncoder.toDatagram(
                    packet, msg.getEndpoint().getAddress(), (InetSocketAddress) ctx.channel().localAddress());
            out.add(datagramPacket);
            logger.debug("Sending request to {}", msg.getEndpoint().getAddress());
        } catch (RadiusException e) {
            logger.warn(e.getMessage());
        }

    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
        InetSocketAddress remoteAddress = msg.sender();

        if (remoteAddress == null) {
            logger.warn("Ignoring request, remoteAddress is null");
            return;
        }

        // can't verify until we know corresponding request auth
        try {
            RadiusPacket packet = packetEncoder.fromDatagram(msg);
            logger.debug("Received packet from {} - {}", remoteAddress, packet);

            out.add(packet);
        } catch (RadiusException e) {
            logger.warn(e.getMessage());
        }
    }
}
