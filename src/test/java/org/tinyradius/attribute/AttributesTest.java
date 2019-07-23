package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.attribute.Attributes.createAttribute;

class AttributesTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createAttributeKnownTypes() {
        final RadiusAttribute a1 = createAttribute(dictionary, -1, 8, new byte[4]);
        assertEquals(IpAttribute.V4.class, a1.getClass());

        final RadiusAttribute a2 = createAttribute(dictionary, -1, 8, "1.1.1.1");
        assertEquals(IpAttribute.V4.class, a2.getClass());

        final RadiusAttribute a3 = createAttribute(dictionary, -1, 1, new byte[1]);
        assertEquals(StringAttribute.class, a3.getClass());

        final RadiusAttribute a4 = createAttribute(dictionary, -1, 1, "mystring");
        assertEquals(StringAttribute.class, a4.getClass());
    }

    @Test
    void createAttributeUnknownTypes() {
        final RadiusAttribute a1 = createAttribute(dictionary, -1, 255, new byte[5]);
        assertEquals(RadiusAttribute.class, a1.getClass());

        final RadiusAttribute a2 = createAttribute(dictionary, -1, 255, "");
        assertEquals(RadiusAttribute.class, a2.getClass());
    }
}