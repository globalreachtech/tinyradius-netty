package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * This class represents a generic Radius attribute. Subclasses implement
 * methods to access the fields of special attributes.
 */
public class RadiusAttribute {

    private final Dictionary dictionary;
    private final int type;
    private final byte[] value;

    private final int vendorId; // for Vendor-Specific (sub)attributes, otherwise -1

    /**
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param value      value of attribute as byte array
     */
    RadiusAttribute(Dictionary dictionary, int vendorId, int type, byte[] value) {
        this.dictionary = requireNonNull(dictionary, "dictionary not set");
        this.vendorId = vendorId;
        if (type < 0 || type > 255)
            throw new IllegalArgumentException("attribute type invalid: " + type);
        this.type = type;
        requireNonNull(value, "attribute data not set");
        if (value.length > 253)
            throw new IllegalArgumentException("attribute data too long, max 253 octets, actual: " + value.length);
        this.value = value;
    }

    RadiusAttribute(Dictionary dictionary, int vendorId, int type, String value) {
        this(dictionary, vendorId, type, value.getBytes(UTF_8));
    }

    /**
     * @return attribute data as raw bytes
     */
    public byte[] getValue() {
        return value;
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
    public String getValueString() {
        return getHexString(value); // todo getValueString should be inverse of string constructor
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

        byte[] attr = new byte[2 + value.length];
        attr[0] = (byte) getType();
        attr[1] = (byte) (2 + value.length);
        System.arraycopy(value, 0, attr, 2, value.length);
        return attr;
    }

    public String toString() {
        StringBuilder name = new StringBuilder();

        // indent sub attributes
        if (getVendorId() != -1)
            name.append("  ");

        // determine attribute name
        AttributeType at = getAttributeType();
        if (at != null)
            name.append(at.getName());
        else if (getVendorId() != -1)
            name.append("Unknown-Sub-Attribute-").append(getType());
        else
            name.append("Unknown-Attribute-").append(getType());


        return name.append(": ").append(getValueString()).toString();
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

    // do not remove - for removing from list of attributes
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RadiusAttribute that = (RadiusAttribute) o;
        return type == that.type &&
                vendorId == that.vendorId &&
                Arrays.equals(value, that.value);
    }
}
