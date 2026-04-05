package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;

import java.nio.ByteBuffer;

/**
 * This class represents a Radius attribute which only contains a 32-bit integer.
 */
public class IntegerAttribute extends OctetsAttribute {

    public static final RadiusAttributeFactory<IntegerAttribute> FACTORY = new Factory();

    /**
     * Creates a new IntegerAttribute.
     *
     * @param dictionary the dictionary to use
     * @param vendorId the vendor ID
     * @param data the attribute data
     */
    public IntegerAttribute(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf data) {
        super(dictionary, vendorId, data);
        if (!isTagged() && getValue().length != 4)
            throw new IllegalArgumentException("Integer / Date should be 4 octets, actual: " + getValue().length);
        if (isTagged() && getValue().length != 3)
            throw new IllegalArgumentException("Integer / Date should be 3 octets if has_tag, actual: " + getValue().length);
    }

    /**
     * Returns the long value of this attribute (unsigned int).
     *
     * @return long value of this attribute (unsigned int)
     */
    public long getValueLong() {
        return Integer.toUnsignedLong(getValueInt());
    }

    /**
     * Returns the int value of this attribute. May be negative as Java ints are signed.
     *
     * @return int value of this attribute. May be negative as Java ints are signed.
     */
    public int getValueInt() {
        byte[] value = getValue();
        return value.length == 4 ?
                ByteBuffer.wrap(value).getInt() :
                ByteBuffer.allocate(Integer.BYTES) // length == 3
                        .put((byte) 0)
                        .put(value)
                        .getInt(0);
    }

    /**
     * Returns the value of this attribute as a string.
     * Tries to resolve enumerations.
     */
    @Override
    @NonNull
    public String getValueString() {
        int value = getValueInt();
        return getAttributeTemplate()
                .map(at -> at.getEnumeration(value))
                .orElseGet(() -> Integer.toUnsignedString(value));
    }

    /**
     * Parses a string value into a byte array for an integer attribute.
     *
     * @param dictionary the dictionary to use
     * @param vendorId the vendor ID
     * @param type the attribute type
     * @param value the string value
     * @return byte array
     */
    @NonNull
    public static byte[] stringParser(@NonNull Dictionary dictionary, int vendorId, int type, @NonNull String value) {
        int integer = dictionary.getAttributeTemplate(vendorId, type)
                .map(at -> at.getEnumeration(value))
                .orElseGet(() -> Integer.parseUnsignedInt(value));

        var byteBuf = Unpooled.buffer(Integer.BYTES, Integer.BYTES).writeInt(integer);

        return dictionary.getAttributeTemplate(vendorId, type)
                .filter(AttributeTemplate::isTagged)
                .map(x -> byteBuf.copy(1, 3)) // skip first octet if has_tag
                .orElse(byteBuf)
                .array();
    }

    private static class Factory implements RadiusAttributeFactory<IntegerAttribute> {

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public IntegerAttribute newInstance(@NonNull Dictionary dictionary, int vendorId, @NonNull ByteBuf value) {
            return new IntegerAttribute(dictionary, vendorId, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @NonNull
        public byte[] parse(@NonNull Dictionary dictionary, int vendorId, int type, @NonNull String value) {
            return IntegerAttribute.stringParser(dictionary, vendorId, type, value);
        }
    }
}
