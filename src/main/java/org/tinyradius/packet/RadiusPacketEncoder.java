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
import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.packet.PacketType.ACCOUNTING_REQUEST;
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
     * creates an appropriate RadiusPacket/subclass.
     * <p>
     * Makes no distinction between reading requests/responses, and
     * does not attempt to decode attributes/verify authenticators.
     * RadiusPacket should be separately verified after this to check
     * authenticators are valid and required attributes present.
     * <p>
     * Typically used to decode a response where the corresponding request
     * (specifically the authenticator/identifier) are not available.
     *
     * @param dictionary dictionary to use for attributes
     * @param packet     DatagramPacket to read packet from
     * @return new RadiusPacket object
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket fromDatagramUnverified(Dictionary dictionary, DatagramPacket packet) throws RadiusException {
        return fromDatagram(dictionary, packet, -1);
    }

    /**
     * Reads a request from the given input stream and
     * creates an appropriate RadiusPacket/subclass.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary   dictionary to use for attributes
     * @param packet       DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @return new RadiusPacket object
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket fromDatagram(Dictionary dictionary, DatagramPacket packet, String sharedSecret) throws RadiusException {
        final RadiusPacket radiusPacket = fromDatagram(dictionary, packet, -1);
        radiusPacket.verify(sharedSecret, new byte[16]);
        return radiusPacket;
    }

    /**
     * Reads a response from the given input stream and
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
        final RadiusPacket radiusPacket = fromDatagram(dictionary, datagram, request.getIdentifier());
        radiusPacket.verify(sharedSecret, request.getAuthenticator());
        return radiusPacket;
    }

    /**
     * Reads a Radius packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary dictionary to use for attributes
     * @param datagram   DatagramPacket to read packet from
     * @param requestId  id that packet identifier has to match, otherwise -1
     * @return new RadiusPacket object
     * @throws RadiusException malformed packet
     */
    private static RadiusPacket fromDatagram(Dictionary dictionary, DatagramPacket datagram, int requestId)
            throws RadiusException {

        final ByteBuffer content = datagram.content().nioBuffer();
        if (content.remaining() < HEADER_LENGTH) {
            throw new RadiusException("readable bytes is less than header length");
        }

        int type = toUnsignedInt(content.get());
        int packetId = toUnsignedInt(content.get());
        int length = content.getShort();

        if (requestId != -1 && requestId != packetId)
            throw new RadiusException("bad packet: invalid packet identifier - request: " + requestId + ", response: " + packetId);
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

        return createRadiusPacket(dictionary, type, packetId, authenticator,
                extractAttributes(dictionary, -1, attributes, 0));
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
