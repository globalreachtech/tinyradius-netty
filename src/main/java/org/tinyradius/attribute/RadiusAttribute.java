package org.tinyradius.attribute;

import org.tinyradius.dictionary.AttributeType;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.util.Arrays;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * This class represents a generic Radius attribute. Subclasses implement
 * methods to access the fields of special attributes.
 */
public class RadiusAttribute {

    private final Dictionary dictionary;
    private final int type;
    private final byte[] data;

    private final int vendorId; //only for Vendor-Specific attributes and their sub-attributes

    /**
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param data       source array to read data from
     * @param offset     offset in array to start reading from
     * @return RadiusAttribute object
     * @throws RadiusException if source data invalid or unable to create attribute for given attribute vendorId/type and data
     */
    public static RadiusAttribute parse(Dictionary dictionary, int vendorId, byte[] data, int offset) throws RadiusException {
        final int length = readLength(data, offset);
        if (length < 2)
            throw new RadiusException("Radius attribute: expected length min 2, packet declared " + length);

        return new RadiusAttribute(dictionary, vendorId, readType(data, offset), readData(data, offset));
    }

    /**
     * @param dictionary
     * @param vendorId
     * @param type
     * @param data
     */
    public RadiusAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        this.dictionary = requireNonNull(dictionary, "dictionary not set");
        this.vendorId = vendorId;
        if (type < 0 || type > 255)
            throw new IllegalArgumentException("attribute type invalid: " + type);
        this.type = type;
        this.data = requireNonNull(data, "attribute data not set");
    }

    /**
     * @return attribute data as raw bytes
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @return attribute type code, 0-255
     */
    public int getType() {
        return type;
    }

    /**
     * @return value of this attribute as a string.
     */
    public String getDataString() {
        return getHexString(data);
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
     * @return entire attribute (including headers) as byte array
     */
    public byte[] toByteArray() {

        byte[] attr = new byte[2 + data.length];
        attr[0] = (byte) getType();
        attr[1] = (byte) (2 + data.length);
        System.arraycopy(data, 0, attr, 2, data.length);
        return attr;
    }

    /**
     * Reads in this attribute from the passed byte array.
     *
     * @param data   input data
     * @param offset byte to start reading from
     * @return
     */
    protected static byte[] readData(byte[] data, int offset) {
        int length = readLength(data, offset);
        return Arrays.copyOfRange(data, offset + 2, offset + length);
    }

    public static int readType(byte[] data, int offset) {
        return data[offset] & 0x0ff;
    }

    public static int readLength(byte[] data, int offset) {
        return data[offset + 1] & 0x0ff;
    }

    public String toString() {
        String name;

        // determine attribute name
        AttributeType at = getAttributeTypeObject();
        if (at != null)
            name = at.getName();
        else if (getVendorId() != -1)
            name = "Unknown-Sub-Attribute-" + getType();
        else
            name = "Unknown-Attribute-" + getType();

        // indent sub attributes
        if (getVendorId() != -1)
            name = "  " + name;

        return name + ": " + getDataString();
    }

    /**
     * @return AttributeType object for (sub-)attribute or null
     */
    public AttributeType getAttributeTypeObject() {
        return dictionary.getAttributeTypeByCode(getVendorId(), getType());
    }

    /**
     * Returns the byte array as a hex string in the format "0x1234".
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RadiusAttribute that = (RadiusAttribute) o;
        return type == that.type &&
                vendorId == that.vendorId &&
                Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, vendorId);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
