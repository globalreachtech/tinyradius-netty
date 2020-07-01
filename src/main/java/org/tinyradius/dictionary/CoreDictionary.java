package org.tinyradius.dictionary;

import org.tinyradius.attribute.AttributeTemplate;

import java.util.Optional;

public interface CoreDictionary {

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
