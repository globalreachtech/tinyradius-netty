package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;

import static org.junit.jupiter.api.Assertions.*;

class IntegerAttributeTest {

    @Test
    void testLargestUnsignedInt() {
        final long bigValue = 0xffffffffL; // big value with highest bit set
        final String bigValueSt = Long.toString(bigValue);
        final IntegerAttribute intAttr = new IntegerAttribute(DefaultDictionary.INSTANCE, -1, 27, bigValueSt);
        assertEquals(bigValueSt, intAttr.getDataString());
    }

    @Test
    void getAttributeValueInt() {
    }

    @Test
    void getDataString() {
    }

    @Test
    void dataTooShort() {
        int type = 10;
        byte[] data = new byte[2];

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerAttribute(DefaultDictionary.INSTANCE, -1, type, data));

        assertTrue(exception.getMessage().toLowerCase().contains("integer attribute value should be 4 octets"));
    }

    @Test
    void dataTooLong() {
        int type = 10;
        byte[] data = new byte[5];

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerAttribute(DefaultDictionary.INSTANCE, -1, type, data));

        assertTrue(exception.getMessage().toLowerCase().contains("integer attribute value should be 4 octets"));
    }

    @Test
    void dataStringTooLong() {
        int type = 27;
        final long bigValue = 0xffffffffffL;
        final String bigValueSt = Long.toString(bigValue);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerAttribute(DefaultDictionary.INSTANCE, -1, type, bigValueSt));

        assertTrue(exception.getMessage().toLowerCase().contains("integer attribute value should be 4 octets"));
    }

}