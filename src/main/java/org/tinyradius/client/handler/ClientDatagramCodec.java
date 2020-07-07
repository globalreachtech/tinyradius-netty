package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.client.PendingRequestCtx;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.util.List;

import static org.tinyradius.packet.response.RadiusResponse.fromDatagram;

/**
 * Datagram codec for sending requests and receiving responses.
 * <p>
 * Only manages datagram conversion, does not call encodeRequest() / decodeResponse().
 */
@ChannelHandler.Sharable
public class ClientDatagramCodec extends MessageToMessageCodec<DatagramPacket, PendingRequestCtx> {

    private static final Logger logger = LogManager.getLogger();

    private final Dictionary dictionary;

    public ClientDatagramCodec(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, PendingRequestCtx msg, List<Object> out) {
        try {
            final DatagramPacket datagramPacket = msg
                    .getRequest()
                    .toDatagram(msg.getEndpoint().getAddress(), (InetSocketAddress) ctx.channel().localAddress());
            logger.debug("Sending request to {}", msg.getEndpoint().getAddress());
            out.add(datagramPacket);
        } catch (RadiusPacketException e) {
            logger.warn("Could not encode Radius packet: {}", e.getMessage());
            msg.getResponse().tryFailure(e);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) {
        final InetSocketAddress remoteAddress = msg.sender();

        if (remoteAddress == null) {
            logger.warn("Ignoring response, remoteAddress is null");
            return;
        }

        try {
            RadiusResponse response = fromDatagram(dictionary, msg);
            logger.debug("Received response from {} - {}", remoteAddress, response);

            // can't decode/verify until we know corresponding request auth
            out.add(response);
        } catch (RadiusPacketException e) {
            logger.warn("Could not decode Radius packet: {}", e.getMessage());
        }
    }
}
