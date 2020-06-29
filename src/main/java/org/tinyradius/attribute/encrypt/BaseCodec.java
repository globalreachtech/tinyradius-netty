package org.tinyradius.attribute.encrypt;

import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.security.MessageDigest;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public abstract class BaseCodec {

    /**
     * Encodes plaintext data
     *
     * @param data         the data to encrypt
     * @param sharedSecret shared secret
     * @param requestAuth  packet authenticator
     * @return the byte array containing the encrypted data
     */
    public abstract byte[] encode(byte[] data, String sharedSecret, byte[] requestAuth) throws RadiusPacketException;

    /**
     * Decodes the passed encoded attribute data and returns the cleartext form as bytes
     *
     * @param data         data to decrypt
     * @param sharedSecret shared secret
     * @param requestAuth  packet authenticator
     * @return decrypted data
     */
    public abstract byte[] decode(byte[] data, String sharedSecret, byte[] requestAuth) throws RadiusPacketException;

    protected static byte[] xor16(byte[] src1, int src1offset, byte[] src2) {
        requireNonNull(src1);
        requireNonNull(src2);

        final byte[] dst = new byte[16];

        if (src1offset < 0)
            throw new IndexOutOfBoundsException("src1offset is less than 0");
        if (src1.length < src1offset + 16)
            throw new IndexOutOfBoundsException("bytes in src1 is less than src1offset + 16");
        if (src2.length < 16)
            throw new IndexOutOfBoundsException("bytes in src2 is less than 16");

        for (int i = 0; i < 16; i++) {
            dst[i] = (byte) (src1[i + src1offset] ^ src2[i]);
        }

        return dst;
    }

    protected byte[] md5(byte[] a, byte[] b) {
        MessageDigest md = RadiusPacket.getMd5Digest();
        md.update(a);
        return md.digest(b);
    }

    /**
     * @param val byte array to pad
     * @return byte array containing input, padded size multiple of 16
     */
    protected static byte[] pad16x(byte[] val) {
        requireNonNull(val, "Byte array cannot be null");

        int length = Math.max(
                (int) (Math.ceil((double) val.length / 16) * 16), 16);

        return Arrays.copyOf(val, length);
    }
}
