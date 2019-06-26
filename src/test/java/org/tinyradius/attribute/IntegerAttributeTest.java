package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegerAttributeTest {

    @Test
    void test() {
        final IntegerAttribute intAttr = new IntegerAttribute(27, 0);
        final long bigValue = 0xffffffffL; // big value with highest bit set
        System.err.println((int)bigValue);
        System.err.println(bigValue);
        final String bigValueSt = Long.toString(bigValue);
        intAttr.setAttributeValue(bigValueSt);
        assertEquals(bigValueSt, intAttr.getAttributeValue());
    }
}