package org.tinyradius.core.attribute.codec;

import io.netty.buffer.Unpooled;
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

    @Override
    protected byte @NonNull [] encodeData(byte @NonNull [] data, byte @NonNull [] auth, byte @NonNull [] secret) {
        byte[] salt = genSalt();
        byte[] combined = ByteBuffer.allocate(data.length + 1)
                .put((byte) data.length)
                .put(data)
                .array();

        byte[] plaintext = pad16x(combined);
        var buffer = ByteBuffer.allocate(plaintext.length + 2)
                .put(salt);

        byte[] c = ByteBuffer.allocate(18)
                .put(auth)
                .put(salt)
                .array();

        for (int i = 0; i < plaintext.length; i += 16) {
            c = xor16(plaintext, i, md5(secret, c));
            buffer.put(c);
        }

        return buffer.array();
    }

    @Override
    protected byte @NonNull [] decodeData(byte @NonNull [] encodedData, byte @NonNull [] auth, byte @NonNull [] secret) throws RadiusPacketException {
        int strLen = encodedData.length - 2;
        if (strLen < 16)
            throw new RadiusPacketException("Malformed attribute while decoding with RFC2868 Tunnel-Password method - " +
                    "string must be at least 16 octets, actual: " + strLen);

        if (strLen % 16 != 0)
            throw new RadiusPacketException("Malformed attribute while decoding with RFC2868 Tunnel-Password method - " +
                    "string octets must be multiple of 16, actual: " + strLen);

        byte[] encodedStr = Arrays.copyOfRange(encodedData, 2, encodedData.length);
        byte[] salt = Arrays.copyOfRange(encodedData, 0, 2);

        byte[] c = ByteBuffer.allocate(18)
                .put(auth)
                .put(salt)
                .array();

        var plaintext = Unpooled.buffer(encodedStr.length, encodedStr.length);

        for (int i = 0; i < strLen; i += 16) {
            plaintext.writeBytes(xor16(encodedStr, i, md5(secret, c)));
            c = Arrays.copyOfRange(encodedStr, i, i + 16);
        }

        byte len = plaintext.readByte(); // first

        return plaintext
                .writerIndex(len + 1) // strip padding
                .copy().array();
    }

    private static byte[] genSalt() {
        var randomBytes = new byte[2];
        RANDOM.nextBytes(randomBytes);
        randomBytes[0] = (byte) (randomBytes[0] | 0b1000_0000); // MSF must be set
        return randomBytes;
    }
}
