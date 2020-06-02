package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * Base Radius Packet implementation without support for authenticators or encoding
 */
public abstract class BaseRadiusPacket implements RadiusPacket {

    private static final int CHILD_VENDOR_ID = -1;

    private final byte type;
    private final byte id;
    private final List<RadiusAttribute> attributes;
    private final byte[] authenticator;

    private final Dictionary dictionary;

    /**
     * Builds a Radius packet with the given type, identifier and attributes.
     * <p>
     * Use {@link RadiusPackets#createRequest(Dictionary, byte, byte, byte[], List)}
     * or {@link RadiusPackets#createResponse(Dictionary, byte, byte, byte[], List)}
     * where possible as that automatically creates Request/Response
     * variants as required.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param id            packet identifier
     * @param authenticator can be null if creating manually
     * @param attributes    list of RadiusAttribute objects (a shallow copy will be created)
     */
    public BaseRadiusPacket(Dictionary dictionary, byte type, byte id, byte[] authenticator, List<RadiusAttribute> attributes) {
        if (authenticator != null && authenticator.length != 16)
            throw new IllegalArgumentException("Authenticator must be 16 octets, actual: " + authenticator.length);

        this.type = type;
        this.id = id;
        this.authenticator = authenticator;
        this.attributes = new ArrayList<>(attributes); // catch nulls, avoid mutating original list
        this.dictionary = requireNonNull(dictionary, "Dictionary is null");
    }

    @Override
    public int getChildVendorId() {
        return CHILD_VENDOR_ID;
    }

    @Override
    public byte getId() {
        return id;
    }

    @Override
    public byte getType() {
        return type;
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

        final byte[] attributeBytes = getAttributeBytes();
        final int length = HEADER_LENGTH + attributeBytes.length;

        MessageDigest md5 = getMd5Digest();
        md5.update(getType());
        md5.update(getId());
        md5.update((byte) (length >> 8));
        md5.update((byte) (length & 0xff));
        md5.update(requestAuthenticator);
        md5.update(attributeBytes);
        return md5.digest(sharedSecret.getBytes(UTF_8));
    }

    protected static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e); // never happens
        }
    }

    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append(PacketType.getPacketTypeName(getType()));
        s.append(", ID ");
        s.append(id);
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
                id == that.id &&
                Objects.equals(attributes, that.attributes) &&
                Arrays.equals(authenticator, that.authenticator) &&
                Objects.equals(dictionary, that.dictionary);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, id, attributes, dictionary);
        result = 31 * result + Arrays.hashCode(authenticator);
        return result;
    }
}
