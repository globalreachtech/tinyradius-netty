package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.VendorSpecificAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.util.PacketType;
import org.tinyradius.packet.util.RadiusPackets;
import org.tinyradius.util.RadiusPacketException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.lang.Byte.toUnsignedInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * Base Radius Packet implementation without support for authenticators or encoding
 */
public abstract class BaseRadiusPacket implements RadiusPacket {

    private static final int VENDOR_SPECIFIC_TYPE = 26;

    private final byte type;
    private final byte identifier;
    private final List<RadiusAttribute> attributes;
    private final byte[] authenticator;

    private final Dictionary dictionary;

    /**
     * Builds a Radius packet with the given type, identifier and attributes.
     * <p>
     * Use {@link RadiusPackets#createRequest(Dictionary, byte, byte, byte[], List)}
     * or {@link RadiusPackets#createResponse(Dictionary, byte, byte, byte[], List)}
     * where possible as that automatically creates Access/Accounting
     * variants as required.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param identifier    packet identifier
     * @param authenticator can be null if creating manually
     * @param attributes    list of RadiusAttribute objects
     */
    public BaseRadiusPacket(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        if (authenticator != null && authenticator.length != 16)
            throw new IllegalArgumentException("Authenticator must be 16 octets, actual: " + authenticator.length);

        this.type = type;
        this.identifier = identifier;
        this.authenticator = authenticator;
        this.attributes = new ArrayList<>(attributes); // catch nulls, avoid mutating original list
        this.dictionary = requireNonNull(dictionary, "Dictionary is null");
    }


    @Override
    public byte getIdentifier() {
        return identifier;
    }

    @Override
    public byte getType() {
        return type;
    }

    public int getIdentifierInt() {
        return toUnsignedInt(identifier);
    }

    public int getTypeInt() {
        return toUnsignedInt(type);
    }

    @Override
    public List<RadiusAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public byte[] getAuthenticator() {
        return authenticator == null ? null : authenticator.clone();
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
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
            removeSubAttribute(attribute);
        }
    }

    private void removeSubAttribute(RadiusAttribute attribute) {
        for (VendorSpecificAttribute vsa : getVendorSpecificAttributes(attribute.getVendorId())) {
            vsa.removeAttribute(attribute);
            if (vsa.getAttributes().isEmpty())
                // removed the last sub-attribute --> remove the whole Vendor-Specific attribute
                attributes.remove(vsa);
        }
    }

    /**
     * @param sharedSecret shared secret
     * @param auth         should be set to request authenticator if verifying response,
     *                     otherwise set to 16 zero octets
     * @throws RadiusPacketException if authenticator check fails
     */
    protected void verifyPacketAuth(String sharedSecret, byte[] auth) throws RadiusPacketException {
        byte[] expectedAuth = createHashedAuthenticator(sharedSecret, auth);
        byte[] receivedAuth = getAuthenticator();
        if (receivedAuth.length != 16 ||
                !Arrays.equals(expectedAuth, receivedAuth))
            throw new RadiusPacketException("Authenticator check failed (bad authenticator or shared secret)");
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

        final byte[] attributes = getAttributeBytes();
        final int length = HEADER_LENGTH + attributes.length;

        MessageDigest md5 = getMd5Digest();
        md5.update(getType());
        md5.update(getIdentifier());
        md5.update((byte) (length >> 8));
        md5.update((byte) (length & 0xff));
        md5.update(requestAuthenticator);
        md5.update(attributes);
        return md5.digest(sharedSecret.getBytes(UTF_8));
    }

    protected static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // never happens
        }
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
        if (!(o instanceof BaseRadiusPacket)) return false;
        BaseRadiusPacket that = (BaseRadiusPacket) o;
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
}
