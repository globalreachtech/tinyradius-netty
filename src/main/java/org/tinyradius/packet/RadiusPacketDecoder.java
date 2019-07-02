package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.RadiusAttribute.createRadiusAttribute;

public class RadiusPacketDecoder {


    /**
     * Reads a Radius request packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param in           input stream to read from
     * @param sharedSecret shared secret to be used to decode this packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeRequestPacket(InputStream in, String sharedSecret) throws IOException, RadiusException {
        return decodePacket(DefaultDictionary.INSTANCE, in, sharedSecret, null);
    }

    /**
     * Reads a Radius response packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Checks the packet authenticator.
     *
     * @param in           input stream to read from
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius request packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeResponsePacket(InputStream in, String sharedSecret, RadiusPacket request) throws IOException, RadiusException {
        return decodePacket(request.getDictionary(), in, sharedSecret,
                requireNonNull(request, "request may not be null"));
    }

    /**
     * Reads a Radius request packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param dictionary   dictionary to use for attributes
     * @param in           InputStream to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeRequestPacket(Dictionary dictionary, InputStream in, String sharedSecret)
            throws IOException, RadiusException {
        return decodePacket(dictionary, in, sharedSecret, null);
    }

    /**
     * Reads a Radius response packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Checks the packet authenticator.
     *
     * @param dictionary   dictionary to use for attributes
     * @param in           InputStream to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius request packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeResponsePacket(Dictionary dictionary, InputStream in, String sharedSecret, RadiusPacket request)
            throws IOException, RadiusException {
        return decodePacket(dictionary, in, sharedSecret,
                requireNonNull(request, "request may not be null"));
    }

    /**
     * Reads a Radius packet from the given input stream and
     * creates an appropriate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param dictionary   dictionary to use for attributes
     * @param in           inputStream to read from
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius request packet if this is a response packet to be
     *                     decoded, null if this is a request packet to be decoded
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException packet malformed
     */
    protected static RadiusPacket decodePacket(Dictionary dictionary, InputStream in, String sharedSecret, RadiusPacket request)
            throws IOException, RadiusException {
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

        // check and count attributes
        int pos = 0;
        while (pos < attributeData.length) {
            if (pos + 1 >= attributeData.length)
                throw new RadiusException("bad packet: attribute length mismatch");
            int attributeLength = attributeData[pos + 1] & 0x0ff;
            if (attributeLength < 2)
                throw new RadiusException("bad packet: invalid attribute length");
            pos += attributeLength;
        }
        if (pos != attributeData.length)
            throw new RadiusException("bad packet: attribute length mismatch");

        // create RadiusPacket object; set properties
        RadiusPacket rp = createRadiusPacket(type, dictionary, identifier);
        rp.authenticator = authenticator;

        // load attributes
        pos = 0;
        while (pos + 1 < attributeData.length) { // pos+1 to avoid ArrayIndexOutOfBoundsException reading length
            int attributeType = attributeData[pos] & 0x0ff;
            int attributeLength = attributeData[pos + 1] & 0x0ff;
            RadiusAttribute a = createRadiusAttribute(dictionary, -1, attributeType);
            a.readAttribute(attributeData, pos);
            rp.addAttribute(a);
            pos += attributeLength;
        }

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

    /**
     * Creates a RadiusPacket object. Depending on the passed type, an
     * appropriate packet is created. Also sets the type, and the
     * the packet identifier.
     *
     * @param type       packet type
     * @param dictionary to use for packet
     * @return RadiusPacket object
     */
    public static RadiusPacket createRadiusPacket(final int type, Dictionary dictionary, int identifier) {
        requireNonNull(dictionary, "dictionary cannot be null");

        RadiusPacket rp;
        switch (type) {
            case PacketType.ACCESS_REQUEST:
                rp = new AccessRequest(identifier);
                break;
            case PacketType.COA_REQUEST:
                rp = new CoaRequest(PacketType.COA_REQUEST, identifier);
                break;
            case PacketType.DISCONNECT_REQUEST:
                rp = new CoaRequest(PacketType.DISCONNECT_REQUEST, identifier);
                break;
            case PacketType.ACCOUNTING_REQUEST:
                rp = new AccountingRequest(identifier);
                break;
            default:
                rp = new RadiusPacket(type, identifier);
        }

        rp.setDictionary(dictionary);
        return rp;
    }
}
