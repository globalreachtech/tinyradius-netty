package org.tinyradius.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.buffer.Unpooled.buffer;
import static java.lang.Byte.toUnsignedInt;
import static org.tinyradius.attribute.Attributes.extractAttributes;
import static org.tinyradius.packet.PacketType.*;
import static org.tinyradius.packet.RadiusPacket.HEADER_LENGTH;

public class RadiusPacketEncoder {

    private static final int MAX_PACKET_LENGTH = 4096;

    private static AtomicInteger nextPacketId = new AtomicInteger();

    /**
     * Increment the next packet identifier.
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
        buf.writeByte(packet.getType());
        buf.writeByte(packet.getIdentifier());
        buf.writeShort(length);
        buf.writeBytes(packet.getAuthenticator());
        buf.writeBytes(attributes);
        return new DatagramPacket(buf, address);
    }

    /**
     * Reads a Radius packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     * <p>
     * Same as calling {@link #fromDatagram(Dictionary, DatagramPacket, String, RadiusPacket)} with null RadiusPacket.
     *
     * @param dictionary   dictionary to use for attributes
     * @param datagram     DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @return new RadiusPacket object
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket fromDatagram(Dictionary dictionary, DatagramPacket datagram, String sharedSecret) throws RadiusException {
        return fromDatagram(dictionary, datagram, sharedSecret, null);
    }

    /**
     * Reads a Radius packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary   dictionary to use for attributes
     * @param datagram     DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      associated request packet if parsing response,
     *                     or null if parsing request
     * @return new RadiusPacket object
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket fromDatagram(Dictionary dictionary, DatagramPacket datagram, String sharedSecret, RadiusPacket request)
            throws RadiusException {

        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new RuntimeException("no shared secret has been set");

        final ByteBuffer content = datagram.content().nioBuffer();
        if (content.remaining() < HEADER_LENGTH) {
            throw new RadiusException("readable bytes is less than header length");
        }

        int type = toUnsignedInt(content.get());
        int packetId = toUnsignedInt(content.get());
        int length = content.getShort();

        if (request != null && request.getIdentifier() != packetId)
            throw new RadiusException("bad packet: invalid packet identifier - request: " + request.getIdentifier() + ", response: " + packetId);
        if (length < HEADER_LENGTH)
            throw new RadiusException("bad packet: packet too short (" + length + " bytes)");
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusException("bad packet: packet too long (" + length + " bytes)");

        byte[] authenticator = new byte[16];
        content.get(authenticator);

        if (content.remaining() != length - HEADER_LENGTH)
            throw new RadiusException("bad packet: packet length mismatch");

        byte[] attributes = new byte[content.remaining()];
        content.get(attributes);

        final RadiusPacket radiusPacket = createRadiusPacket(dictionary, type, packetId, authenticator,
                extractAttributes(dictionary, -1, attributes, 0));

        radiusPacket.decode(sharedSecret, request == null ?
                new byte[16] : request.getAuthenticator());

        return radiusPacket;
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
            default:
                return new RadiusPacket(dictionary, type, identifier, authenticator, attributes);
        }
    }
}
