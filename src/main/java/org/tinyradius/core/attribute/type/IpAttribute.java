package org.tinyradius.core.attribute.type;

import org.tinyradius.core.dictionary.Dictionary;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * This class represents a Radius attribute for an IP address.
 */
public abstract class IpAttribute extends OctetsAttribute {

    private IpAttribute(Dictionary dictionary, int vendorId, int type, byte tag, InetAddress data, Class<? extends InetAddress> clazz) {
        super(dictionary, vendorId, type, tag, data.getAddress());

        if (!clazz.isInstance(data))
            throw new IllegalArgumentException("Expected " + clazz.getSimpleName() + ", actual " + data.getClass().getSimpleName());
    }

    public static byte[] stringParser(Dictionary dictionary, int i, int i1, byte b, String s) {
        return convert(s).getAddress();
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

    @Override
    public String getValueString() {
        return convert(getValue()).getHostAddress();
    }

    /**
     * IPv4 Address
     */
    public static class V4 extends IpAttribute {
        public V4(Dictionary dictionary, int vendorId, int type, byte tag, byte[] data) {
            this(dictionary, vendorId, type, tag, IpAttribute.convert(data));
        }

        public V4(Dictionary dictionary, int vendorId, int type, byte tag, String data) {
            this(dictionary, vendorId, type, tag, IpAttribute.convert(data));
        }

        public V4(Dictionary dictionary, int vendorId, int type, byte tag, InetAddress address) {
            super(dictionary, vendorId, type, tag, address, Inet4Address.class);
        }

        public int getValueInt() {
            return ByteBuffer.wrap(getValue()).getInt();
        }
    }

    /**
     * IPv6 Address
     */
    public static class V6 extends IpAttribute {
        public V6(Dictionary dictionary, int vendorId, int type, byte tag, byte[] data) {
            this(dictionary, vendorId, type, tag, IpAttribute.convert(data));
        }

        public V6(Dictionary dictionary, int vendorId, int type, byte tag, String data) {
            this(dictionary, vendorId, type, tag, IpAttribute.convert(data));
        }

        public V6(Dictionary dictionary, int vendorId, int type, byte tag, InetAddress address) {
            super(dictionary, vendorId, type, tag, address, Inet6Address.class);
        }
    }
}
