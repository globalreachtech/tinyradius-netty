package org.tinyradius.dictionary;

import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.attribute.type.OctetsAttribute;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.attribute.type.decorator.EncodedDecorator;

import java.util.Optional;

/**
 * A dictionary retrieves AttributeTemplate objects by name or attribute ID.
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
        return createAttribute(vendorId, type, (byte) 0, value);
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param vendorId vendor ID or -1
     * @param type     attribute type
     * @param tag      tag as per RFC2868, nullable, ignored if not supported by attribute
     * @param value    attribute data as byte array
     * @return RadiusAttribute object
     */
    default RadiusAttribute createAttribute(int vendorId, byte type, byte tag, byte[] value) {
        return getAttributeTemplate(vendorId, type)
                .map(at -> at.create(this, tag, value))
                .orElseGet(() -> new OctetsAttribute(this, vendorId, type, value));
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
        return createAttribute(vendorId, type, (byte) 0, value);
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param vendorId vendor ID or -1
     * @param type     attribute type
     * @param tag      tag as per RFC2868, nullable, ignored if not supported by attribute
     * @param value    attribute data as String
     * @return RadiusAttribute object
     */
    default RadiusAttribute createAttribute(int vendorId, byte type, byte tag, String value) {
        return getAttributeTemplate(vendorId, type)
                .map(at -> at.create(this, tag, value))
                .orElseGet(() -> new OctetsAttribute(this, vendorId, type, value));
    }

    /**
     * Creates a Radius attribute.
     * Uses AttributeTemplate to lookup the type code and converts the value.
     *
     * @param name  name of the attribute, for example "NAS-IP-Address", should NOT be 'Vendor-Specific'
     * @param value value of the attribute, for example "127.0.0.1"
     * @return RadiusAttribute object
     */
    default RadiusAttribute createAttribute(String name, String value) {
        return createAttribute(name, (byte) 0, value);
    }

    /**
     * Creates a Radius attribute.
     * Uses AttributeTemplate to lookup the type code and converts the value.
     *
     * @param name  name of the attribute, for example "NAS-IP-Address", should NOT be 'Vendor-Specific'
     * @param tag   tag as per RFC2868, nullable, ignored if not supported by attribute
     * @param value value of the attribute, for example "127.0.0.1"
     * @return RadiusAttribute object
     */
    default RadiusAttribute createAttribute(String name, byte tag, String value) {
        return getAttributeTemplate(name)
                .orElseThrow(() -> new IllegalArgumentException("Unknown attribute type name: '" + name + "'"))
                .create(this, tag, value);
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param vendorId vendor ID or -1
     * @param type     attribute type
     * @param tag      tag as per RFC2868, nullable, ignored if not supported by attribute
     * @param value    attribute data as byte array
     * @return RadiusAttribute object
     */
    default EncodedDecorator createEncodedAttribute(int vendorId, byte type, byte tag, byte[] value) {
        return getAttributeTemplate(vendorId, type)
                .map(at -> at.createEncoded(this, tag, value))
                .orElseGet(() -> new EncodedDecorator(new OctetsAttribute(this, vendorId, type, value)));
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param vendorId vendor ID or -1
     * @param type     attribute type
     * @param value    attribute data as byte array
     * @return RadiusAttribute object
     */
    default EncodedDecorator parseEncodedAttribute(int vendorId, byte type, byte[] value) {
        return getAttributeTemplate(vendorId, type)
                .map(at -> at.parseEncoded(this, value))
                .orElseGet(() -> new EncodedDecorator(new OctetsAttribute(this, vendorId, type, value)));
    }

    /**
     * Retrieves an AttributeTemplate by name. This includes
     * vendor-specific attribute types whose name is prefixed
     * by the vendor name.
     *
     * @param name attribute name
     * @return AttributeTemplate object or null
     */
    Optional<AttributeTemplate> getAttributeTemplate(String name);

    /**
     * Returns the AttributeTemplate for the vendor -1 from the cache.
     *
     * @param attributeId attributeId 1-255
     * @return AttributeTemplate
     */
    default Optional<AttributeTemplate> getAttributeTemplate(byte attributeId) {
        return getAttributeTemplate(-1, attributeId);
    }

    /**
     * Retrieves an AttributeTemplate for a vendor-specific
     * attribute.
     *
     * @param vendorId    vendorId if appropriate or -1
     * @param attributeId attributeId 1-255
     * @return AttributeTemplate
     */
    Optional<AttributeTemplate> getAttributeTemplate(int vendorId, byte attributeId);

    /**
     * Retrieves the name of the vendor with the given
     * vendor code.
     *
     * @param vendorId vendor number
     * @return vendor name or null
     */
    Optional<String> getVendorName(int vendorId);

    /**
     * Retrieves the ID of the vendor with the given
     * name.
     *
     * @param vendorName name of the vendor
     * @return vendorId or -1
     */
    int getVendorId(String vendorName);

}
