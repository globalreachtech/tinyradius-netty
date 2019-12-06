package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import java.security.SecureRandom;

import static java.lang.Byte.toUnsignedInt;
import static org.junit.jupiter.api.Assertions.*;

class RadiusAttributeTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createMaxSizeAttribute() {
        // 253 octets ok
        final RadiusAttribute maxSizeAttribute = new RadiusAttribute(dictionary, -1, 2, random.generateSeed(253));
        final byte[] bytes = maxSizeAttribute.toByteArray();

        assertEquals(0xFF, toUnsignedInt(bytes[1]));
        assertEquals(255, toUnsignedInt(bytes[1]));
        assertEquals(255, bytes.length);

        // 254 octets not ok
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new RadiusAttribute(dictionary, -1, 2, random.generateSeed(254)));

        assertTrue(exception.getMessage().contains("too long"));
    }

    @Test
    void testToMap() {
        final RadiusAttribute attribute = new RadiusAttribute(dictionary, -1, 2, "123456");
        assertEquals("{User-Password=123456}", attribute.toAttributeMap().toString());
    }

    @Test
    void testToString() {
        final RadiusAttribute attribute = new RadiusAttribute(dictionary, -1, 2, "123456");
        assertEquals("User-Password: 123456", attribute.toString());
    }
}