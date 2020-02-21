package org.tinyradius.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.attribute.AttributeHolder;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.VendorSpecificAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * A generic Radius packet. Subclasses provide convenience methods for special packet types.
 */
public class RadiusPacket implements AttributeHolder {

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
    @Override
    public void addAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() == getVendorId() || attribute.getType() == VENDOR_SPECIFIC_TYPE) {
            attributes.add(attribute);
        } else {
            VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, attribute.getVendorId());
            vsa.addAttribute(attribute);
            attributes.add(vsa);
        }
    }

    /**
     * Removes the specified attribute from this packet.
     *
     * @param attribute RadiusAttribute to remove
     */
    @Override
    public void removeAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() == getVendorId() || attribute.getType() == VENDOR_SPECIFIC_TYPE) {
            attributes.remove(attribute);
        } else {
            // remove Vendor-Specific sub-attribute
            for (VendorSpecificAttribute vsa : getVendorSpecificAttributes(attribute.getVendorId())) {
                vsa.removeAttribute(attribute);
                if (vsa.getAttributes().isEmpty())
                    // removed the last sub-attribute --> remove the whole Vendor-Specific attribute
                    attributes.remove(vsa);
            }
        }
    }

    /**
     * Returns a list of all attributes belonging to this Radius packet.
     *
     * @return List of RadiusAttribute objects
     */
    @Override
    public List<RadiusAttribute> getAttributes() {
        return attributes;
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
    public RadiusPacket encodeRequest(String sharedSecret) {
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
        requireNonNull(requestAuthenticator, "Authenticator cannot be null");
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

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
     * @throws RadiusPacketException if authenticator check fails
     */
    public void verify(String sharedSecret, byte[] requestAuthenticator) throws RadiusPacketException {
        byte[] expectedAuth = createHashedAuthenticator(sharedSecret, requestAuthenticator);
        byte[] receivedAuth = getAuthenticator();
        if (receivedAuth.length != 16 ||
                !Arrays.equals(expectedAuth, receivedAuth))
            throw new RadiusPacketException("Authenticator check failed (bad authenticator or shared secret)");
    }

    static MessageDigest getMd5Digest() {
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

    @Override
    public int getVendorId() {
        return -1;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RadiusPacket)) return false;
        RadiusPacket that = (RadiusPacket) o;
        return type == that.type &&
                identifier == that.identifier &&
                Objects.equals(attributes, that.attributes) &&
                Arrays.equals(authenticator, that.authenticator) &&
                Objects.equals(dictionary, that.dictionary);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, identifier, attributes, dictionary);
        result = 31 * result + Arrays.hashCode(authenticator);
        return result;
    }

    public RadiusPacket copy() {
        return RadiusPackets.create(getDictionary(), getType(), getIdentifier(), getAuthenticator(), new ArrayList<>(getAttributes()));
    }
}
