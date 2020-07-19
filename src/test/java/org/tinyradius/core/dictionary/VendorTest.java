package org.tinyradius.core.dictionary;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VendorTest {

    @ValueSource(ints = {1, 2, 4})
    @ParameterizedTest
    void typeBytesSizeMatch(int typeSize) {
        final Vendor vendor = new Vendor(1, "foo", typeSize, 1);
        assertEquals(vendor.getTypeSize(), vendor.toTypeBytes(1).length);
    }

    @ValueSource(ints = {0, 1, 2})
    @ParameterizedTest
    void lengthBytesSizeMatch(int lengthSize) {
        final Vendor vendor = new Vendor(1, "foo", 1, lengthSize);
        assertEquals(vendor.getLengthSize(), vendor.toLengthBytes(1).length);
    }

    @CsvSource({"1,0", "2,1", "4,2"})
    @ParameterizedTest
    void headerSize(int typeSize, int lengthSize) {
        final Vendor vendor = new Vendor(1, "foo", typeSize, lengthSize);
        assertEquals(typeSize + lengthSize, vendor.getHeaderSize());
    }

    @ValueSource(ints = {0, 3, 5, 6})
    @ParameterizedTest
    void badTypeSize(int typeSize) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Vendor(1, "foo", typeSize, 1));
        assertEquals("Vendor typeSize must be 1, 2, or 4", e.getMessage());
    }

    @ValueSource(ints = {3, 4})
    @ParameterizedTest
    void badLengthSize(int lengthSize) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Vendor(1, "foo", 1, lengthSize));
        assertEquals("Vendor lengthSize must be 0, 1, or 2", e.getMessage());
    }
}