package org.tinyradius.attribute.type;

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
        final RadiusAttribute maxSizeAttribute = new OctetsAttribute(dictionary, -1, (byte) 2, random.generateSeed(253));
        final byte[] bytes = maxSizeAttribute.toByteArray();

        assertEquals(0xFF, toUnsignedInt(bytes[1]));
        assertEquals(255, toUnsignedInt(bytes[1]));
        assertEquals(255, bytes.length);

        // 254 octets not ok
        final byte[] oversizedArray = random.generateSeed(254);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new OctetsAttribute(dictionary, -1, (byte) 2, oversizedArray));

        assertTrue(exception.getMessage().contains("too long"));
    }

    @Test
    void testFlatten() {
        final RadiusAttribute attribute = new OctetsAttribute(dictionary, -1, (byte) 2, "123456");
        assertEquals("[User-Password: 123456]", attribute.flatten().toString());
    }

    @Test
    void testToString() {
        final RadiusAttribute attribute = new OctetsAttribute(dictionary, -1, (byte) 2, "123456");
        assertEquals("User-Password: 123456", attribute.toString());
    }
}