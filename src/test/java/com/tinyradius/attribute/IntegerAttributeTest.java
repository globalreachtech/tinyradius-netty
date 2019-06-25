package com.tinyradius.attribute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntegerAttributeTest {

    @Test
    void test() {
        final IntegerAttribute intAttr = new IntegerAttribute(27, 0);
        final long bigValue = 0xffffffffL; // big value with highest bit set
        System.out.println((int) bigValue);
        System.out.println(bigValue);

        System.out.println("foooo");
        final String bigValueSt = Long.toString(bigValue);
        System.out.println(bigValueSt);
        intAttr.setAttributeValue(bigValueSt);
        assertEquals(bigValueSt, intAttr.getAttributeValue());
    }
}