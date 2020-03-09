package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributesTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createAttributeKnownTypes() {
        final RadiusAttribute a1 = Attributes.create(dictionary, -1, (byte) 8, new byte[4]);
        assertEquals(IpAttribute.V4.class, a1.getClass());

        final RadiusAttribute a2 = Attributes.create(dictionary, -1, (byte) 8, "1.1.1.1");
        assertEquals(IpAttribute.V4.class, a2.getClass());

        final RadiusAttribute a3 = Attributes.create(dictionary, -1, (byte) 1, new byte[1]);
        assertEquals(StringAttribute.class, a3.getClass());

        final RadiusAttribute a4 = Attributes.create(dictionary, -1, (byte) 1, "mystring");
        assertEquals(StringAttribute.class, a4.getClass());
    }

    @Test
    void createAttributeUnknownTypes() {
        final RadiusAttribute a1 = Attributes.create(dictionary, -1, (byte) 255, new byte[5]);
        assertEquals(RadiusAttribute.class, a1.getClass());

        final RadiusAttribute a2 = Attributes.create(dictionary, -1, (byte) 255, "");
        assertEquals(RadiusAttribute.class, a2.getClass());
    }
}