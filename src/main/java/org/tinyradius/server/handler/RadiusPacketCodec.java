package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.SecretProvider;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.util.List;

public class RadiusPacketCodec<T extends ResponseContext, S extends SecretProvider> extends MessageToMessageCodec<DatagramPacket, T> {

    private static final Logger logger = LoggerFactory.getLogger(RadiusPacketCodec.class);

    private final PacketEncoder packetEncoder;
    private final S secretProvider;

    public RadiusPacketCodec(PacketEncoder packetEncoder, S secretProvider) {
        this.packetEncoder = packetEncoder;
        this.secretProvider = secretProvider;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        InetSocketAddress localAddress = msg.recipient();
        InetSocketAddress remoteAddress = msg.sender();

        String secret = secretProvider.getSharedSecret(remoteAddress);
        if (secret == null)
            throw new RadiusException("Ignoring request from unknown client " + remoteAddress +
                    ", shared secret lookup failed (local address " + localAddress + ")");

        // parse packet
        RadiusPacket request = packetEncoder.fromDatagram(msg, secret);
        logger.info("Received packet from {} on local address {} - {}", remoteAddress, localAddress, request);

        out.add(new RequestContext(request, localAddress, remoteAddress, secret));
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, T msg, List<Object> out) throws Exception {
        if (msg == null)
            return;

        logger.debug("Encoding response for {}", msg.getRemoteAddress());
        out.add(packetEncoder.toDatagram(
                msg.getResponse().encodeResponse(msg.getSecret(), msg.getRequest().getAuthenticator()), msg.getRemoteAddress(), msg.getLocalAddress()));
        logger.info("Request handled, sending response to {}", msg.getRemoteAddress());
    }
}