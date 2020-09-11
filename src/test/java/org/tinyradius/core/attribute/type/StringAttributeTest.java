package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.type.AttributeType.STRING;

class StringAttributeTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void dataBadSizes() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> STRING.create(dictionary, -1, 1, (byte) 0, "")); // User-Name

        assertEquals("String attribute value should be min 3 octets, actual: 2", exception.getCause().getMessage());
    }

    @Test
    void getDataValue() {
        final String s = new Date().toString();
        final StringAttribute stringAttribute = (StringAttribute)
                STRING.create(dictionary, -1, 1, (byte) 0, s); // User-Name
        assertEquals(s, stringAttribute.getValueString());
        assertArrayEquals(s.getBytes(UTF_8), stringAttribute.getValue());
    }
}