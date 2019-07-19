package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * This class represents a Radius attribute for an IP address.
 */
public class IpAttribute extends RadiusAttribute {

    private final String hostAddress;

    /**
     * IPv4 Address
     */
    public static class V4 extends IpAttribute {
        static final short SIZE = 4;

        V4(Dictionary dictionary, int vendorId, int type, byte[] data) {
            super(dictionary, vendorId, type, data, SIZE);
        }

        public V4(Dictionary dictionary, int vendorId, int type, String data) {
            super(dictionary, vendorId, type, IpAttribute.convertIp(data), SIZE);
        }

        public int getValueInt() {
            return ByteBuffer.wrap(getValue()).getInt();
        }
    }

    /**
     * IPv6 Address
     */
    public static class V6 extends IpAttribute {
        static final short SIZE = 16;

        V6(Dictionary dictionary, int vendorId, int type, byte[] data) {
            super(dictionary, vendorId, type, data, SIZE);
        }

        V6(Dictionary dictionary, int vendorId, int type, String data) {
            super(dictionary, vendorId, type, IpAttribute.convertIp(data), SIZE);
        }
    }

    private IpAttribute(Dictionary dictionary, int vendorId, int type, byte[] data, short addressSize) {
        super(dictionary, vendorId, type, data);

        try {
            hostAddress = InetAddress.getByAddress(data).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address", e);
        }

        // todo test ipv4/v6 wrong type
        if (data.length != addressSize)
            throw new IllegalArgumentException("Expected address length " + addressSize + ", actual length " + data.length);
    }

    @Override
    public String getValueString() {
        return hostAddress;
    }

    private static byte[] convertIp(String value) {
        if (value.isEmpty())
            throw new IllegalArgumentException("address can't be empty");

        try {
            return InetAddress.getByName(value).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address: " + value, e);
        }
    }
}
