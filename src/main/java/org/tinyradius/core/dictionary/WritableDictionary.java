package org.tinyradius.core.dictionary;

import org.tinyradius.core.attribute.AttributeTemplate;

/**
 * A dictionary that is not read-only. Provides methods
 * to add entries to the dictionary.
 */
public interface WritableDictionary extends Dictionary {

    /**
     * Adds the given vendor to the dictionary.
     *
     * @param vendor vendor
     * @throws IllegalArgumentException empty vendor name, invalid vendor ID
     */
    void addVendor(Vendor vendor);

    /**
     * Adds an AttributeTemplate object to the dictionary.
     *
     * @param attributeTemplate AttributeTemplate object
     */
    void addAttributeTemplate(AttributeTemplate attributeTemplate);

}
