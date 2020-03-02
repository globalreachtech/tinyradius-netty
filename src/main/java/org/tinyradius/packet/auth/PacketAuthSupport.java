package org.tinyradius.packet.auth;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public interface PacketAuthSupport extends RadiusPacket {

    /**
     * @param sharedSecret shared secret
     * @param auth         should be set to request authenticator if verifying response,
     *                     otherwise set to 16 zero octets
     * @throws RadiusPacketException if authenticator check fails
     */
    default void verifyPacketAuth(String sharedSecret, byte[] auth) throws RadiusPacketException {
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
    default byte[] createHashedAuthenticator(String sharedSecret, byte[] requestAuthenticator) {
        requireNonNull(requestAuthenticator, "Authenticator cannot be null");
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        final byte[] attributes = getAttributeBytes();
        final int length = HEADER_LENGTH + attributes.length;

        MessageDigest md5 = getMd5Digest();
        md5.update((byte) getType());
        md5.update((byte) getIdentifier());
        md5.update((byte) (length >> 8));
        md5.update((byte) (length & 0xff));
        md5.update(requestAuthenticator);
        md5.update(attributes);
        return md5.digest(sharedSecret.getBytes(UTF_8));
    }

    default MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // never happens
        }
    }
}
