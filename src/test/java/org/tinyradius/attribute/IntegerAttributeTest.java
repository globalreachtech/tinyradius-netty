package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.*;

class IntegerAttributeTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void intMaxUnsigned() {
        final IntegerAttribute attribute = new IntegerAttribute(dictionary, -1, (byte) 27, -1);

        assertEquals(-1, attribute.getValueInt());
        assertEquals(0xffffffffL, attribute.getValueLong());
        assertEquals("4294967295", attribute.getValueString());
    }

    @Test
    void intMaxSigned() {
        final int value = Integer.MAX_VALUE;
        final IntegerAttribute attribute = new IntegerAttribute(dictionary, -1, (byte) 27, value);

        assertEquals(value, attribute.getValueInt());
        assertEquals(value, attribute.getValueLong());
        assertEquals(String.valueOf(value), attribute.getValueString());
    }

    @Test
    void bytesOk() {
        final byte[] bytes = new byte[]{0, 0, (byte) 0xff, (byte) 0xff};
        final IntegerAttribute attribute = new IntegerAttribute(dictionary, -1, (byte) 10, bytes);
        assertEquals(65535, attribute.getValueInt());
        assertEquals(65535, attribute.getValueLong());
        assertEquals("65535", attribute.getValueString());
    }

    @Test
    void bytesTooShort() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerAttribute(dictionary, -1, (byte) 10, new byte[2]));
        assertTrue(exception.getMessage().toLowerCase().contains("should be 4 octets"));
    }

    @Test
    void bytesTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerAttribute(dictionary, -1, (byte) 10, new byte[5]));
        assertTrue(exception.getMessage().toLowerCase().contains("should be 4 octets"));
    }

    @Test
    void stringOk() {
        final IntegerAttribute attribute = new IntegerAttribute(dictionary, -1, (byte) 10, "12345");
        assertEquals(12345, attribute.getValueInt());
        assertEquals(12345, attribute.getValueLong());
        assertEquals("12345", attribute.getValueString());
    }

    @Test
    void stringTooBig() {
        final NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> new IntegerAttribute(dictionary, -1, (byte) 27, Long.toString(0xffffffffffL)));

        assertTrue(exception.getMessage().toLowerCase().contains("exceeds range"));
    }

    @Test
    void stringEmpty() {
        final NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> new IntegerAttribute(dictionary, -1, (byte) 27, ""));

        assertTrue(exception.getMessage().toLowerCase().contains("for input string: \"\""));
    }

    @Test
    void stringEnum() {
        final IntegerAttribute attribute = new IntegerAttribute(dictionary, -1, (byte) 6, "Login-User");

        assertEquals(1, attribute.getValueInt());
        assertEquals(1, attribute.getValueLong());
        assertEquals("Login-User", attribute.getValueString());
    }

    @Test
    void stringInvalid() {
        final NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> new IntegerAttribute(dictionary, -1, (byte) 6, "badString"));

        assertTrue(exception.getMessage().toLowerCase().contains("for input string: \"badstring\""));
    }
}