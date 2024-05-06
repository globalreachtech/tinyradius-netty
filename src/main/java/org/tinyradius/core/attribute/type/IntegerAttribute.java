package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;

import java.nio.ByteBuffer;

/**
 * This class represents a Radius attribute which only contains a 32-bit integer.
 */
public class IntegerAttribute extends OctetsAttribute {

    public static final RadiusAttributeFactory<IntegerAttribute> FACTORY = new Factory();

    public IntegerAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        super(dictionary, vendorId, data);
        if (!isTagged() && getValue().length != 4)
            throw new IllegalArgumentException("Integer / Date should be 4 octets, actual: " + getValue().length);
        if (isTagged() && getValue().length != 3)
            throw new IllegalArgumentException("Integer / Date should be 3 octets if has_tag, actual: " + getValue().length);
    }

    /**
     * @return long value of this attribute (unsigned int)
     */
    public long getValueLong() {
        return Integer.toUnsignedLong(getValueInt());
    }

    /**
     * @return int value of this attribute. May be negative as Java ints are signed.
     */
    public int getValueInt() {
        final byte[] value = getValue();
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

        final ByteBuf byteBuf = Unpooled.buffer(Integer.BYTES, Integer.BYTES).writeInt(integer);

        return dictionary.getAttributeTemplate(vendorId, type)
                .filter(AttributeTemplate::isTagged)
                .map(x -> byteBuf.copy(1, 3)) // skip first octet if has_tag
                .orElse(byteBuf)
                .array();
    }

    private static class Factory implements RadiusAttributeFactory<IntegerAttribute> {

        @Override
        public IntegerAttribute newInstance(Dictionary dictionary, int vendorId, ByteBuf value) {
            return new IntegerAttribute(dictionary, vendorId, value);
        }

        @Override
        public byte[] parse(Dictionary dictionary, int vendorId, int type, String value) {
            return IntegerAttribute.stringParser(dictionary, vendorId, type, value);
        }
    }
}
