package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.ResponseCtx;
import org.tinyradius.io.server.SecretProvider;

import java.net.InetSocketAddress;
import java.util.List;

import static org.tinyradius.core.packet.request.RadiusRequest.fromDatagram;

/**
 * Codec for receiving requests and sending responses.
 * <p>
 * Both converts to/from datagrams and calls encodeResponse() / decodeRequest()
 */
@Log4j2
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class ServerPacketCodec extends MessageToMessageCodec<DatagramPacket, ResponseCtx> {

    private final Dictionary dictionary;
    private final SecretProvider secretProvider;

    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseCtx msg, List<Object> out) {
        try {
            final DatagramPacket datagramPacket = new DatagramPacket(
                    msg.getResponse()
                            .encodeResponse(msg.getEndpoint().getSecret(), msg.getRequest().getAuthenticator())
                            .toByteBuf(),
                    msg.getEndpoint().getAddress(),
                    (InetSocketAddress) ctx.channel().localAddress());
            log.debug("Sending packet to {}", msg.getEndpoint().getAddress());
            out.add(datagramPacket);
        } catch (RadiusPacketException e) {
            log.warn("Could not serialize packet: {}", e.getMessage());
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
        final InetSocketAddress remoteAddress = msg.sender();

        try {
            final RadiusRequest request = fromDatagram(dictionary, msg);

            String secret = secretProvider.getSharedSecret(remoteAddress, request);
            if (secret == null) {
                log.warn("Ignoring packet from {}, shared secret lookup failed", remoteAddress);
                return;
            }

            log.debug("Received request from {} - {}", remoteAddress, request);
            // log first before errors may be thrown

            out.add(new RequestCtx(request.decodeRequest(secret), new RadiusEndpoint(remoteAddress, secret)));
        } catch (RadiusPacketException e) {
            log.warn("Could not deserialize packet: {}", e.getMessage());
        }
    }
}