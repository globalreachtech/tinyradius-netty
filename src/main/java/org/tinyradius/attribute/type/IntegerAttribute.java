package org.tinyradius.attribute.type;

import org.tinyradius.dictionary.Dictionary;

import java.nio.ByteBuffer;

import static java.lang.Integer.*;

/**
 * This class represents a Radius attribute which only contains a 32 bit integer.
 */
public class IntegerAttribute extends RadiusAttribute {

    public IntegerAttribute(Dictionary dictionary, int vendorId, byte type, byte[] data) {
        super(dictionary, vendorId, type, data);
        if (data.length != 4)
            throw new IllegalArgumentException("Integer attribute value should be 4 octets, actual: " + data.length);
    }

    public IntegerAttribute(Dictionary dictionary, int vendorId, byte type, String value) {
        this(dictionary, vendorId, type, convertValue(value, dictionary, type, vendorId));
    }

    public IntegerAttribute(Dictionary dictionary, int vendorId, byte type, int value) {
        this(dictionary, vendorId, type, convertValue(value));
    }

    /**
     * Sets the value of this attribute.
     *
     * @throws NumberFormatException if value is not a number and constant cannot be resolved
     */
    private static byte[] convertValue(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private static int convertValue(String value, Dictionary dictionary, byte attributeId, int vendorId) {
        return dictionary.getAttributeTemplate(vendorId, attributeId)
                .map(at -> at.getEnumeration(value))
                .orElseGet(() -> parseUnsignedInt(value));
    }

    /**
     * Returns the long value of this attribute.
     *
     * @return a long
     */
    public long getValueLong() {
        return toUnsignedLong(getValueInt());
    }

    /**
     * Returns the int value of this attribute.
     *
     * @return a int
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
                .orElseGet(() -> toUnsignedString(value));
    }
}
