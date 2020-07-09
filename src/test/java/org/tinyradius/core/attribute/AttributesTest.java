package org.tinyradius.core.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.attribute.type.IpAttribute;
import org.tinyradius.core.attribute.type.OctetsAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.StringAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttributesTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createAttributeKnownTypes() {
        final RadiusAttribute a1 = dictionary.createAttribute(-1, 8, new byte[4]);
        assertEquals(IpAttribute.V4.class, a1.getClass());

        final RadiusAttribute a2 = dictionary.createAttribute(-1, 8, "1.1.1.1");
        assertEquals(IpAttribute.V4.class, a2.getClass());

        final RadiusAttribute a3 = dictionary.createAttribute(-1, 1, new byte[1]);
        assertEquals(StringAttribute.class, a3.getClass());

        final RadiusAttribute a4 = dictionary.createAttribute(-1, 1, "mystring");
        assertEquals(StringAttribute.class, a4.getClass());
    }

    @Test
    void createAttributeUnknownTypes() {
        final RadiusAttribute a1 = dictionary.createAttribute(-1, 255, new byte[5]);
        assertEquals(OctetsAttribute.class, a1.getClass());

        final RadiusAttribute a2 = dictionary.createAttribute(-1, 255, "");
        assertEquals(OctetsAttribute.class, a2.getClass());
    }
}