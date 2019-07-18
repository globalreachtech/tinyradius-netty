package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;

import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StringAttributeTest {

    @Test
    void dataBadSizes() {
        new StringAttribute(DefaultDictionary.INSTANCE, -1, 1, "");

    }

    @Test
    void getDataValue() {
        final String s = new Date().toString();
        final StringAttribute stringAttribute = new StringAttribute(DefaultDictionary.INSTANCE, -1, 1, s);

        assertEquals(s, stringAttribute.getDataString());
        assertArrayEquals(s.getBytes(UTF_8), stringAttribute.getData());
    }
}