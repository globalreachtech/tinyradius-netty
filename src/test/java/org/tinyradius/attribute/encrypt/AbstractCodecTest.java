package org.tinyradius.attribute.encrypt;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.attribute.encrypt.AbstractCodec.pad16;

class AbstractCodecTest {

    @CsvSource({
            "0,16",
            "1,16",
            "2,16",
            "15,16",
            "16,16",
            "17,32",
            "18,32",
            "31,32",
            "32,32",
            "33,48"
    })
    @ParameterizedTest
    void testPad(int before, int after) {
        assertEquals(after, pad16(new byte[before]).length);
    }
}