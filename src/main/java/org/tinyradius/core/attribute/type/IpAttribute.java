package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
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

    private IpAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        super(dictionary, vendorId, data);
    }

    protected void checkType(Class<? extends InetAddress> clazz, InetAddress address) {
        if (!clazz.isInstance(address))
            throw new IllegalArgumentException("Expected " + clazz.getSimpleName() + ", actual " + address.getClass().getSimpleName());

    }

    public static byte[] stringParser(Dictionary dictionary, int i, int i1, String value) {
        if (value.isEmpty())
            throw new IllegalArgumentException("Address can't be empty");

        try {
            return InetAddress.getByName(value).getAddress();
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
        public V4(Dictionary dictionary, int vendorId, ByteBuf data) {
            super(dictionary, vendorId, data);
            checkType(Inet4Address.class, IpAttribute.convert(getValue()));
        }

        public int getValueInt() {
            return ByteBuffer.wrap(getValue()).getInt();
        }
    }

    /**
     * IPv6 Address
     */
    public static class V6 extends IpAttribute {
        public V6(Dictionary dictionary, int vendorId, ByteBuf data) {
            super(dictionary, vendorId, data);
            checkType(Inet6Address.class, IpAttribute.convert(getValue()));
        }
    }
}
