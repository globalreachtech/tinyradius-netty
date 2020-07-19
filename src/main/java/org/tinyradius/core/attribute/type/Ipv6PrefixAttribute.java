package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.dictionary.Dictionary;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

import static java.lang.Byte.toUnsignedInt;

/**
 * This class represents a Radius attribute for an IPv6 prefix.
 */
public class Ipv6PrefixAttribute extends OctetsAttribute {

    public Ipv6PrefixAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        super(dictionary, vendorId, data);
        final byte[] value = getValue();
        validate(convertBytes(value), toUnsignedInt(value[1])); // check, but don't trim
    }

    private static byte[] validate(InetAddress address, int prefixLength) {
        if (prefixLength > 128 || prefixLength < 0)
            throw new IllegalArgumentException("IPv6 Prefix Prefix-Length should be between 0 and 128, declared: " + prefixLength);

        final BitSet bitSet = BitSet.valueOf(address.getAddress());
        final int bitSetLength = bitSet.length();

        // ensure bits beyond Prefix-Length are zero
        if (bitSetLength > prefixLength)
            throw new IllegalArgumentException("Prefix-Length is " + prefixLength + ", actual address has prefix length " + bitSetLength +
                    ", bits outside of the Prefix-Length must be zero");

        final int prefixBytes = (int) Math.ceil((double) prefixLength / 8); // bytes needed to hold bits

        byte[] addressBytes = bitSet.toByteArray();

        return ByteBuffer.allocate(2 + prefixBytes)
                .put((byte) 0)
                .put((byte) prefixLength)
                .put(addressBytes)
                .array();
    }

    private static InetAddress convertString(String value) {
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Invalid IPv6 prefix, empty: " + value);
        try {
            final String[] tokens = value.split("/");

            if (tokens.length != 2)
                throw new IllegalArgumentException("Invalid IPv6 prefix expression, should be in format 'prefix/length': " + value);

            return InetAddress.getByName(tokens[0]);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Bad IPv6 prefix, invalid IPv6 address: " + value, e);
        }
    }

    private static InetAddress convertBytes(byte[] data) {
        if (data.length < 2 || data.length > 18)
            throw new IllegalArgumentException("IPv6 Prefix body should be 2-18 octets (2-octet header + max 16 octet address), actual: " + data.length);

        final int prefixLength = toUnsignedInt(data[1]);
        final int availablePrefixBits = (data.length - 2) * 8;

        if (availablePrefixBits < prefixLength)
            throw new IllegalArgumentException("IPv6 Prefix Prefix-Length declared " + prefixLength + " bits, " +
                    "actual byte array only has space for " + availablePrefixBits + " bits");

        return extractAddress(data);
    }

    private static InetAddress extractAddress(byte[] data) {
        try {
            final byte[] array = ByteBuffer.allocate(16).put(data, 2, data.length - 2).array();
            return InetAddress.getByAddress(array);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Bad IPv6 prefix, invalid IPv6 address: "
                    + Arrays.toString(Arrays.copyOfRange(data, 2, data.length)), e);
        }
    }

    /**
     * Returns the attribute value as a string of the format "x:x:x:x:x:x:x:x/yy".
     */
    @Override
    public String getValueString() {
        final byte[] data = getValue();
        return extractAddress(data).getHostAddress() + "/" + toUnsignedInt(data[1]);
    }

    public static byte[] stringParser(String value) {
        return validate(convertString(value), Integer.parseInt(value.split("/")[1]));
    }
}
