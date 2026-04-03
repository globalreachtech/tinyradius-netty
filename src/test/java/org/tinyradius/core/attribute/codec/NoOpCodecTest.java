package org.tinyradius.core.attribute.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class NoOpCodecTest {

    @Test
    void testNoOpCodec() {
        NoOpCodec codec = new NoOpCodec();
        byte[] data = new byte[]{1, 2, 3};
        assertArrayEquals(data, codec.encodeData(data, null, null));
        assertArrayEquals(data, codec.decodeData(data, null, null));
    }
}
