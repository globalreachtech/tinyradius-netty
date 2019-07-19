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

        public V4(Dictionary dictionary, int vendorId, int type, String data) {
            super(dictionary, vendorId, type, data);
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
        if (value.isEmpty()) {
            throw new RuntimeException("address can't be empty");
        }
        try {
            return InetAddress.getByName(value).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address: " + value, e);
        }
    }
}
