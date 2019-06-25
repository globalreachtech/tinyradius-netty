package com.tinyradius.packet;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import static java.lang.Math.max;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

class Util {

    static byte[] xor(byte[] src1, int src1offset, int src1length,
                      byte[] src2, int src2offset, int src2length) {
        byte[] dst = new byte[max(max(src1length, src2length), 0)];

        return xor(src1, src1offset, src1length, src2, src2offset, src2length, dst, 0);
    }

    private static byte[] xor(byte[] src1, int src1offset, int src1length,
                              byte[] src2, int src2offset, int src2length, byte[] dst, int dstoffset) {

        requireNonNull(src1, "src1 is null");
        requireNonNull(src2, "src2 is null");
        requireNonNull(dst, "dst is null");

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
            } else if (i >= src2length) {
                dst[i + dstoffset] = src1[i + src1offset];
            } else {
                dst[i + dstoffset] = (byte) (src1[i + src1offset] ^ src2[i + src2offset]);
            }
        }

        return dst;
    }

    static byte[] compute(byte[]... values) throws GeneralSecurityException {

        MessageDigest md = MessageDigest.getInstance("MD5");

        for (byte[] b : values)
            md.update(b);

        return md.digest();
    }

    static byte[] pad(byte[] value, int length) {
        requireNonNull(value, "value cannot be null");
        if (length < 0)
            throw new IllegalArgumentException("length cannot be less than 0");

        byte[] padded = new byte[(int) (Math.ceil(value.length / length) * length)];

        System.arraycopy(value, 0, padded, 0, value.length);

        return padded;
    }

    /**
     * Creates a string from the passed byte array containing the
     * string in UTF-8 representation.
     *
     * @param utf8 UTF-8 byte array
     * @return Java string
     */
    static String getStringFromUtf8(byte[] utf8) {
        String s = new String(utf8, UTF_8);
        int i = s.indexOf('\0');
        return (i > 0) ? s.substring(0, i) : s;
    }

}
