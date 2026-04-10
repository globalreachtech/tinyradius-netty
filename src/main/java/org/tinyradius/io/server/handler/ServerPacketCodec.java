package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.Dictionary;
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
@ChannelHandler.Sharable
public class ServerPacketCodec extends MessageToMessageCodec<DatagramPacket, ResponseCtx> {

    private static final Logger log = LogManager.getLogger(ServerPacketCodec.class);
    private final Dictionary dictionary;
    private final SecretProvider secretProvider;

    /**
     * Constructs a {@code ServerPacketCodec} with the specified {@link Dictionary} and {@link SecretProvider}.
     *
     * @param dictionary     the dictionary to use for packet decoding/encoding
     * @param secretProvider the provider to use for looking up shared secrets
     */
    public ServerPacketCodec(Dictionary dictionary, SecretProvider secretProvider) {
        this.dictionary = dictionary;
        this.secretProvider = secretProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseCtx msg, List<Object> out) {
        try {
            // should never be null - decode will have already thrown Exception
            var requestAuth = msg.getRequest().getAuthenticator();
            var datagramPacket = new DatagramPacket(
                    msg.getResponse()
                            .encodeResponse(msg.getEndpoint().secret(), requestAuth == null ? new byte[16] : requestAuth)
                            .toByteBuf(),
                    msg.getEndpoint().address(),
                    (InetSocketAddress) ctx.channel().localAddress());
            log.debug("Sending packet to {}", msg.getEndpoint().address());
            out.add(datagramPacket);
        } catch (RadiusPacketException e) {
            log.warn("Could not serialize packet: {}", e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
        var remoteAddress = msg.sender();

        try {
            var request = fromDatagram(dictionary, msg);

            var secret = secretProvider.getSharedSecret(remoteAddress, request);
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