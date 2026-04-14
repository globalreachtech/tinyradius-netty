package org.tinyradius.core.attribute.type;

import static java.lang.Byte.toUnsignedInt;

import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.dictionary.Dictionary;

/**
 * This class represents a Radius attribute for an IPv6 prefix.
 */
public class Ipv6PrefixAttribute extends OctetsAttribute {

    /**
     * Default factory for creating {@link Ipv6PrefixAttribute} instances.
     */
    public static final RadiusAttributeFactory<Ipv6PrefixAttribute> FACTORY = new Factory();

    /**
     * Creates a new Ipv6PrefixAttribute.
     *
     * @param dictionary the dictionary to use
     * @param vendorId   the vendor ID
     * @param data       the attribute data
     */
    public Ipv6PrefixAttribute(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        super(dictionary, vendorId, data);
        byte[] value = getValue();
        validate(convertBytes(value), toUnsignedInt(value[1])); // check, but don't trim
    }

    /**
     * Validates an IPv6 address and prefix length.
     *
     * @param address      the IPv6 address
     * @param prefixLength the prefix length
     * @return the encoded byte array
     */
    private static byte @NonNull [] validate(@NonNull InetAddress address, int prefixLength) {
        if (prefixLength < 0 || prefixLength > 128)
            throw new IllegalArgumentException("IPv6 Prefix Prefix-Length should be between 0 and 128, declared: " + prefixLength);

        byte[] addressBytes = address.getAddress();

        if (prefixLength < 128 && !bitsOutsidePrefixLengthZero(addressBytes, prefixLength))
            throw new IllegalArgumentException("Prefix-Length is " + prefixLength + ", bits outside of the Prefix-Length must be zero");


        int prefixBytes = (int) Math.ceil((double) prefixLength / 8); // bytes needed to hold bits

        return ByteBuffer.allocate(2 + prefixBytes)
                .put((byte) 0)
                .put((byte) prefixLength)
                .put(Arrays.copyOfRange(addressBytes, 0, prefixBytes))
                .array();
    }

    /**
     * Checks if bits outside the prefix length are zero.
     *
     * @param addressBytes the address bytes
     * @param prefixLength the prefix length
     * @return true if bits outside prefix are zero
     */
    private static boolean bitsOutsidePrefixLengthZero(byte @NonNull [] addressBytes, int prefixLength) {
        // Check that the first byte that must have some zeroed bits is conformant. This first one is special because the
        // comparison requires masking some bits, not the full byte should be zero
        boolean passed = (addressBytes[prefixLength / 8] & 0xff & (0xff >> prefixLength - 8 * (prefixLength / 8))) == 0;
        // Check the rest of the bytes
        for (int i = prefixLength / 8 + 1; i < 16; i++) {
            passed = passed && (addressBytes[i] == 0);
        }
        return passed;
    }

    /**
     * Converts a string prefix into an InetAddress.
     *
     * @param value the string value
     * @return the InetAddress
     */
    @NonNull
    private static InetAddress convertString(@NonNull String value) {
        if (value.isEmpty())
            throw new IllegalArgumentException("Invalid IPv6 prefix, empty: " + value);
        try {
            var tokens = value.split("/");

            if (tokens.length != 2)
                throw new IllegalArgumentException("Invalid IPv6 prefix expression, should be in format 'prefix/length': " + value);

            return InetAddress.getByName(tokens[0]);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Bad IPv6 prefix, invalid IPv6 address: " + value, e);
        }
    }

    /**
     * Converts a byte array into an InetAddress.
     *
     * @param data the byte array
     * @return the InetAddress
     */
    @NonNull
    private static InetAddress convertBytes(byte @NonNull [] data) {
        if (data.length < 2 || data.length > 18)
            throw new IllegalArgumentException("IPv6 Prefix body should be 2-18 octets (2-octet header + max 16 octet address), actual: " + data.length);

        int prefixLength = toUnsignedInt(data[1]);
        int availablePrefixBits = (data.length - 2) * 8;

        if (availablePrefixBits < prefixLength)
            throw new IllegalArgumentException("IPv6 Prefix Prefix-Length declared " + prefixLength + " bits, " +
                    "actual byte array only has space for " + availablePrefixBits + " bits");

        return extractAddress(data);
    }

    /**
     * Extracts the IPv6 address from the byte array.
     *
     * @param data the byte array
     * @return the InetAddress
     */
    @NonNull
    private static InetAddress extractAddress(byte @NonNull [] data) {
        try {
            var array = ByteBuffer.allocate(16).put(data, 2, data.length - 2).array();
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
    @NonNull
    public String getValueString() {
        var data = getValue();
        return extractAddress(data).getHostAddress() + "/" + toUnsignedInt(data[1]);
    }

    /**
     * Parses a string prefix into a byte array.
     *
     * @param value the string value
     * @return the byte array
     */
    public static byte @NonNull [] stringParser(@NonNull String value) {
        return validate(convertString(value), Integer.parseInt(value.split("/")[1]));
    }

    private static class Factory implements RadiusAttributeFactory<Ipv6PrefixAttribute> {

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public Ipv6PrefixAttribute newInstance(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf value) {
            return new Ipv6PrefixAttribute(dictionary, vendorId, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public byte @NonNull [] parse(@NonNull Dictionary dictionary, int vendorId, int type, @NonNull String value) {
            return Ipv6PrefixAttribute.stringParser(value);
        }
    }
}
