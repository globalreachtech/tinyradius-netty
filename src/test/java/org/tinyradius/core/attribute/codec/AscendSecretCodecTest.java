package org.tinyradius.core.attribute.codec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.security.SecureRandom;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class AscendSecretCodecTest {

    private final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final AscendSecretCodec codec = new AscendSecretCodec();

    @ValueSource(strings = {"secret", "mySecret123", "a", "shortPw", "hexadecimal12345", "aVeryLongSecretThatExceeds16Bytes"})
    @ParameterizedTest
    void encodeDecode(String secret) throws RadiusPacketException {
        byte[] requestAuth = random.generateSeed(16);
        String sharedSecret = "sharedSecret1";

        RadiusAttribute attribute = dictionary.createAttribute(1, 1, secret.getBytes(UTF_8));

        assertEquals(secret, new String(attribute.getValue(), UTF_8));

        byte[] encoded = codec.encode(attribute.getValue(), requestAuth, sharedSecret);
        assertTrue(encoded.length >= 16, "Ascend-Secret always produces at least 16 octets");
        assertEquals(0, encoded.length % 16, "Ascend-Secret output must be multiple of 16");
        assertNotEquals(secret, new String(encoded, UTF_8));

        byte[] decoded = codec.decode(encoded, requestAuth, sharedSecret);
        assertEquals(secret, new String(decoded, UTF_8));
    }

    @Test
    void encodeDecodeBinaryData() throws RadiusPacketException {
        byte[] data = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                                  0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
                                  0x10, 0x11};
        byte[] requestAuth = random.generateSeed(16);
        String sharedSecret = "testsharedsecret";

        byte[] encoded = codec.encode(data, requestAuth, sharedSecret);
        assertEquals(32, encoded.length);

        byte[] decoded = codec.decode(encoded, requestAuth, sharedSecret);
        assertEquals(18, decoded.length);
        assertArrayEquals(data, decoded);
    }

    @Test
    void outputMultipleOf16Bytes() throws RadiusPacketException {
        byte[] auth = new byte[16];
        String sharedSecret = "secret";

        // Empty input
        assertEquals(16, codec.encode(new byte[0], auth, sharedSecret).length);

        // 1-byte input
        assertEquals(16, codec.encode(new byte[]{0x42}, auth, sharedSecret).length);

        // 15-byte input
        assertEquals(16, codec.encode(new byte[15], auth, sharedSecret).length);

        // 16-byte input
        assertEquals(16, codec.encode(new byte[16], auth, sharedSecret).length);

        // 17-byte input — should be 32
        assertEquals(32, codec.encode(new byte[17], auth, sharedSecret).length);
    }

    @Test
    void roundTripViaDecode() throws RadiusPacketException {
        byte[] auth = random.generateSeed(16);
        String sharedSecret = "mySharedSecret";
        byte[] data = "test-secret".getBytes(UTF_8); // 11 bytes, fits in 16-octet digest

        byte[] encoded = codec.encode(data, auth, sharedSecret);
        byte[] decoded = codec.decode(encoded, auth, sharedSecret);

        assertArrayEquals(data, decoded);
    }
}
