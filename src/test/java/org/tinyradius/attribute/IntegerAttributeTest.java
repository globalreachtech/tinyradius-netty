package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.*;

class IntegerAttributeTest {

    private Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void maxUnsignedInt() {
        final long maxValue = 0xffffffffL;
        final String maxValueStr = Long.toString(maxValue); // 2^32 - 1 = 4294967295
        final IntegerAttribute attribute = new IntegerAttribute(dictionary, -1, 27, maxValueStr);

        assertEquals(-1, attribute.getValueInt());
        assertEquals(maxValueStr, attribute.getValueString());
        assertEquals(maxValue, attribute.getValueLong());
    }

    @Test
    void maxSignedInt() {
        final int value = Integer.MAX_VALUE;
        final IntegerAttribute attribute = new IntegerAttribute(dictionary, -1, 27, value);
        assertEquals(value, attribute.getValueLong());
        assertEquals(String.valueOf(value), attribute.getValueString());
    }

    @Test
    void dataTooShort() {
        int type = 10;
        byte[] data = new byte[2];
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerAttribute(dictionary, -1, type, data));
        assertTrue(exception.getMessage().toLowerCase().contains("integer attribute value should be 4 octets"));
    }

    @Test
    void dataTooLong() {
        int type = 10;
        byte[] data = new byte[5];
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerAttribute(dictionary, -1, type, data));
        assertTrue(exception.getMessage().toLowerCase().contains("integer attribute value should be 4 octets"));
    }

    @Test
    void dataStringTooLong() {
        int type = 27;
        final long bigValue = 0xffffffffffL;
        final String bigValueSt = Long.toString(bigValue);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new IntegerAttribute(dictionary, -1, type, bigValueSt));
        assertTrue(exception.getMessage().toLowerCase().contains("integer attribute value should be 4 octets"));
    }

    @Test
    void convertFromByteToInt() {
        int type = 27;
        final long bigValue = Integer.MAX_VALUE + 1L;
        final String bigValueSt = Long.toString(bigValue);
        IntegerAttribute integerAttribute = new IntegerAttribute(dictionary, -1, type, bigValueSt);
        assertEquals(bigValue, integerAttribute.getValueLong());
        // not sure what this is testing - similar conditions as first test?
    }

}