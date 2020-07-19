package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.dictionary.Dictionary;

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

    public static byte[] stringParser(String value) {
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
            if (getValue().length != 4)
                throw new IllegalArgumentException("IPv4 address should be 4 octets, actual: " + getValue().length);
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
            if (getValue().length != 16)
                throw new IllegalArgumentException("IPv6 address should be 16 octets, actual: " + getValue().length);
        }
    }
}
