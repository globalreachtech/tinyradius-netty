package org.tinyradius.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.attribute.AttributeBuilder;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.buffer.Unpooled.buffer;
import static java.lang.Byte.toUnsignedInt;
import static java.util.Objects.requireNonNull;
import static org.tinyradius.packet.PacketType.*;
import static org.tinyradius.packet.RadiusPacket.HEADER_LENGTH;
import static org.tinyradius.packet.RadiusPacket.MAX_PACKET_LENGTH;

public class RadiusPacketEncoder {

    private static AtomicInteger nextPacketId = new AtomicInteger();

    /**
     * Increment the next packet identifier to use.
     *
     * @return the next packet identifier to use
     */
    public static int nextPacketId() {
        return nextPacketId.updateAndGet(i -> i >= 255 ? 0 : i + 1);
    }

    public static DatagramPacket toDatagram(RadiusPacket packet, InetSocketAddress address) throws RadiusException {
        byte[] attributes = packet.getAttributeBytes();
        int length = HEADER_LENGTH + attributes.length;
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusException("packet too long");
        if (packet.getAuthenticator().length != 16)
            throw new RadiusException("authenticator must be length 16");

        ByteBuf buf = buffer(length, length);
        buf.writeByte(packet.getPacketType());
        buf.writeByte(packet.getPacketIdentifier());
        buf.writeShort(length);
        buf.writeBytes(packet.getAuthenticator());
        buf.writeBytes(attributes);
        return new DatagramPacket(buf, address);
    }

    /**
     * Reads a Radius request packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param dictionary   dictionary to use for attributes
     * @param datagram     DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @return new RadiusPacket object
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket fromRequestDatagram(Dictionary dictionary, DatagramPacket datagram, String sharedSecret)
            throws RadiusException {
        final RadiusPacket radiusPacket = fromDatagram(dictionary, datagram, sharedSecret, -1);
        radiusPacket.checkAuthenticator(sharedSecret, new byte[16]);

        return radiusPacket;
    }

    /**
     * Reads a Radius response packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Checks the packet authenticator.
     *
     * @param dictionary   dictionary to use for attributes
     * @param datagram     DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius request packet
     * @return new RadiusPacket object
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket fromResponseDatagram(Dictionary dictionary, DatagramPacket datagram, String sharedSecret, RadiusPacket request)
            throws RadiusException {
        requireNonNull(request, "request may not be null");

        if (request.getAuthenticator() == null)
            throw new RuntimeException("request authenticator not set");

        final RadiusPacket radiusPacket = fromDatagram(dictionary, datagram, sharedSecret, request.getPacketIdentifier());
        radiusPacket.checkAuthenticator(sharedSecret, request.getAuthenticator());

        return radiusPacket;
    }

    /**
     * Reads a Radius packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param dictionary      dictionary to use for attributes
     * @param packet          DatagramPacket to read packet from
     * @param sharedSecret    shared secret to be used to decode this packet
     * @param requestPacketId request packet Id if this is a response packet to be
     *                        decoded, -1 if this is a request packet to be decoded
     * @return new RadiusPacket object
     * @throws RadiusException packet malformed
     */
    private static RadiusPacket fromDatagram(Dictionary dictionary, DatagramPacket packet, String sharedSecret, int requestPacketId)
            throws RadiusException {

        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new RuntimeException("no shared secret has been set");

        ByteBuf content = packet.content();
        if (content.readableBytes() < HEADER_LENGTH) {
            throw new RuntimeException("readable bytes is less than header length");
        }

        int type = toUnsignedInt(content.readByte());
        int packetId = toUnsignedInt(content.readByte());
        int length = toUnsignedInt(content.readByte()) << 8 | toUnsignedInt(content.readByte());

        if (requestPacketId != -1 && requestPacketId != packetId)
            throw new RadiusException("bad packet: invalid packet identifier - request: " + requestPacketId + ", response: " + packetId);
        if (length < HEADER_LENGTH)
            throw new RadiusException("bad packet: packet too short (" + length + " bytes)");
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusException("bad packet: packet too long (" + length + " bytes)");

        byte[] authenticator = new byte[16];
        byte[] attributeData = new byte[length - HEADER_LENGTH];
        content.readBytes(authenticator);
        content.readBytes(attributeData);

        final List<RadiusAttribute> attributes = extractAttributes(attributeData, dictionary);
        RadiusPacket rp = createRadiusPacket(dictionary, type, packetId, authenticator, attributes);
        rp.decodeAttributes(sharedSecret);

        return rp;
    }

    private static List<RadiusAttribute> extractAttributes(byte[] attributeData, Dictionary dictionary) throws RadiusException {
        List<RadiusAttribute> attributes = new ArrayList<>();

        int pos = 0;
        while (pos < attributeData.length) {
            if (pos + 1 >= attributeData.length)
                throw new RadiusException("bad packet: attribute length out of bounds");
            int attributeType = toUnsignedInt(attributeData[pos]);
            int attributeLength = toUnsignedInt(attributeData[pos + 1]);
            if (attributeLength < 2)
                throw new RadiusException("bad packet: invalid attribute length");
            RadiusAttribute a = AttributeBuilder.parse(dictionary, -1, attributeType, attributeData, pos);
            attributes.add(a);
            pos += attributeLength;
        }

        if (pos != attributeData.length)
            throw new RadiusException("bad packet: attribute length mismatch");
        return attributes;
    }

    /**
     * Creates a RadiusPacket object. Depending on the passed type, an
     * appropriate packet is created. Also sets the type, and the
     * the packet identifier.
     *
     * @param type packet type
     * @return RadiusPacket object
     */
    public static RadiusPacket createRadiusPacket(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        switch (type) {
            case ACCESS_REQUEST:
                return new AccessRequest(dictionary, identifier, authenticator, attributes);
            case ACCOUNTING_REQUEST:
                return new AccountingRequest(dictionary, identifier, authenticator, attributes);
            case COA_REQUEST:
            case DISCONNECT_REQUEST:
            default:
                return new RadiusPacket(dictionary, type, identifier, authenticator, attributes);
        }
    }
}
