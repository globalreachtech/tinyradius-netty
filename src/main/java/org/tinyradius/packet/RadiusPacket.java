package org.tinyradius.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.attribute.AttributeType;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.VendorSpecificAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.attribute.VendorSpecificAttribute.VENDOR_SPECIFIC;

/**
 * A generic Radius packet. Subclasses provide convenience methods for special packet types.
 */
public class RadiusPacket {

    public static final int HEADER_LENGTH = 20;
    private static final int VENDOR_SPECIFIC_TYPE = 26;

    private final int type;
    private final int identifier;
    private final List<RadiusAttribute> attributes;
    private final byte[] authenticator;

    private final Dictionary dictionary;

    /**
     * Builds a Radius packet with the given type and identifier,
     * without attributes, and with null authenticator.
     * <p>
     * Use {@link RadiusPackets#create(Dictionary, int, int)}
     * where possible as that automatically creates Access/Accounting
     * variants as required.
     *
     * @param dictionary custom dictionary to use
     * @param type       packet type
     * @param identifier packet identifier
     */
    public RadiusPacket(Dictionary dictionary, int type, int identifier) {
        this(dictionary, type, identifier, null, new ArrayList<>());
    }

    /**
     * Builds a Radius packet with the given type and identifier
     * and without attributes.
     * <p>
     * Use {@link RadiusPackets#create(Dictionary, int, int, byte[])}
     * where possible as that automatically creates Access/Accounting
     * variants as required.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     */
    public RadiusPacket(Dictionary dictionary, int type, int identifier, byte[] authenticator) {
        this(dictionary, type, identifier, authenticator, new ArrayList<>());
    }

    /**
     * Builds a Radius packet with the given type and identifier
     * and without attributes.
     * <p>
     * Use {@link RadiusPackets#create(Dictionary, int, int, List)}
     * where possible as that automatically creates Access/Accounting
     * variants as required.
     *
     * @param dictionary custom dictionary to use
     * @param type       packet type
     * @param identifier packet identifier
     * @param attributes list of attributes for packet
     */
    public RadiusPacket(Dictionary dictionary, int type, int identifier, List<RadiusAttribute> attributes) {
        this(dictionary, type, identifier, null, attributes);
    }

    /**
     * Builds a Radius packet with the given type, identifier and attributes.
     * <p>
     * Use {@link RadiusPackets#create(Dictionary, int, int, byte[], List)}
     * where possible as that automatically creates Access/Accounting
     * variants as required.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param identifier    packet identifier
     * @param authenticator can be null if creating manually
     * @param attributes    list of RadiusAttribute objects
     */
    public RadiusPacket(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        if (type < 1 || type > 255)
            throw new IllegalArgumentException("packet type out of bounds: " + type);
        if (identifier < 0 || identifier > 255)
            throw new IllegalArgumentException("packet identifier out of bounds: " + identifier);
        if (authenticator != null && authenticator.length != 16)
            throw new IllegalArgumentException("authenticator must be 16 octets, actual: " + authenticator.length);

        this.type = type;
        this.identifier = identifier;
        this.authenticator = authenticator;
        this.attributes = new ArrayList<>(attributes); // catch nulls, avoid mutating original list
        this.dictionary = requireNonNull(dictionary, "dictionary is null");
    }

    /**
     * @return Radius packet identifier
     */
    public int getIdentifier() {
        return identifier;
    }

