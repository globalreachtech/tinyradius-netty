package org.tinyradius.dictionary;

import org.tinyradius.attribute.AttributeFactory;
import org.tinyradius.attribute.RadiusAttribute;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Represents a Radius attribute type.
 */
public class AttributeType<T extends RadiusAttribute> {

    private final int vendorId;
    private final int typeCode;
    private final String name;
    private final AttributeFactory attributeFactory;
    private final Map<Integer, String> enumeration = new HashMap<>();

    /**
     * Create a new attribute type.
     *
     * @param code             Radius attribute type code
     * @param name             Attribute type name
     * @param attributeFactory RadiusAttribute descendant who handles attributes of this type
     */
    public AttributeType(int code, String name, AttributeFactory attributeFactory) {
        this(-1, code, name, attributeFactory);
    }

    /**
     * Constructs a Vendor-Specific sub-attribute type.
     *
     * @param vendorId         vendor ID
     * @param code             sub-attribute type code
     * @param name             sub-attribute name
     * @param attributeFactory sub-attribute class
     */
    public AttributeType(int vendorId, int code, String name, AttributeFactory attributeFactory) {
        if (code < 1 || code > 255)
            throw new IllegalArgumentException("code out of bounds");
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name is empty");
        requireNonNull(attributeFactory, "type is null");
        this.vendorId = vendorId;
        this.typeCode = code;
        this.name = name;
        this.attributeFactory = attributeFactory;
    }

    /**
     * Retrieves the Radius type code for this attribute type.
     *
     * @return Radius type code
     */
    public int getTypeCode() {
        return typeCode;
    }

    /**
     * Retrieves the name of this type.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the RadiusAttribute descendant class which represents
     * attributes of this type.
     *
     * @return class
     */
    public AttributeFactory getAttributeFactory() {
        return attributeFactory;
    }

    /**
     * Returns the vendor ID.
     * No vendor specific attribute = -1
     *
     * @return vendor ID
     */
    public int getVendorId() {
        return vendorId;
    }

    /**
     * @param value int value
     * @return the name of the given integer value if this attribute
     * is an enumeration, or null if it is not or if the integer value
     * is unknown.
     */
    public String getEnumeration(int value) {
        return enumeration.get(value);
    }

    /**
     * @param value string value
     * @return the number of the given string value if this attribute is
     * an enumeration, or null if it is not or if the string value is unknown.
     */
    public Integer getEnumeration(String value) {
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("value is empty");
        for (Map.Entry<Integer, String> e : enumeration.entrySet()) {
            if (e.getValue().equals(value))
                return e.getKey();
        }
        return null;
    }

    /**
     * Adds a name for an integer value of this attribute.
     *
     * @param num  number that shall get a name
     * @param name the name for this number
     */
    public void addEnumerationValue(int num, String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name is empty");
        enumeration.put(num, name);
    }

    /**
     * String representation of AttributeType object
     * for debugging purposes.
     *
     * @return string
     */
    public String toString() {
        String s = getTypeCode() + "/" + getName() + ": " + attributeFactory.getClass();
        if (getVendorId() != -1)
            s += " (vendor " + getVendorId() + ")";
        return s;
    }
}
