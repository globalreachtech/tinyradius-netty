package org.tinyradius.attribute.encrypt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class UserPasswordCodec implements AttributeCodec {

    private static final Logger logger = LogManager.getLogger();

    public RadiusAttribute encrypt(RadiusAttribute attribute, String sharedSecret, byte[] newAuth) {

        return attribute.getDictionary().createAttribute(attribute.getVendorId(), attribute.getType(),
                encodeData(newAuth, attribute.getValue(), sharedSecret.getBytes(UTF_8)));
    }

    public String decrypt(RadiusAttribute attribute, String sharedSecret, byte[] auth) throws RadiusPacketException {
        return decodeData(attribute.getValue(), sharedSecret.getBytes(UTF_8), auth);
    }

    /**
     * This method encodes plaintext data according to RFC 2865 for User-Password
     *
     * @param data         the data to encrypt
     * @param sharedSecret shared secret
     * @return the byte array containing the encrypted data
     */
    private byte[] encodeData(byte[] authenticator, byte[] data, byte[] sharedSecret) {
        requireNonNull(data, "Password cannot be null");
        requireNonNull(sharedSecret, "Shared secret cannot be null");

        byte[] ciphertext = authenticator;
        byte[] pw = pad(data);
        final ByteBuffer buffer = ByteBuffer.allocate(pw.length);

        for (int i = 0; i < pw.length; i += 16) {
            ciphertext = xor16(pw, i, md5(sharedSecret, ciphertext));
            buffer.put(ciphertext);
        }

        return buffer.array();
    }


    /**
     * Decodes the passed encoded attribute data and returns the cleartext form.
     *
     * @param sharedSecret shared secret
     * @return decrypted data
     */
    private String decodeData(byte[] encodedData, byte[] sharedSecret, byte[] auth) throws RadiusPacketException {
        if (encodedData.length < 16) {
            // PAP passwords require at least 16 bytes, or multiples thereof
            logger.warn("Malformed packet: User-Password attribute length must be greater than 15, actual {}", encodedData.length);
            throw new RadiusPacketException("Malformed User-Password attribute");
        }

        final ByteBuffer buffer = ByteBuffer.allocate(encodedData.length);
        byte[] ciphertext = auth;

        for (int i = 0; i < encodedData.length; i += 16) {
            buffer.put(xor16(encodedData, i, md5(sharedSecret, ciphertext)));
            ciphertext = Arrays.copyOfRange(encodedData, i, 16);
        }

        return stripNullPadding(new String(buffer.array(), UTF_8));
    }

    private byte[] md5(byte[] a, byte[] b) {
        MessageDigest md = RadiusPacket.getMd5Digest();
        md.update(a);
        return md.digest(b);
    }


    private static byte[] xor16(byte[] src1, int src1offset, byte[] src2) {

        byte[] dst = new byte[16];

        requireNonNull(src1, "src1 is null");
        requireNonNull(src2, "src2 is null");

        if (src1offset < 0)
            throw new IndexOutOfBoundsException("src1offset is less than 0");
        if ((src1offset + 16) > src1.length)
            throw new IndexOutOfBoundsException("bytes in src1 is less than src1offset plus 16");
        if (16 > src2.length)
            throw new IndexOutOfBoundsException("bytes in src2 is less than 16");

        for (int i = 0; i < 16; i++) {
            dst[i] = (byte) (src1[i + src1offset] ^ src2[i]);
        }

        return dst;
    }

    static byte[] pad(byte[] val) {
        requireNonNull(val, "Byte array cannot be null");

        int length = Math.max(
                (int) (Math.ceil((double) val.length / 16) * 16), 16);

        return Arrays.copyOf(val, length);
    }

    private static String stripNullPadding(String s) {
        int i = s.indexOf('\0');
        return (i > 0) ? s.substring(0, i) : s;
    }
}
