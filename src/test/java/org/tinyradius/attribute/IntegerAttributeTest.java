package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;

import static org.junit.jupiter.api.Assertions.*;

class IntegerAttributeTest {

    @Test
    void testLargestUnsignedInt() {
        final long bigValue = 0xffffffffL; // big value with highest bit set
        System.err.println((int) bigValue);
        System.err.println(bigValue);
        final String bigValueSt = Long.toString(bigValue);
        final IntegerAttribute intAttr = new IntegerAttribute(DefaultDictionary.INSTANCE, 27, -1, bigValueSt);
        assertEquals(bigValueSt, intAttr.getAttributeValue());
    }
}