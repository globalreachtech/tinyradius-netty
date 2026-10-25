package org.tinyradius.core.attribute.codec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.tinyradius.core.RadiusPacketException;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.codec.BaseCodec.pad16x;

class BaseCodecTest {

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
        assertEquals(after, pad16x(new byte[before]).length);
    }

    @Test
    void testNoOpCodecAndBaseCodecValidations() throws RadiusPacketException {
        var noOp = new NoOpCodec();
        byte[] data = new byte[]{1, 2, 3};
        byte[] validAuth = new byte[16];
        assertSame(data, noOp.encode(data, validAuth, "secret"));
        assertSame(data, noOp.decode(data, validAuth, "secret"));

        byte[] shortAuth = new byte[15];
        assertThrows(RadiusPacketException.class, () -> noOp.encode(data, shortAuth, "secret"));
        assertThrows(RadiusPacketException.class, () -> noOp.decode(data, shortAuth, "secret"));

        assertThrows(RadiusPacketException.class, () -> noOp.cbcMd5Decode(new byte[8], validAuth, new byte[]{1}, true));
        assertThrows(RadiusPacketException.class, () -> noOp.cbcMd5Decode(new byte[18], validAuth, new byte[]{1}, true));

        assertThrows(IndexOutOfBoundsException.class, () -> BaseCodec.xor16(new byte[16], -1, new byte[16]));
        assertThrows(IndexOutOfBoundsException.class, () -> BaseCodec.xor16(new byte[16], 1, new byte[16]));
        assertThrows(IndexOutOfBoundsException.class, () -> BaseCodec.xor16(new byte[16], 0, new byte[15]));
    }

    @Test
    void testAttributeCodecType() {
        assertEquals(AttributeCodecType.NO_ENCRYPT, AttributeCodecType.fromName(""));
        assertEquals(AttributeCodecType.RFC2865_USER_PASSWORD, AttributeCodecType.fromName("User-Password"));
        assertEquals(AttributeCodecType.RFC2868_TUNNEL_PASSWORD, AttributeCodecType.fromName("Tunnel-Password"));
        assertEquals(AttributeCodecType.ASCEND_SECRET, AttributeCodecType.fromName("Ascend-Secret"));
        assertEquals(AttributeCodecType.NO_ENCRYPT, AttributeCodecType.fromName("unknown"));

        assertEquals(AttributeCodecType.NO_ENCRYPT, AttributeCodecType.fromId((byte) 99));
        assertEquals("User-Password", AttributeCodecType.RFC2865_USER_PASSWORD.getCodecName());
        assertEquals((byte) 1, AttributeCodecType.RFC2865_USER_PASSWORD.getId());
    }
}