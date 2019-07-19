package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.nio.ByteBuffer;

import static java.lang.Integer.*;

/**
 * This class represents a Radius attribute which only contains a 32 bit integer.
 */
public class IntegerAttribute extends RadiusAttribute {

    IntegerAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        super(dictionary, vendorId, type, data);
        if (data.length != 4)
            throw new IllegalArgumentException("integer attribute value should be 4 octets, actual: " + data.length);
    }

    IntegerAttribute(Dictionary dictionary, int vendorId, int type, String value) {
        this(dictionary, vendorId, type, convertValue(value, dictionary, type, vendorId));
    }

    public IntegerAttribute(Dictionary dictionary, int vendorId, int type, int value) {
        this(dictionary, vendorId, type, convertValue(value));
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
        int value = getValueInt();
        AttributeType at = getAttributeType();
        if (at != null) {
            String name = at.getEnumeration(value);
            if (name != null)
                return name;
        }

        return toUnsignedString(value);
    }

    /**
     * Sets the value of this attribute.
     *
     * @throws NumberFormatException if value is not a number and constant cannot be resolved
     */
    private static byte[] convertValue(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    private static int convertValue(String value, Dictionary dictionary, int attributeType, int vendorId) {
        AttributeType at = dictionary.getAttributeTypeByCode(vendorId, attributeType);
        if (at != null) {
            Integer val = at.getEnumeration(value);
            if (val != null)
                return val;
        }

        return parseUnsignedInt(value);
    }
}
