package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.dictionary.Dictionary;

/**
 * This class represents a Radius attribute for an IP address.
 */
public abstract class IpAttribute extends OctetsAttribute {

    private IpAttribute(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        super(dictionary, vendorId, data);
    }

    /**
     * Parses a string into an IP address byte array.
     *
     * @param value the string value
     * @return the byte array
     */
    public static byte @NonNull [] stringParser(@NonNull String value) {
        if (value.isEmpty())
            throw new IllegalArgumentException("Address can't be empty");

        try {
            return InetAddress.getByName(value).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Bad address: " + value, e);
        }
    }

    /**
     * Converts a byte array to an InetAddress.
     *
     * @param data the byte array
     * @return the InetAddress
     */
    @NonNull
    private static InetAddress convert(byte @NonNull [] data) {
        try {
            return InetAddress.getByAddress(data);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Bad address", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String getValueString() {
        return convert(getValue()).getHostAddress();
    }

    /**
     * IPv4 Address
     */
    public static class V4 extends IpAttribute {
        public static final RadiusAttributeFactory<V4> FACTORY = new Factory();

        /**
         * Creates a new IPv4 attribute.
         *
         * @param dictionary the dictionary to use
         * @param vendorId   the vendor ID
         * @param data       the attribute data
         */
        public V4(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
            super(dictionary, vendorId, data);
            if (getValue().length != 4)
                throw new IllegalArgumentException("IPv4 address should be 4 octets, actual: " + getValue().length);
        }

        /**
         * Returns the IPv4 address as an int.
         *
         * @return the IPv4 address as an int
         */
        public int getValueInt() {
            return ByteBuffer.wrap(getValue()).getInt();
        }

        private static class Factory implements RadiusAttributeFactory<V4> {

            /**
             * {@inheritDoc}
             */
            @Override
            @NonNull
            public V4 newInstance(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf value) {
                return new V4(dictionary, vendorId, value);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public byte @NonNull [] parse(@NonNull Dictionary dictionary, int vendorId, int type, @NonNull String value) {
                return IpAttribute.stringParser(value);
            }
        }
    }

    /**
     * IPv6 Address
     */
    public static class V6 extends IpAttribute {
        public static final RadiusAttributeFactory<V6> FACTORY = new Factory();

        /**
         * Creates a new IPv6 attribute.
         *
         * @param dictionary the dictionary to use
         * @param vendorId   the vendor ID
         * @param data       the attribute data
         */
        public V6(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
            super(dictionary, vendorId, data);
            if (getValue().length != 16)
                throw new IllegalArgumentException("IPv6 address should be 16 octets, actual: " + getValue().length);
        }

        private static class Factory implements RadiusAttributeFactory<V6> {

            /**
             * {@inheritDoc}
             */
            @Override
            @NonNull
            public V6 newInstance(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf value) {
                return new V6(dictionary, vendorId, value);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public byte @NonNull [] parse(@NonNull Dictionary dictionary, int vendorId, int type, @NonNull String value) {
                return IpAttribute.stringParser(value);
            }
        }
    }
}
