package org.tinyradius.core.attribute.codec;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;

/**
 * Attribute is encrypted with the method as defined in RFC2868 for the Tunnel-Password attribute
 */
class TunnelPasswordCodec extends BaseCodec {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte @NonNull [] encodeData(byte @NonNull [] data, byte @NonNull [] auth, byte @NonNull [] secret) {
        byte[] salt = genSalt();
        byte[] combined = ByteBuffer.allocate(data.length + 1)
                .put((byte) data.length)
                .put(data)
                .array();

        byte[] c = ByteBuffer.allocate(18)
                .put(auth)
                .put(salt)
                .array();

        byte[] encrypted = cbcMd5Encode(combined, c, secret, true);
        return ByteBuffer.allocate(encrypted.length + 2)
                .put(salt)
                .put(encrypted)
                .array();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected byte @NonNull [] decodeData(byte @NonNull [] encodedData, byte @NonNull [] auth, byte @NonNull [] secret) throws RadiusPacketException {
        byte[] encodedStr = Arrays.copyOfRange(encodedData, 2, encodedData.length);
        byte[] salt = Arrays.copyOfRange(encodedData, 0, 2);

        byte[] c = ByteBuffer.allocate(18)
                .put(auth)
                .put(salt)
                .array();

        byte[] decoded = cbcMd5Decode(encodedStr, c, secret, true);
        byte len = decoded[0];
        return Arrays.copyOfRange(decoded, 1, len + 1);
    }

    private static byte[] genSalt() {
        var randomBytes = new byte[2];
        RANDOM.nextBytes(randomBytes);
        randomBytes[0] = (byte) (randomBytes[0] | 0x80); // MSF must be set
        return randomBytes;
    }
}
