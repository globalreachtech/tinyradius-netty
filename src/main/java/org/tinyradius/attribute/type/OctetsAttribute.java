package org.tinyradius.attribute.type;

import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.dictionary.Dictionary;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * This class represents a generic Radius attribute. Subclasses implement
 * methods to access the fields of special attributes.
 */
public class OctetsAttribute implements RadiusAttribute {

    private final Dictionary dictionary;
    private final byte type;
    private final byte[] value;

    private final int vendorId; // for Vendor-Specific sub-attributes, otherwise -1

    /**
     * @param dictionary dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as byte array, excluding type and length bytes
     */
    public OctetsAttribute(Dictionary dictionary, int vendorId, byte type, byte[] value) {
        this.dictionary = requireNonNull(dictionary, "Dictionary not set");
        this.vendorId = vendorId;
        this.type = type;
        this.value = requireNonNull(value, "Attribute data not set");
        if (value.length > 253)
            throw new IllegalArgumentException("Attribute data too long, max 253 octets, actual: " + value.length);
    }

    /**
     * @param dictionary dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as hex string
     */
    public OctetsAttribute(Dictionary dictionary, int vendorId, byte type, String value) {
        this(dictionary, vendorId, type, DatatypeConverter.parseHexBinary(value));
    }

    /**
     * @return attribute data as raw bytes
     */
    @Override
    public byte[] getValue() {
        return value;
    }

    /**
     * @return attribute type code, 0-255
     */
    @Override
    public byte getType() {
        return type;
    }

    /**
     * @return value of this attribute as a hex string.
     */
    @Override
    public String getValueString() {
        return DatatypeConverter.printHexBinary(value);
    }

    /**
     * @return vendor Id if Vendor-Specific attribute or sub-attribute, otherwise -1
     */
    @Override
    public int getVendorId() {
        return vendorId;
    }

    /**
     * @return dictionary that attribute uses
     */
    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * @return entire attribute (including headers) as byte array
     */
    @Override
    public byte[] toByteArray() {
        final int len = value.length + 2;
        return ByteBuffer.allocate(len)
                .put(getType())
                .put((byte) len)
                .put(value)
                .array();
    }

    @Override
    public String toString() {
        return getAttributeName() + ": " + getValueString();
    }

    @Override
    public String getAttributeName() {
        return getAttributeTemplate()
                .map(AttributeTemplate::getName)
                .orElse(getVendorId() != -1 ?
                        "Unknown-Sub-Attribute-" + getType() :
                        "Unknown-Attribute-" + getType());
    }

    /**
     * Returns set of all nested attributes if contains sub-attributes,
     * otherwise singleton set of current attribute.
     *
     * @return Set of String/String Entry
     */
    @Override
    public List<RadiusAttribute> flatten() {
        return Collections.singletonList(this);
    }

    /**
     * @return AttributeTemplate
     */
    @Override
    public Optional<AttributeTemplate> getAttributeTemplate() {
        return dictionary.getAttributeTemplate(getVendorId(), getType());
    }

    // do not remove - for removing from list of attributes
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OctetsAttribute that = (OctetsAttribute) o;
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
