package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.lang.Byte.toUnsignedInt;

/**
 * This class represents a Radius attribute for an IP address.
 */
public class IpAttribute extends RadiusAttribute {

    private String hostAddress;

    /**
     *
     */
    public static class V4 extends IpAttribute {

        V4(Dictionary dictionary, int vendorId, int type, byte[] data) {
            super(dictionary, vendorId, type, data);
        }

        V4(Dictionary dictionary, int vendorId, int type, String data) {
            super(dictionary, vendorId, type, data);
        }

        public V4(Dictionary dictionary, int vendorId, int type, long value) {
            this(dictionary, vendorId, type, convertIpV4(value));
        }

        /**
         * Returns the IP number as a 32 bit unsigned number. The number is
         * returned in a long because Java does not support unsigned ints.
         *
         * @return IP number
         */
        public long getValueLong() {
            byte[] data = getValue();
            return (long) toUnsignedInt(data[0]) << 24
                    | toUnsignedInt(data[1]) << 16
                    | toUnsignedInt(data[2]) << 8
                    | toUnsignedInt(data[3]);
            // todo Integer.toUnsignedLong / ByteBuffer.getInt()
        }

        /**
         * Sets the IP number represented by this IpAttribute
         * as a 32 bit unsigned number.
         *
         * @param ip IP address as 32-bit unsigned number
         */
        private static byte[] convertIpV4(long ip) {
            byte[] data = new byte[4];
            data[0] = (byte) ((ip >> 24) & 0x0ff);
            data[1] = (byte) ((ip >> 16) & 0x0ff);
            data[2] = (byte) ((ip >> 8) & 0x0ff);
            data[3] = (byte) (ip & 0x0ff);
            return data;
        }
    }

    /**
     *
     */
    public static class V6 extends IpAttribute {

        V6(Dictionary dictionary, int vendorId, int type, byte[] data) {
            super(dictionary, vendorId, type, data);
        }

        V6(Dictionary dictionary, int vendorId, int type, String data) {
            super(dictionary, vendorId, type, data);
        }
    }

    private IpAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        super(dictionary, vendorId, type, data);

        try {
            hostAddress = InetAddress.getByAddress(data).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address", e);
        }
    }

    private IpAttribute(Dictionary dictionary, int vendorId, int type, String data) {
        this(dictionary, vendorId, type, convertIp(data));
    }

    @Override
    public String getValueString() {
        return hostAddress;
    }

    private static byte[] convertIp(String value) {
        try {
            return InetAddress.getByName(value).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address: " + value, e);
        }
    }
}
