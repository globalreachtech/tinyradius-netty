package org.tinyradius.attribute.encrypt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * Attribute is encrypted with the method as defined in RFC2865 for the User-Password attribute
 */
public class UserPasswordCodec implements AttributeCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] authenticator) {
        return encodeData(authenticator, data, sharedSecret.getBytes(UTF_8));
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] authenticator) throws RadiusPacketException {
        return decodeData(data, sharedSecret.getBytes(UTF_8), authenticator);
    }

    private byte[] encodeData(byte[] authenticator, byte[] data, byte[] sharedSecret) {
        requireNonNull(data, "Data to encode cannot be null");
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

    private byte[] decodeData(byte[] encodedData, byte[] sharedSecret, byte[] auth) throws RadiusPacketException {
        if (encodedData.length < 16)
            throw new RadiusPacketException("Malformed attribute while decoding with RFC2865 User-Password method - " +
                    "data must be at least 16 octets, actual: " + encodedData.length);

        if (encodedData.length % 16 != 0)
            throw new RadiusPacketException("Malformed attribute while decoding with RFC2865 User-Password method - " +
                    "data octets must be multiple of 16, actual: " + encodedData.length);

        final ByteBuf buf = Unpooled.buffer(encodedData.length, encodedData.length);
        byte[] ciphertext = auth;

        for (int i = 0; i < encodedData.length; i += 16) {
            buf.writeBytes(xor16(encodedData, i, md5(sharedSecret, ciphertext)));
            ciphertext = Arrays.copyOfRange(encodedData, i, 16);
        }

        final int nullIndex = buf.indexOf(0, encodedData.length - 1, (byte) 0);

        return nullIndex == -1 ?
                buf.copy().array() :
                buf.writerIndex(nullIndex).copy().array();
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
}
