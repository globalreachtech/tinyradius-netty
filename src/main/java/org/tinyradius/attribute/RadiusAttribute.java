package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import javax.xml.bind.DatatypeConverter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * This class represents a generic Radius attribute. Subclasses implement
 * methods to access the fields of special attributes.
 */
public class RadiusAttribute {

    private final Dictionary dictionary;
    private final byte type;
    private final byte[] value;

    private final int vendorId; // for Vendor-Specific (sub)attributes, otherwise -1

    /**
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as byte array
     */
    RadiusAttribute(Dictionary dictionary, int vendorId, byte type, byte[] value) {
        this.dictionary = requireNonNull(dictionary, "Dictionary not set");
        this.vendorId = vendorId;
        this.type = type;
        requireNonNull(value, "Attribute data not set");
        if (value.length > 253)
            throw new IllegalArgumentException("Attribute data too long, max 253 octets, actual: " + value.length);
        this.value = value;
    }

    /**
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as hex string
     */
    RadiusAttribute(Dictionary dictionary, int vendorId, byte type, String value) {
        this(dictionary, vendorId, type, DatatypeConverter.parseHexBinary(value));
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
    public byte getType() {
        return type;
    }

    /**
     * @return value of this attribute as a hex string.
     */
    public String getValueString() {
        return DatatypeConverter.printHexBinary(value);
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
        attr[0] = getType();
        attr[1] = (byte) (2 + value.length);
        System.arraycopy(value, 0, attr, 2, value.length);
        return attr;
    }

    @Override
    public String toString() {
        return getAttributeKey() + ": " + getValueString();
    }

    public String getAttributeKey() {
        AttributeType at = getAttributeType();
        if (at != null)
            return at.getName();
        else if (getVendorId() != -1)
            return "Unknown-Sub-Attribute-" + getType();
        else
            return "Unknown-Attribute-" + getType();
    }

    /**
     * Returns set of entry of Attribute name and Value as string.
     * Size is generally 1, except in case of VendorSpecificAttribute
     * where it is merge of sub-attributes.
     *
     * @return Set of String/String Entry
     */
    public Map<String, String> getAttributeMap() {
        final HashMap<String, String> map = new HashMap<>();
        map.put(getAttributeKey(), getValueString());
        return map;
    }

    /**
     * @return AttributeType object for (sub-)attribute or null
     */
    public AttributeType getAttributeType() {
        return dictionary.getAttributeTypeByCode(getVendorId(), getType());
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

    @Override
    public int hashCode() {
        int result = Objects.hash(type, vendorId);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }
}
