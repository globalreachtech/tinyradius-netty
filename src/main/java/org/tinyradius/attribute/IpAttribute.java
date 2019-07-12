package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.lang.Byte.toUnsignedInt;

/**
 * This class represents a Radius attribute for an IP address.
 */
public class IpAttribute extends RadiusAttribute {

    /**
     *
     */
    public static class V4 extends IpAttribute {
        static final int addressLength = 4;

        static IpAttribute parse(Dictionary dictionary, int vendorId, byte[] data, int offset) throws RadiusException {
            return IpAttribute.parse(dictionary, vendorId, data, offset, addressLength);
        }

        V4(Dictionary dictionary, int vendorId, int type, byte[] data) {
            super(dictionary, vendorId, type, data, addressLength);
        }

        V4(Dictionary dictionary, int vendorId, int type, String data) {
            super(dictionary, vendorId, type, data, addressLength);
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
        public long getDataLong() {
            byte[] data = getData();
            if (data == null || data.length != 4)
                throw new RuntimeException("expected 4 bytes attribute data");
            return (long) toUnsignedInt(data[0]) << 24
                    | toUnsignedInt(data[1]) << 16
                    | toUnsignedInt(data[2]) << 8
                    | toUnsignedInt(data[3]);
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
        static final int addressLength = 16;

        static IpAttribute parse(Dictionary dictionary, int vendorId, byte[] data, int offset) throws RadiusException {
            return IpAttribute.parse(dictionary, vendorId, data, offset, addressLength);
        }

        V6(Dictionary dictionary, int vendorId, int type, byte[] data) {
            super(dictionary, vendorId, type, data, addressLength);
        }

        V6(Dictionary dictionary, int vendorId, int type, String data) {
            super(dictionary, vendorId, type, data, addressLength);
        }
    }

    private static IpAttribute parse(Dictionary dictionary, int vendorId, byte[] data, int offset, int expectedLength) throws RadiusException {
        final int length = readLength(data, offset);
        if (length != expectedLength + 2)
            throw new RadiusException("Ip Address attribute: expected length " + (expectedLength + 2) + ", packet declared " + length);

        return new IpAttribute(dictionary, vendorId, readType(data, offset), readData(data, offset), expectedLength) {
        };
    }

    private IpAttribute(Dictionary dictionary, int vendorId, int type, byte[] data, int addressLength) {
        super(dictionary, vendorId, type, data);
        if (data.length != addressLength)
            throw new IllegalArgumentException("Expected address length " + addressLength + ", actual length " + data.length);
    }

    private IpAttribute(Dictionary dictionary, int vendorId, int type, String data, int addressLength) {
        this(dictionary, vendorId, type, convertIp(data), addressLength);
    }

    @Override
    public String getDataString() {
        try {
            return InetAddress.getByAddress(getData()).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address", e);
        }
    }

    private static byte[] convertIp(String value) {
        try {
            return InetAddress.getByName(value).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address: " + value, e);
        }
    }
}
