package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.parser.DictionaryParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import static java.lang.Byte.toUnsignedInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class OctetsAttributeTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createMaxSizeAttribute() {
        // 255 octets ok
        final OctetsAttribute attribute = (OctetsAttribute) dictionary.createAttribute(-1, 2, random.generateSeed(253));
        final byte[] bytes = attribute.toByteArray();

        assertEquals(0xFF, toUnsignedInt(bytes[1]));
        assertEquals(255, toUnsignedInt(bytes[1]));
        assertEquals(255, bytes.length);

        // 256 octets not ok
        final byte[] oversizedArray = random.generateSeed(254);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                dictionary.createAttribute(-1, 2, oversizedArray));

        assertTrue(exception.getMessage().contains("too long"));
    }

    @Test
    void testFlatten() {
        final OctetsAttribute attribute = (OctetsAttribute) dictionary.createAttribute(-1, 2, "123456");
        assertEquals("[User-Password: 123456]", attribute.flatten().toString());
    }

    @Test
    void testToString() {
        final OctetsAttribute attribute = (OctetsAttribute) dictionary.createAttribute(-1, 2, "123456");
        assertEquals("User-Password: 123456", attribute.toString());
    }

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String secret = "mySecret";
        final String pw = "myPw";

        final byte[] requestAuth = random.generateSeed(16);

        final OctetsAttribute attribute = (OctetsAttribute) dictionary.createAttribute(-1, 2, (byte) 0, pw);
        assertFalse(attribute.isEncoded());
        assertEquals(pw, new String(attribute.getValue(), UTF_8));

        // encode
        final EncodedAttribute encode = (EncodedAttribute) attribute.encode(requestAuth, secret);
        assertTrue(encode.isEncoded());
        assertNotEquals(pw, new String(encode.getValue(), UTF_8));

        // encode again
        final RadiusAttribute encode1 = encode.encode(requestAuth, secret);
        assertEquals(encode1, encode);

        // decode
        final OctetsAttribute decode = (OctetsAttribute) encode.decode(requestAuth, secret);
        assertFalse(decode.isEncoded());
        assertEquals(pw, new String(decode.getValue(), UTF_8));

        // decode again
        final RadiusAttribute decode1 = decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }

    @Test
    void encodeDecodeWithTag() throws RadiusPacketException, IOException {
        final Dictionary testDictionary = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/test_dictionary");

        final String secret = "mySecret";
        final byte[] value = ByteBuffer.allocate(4).putInt(10000).array();
        final byte tag = 123;
        final byte[] requestAuth = random.generateSeed(16);

        final OctetsAttribute attribute = (OctetsAttribute) testDictionary.createAttribute(-1, 69, tag, value);
        assertFalse(attribute.isEncoded());
        assertEquals(tag, attribute.getTag().get());
        assertArrayEquals(value, attribute.getValue());

        // encode
        final EncodedAttribute encode = (EncodedAttribute) attribute.encode(requestAuth, secret);
        assertTrue(encode.isEncoded());
        assertEquals(tag, encode.getTag().get());
        assertFalse(Arrays.equals(value, encode.getValue()));

        // encode again
        final RadiusAttribute encode1 = encode.encode(requestAuth, secret);
        assertEquals(encode1, encode);

        // decode
        final OctetsAttribute decode = (OctetsAttribute) encode.decode(requestAuth, secret);
        assertFalse(decode.isEncoded());
        assertEquals(tag, decode.getTag().get());
        assertArrayEquals(value, decode.getValue());

        // decode again
        final RadiusAttribute decode1 = decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }
}