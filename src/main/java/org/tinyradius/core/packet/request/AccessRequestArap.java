package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import jdk.jfr.Experimental;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tinyradius.core.attribute.AttributeTypes.ARAP_PASSWORD;

/**
 * ARAP AccessRequest RFC2869
 */
@Experimental
public class AccessRequestArap extends AccessRequest {

    /**
     * Constructs an AccessRequestArap.
     *
     * @param dictionary the dictionary to use
     * @param header     the packet header
     * @param attributes the packet attributes
     * @throws RadiusPacketException if there is an error creating the request
     */
    public AccessRequestArap(@NonNull Dictionary dictionary, @NonNull ByteBuf header, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    /**
     * Set ARAP-Password attribute with the provided password.
     * <p>
     * Will remove existing ARAP-Password attributes if they exist.
     *
     * @param request  the request to modify
     * @param password plaintext password
     * @return AccessRequestArap with encoded ARAP-Password
     * @throws RadiusPacketException if encoding fails
     */
    static @NonNull AccessRequest withPassword(@NonNull AccessRequest request, @NonNull String password) throws RadiusPacketException {
        if (password.isEmpty())
            throw new IllegalArgumentException("Could not encode ARAP attributes, password not set");

        byte[] authenticator = request.getAuthenticator();
        if (authenticator == null || authenticator.length != 16) {
            // AccessRequest should have a random 16-octet authenticator
            // If missing, we must generate one to derive the server challenge
            request = (AccessRequest) request.withAuthAttributes(random16bytes(), request.getAttributes());
            authenticator = request.getAuthenticator();
        }

        // Server challenge is the lower 8 octets of the RADIUS Request Authenticator (RFC 2869 5.7)
        byte[] serverChallenge = new byte[8];
        System.arraycopy(authenticator, 8, serverChallenge, 0, 8);

        byte[] peerChallenge = random8bytes();
        byte[] response = computeArapResponse(password, serverChallenge);

        byte[] arapPassword = ByteBuffer.allocate(16)
                .put(peerChallenge)
                .put(response)
                .array();

        var newAttributes = request.getAttributes().stream()
                .filter(a -> !(a.getVendorId() == -1 && a.getType() == ARAP_PASSWORD))
                .collect(Collectors.toList());

        newAttributes.add(request.getDictionary().createAttribute(-1, ARAP_PASSWORD, arapPassword));

        return (AccessRequest) request.withAttributes(newAttributes);
    }

    /**
     * Checks that the passed plain-text password matches the response
     * sent in the ARAP-Password attribute.
     *
     * @param password plaintext password to verify against
     * @return true if the password is valid, false otherwise
     */
    public boolean checkPassword(@NonNull String password) {
        if (password.isEmpty()) {
            logger.warn("Plaintext password to check against is empty");
            return false;
        }

        byte[] arapPassword = getAttribute(ARAP_PASSWORD)
                .map(RadiusAttribute::getValue)
                .orElse(null);

        if (arapPassword == null || arapPassword.length != 16) {
            logger.warn("ARAP-Password attribute missing or invalid length");
            return false;
        }

        byte[] authenticator = getAuthenticator();
        if (authenticator == null || authenticator.length != 16) {
            logger.warn("Request authenticator missing or invalid length");
            return false;
        }

        // Server challenge is the lower 8 octets of the RADIUS Request Authenticator
        byte[] serverChallenge = new byte[8];
        System.arraycopy(authenticator, 8, serverChallenge, 0, 8);

        byte[] expectedResponse = computeArapResponse(password, serverChallenge);
        byte[] actualResponse = new byte[8];
        System.arraycopy(arapPassword, 8, actualResponse, 0, 8);

        return Arrays.equals(expectedResponse, actualResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateAttributes() throws RadiusPacketException {
        int count = getAttributes(ARAP_PASSWORD).size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (ARAP) should have exactly one ARAP-Password attribute, has " + count);
    }

    private static byte @NonNull [] computeArapResponse(@NonNull String password, byte @NonNull [] challenge) {
        try {
            // DES key is derived from the password (first 8 octets, padded with 0 if shorter)
            byte[] keyBytes = new byte[8];
            byte[] passwordBytes = password.getBytes(UTF_8);
            System.arraycopy(passwordBytes, 0, keyBytes, 0, Math.min(passwordBytes.length, 8));

            var secretKey = new SecretKeySpec(keyBytes, "DES");
            var cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(challenge);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute ARAP response", e);
        }
    }

    private static byte @NonNull [] random8bytes() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}

