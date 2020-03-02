package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusRequest;
import org.tinyradius.packet.RadiusResponse;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.ResponseCtx;
import org.tinyradius.server.SecretProvider;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.util.List;

import static org.tinyradius.packet.PacketCodec.fromDatagram;
import static org.tinyradius.packet.PacketCodec.toDatagram;

/**
 * Codec for receiving requests and sending responses
 */
@ChannelHandler.Sharable
public class ServerPacketCodec extends MessageToMessageCodec<DatagramPacket, ResponseCtx> {

    private static final Logger logger = LogManager.getLogger();

    private final Dictionary dictionary;
    private final SecretProvider secretProvider;

    public ServerPacketCodec(Dictionary dictionary, SecretProvider secretProvider) {
        this.dictionary = dictionary;
        this.secretProvider = secretProvider;
    }

    protected RequestCtx decodePacket(DatagramPacket msg) {
        final InetSocketAddress remoteAddress = msg.sender();

        String secret = secretProvider.getSharedSecret(remoteAddress);
        if (secret == null) {
            logger.warn("Ignoring request from {}, shared secret lookup failed", remoteAddress);
            return null;
        }

        try {
            RadiusRequest packet = fromDatagram(dictionary, msg, secret);
            logger.debug("Received packet from {} - {}", remoteAddress, packet);

            return new RequestCtx(packet, new RadiusEndpoint(remoteAddress, secret));
        } catch (RadiusPacketException e) {
            logger.warn("Could not decode Radius packet: {}", e.getMessage());
            return null;
        }
    }

    protected DatagramPacket encodePacket(InetSocketAddress localAddress, ResponseCtx msg) {
        final RadiusResponse packet = msg.getResponse()
                .encodeResponse(msg.getEndpoint().getSecret(), msg.getRequest().getAuthenticator());
        try {
            final DatagramPacket datagramPacket = toDatagram(
                    packet, msg.getEndpoint().getAddress(), localAddress);
            logger.debug("Sending response to {}", msg.getEndpoint().getAddress());
            return datagramPacket;
        } catch (RadiusPacketException e) {
            logger.warn("Could not encode Radius packet: {}", e.getMessage());
            return null;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
        final RequestCtx requestCtx = decodePacket(msg);
        if (requestCtx != null)
            out.add(requestCtx);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseCtx msg, List<Object> out) {
        final DatagramPacket datagramPacket = encodePacket((InetSocketAddress) ctx.channel().localAddress(), msg);
        if (datagramPacket != null)
            out.add(datagramPacket);
    }
}