package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.ResponseCtx;
import org.tinyradius.io.server.SecretProvider;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.core.RadiusPacketException;

import java.net.InetSocketAddress;
import java.util.List;

import static org.tinyradius.core.packet.request.RadiusRequest.fromDatagram;

/**
 * Codec for receiving requests and sending responses.
 * <p>
 * Both converts to/from datagrams and calls encodeResponse() / decodeRequest()
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

    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseCtx msg, List<Object> out) {
        try {
            final DatagramPacket datagramPacket = msg
                    .getResponse()
                    .encodeResponse(msg.getEndpoint().getSecret(), msg.getRequest().getAuthenticator())
                    .toDatagram(msg.getEndpoint().getAddress(), (InetSocketAddress) ctx.channel().localAddress());
            logger.debug("Sending response to {}", msg.getEndpoint().getAddress());
            out.add(datagramPacket);
        } catch (RadiusPacketException e) {
            logger.warn("Could not encode Radius packet: {}", e.getMessage());
        }
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
            final RadiusRequest request = fromDatagram(dictionary, msg);
            logger.debug("Received request from {} - {}", remoteAddress, request);
            // log first before errors may be thrown

            out.add(new RequestCtx(request.decodeRequest(secret), new RadiusEndpoint(remoteAddress, secret)));
        } catch (RadiusPacketException e) {
            logger.warn("Could not decode Radius packet: {}", e.getMessage());
        }
    }
}