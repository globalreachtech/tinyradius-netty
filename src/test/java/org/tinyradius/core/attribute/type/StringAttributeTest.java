package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class StringAttributeTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void dataBadSizes() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 1, "")); // User-Name

        assertEquals("String attribute value should be min 3 octets, actual: 2", exception.getMessage());
    }

    @Test
    void getDataValue() {
        final String s = new Date().toString();
        final RadiusAttribute stringAttribute = dictionary.createAttribute(-1, 1, s); // User-Name
        assertTrue(stringAttribute instanceof StringAttribute);
        assertEquals(s, stringAttribute.getValueString());
        assertArrayEquals(s.getBytes(UTF_8), stringAttribute.getValue());
    }
}