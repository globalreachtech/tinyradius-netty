package org.tinyradius.core.dictionary;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.type.OctetsAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.StringAttribute;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DictionaryTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createAttributeString() {
        // parsed as string
        final Optional<AttributeTemplate<?>> exists = dictionary.getAttributeTemplate(-1, 1);
        assertTrue(exists.isPresent());

        final RadiusAttribute string = dictionary.createAttribute(-1, 1, (byte) 0, "00");
        assertEquals(StringAttribute.class, string.getClass());
        assertEquals("User-Name", string.getAttributeName());

        // parsed as octets if attributeTemplate not found
        final Optional<AttributeTemplate<?>> noExists = dictionary.getAttributeTemplate(999, 1);
        assertFalse(noExists.isPresent());

        final RadiusAttribute octets = dictionary.createAttribute(999, 1, (byte) 0, "00");
        assertEquals(OctetsAttribute.class, octets.getClass()); // check not subclass
        assertTrue(octets.getAttributeName().contains("Unknown"));
    }

    @Test
    void createAttributeBytes() {
        // parsed as string
        final Optional<AttributeTemplate<?>> exists = dictionary.getAttributeTemplate(-1, 1);
        assertTrue(exists.isPresent());

        final RadiusAttribute string = dictionary.createAttribute(-1, 1, (byte) 0, new byte[1]);
        assertEquals(StringAttribute.class, string.getClass());
        assertEquals("User-Name", string.getAttributeName());

        // parsed as octets if attributeTemplate not found
        final Optional<AttributeTemplate<?>> noExists = dictionary.getAttributeTemplate(999, 1);
        assertFalse(noExists.isPresent());

        final RadiusAttribute octets = dictionary.createAttribute(999, 1, (byte) 0, new byte[1]);
        assertEquals(OctetsAttribute.class, octets.getClass()); // check not subclass
        assertTrue(octets.getAttributeName().contains("Unknown"));
    }

    @Test
    void createAttributeByteBuf() {
        // parsed as string
        final Optional<AttributeTemplate<?>> exists = dictionary.getAttributeTemplate(-1, 1);
        assertTrue(exists.isPresent());

        final RadiusAttribute string = dictionary.createAttribute(-1, 1, Unpooled.wrappedBuffer(new byte[]{1, 3, 0}));
        assertEquals(StringAttribute.class, string.getClass());
        assertEquals("User-Name", string.getAttributeName());

        // parsed as octets if attributeTemplate not found
        final Optional<AttributeTemplate<?>> noExists = dictionary.getAttributeTemplate(999, 1);
        assertFalse(noExists.isPresent());

        final RadiusAttribute octets = dictionary.createAttribute(999, 1, Unpooled.wrappedBuffer(new byte[]{1, 3, 0}));
        assertEquals(OctetsAttribute.class, octets.getClass()); // check not subclass
        assertTrue(octets.getAttributeName().contains("Unknown"));
    }
}