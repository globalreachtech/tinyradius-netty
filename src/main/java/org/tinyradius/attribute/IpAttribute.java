package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * This class represents a Radius attribute for an IP address.
 */
public abstract class IpAttribute extends RadiusAttribute {

    /**
     * IPv4 Address
     */
    public static class V4 extends IpAttribute {
        public V4(Dictionary dictionary, int vendorId, byte type, byte[] data) {
            super(dictionary, vendorId, type, IpAttribute.convert(data), Inet4Address.class);
        }

        public V4(Dictionary dictionary, int vendorId, byte type, String data) {
            super(dictionary, vendorId, type, IpAttribute.convert(data), Inet4Address.class);
        }

        public int getValueInt() {
            return ByteBuffer.wrap(getValue()).getInt();
        }
    }

    /**
     * IPv6 Address
     */
    public static class V6 extends IpAttribute {
        public V6(Dictionary dictionary, int vendorId, byte type, byte[] data) {
            super(dictionary, vendorId, type, IpAttribute.convert(data), Inet6Address.class);
        }

        public V6(Dictionary dictionary, int vendorId, byte type, String data) {
            super(dictionary, vendorId, type, IpAttribute.convert(data), Inet6Address.class);
        }
    }

    private IpAttribute(Dictionary dictionary, int vendorId, byte type, InetAddress data, Class<? extends InetAddress> clazz) {
        super(dictionary, vendorId, type, data.getAddress());

        if (!clazz.isInstance(data))
            throw new IllegalArgumentException("Expected " + clazz.getSimpleName() + ", actual " + data.getClass().getSimpleName());
    }

    @Override
    public String getValueString() {
        return convert(getValue()).getHostAddress();
    }

    private static InetAddress convert(String value) {
        if (value.isEmpty())
            throw new IllegalArgumentException("Address can't be empty");

        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Bad address: " + value, e);
        }
    }

    private static InetAddress convert(byte[] data) {
        try {
            return InetAddress.getByAddress(data);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Bad address", e);
        }
    }
}
