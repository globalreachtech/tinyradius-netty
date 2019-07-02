package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.VendorSpecificAttribute;
import org.tinyradius.dictionary.AttributeType;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.RadiusAttribute.createRadiusAttribute;
import static org.tinyradius.attribute.VendorSpecificAttribute.VENDOR_SPECIFIC;

/**
 * This class represents a Radius packet. Subclasses provide convenience methods
 * for special packet types.
 */
public class RadiusPacket {

    public static final int MAX_PACKET_LENGTH = 4096;
    public static final int RADIUS_HEADER_LENGTH = 20;

    private final int packetType;

    private int packetIdentifier = 0;

    private final List<RadiusAttribute> attributes;

    byte[] authenticator = null;

    /**
     * Dictionary to look up attribute names.
     */
    private Dictionary dictionary = DefaultDictionary.INSTANCE;

    /**
     * Next packet identifier.
     */
    private static AtomicInteger nextPacketId = new AtomicInteger();

    private static final SecureRandom random = new SecureRandom();

    /**
     * Builds a Radius packet without attributes. Retrieves
     * the next packet identifier.
     *
     * @param type packet type
     */
    public RadiusPacket(final int type) {
        this(type, getNextPacketIdentifier());
    }

    /**
     * Builds a Radius packet with the given type and identifier
     * and without attributes.
     *
     * @param type       packet type
     * @param identifier packet identifier
     */
    public RadiusPacket(final int type, final int identifier) {
        this(type, identifier, new ArrayList<>());
    }

    /**
     * Builds a Radius packet with the given type, identifier and
     * attributes.
     *
     * @param type       packet type
     * @param identifier packet identifier
     * @param attributes list of RadiusAttribute objects
     */
    public RadiusPacket(final int type, final int identifier, final List<RadiusAttribute> attributes) {
        if (type < 1 || type > 255)
            throw new IllegalArgumentException("packet type out of bounds");
        this.packetType = type;
        setPacketIdentifier(identifier);
        this.attributes = requireNonNull(attributes, "attributes list is null");
    }

    /**
     * Returns the packet identifier for this Radius packet.
     *
     * @return packet identifier
     */
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    /**
     * Sets the packet identifier for this Radius packet.
     *
     * @param identifier packet identifier, 0-255
     */
    public void setPacketIdentifier(int identifier) {
        if (identifier < 0 || identifier > 255)
            throw new IllegalArgumentException("packet identifier out of bounds");
        this.packetIdentifier = identifier;
    }

    /**
     * Returns the type of this Radius packet.
     *
     * @return packet type
     */
    public int getPacketType() {
        return packetType;
    }

    /**
     * Adds a Radius attribute to this packet. Can also be used
     * to add Vendor-Specific sub-attributes. If a attribute with
     * a vendor code != -1 is passed in, a VendorSpecificAttribute
     * is created for the sub-attribute.
     *
     * @param attribute RadiusAttribute object
     */
    public void addAttribute(RadiusAttribute attribute) {
        requireNonNull(attributes, "attribute is null");

        attribute.setDictionary(dictionary);
        if (attribute.getVendorId() == -1)
            this.attributes.add(attribute);
        else {
            VendorSpecificAttribute vsa = new VendorSpecificAttribute(attribute.getVendorId());
            vsa.addSubAttribute(attribute);
            this.attributes.add(vsa);
        }
    }

    /**
     * Adds a Radius attribute to this packet.
     * Uses AttributeTypes to lookup the type code and converts
     * the value.
     * Can also be used to add sub-attributes.
     *
     * @param typeName name of the attribute, for example "NAS-Ip-Address"
     * @param value    value of the attribute, for example "127.0.0.1"
     * @throws IllegalArgumentException if type name is unknown
     */
    public void addAttribute(String typeName, String value) {
        if (typeName == null || typeName.isEmpty())
            throw new IllegalArgumentException("type name is empty");
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("value is empty");

        AttributeType type = dictionary.getAttributeTypeByName(typeName);
        if (type == null)
            throw new IllegalArgumentException("unknown attribute type '" + typeName + "'");

        RadiusAttribute attribute = createRadiusAttribute(getDictionary(), type.getVendorId(), type.getTypeCode());
        attribute.setAttributeValue(value);
        addAttribute(attribute);
    }

