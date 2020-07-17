package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.*;

class IntegerAttributeTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void intMaxUnsigned() {
        final IntegerAttribute attribute = (IntegerAttribute) dictionary.createAttribute(-1, 27, "-1"); // Session-Timeout

        assertEquals(-1, attribute.getValueInt());
        assertEquals(0xffffffffL, attribute.getValueLong());
        assertEquals("4294967295", attribute.getValueString());
    }

    @Test
    void intMaxSigned() {
        final int value = Integer.MAX_VALUE;
        final IntegerAttribute attribute = (IntegerAttribute) dictionary.createAttribute(-1, 27, String.valueOf(value)); // Session-Timeout

        assertEquals(value, attribute.getValueInt());
        assertEquals(value, attribute.getValueLong());
        assertEquals(String.valueOf(value), attribute.getValueString());
    }

    @Test
    void bytesOk() {
        final byte[] bytes = new byte[]{0, 0, (byte) 0xff, (byte) 0xff};
        final IntegerAttribute attribute = (IntegerAttribute) dictionary.createAttribute(-1, 10, bytes); // Framed-Routing

        assertEquals(65535, attribute.getValueInt());
        assertEquals(65535, attribute.getValueLong());
        assertEquals("65535", attribute.getValueString());
    }

    @Test
    void bytesTooShort() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 10, new byte[2])); // Framed-Routing
        assertTrue(exception.getMessage().toLowerCase().contains("should be 4 octets"));
    }

    @Test
    void bytesTooLong() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> dictionary.createAttribute(-1, 10, new byte[5])); // Framed-Routing
        assertTrue(exception.getMessage().toLowerCase().contains("should be 4 octets"));
    }

    @Test
    void stringOk() {
        final IntegerAttribute attribute = (IntegerAttribute) dictionary.createAttribute(-1, 10, "12345"); // Framed-Routing

        assertEquals(12345, attribute.getValueInt());
        assertEquals(12345, attribute.getValueLong());
        assertEquals("12345", attribute.getValueString());
    }

    @Test
    void stringTooBig() {
        final String strLong = Long.toString(0xffffffffffL);
        final NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> dictionary.createAttribute(-1, 27, strLong)); // Session-Timeout

        assertTrue(exception.getMessage().toLowerCase().contains("exceeds range"));
    }

    @Test
    void stringEmpty() {
        final NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> dictionary.createAttribute(-1, 27, "")); // Session-Timeout

        assertTrue(exception.getMessage().toLowerCase().contains("for input string: \"\""));
    }

    @Test
    void stringEnum() {
        final IntegerAttribute attribute = (IntegerAttribute) dictionary.createAttribute(-1, 6, "Login-User"); // Service-Type

        assertEquals(1, attribute.getValueInt());
        assertEquals(1, attribute.getValueLong());
        assertEquals("Login-User", attribute.getValueString());
    }

    @Test
    void stringInvalid() {
        final NumberFormatException exception = assertThrows(NumberFormatException.class,
                () -> dictionary.createAttribute(-1, 6, "badString")); // Service-Type

        assertTrue(exception.getMessage().toLowerCase().contains("for input string: \"badstring\""));
    }
}