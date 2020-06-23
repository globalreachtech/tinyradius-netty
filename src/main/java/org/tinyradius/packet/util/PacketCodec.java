package org.tinyradius.packet.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.attribute.AttributeHolder;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


/**
 * To encode/decode packets to/from Datagram.
 * <p>
 * A dictionary is required in constructor to avoid passing in dictionary
 * on every conversion - in typical use cases, only one instance of this
 * class will be required as the server/proxy/client will be using one Dictionary.
 */
public class PacketCodec {

    private static final int HEADER_LENGTH = 20;
    private static final int MAX_PACKET_LENGTH = 4096;

    private PacketCodec() {
    }

    /**
     * @param packet    packet to convert
     * @param recipient destination socket
     * @return converted DatagramPacket
     * @throws RadiusPacketException if packet could not be encoded/serialized to datagram
     */
    public static DatagramPacket toDatagram(RadiusPacket<?> packet, InetSocketAddress recipient) throws RadiusPacketException {
        return new DatagramPacket(toByteBuf(packet), recipient);
    }

    /**
     * @param packet    packet to convert
     * @param recipient destination socket
     * @param sender    source socket, nullable
     * @return converted DatagramPacket
     * @throws RadiusPacketException if packet could not be encoded/serialized to datagram
     */
    public static DatagramPacket toDatagram(RadiusPacket<?> packet, InetSocketAddress recipient, InetSocketAddress sender) throws RadiusPacketException {
        return new DatagramPacket(toByteBuf(packet), recipient, sender);
    }

    public static ByteBuf toByteBuf(RadiusPacket<?> packet) throws RadiusPacketException {
        byte[] attributes = packet.getAttributeBytes();
        int length = HEADER_LENGTH + attributes.length;
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusPacketException("Packet too long");
        if (packet.getAuthenticator() == null)
            throw new RadiusPacketException("Missing authenticator");
        if (packet.getAuthenticator().length != 16)
            throw new RadiusPacketException("Authenticator must be length 16");

        return Unpooled.buffer(length, length)
                .writeByte(packet.getType())
                .writeByte(packet.getId())
                .writeShort(length)
                .writeBytes(packet.getAuthenticator())
                .writeBytes(attributes);
    }

    /**
     * Reads a request from the given input stream and
     * creates an appropriate RadiusPacket/subclass.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary dictionary to use for attributes
     * @param datagram   DatagramPacket to read packet from
     * @return new RadiusPacket object
     * @throws RadiusPacketException malformed packet
     */
    public static RadiusRequest fromDatagramRequest(Dictionary dictionary, DatagramPacket datagram) throws RadiusPacketException {
        return fromByteBuf(dictionary, datagram.content());
    }

    /**
     * Reads a response from the given input stream and
     * creates an appropriate RadiusPacket/subclass.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary dictionary to use for attributes
     * @param datagram   DatagramPacket to read packet from
     * @return new RadiusPacket object
     * @throws RadiusPacketException malformed packet
     */
    public static RadiusResponse fromDatagramResponse(Dictionary dictionary, DatagramPacket datagram) throws RadiusPacketException {
        final RadiusRequest rr = fromByteBuf(dictionary, datagram.content());
        return RadiusResponse.create(rr.getDictionary(), rr.getType(), rr.getId(), rr.getAuthenticator(), rr.getAttributes());
    }

    /**
     * Reads a Radius packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary dictionary to use for attributes
     * @param byteBuf    DatagramPacket to read packet from
     * @return new RadiusPacket object
     * @throws RadiusPacketException malformed packet
     */
    private static RadiusRequest fromByteBuf(Dictionary dictionary, ByteBuf byteBuf) throws RadiusPacketException {

        final ByteBuffer content = byteBuf.nioBuffer();
        if (content.remaining() < HEADER_LENGTH) {
            throw new RadiusPacketException("Readable bytes is less than header length");
        }

        byte type = content.get();
        byte packetId = content.get();
        int length = content.getShort();

        if (length < HEADER_LENGTH)
            throw new RadiusPacketException("Bad packet: packet too short (" + length + " bytes)");
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusPacketException("Bad packet: packet too long (" + length + " bytes)");

        byte[] authenticator = new byte[16];
        content.get(authenticator);

        if (content.remaining() != length - HEADER_LENGTH)
            throw new RadiusPacketException("Bad packet: packet length mismatch");

        byte[] attributes = new byte[content.remaining()];
        content.get(attributes);

        return RadiusRequest.create(dictionary, type, packetId, authenticator,
                AttributeHolder.extractAttributes(dictionary, -1, attributes, 0));
    }
}
