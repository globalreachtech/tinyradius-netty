package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.SecretProvider;
import org.tinyradius.server.ServerResponseCtx;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.util.List;

@ChannelHandler.Sharable
public class ServerPacketCodec extends MessageToMessageCodec<DatagramPacket, ServerResponseCtx> {

    private static final Logger logger = LoggerFactory.getLogger(ServerPacketCodec.class);

    private final PacketEncoder packetEncoder;
    private final SecretProvider secretProvider;

    public ServerPacketCodec(PacketEncoder packetEncoder, SecretProvider secretProvider) {
        this.packetEncoder = packetEncoder;
        this.secretProvider = secretProvider;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
        final InetSocketAddress remoteAddress = msg.sender();

        String secret = secretProvider.getSharedSecret(remoteAddress);
        if (secret == null) {
            logger.warn("Ignoring request from {}, shared secret lookup failed", remoteAddress);
            return;
        }

        try {
            RadiusPacket packet = packetEncoder.fromDatagram(msg, secret);
            logger.debug("Received packet from {} - {}", remoteAddress, packet);

            out.add(new RequestCtx(packet, new RadiusEndpoint(remoteAddress, secret)));
        } catch (RadiusPacketException e) {
            logger.warn("Could not decode Radius packet: {}", e.getMessage());
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ServerResponseCtx msg, List<Object> out) {
        final RadiusPacket packet = msg.getResponse()
                .encodeResponse(msg.getEndpoint().getSecret(), msg.getRequest().getAuthenticator());
        try {
            final DatagramPacket datagramPacket = packetEncoder.toDatagram(
                    packet, msg.getEndpoint().getAddress(), (InetSocketAddress) ctx.channel().localAddress());

            out.add(datagramPacket);
            logger.debug("Sending response to {}", msg.getEndpoint().getAddress());

        } catch (RadiusPacketException e) {
            logger.warn("Could not encode Radius packet: {}", e.getMessage());
        }
    }
}