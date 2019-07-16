package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.util.Arrays;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
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
     * @return RadiusAttribute object
     */
    RadiusAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        this.dictionary = requireNonNull(dictionary, "dictionary not set");
        this.vendorId = vendorId;
        if (type < 0 || type > 255)
            throw new IllegalArgumentException("attribute type invalid: " + type);
        this.type = type;
        requireNonNull(data, "attribute data not set");
        if (data.length > 253)
            throw new IllegalArgumentException("attribute data too long, max 255 octets, actual: " + data.length);
        this.data = data;
    }

    RadiusAttribute(Dictionary dictionary, int vendorId, int type, String data) {
        this(dictionary, vendorId, type, data.getBytes(UTF_8));
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

    public String toString() {
        String name;

        // determine attribute name
        AttributeType at = getAttributeType();
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
    public AttributeType getAttributeType() {
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
