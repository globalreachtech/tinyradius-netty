package org.tinyradius.core.dictionary;

import org.tinyradius.core.attribute.AttributeTemplate;

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
     * @param type type 1-255
     * @return AttributeTemplate
     */
    default Optional<AttributeTemplate> getAttributeTemplate(int type) {
        return getAttributeTemplate(-1, type);
    }

    /**
     * Retrieves an AttributeTemplate for a vendor-specific
     * attribute.
     *
     * @param vendorId    vendorId if appropriate or -1
     * @param type type 1-255
     * @return AttributeTemplate
     */
    Optional<AttributeTemplate> getAttributeTemplate(int vendorId, int type);

    /**
     * Retrieves the vendor with the given vendor code.
     *
     * @param vendorId vendor number
     * @return vendor
     */
    Optional<Vendor> getVendor(int vendorId);

    /**
     * Retrieves the ID of the vendor with the given
     * name.
     *
     * @param vendorName name of the vendor
     * @return vendorId or -1
     */
    Optional<Vendor> getVendor(String vendorName);

}