    /**
     * @return Radius packet type
     */
    public int getType() {
        return type;
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

        if (attribute.getVendorId() == -1) {
            this.attributes.add(
                    createAttribute(dictionary, attribute.getVendorId(), attribute.getType(), attribute.getValue()));
        } else {
            VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, attribute.getVendorId());
            vsa.addSubAttribute(attribute);
            this.attributes.add(vsa);
        }
    }

    /**
     * Adds a Radius attribute to this packet.
     * Uses AttributeTypes to lookup the type code and converts the value.
     * Can also be used to add sub-attributes.
     *
     * @param typeName name of the attribute, for example "NAS-Ip-Address", should NOT be 'Vendor-Specific'
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

        RadiusAttribute attribute = createAttribute(getDictionary(), type.getVendorId(), type.getTypeCode(), value);
        addAttribute(attribute);
    }

    /**
     * Removes the specified attribute from this packet.
     *
     * @param attribute RadiusAttribute to remove
     */
    public void removeAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() == -1 || attribute.getType() == VENDOR_SPECIFIC_TYPE) {
            this.attributes.remove(attribute);
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
     * Removes all attributes from this packet which have got the specified type.
     *
     * @param type attribute type to remove
     */
    public void removeAttributes(int type) {
        attributes.removeIf(a -> a.getType() == type);
    }

    /**
     * Removes the last occurrence of the attribute of the given
     * type from the packet.
     *
     * @param type attribute type code
     */
    public void removeLastAttribute(int type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        if (attrs == null || attrs.isEmpty())
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
            sas.removeIf(attr -> attr.getType() == typeCode && attr.getVendorId() == vendorId);
            if (sas.isEmpty())
                // removed the last sub-attribute --> remove the whole Vendor-Specific attribute
                removeAttribute(vsa);
        }
    }

    /**
     * Returns all attributes of this packet of the given type.
     * Returns an empty list if there are no such attributes.
     *
     * @param type type of attributes to get
     * @return list of RadiusAttribute objects, does not return null
     */
    public List<RadiusAttribute> getAttributes(int type) {
        return attributes.stream()
                .filter(a -> a.getType() == type)
                .collect(Collectors.toList());
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

        return getVendorAttributes(vendorId).stream()
                .map(VendorSpecificAttribute::getSubAttributes)
                .flatMap(Collection::stream)
                .filter(sa -> sa.getType() == attributeType && sa.getVendorId() == vendorId)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of all attributes belonging to this Radius packet.
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
     * @throws RuntimeException if there are multiple occurrences of the
     *                          requested attribute type
     */
    public RadiusAttribute getAttribute(int type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        if (attrs.size() > 1)
            throw new RuntimeException("multiple attributes of requested type " + type);

        return attrs.isEmpty() ? null : attrs.get(0);
    }

    /**
     * Returns a Radius attribute of the given type and vendor ID
     * which may only occur once in the Radius packet.
     *
     * @param vendorId vendor ID
     * @param type     attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     * @throws RuntimeException if there are multiple occurrences of the
     *                          requested attribute type
     */
    public RadiusAttribute getAttribute(int vendorId, int type) {
        if (vendorId == -1)
            return getAttribute(type);

        List<RadiusAttribute> attrs = getAttributes(vendorId, type);
        if (attrs.size() > 1)
            throw new RuntimeException("multiple attributes of requested type " + type);

        return attrs.isEmpty() ? null : attrs.get(0);
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
                null : attr.getValueString();
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
                .filter(a -> a.getVendorId() == vendorId)
                .collect(Collectors.toList());
    }

    /**
     * Encode request and generate authenticator. Should be idempotent.
     * <p>
     * Base implementation generates hashed authenticator.
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @return RadiusPacket with new authenticator and/or encoded attributes
     */
    public RadiusPacket encodeRequest(String sharedSecret) throws RadiusException {
        return encodeResponse(sharedSecret, new byte[16]);
    }

    /**
     * Encode and generate authenticator. Should be idempotent.
     * <p>
     * Requires request authenticator to generator response authenticator.
     *
     * @param sharedSecret         shared secret to be used to encode this packet
     * @param requestAuthenticator request packet authenticator
     * @return new RadiusPacket instance with same properties and valid authenticator
     */
    public RadiusPacket encodeResponse(String sharedSecret, byte[] requestAuthenticator) {
        final byte[] authenticator = createHashedAuthenticator(sharedSecret, requestAuthenticator);
        return RadiusPackets.create(dictionary, type, identifier, authenticator, attributes);
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
        return authenticator == null ? null : authenticator.clone();
    }

    /**
     * @return the dictionary this Radius packet uses.
     */
    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * Creates an authenticator for a Radius response packet.
     *
     * @param sharedSecret         shared secret
     * @param requestAuthenticator request packet authenticator
     * @return new 16 byte response authenticator
     */
    protected byte[] createHashedAuthenticator(String sharedSecret, byte[] requestAuthenticator) {
        requireNonNull(requestAuthenticator, "request authenticator cannot be null");
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("shared secret cannot be null/empty");

        byte[] attributes = getAttributeBytes();
        int packetLength = HEADER_LENGTH + attributes.length;

        MessageDigest md5 = getMd5Digest();
        md5.update((byte) getType());
        md5.update((byte) getIdentifier());
        md5.update((byte) (packetLength >> 8));
        md5.update((byte) (packetLength & 0xff));
        md5.update(requestAuthenticator);
        md5.update(attributes);
        return md5.digest(sharedSecret.getBytes(UTF_8));
    }

    /**
     * Checks the request authenticator against the supplied shared secret.
     *
     * @param sharedSecret         shared secret
     * @param requestAuthenticator should be set to request authenticator if verifying response,
     *                             otherwise set to 16 zero octets
     * @throws RadiusException if authenticator check fails
     */
    public void verify(String sharedSecret, byte[] requestAuthenticator) throws RadiusException {
        byte[] expectedAuth = createHashedAuthenticator(sharedSecret, requestAuthenticator);
        byte[] receivedAuth = getAuthenticator();
        if (receivedAuth.length != 16 ||
                !Arrays.equals(expectedAuth, receivedAuth))
            throw new RadiusException("Authenticator check failed (bad authenticator or shared secret)");
    }

    MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // never happen
        }
    }

    /**
     * Encodes the attributes of this Radius packet to a byte array.
     *
     * @return byte array with encoded attributes
     */
    protected byte[] getAttributeBytes() {
        final ByteBuf buffer = Unpooled.buffer();

        for (RadiusAttribute attribute : attributes) {
            buffer.writeBytes(attribute.toByteArray());
        }

        return buffer.copy().array();
    }

    /**
     * @return Map of attribute key-value
     */
    public Map<String, String> getAttributeMap() {
        final HashMap<String, String> map = new HashMap<>();
        attributes.forEach(a -> map.putAll(a.toAttributeMap()));
        return map;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(PacketType.getPacketTypeName(getType()));
        s.append(", ID ");
        s.append(identifier);
        for (RadiusAttribute attr : attributes) {
            s.append("\n");
            s.append(attr.toString());
        }
        return s.toString();
    }

    public RadiusPacket copy() {
        return RadiusPackets.create(getDictionary(), getType(), getIdentifier(), getAuthenticator(), getAttributes());
    }
}
