package com.globalreachtech.tinyradius.server;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.MAX_PACKET_LENGTH;
import static io.netty.buffer.Unpooled.buffer;

public abstract class BaseHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    protected final ServerPacketManager packetManager;
    protected final Dictionary dictionary;

    protected BaseHandler(Dictionary dictionary, ServerPacketManager packetManager) {
        this.dictionary = dictionary;
        this.packetManager = packetManager;
    }

    /**
     * Returns the shared secret used to communicate with the client with the
     * passed IP address or null if the client is not allowed at this server.
     *
     * @param client IP address and port number of client
     * @return shared secret or null
     */
    protected abstract String getSharedSecret(InetSocketAddress client);

    /**
     * Copies all Proxy-State attributes from the clientRequest
     * packet to the clientResponse packet.
     *
     * @param request clientRequest packet
     * @param answer  clientResponse packet
     */
    protected void copyProxyState(RadiusPacket request, RadiusPacket answer) {
        request.getAttributes(33).forEach(answer::addAttribute);
    }

    /**
     * Creates a Radius clientResponse datagram packet from a RadiusPacket to be send.
     *
     * @param packet  RadiusPacket
     * @param secret  shared secret to encode packet
     * @param address where to send the packet
     * @param request clientRequest packet
     * @return new datagram packet
     * @throws IOException
     */
    protected DatagramPacket makeDatagramPacket(RadiusPacket packet, String secret, InetSocketAddress address,
                                                RadiusPacket request) throws IOException, RadiusException {

        ByteBuf buf = buffer(MAX_PACKET_LENGTH, MAX_PACKET_LENGTH);
        packet.setDictionary(dictionary);
        packet.encodeResponsePacket(new ByteBufOutputStream(buf), secret, request);

        return new DatagramPacket(buf, address);
    }

    /**
     * Creates a RadiusPacket for a Radius clientRequest from a received
     * datagram packet.
     *
     * @param packet received datagram
     * @return RadiusPacket object
     * @throws RadiusException malformed packet
     * @throws IOException     communication error (after getRetryCount()
     *                         retries)
     */
    protected RadiusPacket makeRadiusPacket(DatagramPacket packet, String sharedSecret) throws IOException, RadiusException {
        ByteBufInputStream in = new ByteBufInputStream(packet.content());
        return RadiusPacket.decodeRequestPacket(dictionary, in, sharedSecret);
    }
}
