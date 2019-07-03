package org.tinyradius.attribute;

import org.tinyradius.dictionary.AttributeType;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

public class RadiusAttributeBuilder {
    /**
     * Creates a RadiusAttribute object of the appropriate type.
     *
     * @param dictionary    Dictionary to use
     * @param vendorId      vendor ID or -1
     * @param attributeType attribute type
     * @return RadiusAttribute object
     */
    public static RadiusAttribute createRadiusAttribute(Dictionary dictionary, int vendorId, int attributeType) {
        RadiusAttribute attribute = new RadiusAttribute();

        AttributeType<?> at = dictionary.getAttributeTypeByCode(vendorId, attributeType);
        if (at != null && at.getAttributeClass() != null) {
            attribute = at.getAttributeClass().getDeclaredConstructor().newInstance();
        }

        attribute.setDictionary(dictionary);
        return attribute;
    }

    /**
     * Creates a Radius attribute, including vendor-specific
     * attributes. The default dictionary is used.
     *
     * @param vendorId      vendor ID or -1
     * @param attributeType attribute type
     * @return RadiusAttribute INSTANCE
     */
    public static RadiusAttribute createRadiusAttribute(int vendorId, int attributeType) {
        Dictionary dictionary = DefaultDictionary.INSTANCE;
        return createRadiusAttribute(dictionary, vendorId, attributeType);
    }

    /**
     * Creates a Radius attribute. The default dictionary is
     * used.
     *
     * @param attributeType attribute type
     * @return RadiusAttribute INSTANCE
     */
    public static RadiusAttribute createRadiusAttribute(int attributeType) {
        Dictionary dictionary = DefaultDictionary.INSTANCE;
        return createRadiusAttribute(dictionary, -1, attributeType);
    }
}
