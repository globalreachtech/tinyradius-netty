package org.tinyradius.dictionary;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.util.AttributeType;

/**
 * A dictionary retrieves AttributeType objects by name or
 * type code.
 */
public interface Dictionary {

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param vendorId vendor ID or -1
     * @param type     attribute type
     * @param value    attribute data as byte array
     * @return RadiusAttribute object
     */
    default RadiusAttribute createAttribute(int vendorId, byte type, byte[] value) {
        final AttributeType attributeType = getAttributeTypeByCode(vendorId, type);
        if (attributeType != null)
            return attributeType.create(this, value);

        return new RadiusAttribute(this, vendorId, type, value);
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param vendorId vendor ID or -1
     * @param type     attribute type
     * @param value    attribute data as String
     * @return RadiusAttribute object
     */
    default RadiusAttribute createAttribute(int vendorId, byte type, String value) {
        final AttributeType attributeType = getAttributeTypeByCode(vendorId, type);
        if (attributeType != null)
            return attributeType.create(this, value);

        return new RadiusAttribute(this, vendorId, type, value);
    }

    /**
     * Creates a Radius attribute.
     * Uses AttributeTypes to lookup the type code and converts the value.
     *
     * @param name  name of the attribute, for example "NAS-IP-Address", should NOT be 'Vendor-Specific'
     * @param value value of the attribute, for example "127.0.0.1"
     * @return RadiusAttribute object
     * @throws IllegalArgumentException if type name or value is invalid
     */
    default RadiusAttribute createAttribute(String name, String value) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Type name is null/empty");
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Value is null/empty");

        final AttributeType type = getAttributeTypeByName(name);
        if (type == null)
            throw new IllegalArgumentException("Unknown attribute type name'" + name + "'");

        return type.create(this, value);
    }

    /**
     * Retrieves an attribute type by name. This includes
     * vendor-specific attribute types whose name is prefixed
     * by the vendor name.
     *
     * @param typeName name of the attribute type
     * @return AttributeType object or null
     */
    AttributeType getAttributeTypeByName(String typeName);

    /**
     * Returns the AttributeType for the vendor -1 from the cache.
     *
     * @param type type code
     * @return AttributeType object or null
     */
    default AttributeType getAttributeTypeByCode(byte type) {
        return getAttributeTypeByCode(-1, type);
    }

    /**
     * Retrieves an attribute type for a vendor-specific
     * attribute.
     *
     * @param vendorId vendor ID
     * @param type     type code, 1-255
     * @return AttributeType object or null
     */
    AttributeType getAttributeTypeByCode(int vendorId, byte type);

    /**
     * Retrieves the name of the vendor with the given
     * vendor code.
     *
     * @param vendorId vendor number
     * @return vendor name or null
     */
    String getVendorName(int vendorId);

    /**
     * Retrieves the ID of the vendor with the given
     * name.
     *
     * @param vendorName name of the vendor
     * @return vendor ID or -1
     */
    int getVendorId(String vendorName);


}
