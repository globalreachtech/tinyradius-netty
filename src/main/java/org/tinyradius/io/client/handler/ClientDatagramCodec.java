package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
            logger.debug("Sending request to {} - {}", msg.getEndpoint().getAddress(), msg.getRequest());
            final DatagramPacket datagramPacket = msg
                    .getRequest()
                    .toDatagram(msg.getEndpoint().getAddress(), (InetSocketAddress) ctx.channel().localAddress());

            out.add(datagramPacket);
        } catch (RadiusPacketException e) {
            logger.warn("Could not serialize Radius packet: {}", e.getMessage());
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

            out.add(response);
        } catch (RadiusPacketException e) {
            logger.warn("Could not deserialize Radius packet: {}", e.getMessage());
        }
    }
}
