package org.tinyradius.attribute.encrypt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.attribute.encrypt.UserPasswordCodec.pad;

class UserPasswordCodecTest {

    @Test
    void testPad() {
        assertEquals(16, pad(new byte[0]).length);
        assertEquals(16, pad(new byte[1]).length);
        assertEquals(16, pad(new byte[2]).length);
        assertEquals(16, pad(new byte[15]).length);
        assertEquals(16, pad(new byte[16]).length);
        assertEquals(32, pad(new byte[17]).length);
        assertEquals(32, pad(new byte[18]).length);
        assertEquals(32, pad(new byte[32]).length);
        assertEquals(48, pad(new byte[33]).length);
    }
}