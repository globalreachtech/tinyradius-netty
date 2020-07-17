package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.dictionary.Dictionary;

import java.nio.ByteBuffer;

/**
 * This class represents a Radius attribute which only contains a 32 bit integer.
 */
public class IntegerAttribute extends OctetsAttribute {


    public IntegerAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        super(dictionary, vendorId, data);
        if (data.readableBytes() != 6)
            throw new IllegalArgumentException("Integer / Date attribute should be 4 octets, actual: " + data.readableBytes());
    }

    /**
     * Returns the long value of this attribute.
     *
     * @return a long
     */
    public long getValueLong() {
        return Integer.toUnsignedLong(getValueInt());
    }

    /**
     * Returns the int value of this attribute. May be negative as Java ints are signed.
     *
     * @return an int
     */
    public int getValueInt() {
        return ByteBuffer.wrap(getValue()).getInt();
    }

    /**
     * Returns the value of this attribute as a string.
     * Tries to resolve enumerations.
     */
    @Override
    public String getValueString() {
        final int value = getValueInt();
        return getAttributeTemplate()
                .map(at -> at.getEnumeration(value))
                .orElseGet(() -> Integer.toUnsignedString(value));
    }

    public static byte[] stringParser(Dictionary dictionary, int vendorId, int type, String value) {
        final int integer = dictionary.getAttributeTemplate(vendorId, type)
                .map(at -> at.getEnumeration(value))
                .orElseGet(() -> Integer.parseUnsignedInt(value));

        return ByteBuffer.allocate(Integer.BYTES).putInt(integer).array();
    }
}
