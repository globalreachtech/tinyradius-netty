/**
 * $Id: RadiusUtil.java,v 1.2 2006/11/06 19:32:06 wuttke Exp $
 * Created on 09.04.2005
 * @author Matthias Wuttke
 * @version $Revision: 1.2 $
 */
package org.tinyradius.util;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Random;

/**
 * This class contains miscellaneous static utility functions.
 */
public class RadiusUtil {

	/**
	 * Returns the passed string as a byte array containing the
	 * string in UTF-8 representation.
	 * @param str Java string
	 * @return UTF-8 byte array
	 */
	public static byte[] getUtf8Bytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException uee) {
			return str.getBytes();
		}
	}
	
	/**
	 * Creates a string from the passed byte array containing the
	 * string in UTF-8 representation.
	 * @param utf8 UTF-8 byte array
	 * @return Java string
	 */
	public static String getStringFromUtf8(byte[] utf8) {
		try {
			String s = new String(utf8, "UTF-8");
			int i = s.indexOf('\0');
			return (i > 0) ? s.substring(0, i) : s;
		} catch (UnsupportedEncodingException uee) {
			return new String(utf8);
		}
	}
	
	/**
	 * Returns the byte array as a hex string in the format
	 * "0x1234".
	 * @param data byte array
	 * @return hex string
	 */
	public static String getHexString(byte[] data) {
		StringBuffer hex = new StringBuffer("0x");
		if (data != null)
			for (int i = 0; i < data.length; i++) {
				String digit = Integer.toString(data[i] & 0x0ff, 16);
				if (digit.length() < 2)
					hex.append('0');
				hex.append(digit);
			}
		return hex.toString();
	}

	public static byte[] xor(byte[] src1, int src1offset, int src1length,
							  byte[] src2, int src2offset, int src2length) {
		if (src1 == null)
			throw new NullPointerException("src1 is null");
		if (src1offset < 0)
			throw new IndexOutOfBoundsException("src1offset is less than 0");
		if (src1length < 0)
			throw new IndexOutOfBoundsException("src1length is less than 0");
		if ((src1offset + src1length) > src1.length)
			throw new IndexOutOfBoundsException("bytes in src1 is less than src1offset plus src1length");
		if (src2 == null)
			throw new NullPointerException("src2 is null");
		if (src2offset < 0)
			throw new IndexOutOfBoundsException("src2offset is less than 0");
		if (src2length < 0)
			throw new IndexOutOfBoundsException("src2length is less than 0");
		if ((src2offset + src2length) > src2.length)
			throw new IndexOutOfBoundsException("bytes in src2 is less than src2offset plus src2length");

		int length = Math.max(src1length, src2length);
		byte[] dst = new byte[length];

		return xor(src1, src1offset, src1length, src2, src2offset, src2length, dst, 0);
	}

	public static byte[] xor(byte[] src1, int src1offset, int src1length,
							  byte[] src2, int src2offset, int src2length, byte[] dst, int dstoffset) {

		if (src1 == null)
			throw new NullPointerException("src1 is null");
		if (src1offset < 0)
			throw new IndexOutOfBoundsException("src1offset is less than 0");
		if (src1length < 0)
			throw new IndexOutOfBoundsException("src1length is less than 0");
		if ((src1offset + src1length) > src1.length)
			throw new IndexOutOfBoundsException("bytes in src1 is less than src1offset plus src1length");
		if (src2 == null)
			throw new NullPointerException("src2 is null");
		if (src2offset < 0)
			throw new IndexOutOfBoundsException("src2offset is less than 0");
		if (src2length < 0)
			throw new IndexOutOfBoundsException("src2length is less than 0");
		if ((src2offset + src2length) > src2.length)
			throw new IndexOutOfBoundsException("bytes in src2 is less than src2offset plus src2length");
		if (dst == null)
			throw new NullPointerException("dst is null");
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
				dst[i + dstoffset] = (byte)(src1[i + src1offset] ^ src2[i + src2offset]);
			}
		}

		return dst;
	}

	public static byte[] compute(byte[]... values) throws GeneralSecurityException {

		MessageDigest md = MessageDigest.getInstance("MD5");

		for (byte[] b : values)
			md.update(b);

		return md.digest();
	}

	public static byte[] pad(byte[] value, int length) {
		if (value == null)
			throw new NullPointerException("value cannot be null");
		if (length < 0)
			throw new IllegalArgumentException("length cannot be less than 0");

		int length1 = Math.max((int)(Math.ceil((double)
			value.length / length) * length), length);

		byte[] padded = new byte[length1];

		System.arraycopy(value, 0, padded, 0, value.length);

		return padded;
	}

}