    /**
     * Removes the specified attribute from this packet.
     *
     * @param attribute RadiusAttribute to remove
     */
    public void removeAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() == -1) {
            if (!this.attributes.remove(attribute))
                throw new IllegalArgumentException("no such attribute");
        } else {
            // remove Vendor-Specific sub-attribute
            List<VendorSpecificAttribute> vsas = getVendorAttributes(attribute.getVendorId());
            for (VendorSpecificAttribute vsa : vsas) {
                vsa.removeSubAttribute(attribute);
                if (vsa.getSubAttributes().isEmpty())
                    // removed the last sub-attribute --> remove the whole Vendor-Specific attribute
                    removeAttribute(vsa);
            }
        }
    }

    /**
     * Removes all attributes from this packet which have got
     * the specified type.
     *
     * @param type attribute type to remove
     */
    public void removeAttributes(int type) {
        if (type < 1 || type > 255)
            throw new IllegalArgumentException("attribute type out of bounds");

        attributes.removeIf(attribute -> attribute.getAttributeType() == type);
    }

    /**
     * Removes the last occurence of the attribute of the given
     * type from the packet.
     *
     * @param type attribute type code
     */
    public void removeLastAttribute(int type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        if (attrs == null || attrs.size() == 0)
            return;

        removeAttribute(attrs.get(attrs.size() - 1));
    }

    /**
     * Removes all sub-attributes of the given vendor and
     * type.
     *
     * @param vendorId vendor ID
     * @param typeCode attribute type code
     */
    public void removeAttributes(int vendorId, int typeCode) {
        if (vendorId == -1) {
            removeAttributes(typeCode);
            return;
        }

        List<VendorSpecificAttribute> vsas = getVendorAttributes(vendorId);
        for (VendorSpecificAttribute vsa : vsas) {
            List<RadiusAttribute> sas = vsa.getSubAttributes();
            sas.removeIf(attr -> attr.getAttributeType() == typeCode && attr.getVendorId() == vendorId);
            if (sas.isEmpty())
                // removed the last sub-attribute --> remove the whole Vendor-Specific attribute
                removeAttribute(vsa);
        }
    }

    /**
     * Returns all attributes of this packet of the given type.
     * Returns an empty list if there are no such attributes.
     *
     * @param attributeType type of attributes to get
     * @return list of RadiusAttribute objects, does not return null
     */
    public List<RadiusAttribute> getAttributes(int attributeType) {
        if (attributeType < 1 || attributeType > 255)
            throw new IllegalArgumentException("attribute type out of bounds");

        List<RadiusAttribute> result = new LinkedList<>();
        for (RadiusAttribute attribute : attributes) {
            if (attributeType == attribute.getAttributeType())
                result.add(attribute);
        }
        return result;
    }

    /**
     * Returns all attributes of this packet that have got the
     * given type and belong to the given vendor ID.
     * Returns an empty list if there are no such attributes.
     *
     * @param vendorId      vendor ID
     * @param attributeType attribute type code
     * @return list of RadiusAttribute objects, never null
     */
    public List<RadiusAttribute> getAttributes(int vendorId, int attributeType) {
        if (vendorId == -1)
            return getAttributes(attributeType);

        List<RadiusAttribute> result = new LinkedList<>();
        List<VendorSpecificAttribute> vsas = getVendorAttributes(vendorId);
        for (VendorSpecificAttribute vsa : vsas) {
            for (RadiusAttribute sa : vsa.getSubAttributes()) {
                if (sa.getAttributeType() == attributeType &&
                        sa.getVendorId() == vendorId)
                    result.add(sa);
            }
        }

        return result;
    }

    /**
     * Returns a list of all attributes belonging to this Radius
     * packet.
     *
     * @return List of RadiusAttribute objects
     */
    public List<RadiusAttribute> getAttributes() {
        return attributes;
    }

    /**
     * Returns a Radius attribute of the given type which may only occur once
     * in the Radius packet.
     *
     * @param type attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     * @throws RuntimeException if there are multiple occurences of the
     *                          requested attribute type
     */
    public RadiusAttribute getAttribute(int type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        if (attrs.size() > 1)
            throw new RuntimeException("multiple attributes of requested type " + type);

        return attrs.size() == 0 ?
                null : attrs.get(0);
    }

    /**
     * Returns a Radius attribute of the given type and vendor ID
     * which may only occur once in the Radius packet.
     *
     * @param vendorId vendor ID
     * @param type     attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     * @throws RuntimeException if there are multiple occurences of the
     *                          requested attribute type
     */
    public RadiusAttribute getAttribute(int vendorId, int type) {
        if (vendorId == -1)
            return getAttribute(type);

        List<RadiusAttribute> attrs = getAttributes(vendorId, type);
        if (attrs.size() > 1)
            throw new RuntimeException("multiple attributes of requested type " + type);

        return attrs.size() == 0 ?
                null : attrs.get(0);
    }

    /**
     * Returns a single Radius attribute of the given type name.
     * Also returns sub-attributes.
     *
     * @param type attribute type name
     * @return RadiusAttribute object or null if there is no such attribute
     * @throws RuntimeException if the attribute occurs multiple times
     */
    public RadiusAttribute getAttribute(String type) {
        if (type == null || type.isEmpty())
            throw new IllegalArgumentException("type name is empty");

        AttributeType t = dictionary.getAttributeTypeByName(type);
        if (t == null)
            throw new IllegalArgumentException("unknown attribute type name '" + type + "'");

        return getAttribute(t.getVendorId(), t.getTypeCode());
    }

    /**
     * Returns the value of the Radius attribute of the given type or
     * null if there is no such attribute.
     * Also returns sub-attributes.
     *
     * @param type attribute type name
     * @return value of the attribute as a string or null if there
     * is no such attribute
     * @throws IllegalArgumentException if the type name is unknown
     * @throws RuntimeException         attribute occurs multiple times
     */
    public String getAttributeValue(String type) {
        RadiusAttribute attr = getAttribute(type);
        return attr == null ?
                null : attr.getAttributeValue();
    }

    /**
     * Returns the Vendor-Specific attribute(s) for the given vendor ID.
     *
     * @param vendorId vendor ID of the attribute(s)
     * @return List with VendorSpecificAttribute objects, never null
     */
    public List<VendorSpecificAttribute> getVendorAttributes(int vendorId) {
        return getAttributes(VENDOR_SPECIFIC).stream()
                .filter(VendorSpecificAttribute.class::isInstance)
                .map(VendorSpecificAttribute.class::cast)
                .filter(a -> a.getChildVendorId() == vendorId)
                .collect(Collectors.toList());
    }

    /**
     * Encodes this Radius packet and sends it to the specified output stream.
     *
     * @param out          output stream to use
     * @param sharedSecret shared secret to be used to encode this packet
     * @throws IOException     communication error
     * @throws RadiusException malformed packet
     */
    public void encodeRequestPacket(OutputStream out, String sharedSecret) throws IOException, RadiusException {
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("no shared secret has been set");

        encodeRequest(sharedSecret);
        writeOutput(out);
    }

    /**
     * Encodes this Radius response packet and sends it to the specified output
     * stream.
     *
     * @param out                  output stream to use
     * @param sharedSecret         shared secret to be used to encode this packet
     * @param requestAuthenticator Radius request packet authenticator
     * @throws IOException communication error
     */
    public void encodeResponsePacket(OutputStream out, String sharedSecret, byte[] requestAuthenticator) throws IOException {
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("no shared secret has been set");
        requireNonNull(requestAuthenticator, "request authenticator not set");

        encodePacket(sharedSecret, requestAuthenticator);
        writeOutput(out);
    }

    /**
     * Retrieves the next packet identifier to use and increments the static
     * storage.
     *
     * @return the next packet identifier to use
     */
    public static int getNextPacketIdentifier() {
        return nextPacketId.updateAndGet(i -> i >= 255 ? 0 : i + 1);
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(PacketType.getPacketTypeName(getPacketType()));
        s.append(", ID ");
        s.append(packetIdentifier);
        for (RadiusAttribute attr : attributes) {
            s.append("\n");
            s.append(attr.toString());
        }
        return s.toString();
    }

    /**
     * Returns the authenticator for this Radius packet.
     * For a Radius packet read from a stream, this will return the
     * authenticator sent by the server. For a new Radius packet to be sent,
     * this will return the authenticator created and will return null if no authenticator
     * has been created yet.
     *
     * @return authenticator, 16 bytes
     */
    public byte[] getAuthenticator() {
        return authenticator;
    }

    /**
     * Sets the authenticator to be used for this Radius packet.
     * This method should seldomly be used.
     * Authenticators are created and managed by this class internally.
     *
     * @param authenticator authenticator
     */
    public void setAuthenticator(byte[] authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * @return the dictionary this Radius packet uses.
     */
    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * Sets a custom dictionary to use. If no dictionary is set,
     * the default dictionary is used.
     * Also copies the dictionary to the attributes.
     *
     * @param dictionary Dictionary class to use
     * @see DefaultDictionary
     */
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
        attributes.forEach(a -> a.setDictionary(dictionary));
    }

    private void writeOutput(OutputStream out) throws IOException {
        byte[] attributes = getAttributeBytes();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeByte(getPacketType());
        dos.writeByte(getPacketIdentifier());
        dos.writeShort(RADIUS_HEADER_LENGTH + attributes.length);
        dos.write(getAuthenticator());
        dos.write(attributes);
        dos.flush();
    }

    /**
     * Encode request and generate authenticator if required.
     * <p>
     * AccountingRequest/CoA/DisconnectRequest overrides this method to create a hash authenticator as per RFC 2866.
     * <p>
     * AccessRequest overrides this method to generate a randomized authenticator as per RFC 2865
     * and encode required attributes (i.e. User-Password).
     * <p>
     * Base implementation in RadiusPacket generates randomized authenticator as used by
     * Access-Request/Status-Server. As more packet types are supported, consider changing
     * the default encodeRequest() method.
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @throws RadiusException malformed packet
     * @throws IOException     error writing data
     */
    protected void encodeRequest(String sharedSecret) throws RadiusException, IOException {
        byte[] attributes = getAttributeBytes();
        int packetLength = RADIUS_HEADER_LENGTH + attributes.length;
        if (packetLength > MAX_PACKET_LENGTH)
            throw new RuntimeException("packet too long");

        if (authenticator == null)
            authenticator = generateRandomizedAuthenticator(sharedSecret);
    }

    /**
     * Encodes packet by checking packet length is legal and setting authenticator
     * to MD5 hash of requestAuthenticator and current packet. Should be idempotent.
     * <p>
     * Used for encoding responses, and request types which generate the
     * authenticator by hashing (e.g. AcountingRequest, CoaRequest, DisconnectRequest)
     *
     * @param sharedSecret         shared secret that secures the communication
     *                             with the other Radius server/client
     * @param requestAuthenticator request authenticator if encoding responses, or set to
     *                             16 zero octets if encoding requests
     * @throws IOException error writing data
     */
    protected void encodePacket(String sharedSecret, byte[] requestAuthenticator) throws IOException {
        byte[] attributes = getAttributeBytes();
        int packetLength = RADIUS_HEADER_LENGTH + attributes.length;
        if (packetLength > MAX_PACKET_LENGTH)
            throw new RuntimeException("packet too long");

        authenticator = createHashedAuthenticator(sharedSecret, packetLength, attributes, requestAuthenticator);
    }

    /**
     * Generates a request authenticator for this packet. This request authenticator
     * is constructed as described in RFC 2865.
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @return request authenticator, 16 bytes
     */
    protected byte[] generateRandomizedAuthenticator(String sharedSecret) {
        byte[] secretBytes = sharedSecret.getBytes(UTF_8);
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);

        MessageDigest md5 = getMd5Digest();
        md5.update(secretBytes);
        return md5.digest(randomBytes);
    }

    /**
     * Creates an authenticator for a Radius response packet.
     *
     * @param sharedSecret         shared secret
     * @param packetLength         length of response packet
     * @param attributes           encoded attributes of response packet
     * @param requestAuthenticator request packet authenticator
     * @return new 16 byte response authenticator
     */
    protected byte[] createHashedAuthenticator(String sharedSecret, int packetLength, byte[] attributes, byte[] requestAuthenticator) {
        MessageDigest md5 = getMd5Digest();
        md5.update((byte) getPacketType());
        md5.update((byte) getPacketIdentifier());
        md5.update((byte) (packetLength >> 8));
        md5.update((byte) (packetLength & 0x0ff));
        md5.update(requestAuthenticator);
        md5.update(attributes);
        return md5.digest(sharedSecret.getBytes(UTF_8));
    }

    /**
     * Checks the request authenticator against the supplied shared secret.
     * Overridden by AccountingRequest to handle special accounting request
     * authenticators. There is no way to check request authenticators for
     * authentication requests as they contain secret bytes.
     *
     * @param sharedSecret shared secret
     * @param packetLength total length of the packet
     * @param attributes   request attribute data
     * @throws RadiusException malformed packet
     */
    protected void checkRequestAuthenticator(String sharedSecret, int packetLength, byte[] attributes) throws RadiusException {
    }

    /**
     * Can be overridden to decode encoded request attributes such as
     * User-Password. This method may use getAuthenticator() to get the
     * request authenticator.
     *
     * @param sharedSecret used for decoding
     * @throws RadiusException malformed packet
     */
    protected void decodeRequestAttributes(String sharedSecret) throws RadiusException {
    }

    /**
     * This method checks the authenticator of this Radius packet. This method
     * may be overridden to include special attributes in the authenticator check.
     *
     * @param sharedSecret         shared secret to be used to encrypt the authenticator
     * @param packetLength         length of the response packet
     * @param attributes           attribute data of the response packet
     * @param requestAuthenticator 16 bytes authenticator of the request packet belonging
     *                             to this response packet
     * @throws RadiusException malformed packet
     */
    protected void checkResponseAuthenticator(String sharedSecret, int packetLength, byte[] attributes, byte[] requestAuthenticator)
            throws RadiusException {
        byte[] authenticator = createHashedAuthenticator(sharedSecret, packetLength, attributes, requestAuthenticator);
        byte[] receivedAuth = getAuthenticator();
        for (int i = 0; i < 16; i++)
            if (authenticator[i] != receivedAuth[i])
                throw new RadiusException("response authenticator invalid");
    }

    protected MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae); // never happen
        }
    }

    /**
     * Encodes the attributes of this Radius packet to a byte array.
     *
     * @return byte array with encoded attributes
     * @throws IOException error writing data
     */
    protected byte[] getAttributeBytes() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(MAX_PACKET_LENGTH);
        for (RadiusAttribute a : attributes)
            bos.write(a.writeAttribute());

        bos.flush();
        return bos.toByteArray();
    }
}
