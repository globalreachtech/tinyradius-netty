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

        byte[] addressBytes = address.getAddress();
            
        if(prefixLength < 128){
            // Ensure bits beyond Prefix-Length are zero
            // Check that the first byte that must have some zeroed bits is conformant. This first one is special because the
            // comparison requires masking some bits, not the full byte should be zero
            boolean passed = (addressBytes[prefixLength/8] &0xff & (0xff >> prefixLength - 8*(prefixLength/8))) == 0;
            // Check the rest of the bytes
            for(int i = prefixLength/8 + 1; i < 16; i++){
                passed = passed && (addressBytes[i] == 0);
            }

            // Throw exception if validation is not passed
            if(!passed) throw new IllegalArgumentException("Prefix-Length is " + prefixLength + ", bits outside of the Prefix-Length must be zero");

        }
        
        final int prefixBytes = (int) Math.ceil((double) prefixLength / 8); // bytes needed to hold bits

        return ByteBuffer.allocate(2 + prefixBytes)
                .put((byte) 0)
                .put((byte) prefixLength)
                .put(Arrays.copyOfRange(addressBytes, 0, prefixBytes))
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
