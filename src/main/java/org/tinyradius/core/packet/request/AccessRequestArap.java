package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import jdk.jfr.Experimental;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
            throw new IllegalArgumentException("Could not encode ARAP attributes, password is empty");

        var auth = randomBytes(16);
        byte[] arapPassword = ByteBuffer.allocate(16)
                .put(randomBytes(8))
                .put(computeArapResponse(password, auth))
                .array();

        var newAttributes = request.getAttributes().stream()
                .filter(a -> !(a.getVendorId() == -1 && a.getType() == ARAP_PASSWORD))
                .collect(Collectors.toList());

        newAttributes.add(request.getDictionary().createAttribute(-1, ARAP_PASSWORD, arapPassword));

        return (AccessRequest) request.withAuthAttributes(auth, newAttributes);
    }

    public byte @Nullable [] getClientChallenge(){
        return getAttribute(ARAP_PASSWORD)
                .map(RadiusAttribute::getValue)
                .map(i -> Arrays.copyOfRange(i, 0, 8))
                .orElse(null);
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

        byte[] auth = getAuthenticator();
        if (auth == null) {
            logger.warn("Request authenticator missing");
            return false;
        }

        byte[] expectedResponse = computeArapResponse(password, auth);
        byte[] actualResponse = new byte[8];
        System.arraycopy(arapPassword, 8, actualResponse, 0, 8);

        return Arrays.equals(expectedResponse, actualResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateAttributes() throws RadiusPacketException {
        var attributes = getAttributes(ARAP_PASSWORD);
        int count = attributes.size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (ARAP) should have exactly one ARAP-Password attribute, has " + count);

        if (attributes.get(0).getValue().length != 16)
            throw new RadiusPacketException("ARAP-Password attribute should be 16 octets, has " + attributes.get(0).getValue().length);
    }

    private static byte @NonNull [] computeArapResponse(@NonNull String password, byte @NonNull [] authenticator) {
        // Server challenge is the lower 8 octets of the RADIUS Request Authenticator (RFC 2869 5.7)
        byte[] challenge = new byte[8];
        System.arraycopy(authenticator, 8, challenge, 0, 8);

        // DES key is derived from the password (first 8 octets, padded with 0 if shorter)
        byte[] keyBytes = new byte[8];
        byte[] passwordBytes = password.getBytes(UTF_8);
        System.arraycopy(passwordBytes, 0, keyBytes, 0, Math.min(passwordBytes.length, 8));

        return doCipher(keyBytes, challenge);
    }

    private static byte[] doCipher(byte[] key, byte[] challenge) {
        var secretKey = new SecretKeySpec(key, "DES");
        try {
            var cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(challenge);
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException |
                 NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

