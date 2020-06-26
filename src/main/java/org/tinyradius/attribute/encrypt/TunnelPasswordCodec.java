package org.tinyradius.attribute.encrypt;

import org.tinyradius.util.RadiusPacketException;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import static java.lang.Math.max;

/**
 * Attribute is encrypted with the method as defined in RFC2868 for the Tunnel-Password attribute
 */
public class TunnelPasswordCodec implements AttributeCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] authenticator) {
        return data;
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] authenticator) throws RadiusPacketException {
        return data;
    }

    private byte[] decrypt(byte[] value, byte[] iv, String sharedSecret)
            throws GeneralSecurityException {

        byte[] P = new byte[value.length - 2];
        byte[] C = new byte[iv.length + 2];
        byte[] S = sharedSecret.getBytes(StandardCharsets.UTF_8);

        System.arraycopy(value, 2, P, 0, P.length);
        System.arraycopy(iv, 0, C, 0, iv.length);
        System.arraycopy(value, 0, C, iv.length, 2);

        byte[] tmp = new byte[P.length];

        for (int i = 0; i < P.length; i += C.length) {
            C = compute(S, C);
            C = xor(P, i, C.length, C, 0, C.length);
            System.arraycopy(C, 0, tmp, i, C.length);
            System.arraycopy(P, i, C, 0, C.length);
        }

        byte[] result = new byte[tmp[0]];

        System.arraycopy(tmp, 1, result, 0, result.length);

        return result;
    }

    private byte[] encrypt(byte[] value, byte[] iv, byte[] salt, String sharedSecret)
            throws GeneralSecurityException {

        int length = (((value.length + 1) + iv.length) / iv.length) * iv.length;

        byte[] P = new byte[length];
        byte[] C = new byte[iv.length + salt.length];
        byte[] S = sharedSecret.getBytes(StandardCharsets.UTF_8);

        P[0] = (byte) value.length;

        System.arraycopy(value, 0, P, 1, value.length);
        System.arraycopy(iv, 0, C, 0, iv.length);
        System.arraycopy(salt, 0, C, iv.length, salt.length);

        byte[] result = new byte[P.length + salt.length];

        for (int i = 0; i < P.length; i += C.length) {
            C = compute(S, C);
            C = xor(P, i, C.length, C, 0, C.length);
            System.arraycopy(C, 0, result, i + salt.length, C.length);
        }

        System.arraycopy(salt, 0, result, 0, salt.length);

        return result;
    }

    public static byte[] compute(byte[]... values) throws GeneralSecurityException {

        MessageDigest md = MessageDigest.getInstance("MD5");

        for (byte[] b : values)
            md.update(b);

        return md.digest();
    }

    public static byte[] xor(byte[] src1, int src1offset, int src1length,
                             byte[] src2, int src2offset, int src2length) {
        byte[] dst = new byte[max(max(src1length, src2length), 0)];

        return xor(src1, src1offset, src1length, src2, src2offset, src2length, dst, 0);
    }


    public static byte[] xor(byte[] src1, int src1offset, int src1length,
                             byte[] src2, int src2offset, int src2length, byte[] dst, int dstoffset) {

        if (src1offset < 0)
            throw new IndexOutOfBoundsException("src1offset is less than 0");
        if (src1length < 0)
            throw new IndexOutOfBoundsException("src1length is less than 0");
        if ((src1offset + src1length) > src1.length)
            throw new IndexOutOfBoundsException("bytes in src1 is less than src1offset plus src1length");
        if (src2offset < 0)
            throw new IndexOutOfBoundsException("src2offset is less than 0");
        if (src2length < 0)
            throw new IndexOutOfBoundsException("src2length is less than 0");
        if ((src2offset + src2length) > src2.length)
            throw new IndexOutOfBoundsException("bytes in src2 is less than src2offset plus src2length");
        if ((dstoffset + src1length) > dst.length)
            throw new IndexOutOfBoundsException("bytes in dst is less than dstoffset plus src1length");
        if ((dstoffset + src2length) > dst.length)
            throw new IndexOutOfBoundsException("bytes in dst is less than dstoffset plus src2length");

        int length = Math.min(src1length, src2length);

        for (int i = 0; i < length; i++) {
            if (i >= src1length) {
                dst[i + dstoffset] = src2[i + src2offset];
            } else {
                dst[i + dstoffset] = (byte) (src1[i + src1offset] ^ src2[i + src2offset]);
            }
        }

        return dst;
    }

}
