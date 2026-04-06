package org.tinyradius.core.dictionary;

import org.jspecify.annotations.NonNull;
import org.tinyradius.core.attribute.AttributeTemplate;

import java.util.Optional;

/**
 * A dictionary that provides methods to look up attribute templates and vendors.
 */
public interface CoreDictionary {

    /**
     * Retrieves an AttributeTemplate by name. This includes
     * vendor-specific attribute types whose name is prefixed
     * by the vendor name.
     *
     * @param name attribute name
     * @return AttributeTemplate object or null
     */
    @NonNull
    Optional<AttributeTemplate> getAttributeTemplate(@NonNull String name);

    /**
     * Returns the AttributeTemplate for the vendor -1 from the cache.
     *
     * @param type type 1-255
     * @return AttributeTemplate
     */
    @NonNull
    default Optional<AttributeTemplate> getAttributeTemplate(int type) {
        return getAttributeTemplate(-1, type);
    }

    /**
     * Retrieves an AttributeTemplate for a vendor-specific
     * attribute.
     *
     * @param vendorId vendorId if appropriate or -1
     * @param type     type 1-255
     * @return AttributeTemplate
     */
    @NonNull
    Optional<AttributeTemplate> getAttributeTemplate(int vendorId, int type);

    /**
     * Retrieves the vendor with the given vendor code.
     *
     * @param vendorId vendor number
     * @return vendor
     */
    @NonNull
    Optional<Vendor> getVendor(int vendorId);

    /**
     * Retrieves the ID of the vendor with the given
     * name.
     *
     * @param vendorName name of the vendor
     * @return vendorId or -1
     */
    @NonNull
    Optional<Vendor> getVendor(@NonNull String vendorName);

}