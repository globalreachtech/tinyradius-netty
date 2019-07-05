package org.tinyradius.attribute;

import org.tinyradius.dictionary.AttributeType;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

/**
 * This class represents a Radius attribute which only contains a 32 bit integer.
 */
public class IntegerAttribute extends RadiusAttribute {

    public static IntegerAttribute parse(Dictionary dictionary, int vendorId, byte[] data, int offset) throws RadiusException {
        final int length = readLength(data, offset);
        if (length != 6)
            throw new RadiusException("integer attribute: expected length 6, packet declared " + length);

        return new IntegerAttribute(dictionary, vendorId, readType(data, offset), readData(data, offset));
    }

    public IntegerAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        super(dictionary, vendorId, type, data);
    }

    public IntegerAttribute(Dictionary dictionary, int vendorId, int type, int value) {
        this(dictionary, vendorId, type, convertValue(value));
    }

    public IntegerAttribute(Dictionary dictionary, int vendorId, int type, String value) {
        this(dictionary, vendorId, type, convertValue(value, dictionary, type, vendorId));
    }

    /**
     * Returns the string value of this attribute.
     *
     * @return a string
     */
    public int getAttributeValueInt() {
        byte[] data = getData();
        return (((data[0] & 0x0ff) << 24) | ((data[1] & 0x0ff) << 16) |
                ((data[2] & 0x0ff) << 8) | (data[3] & 0x0ff));
    }

    /**
     * Returns the value of this attribute as a string.
     * Tries to resolve enumerations.
     */
    @Override
    public String getDataString() {
        int value = getAttributeValueInt();
        AttributeType at = getAttributeTypeObject();
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
}
