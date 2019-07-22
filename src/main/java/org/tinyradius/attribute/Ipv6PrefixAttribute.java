package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.BitSet;

import static java.lang.Byte.toUnsignedInt;

/**
 * This class represents a Radius attribute for an IPv6 prefix.
 */
public class Ipv6PrefixAttribute extends RadiusAttribute {

    private final String prefix;

    Ipv6PrefixAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        this(dictionary, vendorId, type, convertValue(data), data);
        if (data.length < 2 || data.length > 18)
            throw new IllegalArgumentException("IPv6 Prefix body should be 2-18 octets (2-octet header + actual prefix), actual: " + data.length);
    }

    /**
     * Constructs an IPv6 prefix attribute.
     *
     * @param type  attribute type code
     * @param value value, format: "ipv6 address"/prefix
     */
    Ipv6PrefixAttribute(Dictionary dictionary, int vendorId, int type, String value) {
        this(dictionary, vendorId, type, value, convertValue(value));
    }

    private Ipv6PrefixAttribute(Dictionary dictionary, int vendorId, int type, String string, byte[] bytes) {
        super(dictionary, vendorId, type, bytes);
        this.prefix = string;
    }

    /**
     * Returns the attribute value (IP number) as a string of the format "xx.xx.xx.xx".
     */
    @Override
    public String getValueString() {
        return prefix;
    }

    private static byte[] convertValue(String value) {
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("bad IPv6 prefix empty : " + value);
        try {
            final String[] tokens = value.split("/");

            if (tokens.length != 2)
                throw new IllegalArgumentException("Invalid IPv6 prefix: " + value);

            final int prefixBits = Integer.parseInt(tokens[1]);
            final int prefixBytes = (int) Math.ceil((double) prefixBits / 8); // bytes needed to hold bits

            byte[] ipData = InetAddress.getByName(tokens[0]).getAddress();
            final BitSet bitSet = BitSet.valueOf(ipData);

            bitSet.set(prefixBits, bitSet.size(), false); // bits beyond Prefix-Length must be 0


            final ByteBuffer buffer = ByteBuffer.allocate(2 + prefixBytes); // max 18
            buffer.put((byte) 0);
            buffer.put((byte) prefixBits);
            buffer.put(bitSet.toByteArray(), 0, prefixBytes);

            return buffer.array();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad IPv6 address : " + value, e);
        }
    }

    private static String convertValue(byte[] data) {
        final int declaredPrefixBits = toUnsignedInt(data[1]); // bits
        if (declaredPrefixBits > 128)
            throw new IllegalArgumentException("IPv6 Prefix Prefix-Length should be no greater than 128 bits, declared: " + declaredPrefixBits);

        final int declaredPrefixBytes = (int) Math.ceil((double) declaredPrefixBits / 8);
        final int availablePrefixBytes = data.length - 2;

        if (availablePrefixBytes < declaredPrefixBytes)
            throw new IllegalArgumentException("IPv6 Prefix Prefix-Length declared " + declaredPrefixBits + " bits, actual byte array has space for " + availablePrefixBytes + " bytes, cannot hold declared Prefix-Length");

        try {
            final int prefix = toUnsignedInt(data[1]);
            final byte[] addressArray = ByteBuffer.allocate(16)
                    .put(data, 2, declaredPrefixBits).array(); // only get declared bits, ignore rest of address

            return InetAddress.getByAddress(addressArray).getHostAddress() + "/" + prefix;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("bad IPv6 prefix", e);
        }
    }
}
