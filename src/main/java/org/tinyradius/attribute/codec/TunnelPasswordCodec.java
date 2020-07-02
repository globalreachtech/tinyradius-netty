package org.tinyradius.attribute.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.util.RadiusPacketException;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Attribute is encrypted with the method as defined in RFC2868 for the Tunnel-Password attribute
 */
class TunnelPasswordCodec extends BaseCodec {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected byte[] encodeData(byte[] data, byte[] auth, byte[] secret) {
        final byte[] salt = genSalt();
        final byte[] combined = ByteBuffer.allocate(data.length + 1)
                .put((byte) data.length)
                .put(data)
                .array();

        final byte[] plaintext = pad16x(combined);
        final ByteBuffer buffer = ByteBuffer.allocate(plaintext.length + 2)
                .put(salt);

        byte[] C = ByteBuffer.allocate(18)
                .put(auth)
                .put(salt)
                .array();

        for (int i = 0; i < plaintext.length; i += 16) {
            C = xor16(plaintext, i, md5(secret, C));
            buffer.put(C);
        }

        return buffer.array();
    }

    @Override
    protected byte[] decodeData(byte[] encodedData, byte[] auth, byte[] secret) throws RadiusPacketException {
        final int strLen = encodedData.length - 2;
        if (strLen < 16)
            throw new RadiusPacketException("Malformed attribute while decoding with RFC2868 Tunnel-Password method - " +
                    "string must be at least 16 octets, actual: " + strLen);

        if (strLen % 16 != 0)
            throw new RadiusPacketException("Malformed attribute while decoding with RFC2865 Tunnel-Password method - " +
                    "string octets must be multiple of 16, actual: " + strLen);

        final byte[] encodedStr = Arrays.copyOfRange(encodedData, 2, encodedData.length);
        final byte[] salt = Arrays.copyOfRange(encodedData, 0, 2);

        byte[] C = ByteBuffer.allocate(18)
                .put(auth)
                .put(salt)
                .array();

        final ByteBuf plaintext = Unpooled.buffer(encodedStr.length, encodedStr.length);

        for (int i = 0; i < strLen; i += 16) {
            plaintext.writeBytes(xor16(encodedStr, i, md5(secret, C)));
            C = Arrays.copyOfRange(encodedStr, i, 16);
        }

        final byte len = plaintext.readByte(); // first

        return plaintext
                .writerIndex(len + 1) // strip padding
                .copy().array();
    }

    private static byte[] genSalt() {
        final byte[] randomBytes = new byte[2];
        RANDOM.nextBytes(randomBytes);
        randomBytes[0] = (byte) (randomBytes[0] | 0b1000_0000); // MSF must be set
        return randomBytes;
    }
}
