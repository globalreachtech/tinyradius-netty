package org.tinyradius.core.attribute.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AscendSendSecretCodecTest {

    @Test
    void testAscendSendSecretCodec() {
        AscendSendSecretCodec codec = new AscendSendSecretCodec();
        byte[] data = new byte[]{1, 2, 3};
        assertArrayEquals(data, codec.encodeData(data, null, null));
        assertArrayEquals(data, codec.decodeData(data, null, null));
    }
}
