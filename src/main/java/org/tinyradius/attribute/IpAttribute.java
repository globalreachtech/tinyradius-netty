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

    private final String inetAddress;

    /**
     * IPv4 Address
     */
    public static class V4 extends IpAttribute {
        V4(Dictionary dictionary, int vendorId, int type, byte[] data) {
            super(dictionary, vendorId, type, IpAttribute.convert(data), Inet4Address.class);
        }

        public V4(Dictionary dictionary, int vendorId, int type, String data) {
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
        V6(Dictionary dictionary, int vendorId, int type, byte[] data) {
            super(dictionary, vendorId, type, IpAttribute.convert(data), Inet6Address.class);
        }

        V6(Dictionary dictionary, int vendorId, int type, String data) {
            super(dictionary, vendorId, type, IpAttribute.convert(data),  Inet6Address.class);
        }
    }

    private IpAttribute(Dictionary dictionary, int vendorId, int type, InetAddress data, Class<? extends InetAddress> clazz) {
        super(dictionary, vendorId, type, data.getAddress());

        this.inetAddress = data.getHostAddress();

        if (!clazz.isInstance(data))
            throw new IllegalArgumentException("Expected " + clazz.getSimpleName() + ", actual " + data.getClass().getSimpleName());
    }

    @Override
    public String getValueString() {
        return inetAddress;
    }

    private static InetAddress convert(String value) {
        if (value.isEmpty())
            throw new IllegalArgumentException("address can't be empty");

        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address: " + value, e);
        }
    }

    private static InetAddress convert(byte[] data) {
        try {
            return InetAddress.getByAddress(data);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad address", e);
        }
    }
}
