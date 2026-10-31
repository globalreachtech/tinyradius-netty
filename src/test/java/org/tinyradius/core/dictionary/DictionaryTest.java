package org.tinyradius.core.dictionary;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.type.OctetsAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.StringAttribute;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

class DictionaryTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createAttributeString() {
        // parsed as string
        Optional<AttributeTemplate> exists = dictionary.getAttributeTemplate(-1, 1);
        assertTrue(exists.isPresent());

        RadiusAttribute string = dictionary.createAttribute(-1, 1, (byte) 0, "00");
        assertEquals(StringAttribute.class, string.getClass());
        assertEquals("User-Name", string.getAttributeName());

        // parsed as octets if attributeTemplate not found
        Optional<AttributeTemplate> noExists = dictionary.getAttributeTemplate(999, 1);
        assertFalse(noExists.isPresent());

        RadiusAttribute octets = dictionary.createAttribute(999, 1, (byte) 0, "00");
        assertEquals(OctetsAttribute.class, octets.getClass()); // check not subclass
        assertTrue(octets.getAttributeName().contains("Unknown"));
    }

    @Test
    void createAttributeBytes() {
        // parsed as string
        Optional<AttributeTemplate> exists = dictionary.getAttributeTemplate(-1, 1);
        assertTrue(exists.isPresent());

        RadiusAttribute string = dictionary.createAttribute(-1, 1, (byte) 0, new byte[1]);
        assertEquals(StringAttribute.class, string.getClass());
        assertEquals("User-Name", string.getAttributeName());

        // parsed as octets if attributeTemplate not found
        Optional<AttributeTemplate> noExists = dictionary.getAttributeTemplate(999, 1);
        assertFalse(noExists.isPresent());

        RadiusAttribute octets = dictionary.createAttribute(999, 1, (byte) 0, new byte[1]);
        assertEquals(OctetsAttribute.class, octets.getClass()); // check not subclass
        assertTrue(octets.getAttributeName().contains("Unknown"));
    }

    @Test
    void createAttributeByteBuf() {
        // parsed as string
        Optional<AttributeTemplate> exists = dictionary.getAttributeTemplate(-1, 1);
        assertTrue(exists.isPresent());

        RadiusAttribute string = dictionary.createAttribute(-1, 1, Unpooled.wrappedBuffer(new byte[]{1, 3, 0}));
        assertEquals(StringAttribute.class, string.getClass());
        assertEquals("User-Name", string.getAttributeName());

        // parsed as octets if attributeTemplate not found
        Optional<AttributeTemplate> noExists = dictionary.getAttributeTemplate(999, 1);
        assertFalse(noExists.isPresent());

        RadiusAttribute octets = dictionary.createAttribute(999, 1, Unpooled.wrappedBuffer(new byte[]{1, 3, 0}));
        assertEquals(OctetsAttribute.class, octets.getClass()); // check not subclass
        assertTrue(octets.getAttributeName().contains("Unknown"));
    }

    @Test
    void memoryDictionaryDuplicates() {
        var memDict = new MemoryDictionary();
        var vendor1 = new Vendor(100, "Vendor100", 1, 1);
        var vendor1Same = new Vendor(100, "Vendor100", 1, 1);
        var vendor1Conflict = new Vendor(100, "OtherVendor", 1, 1);

        memDict.addVendor(vendor1);
        memDict.addVendor(vendor1Same); // duplicate equal vendor ignored
        assertThrows(IllegalArgumentException.class, () -> memDict.addVendor(vendor1Conflict));

        var attr1 = new AttributeTemplate(-1, 10, "Attr-10", "string", StringAttribute.FACTORY, NO_ENCRYPT, false);
        var attr1Same = new AttributeTemplate(-1, 10, "Attr-10", "string", StringAttribute.FACTORY, NO_ENCRYPT, false);
        var attr1ConflictName = new AttributeTemplate(-1, 11, "Attr-10", "octets", OctetsAttribute.FACTORY, NO_ENCRYPT, false);
        var attr1SameCodeDiffName = new AttributeTemplate(-1, 10, "Attr-10-Alias", "string", StringAttribute.FACTORY, NO_ENCRYPT, false);

        memDict.addAttributeTemplate(attr1);
        memDict.addAttributeTemplate(attr1Same); // duplicate equal template ignored
        assertThrows(IllegalArgumentException.class, () -> memDict.addAttributeTemplate(attr1ConflictName));

        memDict.addAttributeTemplate(attr1SameCodeDiffName); // overwrites code mapping with warning
        assertEquals("Attr-10-Alias", memDict.getAttributeTemplate(-1, 10).orElseThrow().name());
    }
}