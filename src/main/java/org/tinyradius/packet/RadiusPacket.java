package org.tinyradius.packet;

import org.tinyradius.attribute.NestedAttributeHolder;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public interface RadiusPacket<T extends RadiusPacket<T>> extends NestedAttributeHolder<T> {

    static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e); // never happens
        }
    }

    /**
     * @return Radius packet type
     */
    byte getType();

    /**
     * @return Radius packet identifier
     */
    byte getId();

    /**
     * Returns the authenticator for this Radius packet.
     * <p>
     * For a Radius packet read from a stream, this will return the
     * authenticator sent by the server.
     * <p>
     * For a new Radius packet to be sent, this will return the authenticator created,
     * or null if no authenticator has been created yet.
     *
     * @return authenticator, 16 bytes
     */
    byte[] getAuthenticator();

    /**
     * @return list of RadiusAttributes in packet
     */
    @Override
    List<RadiusAttribute> getAttributes();

    /**
     * @return the dictionary this Radius packet uses.
     */
    @Override
    Dictionary getDictionary();

    /**
     * @param sharedSecret shared secret
     * @param auth         should be set to request authenticator if verifying response,
     *                     otherwise set to 16 zero octets
     * @throws RadiusPacketException if authenticator check fails
     */
    default void verifyPacketAuth(String sharedSecret, byte[] auth) throws RadiusPacketException {
        final byte[] expectedAuth = genHashedAuth(sharedSecret, auth);
        final byte[] receivedAuth = getAuthenticator();
        if (receivedAuth == null)
            throw new RadiusPacketException("Authenticator check failed - authenticator missing");

        if (receivedAuth.length != 16 || !Arrays.equals(expectedAuth, receivedAuth))
            throw new RadiusPacketException("Authenticator check failed - bad authenticator or shared secret");
    }

    /**
     * Creates an authenticator for a Radius response packet.
     *
     * @param sharedSecret         shared secret
     * @param requestAuth request packet authenticator
     * @return new 16 byte response authenticator
     */
    default byte[] genHashedAuth(String sharedSecret, byte[] requestAuth) {
        requireNonNull(requestAuth, "Authenticator cannot be null");
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        final int HEADER_LENGTH = 20;
        final byte[] attributeBytes = getAttributeBytes();
        final int length = HEADER_LENGTH + attributeBytes.length;

        MessageDigest md5 = getMd5Digest();
        md5.update(getType());
        md5.update(getId());
        md5.update((byte) (length >> 8));
        md5.update((byte) (length & 0xff));
        md5.update(requestAuth);
        md5.update(attributeBytes);
        return md5.digest(sharedSecret.getBytes(UTF_8));
    }
}
