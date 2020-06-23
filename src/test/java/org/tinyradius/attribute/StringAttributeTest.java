package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.type.StringAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class StringAttributeTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void dataBadSizes() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new StringAttribute(dictionary, -1, (byte) 1, ""));

        assertTrue(exception.getMessage().toLowerCase().contains("min 1 octet"));
    }

    @Test
    void getDataValue() {
        final String s = new Date().toString();
        final StringAttribute stringAttribute = new StringAttribute(dictionary, -1, (byte) 1, s);

        assertEquals(s, stringAttribute.getValueString());
        assertArrayEquals(s.getBytes(UTF_8), stringAttribute.getValue());
    }
}