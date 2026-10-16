package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.dictionary.Dictionary;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

/**
 * This class represents a Radius attribute which only contains a string.
 */
public class StringAttribute extends OctetsAttribute {

    /**
     * Default factory for creating {@link StringAttribute} instances.
     */
    public static final RadiusAttributeFactory<StringAttribute> FACTORY = new Factory();

    /**
     * Creates a new StringAttribute.
     *
     * @param dictionary the dictionary to use
     * @param vendorId   the vendor ID
     * @param data       the attribute data
     */
    public StringAttribute(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        super(dictionary, vendorId, data);
        int minLength = getHeaderSize() + getTagSize() + (codecType() == NO_ENCRYPT ? 1 : 0);
        if (!data.isReadable(minLength))
            throw new IllegalArgumentException("String attribute value should be min " + minLength + " octets, actual: " + data.readableBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String getValueString() {
        return new String(getValue(), UTF_8);
    }

    /**
     * Parses a string into a byte array using UTF-8.
     *
     * @param s string to parse
     * @return byte array
     */
    public static byte @NonNull [] stringParser(@NonNull String s) {
        return s.getBytes(UTF_8);
    }

    private static class Factory implements RadiusAttributeFactory<StringAttribute> {

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public StringAttribute newInstance(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf value) {
            return new StringAttribute(dictionary, vendorId, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public byte @NonNull [] parse(@NonNull Dictionary dictionary, int vendorId, int type, @NonNull String value) {
            return stringParser(value);
        }
    }

}
