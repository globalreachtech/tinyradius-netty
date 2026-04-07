package org.tinyradius.core.attribute.codec;

import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.packet.RadiusPacket;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Base class for RADIUS attribute encryption/decryption codecs.
 */
public abstract class BaseCodec {

    /**
     * Encodes plaintext data.
     *
     * @param data         the data to encrypt
     * @param requestAuth  packet authenticator (16 bytes)
     * @param sharedSecret shared secret
     * @return the byte array containing the encrypted data
     * @throws RadiusPacketException if the request authenticator is invalid or encoding fails
     */
    public byte @NonNull [] encode(byte @NonNull [] data, byte @NonNull [] requestAuth, @NonNull String sharedSecret) throws RadiusPacketException {
        if (requestAuth.length != 16)
            throw new RadiusPacketException("Request Authenticator must be 16 octets");

        return encodeData(data, requestAuth, sharedSecret.getBytes(UTF_8));
    }

    /**
     * Decodes the passed encoded attribute data and returns the cleartext form as bytes.
     *
     * @param data         data to decrypt, excluding type/length/tag
     * @param requestAuth  packet authenticator (16 bytes)
     * @param sharedSecret shared secret
     * @return decrypted data
     * @throws RadiusPacketException if the request authenticator is invalid or decoding fails
     */
    public byte @NonNull [] decode(byte @NonNull [] data, byte @NonNull [] requestAuth, @NonNull String sharedSecret) throws RadiusPacketException {
        if (requestAuth.length != 16)
            throw new RadiusPacketException("Request Authenticator must be 16 octets");

        return decodeData(data, requestAuth, sharedSecret.getBytes(UTF_8));
    }

    /**
     * Encodes the data using the codec-specific implementation.
     *
     * @param data   data to encrypt, excluding derived or random data like salt/length/padding
     * @param auth   request authenticator (16 bytes)
     * @param secret shared secret as bytes
     * @return the encoded byte array, which may include metadata like salt or length
     */
    protected abstract byte @NonNull [] encodeData(byte @NonNull [] data, byte @NonNull [] auth, byte @NonNull [] secret);

    /**
     * Decodes the data using the codec-specific implementation.
     *
     * @param encodedData the encoded attribute data to decrypt
     * @param auth        request authenticator (16 bytes)
     * @param secret      shared secret as bytes
     * @return the decrypted plaintext data
     * @throws RadiusPacketException if the data is malformed or decoding fails
     */
    protected abstract byte @NonNull [] decodeData(byte @NonNull [] encodedData, byte @NonNull [] auth, byte @NonNull [] secret) throws RadiusPacketException;

    /**
     * Performs a CBC-like MD5 encryption used by several RADIUS password-like attributes.
     *
     * @param data           plaintext data to encrypt
     * @param c              initialization vector (usually the request authenticator)
     * @param secret         shared secret
     * @param md5SecretFirst if true, MD5 is calculated as MD5(secret || c); otherwise MD5(c || secret)
     * @return the encrypted byte array (multiple of 16 bytes)
     */
    protected byte @NonNull [] cbcMd5Encode(byte @NonNull [] data, byte @NonNull [] c, byte @NonNull [] secret, boolean md5SecretFirst) {
        byte[] str = pad16x(data);
        var buffer = ByteBuffer.allocate(str.length);

        for (int i = 0; i < str.length; i += 16) {
            c = xor16(str, i, md5SecretFirst ?
                    md5(secret, c) : md5(c, secret));
            buffer.put(c);
        }

        return buffer.array();
    }

    /**
     * Performs a CBC-like MD5 decryption used by several RADIUS password-like attributes.
     *
     * @param data           encoded data to decrypt (must be a multiple of 16 bytes)
     * @param c              initialization vector (usually the request authenticator)
     * @param secret         shared secret
     * @param md5SecretFirst if true, MD5 is calculated as MD5(secret || c); otherwise MD5(c || secret)
     * @return the decrypted byte array (including any padding)
     * @throws RadiusPacketException if the data is malformed
     */
    protected byte @NonNull [] cbcMd5Decode(byte @NonNull [] data, byte @NonNull [] c, byte @NonNull [] secret, boolean md5SecretFirst) throws RadiusPacketException {
        if (data.length < 16)
            throw new RadiusPacketException("Malformed attribute while decoding - data must be at least 16 octets, actual: " + data.length);

        if (data.length % 16 != 0)
            throw new RadiusPacketException("Malformed attribute while decoding - data octets must be multiple of 16, actual: " + data.length);

        var buffer = ByteBuffer.allocate(data.length);

        for (int i = 0; i < data.length; i += 16) {
            buffer.put(xor16(data, i, md5SecretFirst ?
                    md5(secret, c) : md5(c, secret)));
            c = Arrays.copyOfRange(data, i, i + 16);
        }

        return buffer.array();
    }

    /**
     * Trims trailing null (0x00) bytes from the end of the byte array.
     *
     * @param data the byte array to trim
     * @return a new byte array with trailing nulls removed
     */
    protected byte @NonNull [] rTrim(byte @NonNull [] data){
        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] != 0)
                return Arrays.copyOfRange(data, 0, i + 1);
        }

        return new byte[0];
    }

    /**
     * XORs 16 bytes from the source array starting at the given offset with the second source array.
     *
     * @param src1       first source array
     * @param src1offset offset in the first source array
     * @param src2       second source array (must be at least 16 bytes)
     * @return a new 16-byte array containing the XOR result
     * @throws IndexOutOfBoundsException if the source arrays are too small or the offset is invalid
     */
    protected static byte @NonNull [] xor16(byte @NonNull [] src1, int src1offset, byte @NonNull [] src2) {
        byte[] dst = new byte[16];

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

    /**
     * Computes the MD5 digest of the concatenated input byte arrays.
     *
     * @param a one or more byte arrays to be digested
     * @return the 16-byte MD5 digest
     */
    protected byte @NonNull [] md5(byte @NonNull []... a) {
        var md5 = RadiusPacket.getMd5Digest();
        for (byte[] bytes : a) {
            md5.update(bytes);
        }
        return md5.digest();
    }

    /**
     * Pads the input byte array with null bytes so its length is a multiple of 16.
     * Ensures the result is at least 16 bytes long.
     *
     * @param val byte array to pad
     * @return a new byte array containing the input followed by padding
     */
    protected static byte @NonNull [] pad16x(byte @NonNull [] val) {
        int length = Math.max(
                (int) (Math.ceil((double) val.length / 16) * 16), 16);

        return Arrays.copyOf(val, length);
    }
}
