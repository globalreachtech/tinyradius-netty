package org.tinyradius.packet;

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.AttributeBuilder;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;
import static org.tinyradius.packet.PacketType.COA_REQUEST;
import static org.tinyradius.packet.PacketType.DISCONNECT_REQUEST;

public class RadiusPacketDecoder {

    private static AtomicInteger nextPacketId = new AtomicInteger();

    /**
     * Increment the next packet identifier to use.
     *
     * @return the next packet identifier to use
     */
    public static int getNextPacketIdentifier() {
        return nextPacketId.updateAndGet(i -> i >= 255 ? 0 : i + 1);
    }

    /**
     * Reads a Radius request packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param packet       DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeRequestPacket(DatagramPacket packet, String sharedSecret) throws IOException, RadiusException {
        return decodePacket(DefaultDictionary.INSTANCE, packet, sharedSecret, null);
    }

    /**
     * Reads a Radius response packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Checks the packet authenticator.
     *
     * @param packet       DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius request packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeResponsePacket(DatagramPacket packet, String sharedSecret, RadiusPacket request) throws IOException, RadiusException {
        return decodePacket(request.getDictionary(), packet, sharedSecret,
                requireNonNull(request, "request may not be null"));
    }

    /**
     * Reads a Radius request packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param dictionary   dictionary to use for attributes
     * @param packet       DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeRequestPacket(Dictionary dictionary, DatagramPacket packet, String sharedSecret)
            throws IOException, RadiusException {
        return decodePacket(dictionary, packet, sharedSecret, null);
    }

    /**
     * Reads a Radius response packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Checks the packet authenticator.
     *
     * @param dictionary   dictionary to use for attributes
     * @param packet       DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius request packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeResponsePacket(Dictionary dictionary, DatagramPacket packet, String sharedSecret, RadiusPacket request)
            throws IOException, RadiusException {
        return decodePacket(dictionary, packet, sharedSecret,
                requireNonNull(request, "request may not be null"));
    }

    /**
     * Reads a Radius packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param dictionary   dictionary to use for attributes
     * @param packet       DatagramPacket to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius request packet if this is a response packet to be
     *                     decoded, null if this is a request packet to be decoded
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException packet malformed
     */
    protected static RadiusPacket decodePacket(Dictionary dictionary, DatagramPacket packet, String sharedSecret, RadiusPacket request)
            throws IOException, RadiusException {

        try (ByteBufInputStream in = new ByteBufInputStream(packet.content())) {
            // check shared secret
            if (sharedSecret == null || sharedSecret.isEmpty())
                throw new RuntimeException("no shared secret has been set");

            // check request authenticator
            if (request != null && request.getAuthenticator() == null)
                throw new RuntimeException("request authenticator not set");

            // read and check header
            int type = in.read() & 0x0ff;
            int identifier = in.read() & 0x0ff;
            int length = (in.read() & 0x0ff) << 8 | (in.read() & 0x0ff);

            if (request != null && request.getPacketIdentifier() != identifier)
                throw new RadiusException("bad packet: invalid packet identifier (request: " + request.getPacketIdentifier() + ", response: " + identifier);
            if (length < RadiusPacket.RADIUS_HEADER_LENGTH)
                throw new RadiusException("bad packet: packet too short (" + length + " bytes)");
            if (length > RadiusPacket.MAX_PACKET_LENGTH)
                throw new RadiusException("bad packet: packet too long (" + length + " bytes)");

            // read rest of packet
            byte[] authenticator = new byte[16];
            byte[] attributeData = new byte[length - RadiusPacket.RADIUS_HEADER_LENGTH];
            in.read(authenticator);
            in.read(attributeData);

            final List<RadiusAttribute> attributes = extractAttributes(attributeData, dictionary);

            // create RadiusPacket object
            RadiusPacket rp = createRadiusPacket(dictionary, type, identifier, authenticator, attributes);

            if (request == null) {
                // decode attributes
                rp.decodeRequestAttributes(sharedSecret);
                rp.checkRequestAuthenticator(sharedSecret, length, attributeData);
            } else {
                // response packet: check authenticator
                rp.checkResponseAuthenticator(sharedSecret, length, attributeData, request.getAuthenticator());
            }

            return rp;
        }
    }

    private static List<RadiusAttribute> extractAttributes(byte[] attributeData, Dictionary dictionary) throws RadiusException {
        List<RadiusAttribute> attributes = new ArrayList<>();

        int pos = 0;
        while (pos < attributeData.length) {
            if (pos + 1 >= attributeData.length)
                throw new RadiusException("bad packet: attribute length out of bounds");
            int attributeType = attributeData[pos] & 0x0ff;
            int attributeLength = attributeData[pos + 1] & 0x0ff;
            if (attributeLength < 2)
                throw new RadiusException("bad packet: invalid attribute length");
            RadiusAttribute a = AttributeBuilder.parseRadiusAttribute(dictionary, -1, attributeType, attributeData, pos);
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
            case PacketType.ACCESS_REQUEST:
                return new AccessRequest(dictionary, identifier, authenticator, attributes);
            case COA_REQUEST:
                return new CoaRequest(dictionary, COA_REQUEST, identifier, authenticator, attributes);
            case DISCONNECT_REQUEST:
                return new CoaRequest(dictionary, DISCONNECT_REQUEST, identifier, authenticator, attributes);
            case PacketType.ACCOUNTING_REQUEST:
                return new AccountingRequest(dictionary, identifier, authenticator, attributes);
            default:
                return new RadiusPacket(dictionary, type, identifier, authenticator, attributes);
        }
    }
}
