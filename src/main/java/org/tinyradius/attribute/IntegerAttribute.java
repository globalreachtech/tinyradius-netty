package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.nio.ByteBuffer;

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
     * Returns the string value of this attribute.
     *
     * @return a string
     */
    public int getAttributeValueInt() {
        return ByteBuffer.wrap(getData()).getInt();
    }

    /**
     * Returns the value of this attribute as a string.
     * Tries to resolve enumerations.
     */
    @Override
    public String getDataString() {
        int value = getAttributeValueInt();
        AttributeType at = getAttributeType();
        if (at != null) {
            String name = at.getEnumeration(value);
            if (name != null)
                return name;
        }
        // Radius uses only unsigned values....
        return Long.toString(((long) value & 0xffffffffL));
    }

    /**
     * Sets the value of this attribute.
     *
     * @throws NumberFormatException if value is not a number and constant cannot be resolved
     */
    private static byte[] convertValue(int value) {
        byte[] data = new byte[4];
        data[0] = (byte) (value >> 24 & 0x0ff);
        data[1] = (byte) (value >> 16 & 0x0ff);
        data[2] = (byte) (value >> 8 & 0x0ff);
        data[3] = (byte) (value & 0x0ff);
        return data;
    }

    private static byte[] convertValue(String value, Dictionary dictionary, int attributeType, int vendorId) {
        if (isOverflow(value)) {
            return new byte[8];
        }

        AttributeType at = dictionary.getAttributeTypeByCode(vendorId, attributeType);
        if (at != null) {
            Integer val = at.getEnumeration(value);
            if (val != null) {
                return convertValue(val);
            }
        }

        // Radius uses only unsigned integers for this the parser should consider as Long to parse high bit correctly...
        return convertValue((int) Long.parseLong(value));
    }

    private static boolean isOverflow(String value) {
        return Long.parseLong(value) > 0xffffffffL || Long.parseLong(value) < 0;
    }
}
