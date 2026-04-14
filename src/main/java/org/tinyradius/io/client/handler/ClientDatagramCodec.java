package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.io.client.PendingRequestCtx;

import java.net.InetSocketAddress;
import java.util.List;

import static org.tinyradius.core.packet.response.RadiusResponse.fromDatagram;

/**
 * Datagram codec for sending requests and receiving responses.
 * <p>
 * Only manages datagram conversion, does not call encodeRequest() / decodeResponse().
 */
@ChannelHandler.Sharable
public class ClientDatagramCodec extends MessageToMessageCodec<DatagramPacket, PendingRequestCtx> {

    private static final Logger log = LogManager.getLogger(ClientDatagramCodec.class);
    private final Dictionary dictionary;

    /**
     * Constructs a {@code ClientDatagramCodec} with the specified {@link Dictionary}.
     *
     * @param dictionary the dictionary to use for packet decoding
     */
    public ClientDatagramCodec(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void encode(@NonNull ChannelHandlerContext ctx, @NonNull PendingRequestCtx msg, @NonNull List<Object> out) {
        log.debug("Sending packet to {} - {}", msg.getEndpoint().address(), msg.getRequest());

        var datagramPacket = new DatagramPacket(
                msg.getRequest().toByteBuf(),
                msg.getEndpoint().address(),
                (InetSocketAddress) ctx.channel().localAddress());

        out.add(datagramPacket);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decode(@NonNull ChannelHandlerContext ctx, @NonNull DatagramPacket msg, @NonNull List<Object> out) {
        var remoteAddress = msg.sender();

        if (remoteAddress == null) {
            log.warn("Ignoring response, remoteAddress is null");
            return;
        }

        try {
            var response = fromDatagram(dictionary, msg);
            log.debug("Received packet from {} - {}", remoteAddress, response);

            out.add(response);
        } catch (RadiusPacketException e) {
            log.warn("Could not deserialize packet: {}", e.getMessage());
        }
    }
}
