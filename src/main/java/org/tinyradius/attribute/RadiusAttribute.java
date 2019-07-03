package org.tinyradius.attribute;

import org.tinyradius.dictionary.AttributeType;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import static java.util.Objects.requireNonNull;

/**
 * This class represents a generic Radius attribute. Subclasses implement
 * methods to access the fields of special attributes.
 */
public class RadiusAttribute {

    //todo implement equals/hashcode

    private final Dictionary dictionary;
    private final int type;
    private final byte[] attributeData;

    private final int vendorId; //only for Vendor-Specific attributes and their sub-attributes

    public static RadiusAttribute parse(Dictionary dictionary, int vendorId, byte[] data, int offset) throws RadiusException {
        final int type = readType(data, offset);
        final byte[] bytes = readData(data, offset);
        return new RadiusAttribute(dictionary, type, vendorId, bytes);
    }

    public RadiusAttribute(Dictionary dictionary, int type, int vendorId, byte[] data) {
        this.dictionary = dictionary;
        this.vendorId = vendorId;
        if (type < 0 || type > 255)
            throw new IllegalArgumentException("attribute type invalid: " + type);
        this.type = type;
        this.attributeData = data;
    }

    /**
     * @return attribute data as raw bytes
     */
    public byte[] getAttributeData() {
        return attributeData;
    }

    /**
     * @return attribute type code, 0-255
     */
    public int getAttributeType() {
        return type;
    }

    /**
     * @return value of this attribute as a string.
     */
    public String getAttributeValue() {
        return getHexString(getAttributeData());
    }

    /**
     * @return vendor Id if Vendor-Specific attribute or sub-attribute, otherwise -1
     */
    public int getVendorId() {
        return vendorId;
    }

    /**
     * @return dictionary that attribute uses
     */
    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * @return entire attribute (including attribute type/length) as byte array
     */
    public byte[] writeAttribute() {
        if (getAttributeType() == -1)
            throw new IllegalArgumentException("attribute type not set");
        requireNonNull(attributeData, "attribute data not set");

        byte[] attr = new byte[2 + attributeData.length];
        attr[0] = (byte) getAttributeType();
        attr[1] = (byte) (2 + attributeData.length);
        System.arraycopy(attributeData, 0, attr, 2, attributeData.length);
        return attr;
    }

    /**
     * Reads in this attribute from the passed byte array.
     *
     * @param data   input data
     * @param offset byte to start reading from
     * @throws RadiusException malformed packet
     */
    protected static byte[] readData(byte[] data, int offset) throws RadiusException {
        int length = data[offset + 1] & 0x0ff;
        if (length < 2)
            throw new RadiusException("attribute length too small: " + length + ", expecting min length 2");
        byte[] attrData = new byte[length - 2];
        System.arraycopy(data, offset + 2, attrData, 0, length - 2);
        return attrData;
    }

    public static int readType(byte[] data, int offset) {
        return data[offset] & 0x0ff;
    }

    public String toString() {
        String name;

        // determine attribute name
        AttributeType at = getAttributeTypeObject();
        if (at != null)
            name = at.getName();
        else if (getVendorId() != -1)
            name = "Unknown-Sub-Attribute-" + getAttributeType();
        else
            name = "Unknown-Attribute-" + getAttributeType();

        // indent sub attributes
        if (getVendorId() != -1)
            name = "  " + name;

        return name + ": " + getAttributeValue();
    }

    /**
     * @return AttributeType object for (sub-)attribute or null
     */
    public AttributeType getAttributeTypeObject() {
        return dictionary.getAttributeTypeByCode(getVendorId(), getAttributeType());
    }

    /**
     * Returns the byte array as a hex string in the format
     * "0x1234".
     *
     * @param data byte array
     * @return hex string
     */
    private static String getHexString(byte[] data) {
        StringBuilder hex = new StringBuilder("0x");
        if (data != null)
            for (byte datum : data) {
                String digit = Integer.toString(datum & 0x0ff, 16);
                if (digit.length() < 2)
                    hex.append('0');
                hex.append(digit);
            }
        return hex.toString();
    }
}
