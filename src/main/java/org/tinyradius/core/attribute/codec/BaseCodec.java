package org.tinyradius.core.attribute.codec;

import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.packet.RadiusPacket;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class BaseCodec {

    /**
     * Encodes plaintext data
     *
     * @param data         the data to encrypt
     * @param requestAuth  packet authenticator
     * @param sharedSecret shared secret
     * @return the byte array containing the encrypted data
     * @throws RadiusPacketException errors encoding attribute data
     */
    public byte[] encode(byte[] data, byte[] requestAuth, String sharedSecret) throws RadiusPacketException {
        Objects.requireNonNull(sharedSecret);
        if (requestAuth.length != 16)
            throw new RadiusPacketException("Request Authenticator must be 16 octets");

        return encodeData(data, requestAuth, sharedSecret.getBytes(UTF_8));
    }

    /**
     * Decodes the passed encoded attribute data and returns the cleartext form as bytes
     *
     * @param data         data to decrypt, excl. type/length/tag
     * @param requestAuth  packet authenticator
     * @param sharedSecret shared secret
     * @return decrypted data
     * @throws RadiusPacketException errors decoding attribute data
     */
    public byte[] decode(byte[] data, byte[] requestAuth, String sharedSecret) throws RadiusPacketException {
        Objects.requireNonNull(sharedSecret);
        if (requestAuth.length != 16)
            throw new RadiusPacketException("Request Authenticator must be 16 octets");

        return decodeData(data, requestAuth, sharedSecret.getBytes(UTF_8));
    }

    /**
     * @param data   data to encrypt, excl. derived/random generated data e.g. salt/length/padding
     * @param auth   request authenticator
     * @param secret shared secret
     * @return byte array representing salt+string
     */
    protected abstract byte[] encodeData(byte[] data, byte[] auth, byte[] secret);

    /**
     * @param encodedData byte array representing salt+string
     * @param auth        request authenticator
     * @param secret      shared secret
     * @return password sub-field (excl. salt, length, padding)
     * @throws RadiusPacketException error while decoding attribute data
     */
    protected abstract byte[] decodeData(byte[] encodedData, byte[] auth, byte[] secret) throws RadiusPacketException;

    protected static byte[] xor16(byte[] src1, int src1offset, byte[] src2) {
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
        int length = Math.max(
                (int) (Math.ceil((double) val.length / 16) * 16), 16);

        return Arrays.copyOf(val, length);
    }
}
