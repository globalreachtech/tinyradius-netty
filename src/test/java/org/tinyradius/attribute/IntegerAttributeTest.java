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
}