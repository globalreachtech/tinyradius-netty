package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.decorator.EncodedAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.security.SecureRandom;

import static java.lang.Byte.toUnsignedInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class OctetsAttributeTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createMaxSizeAttribute() {
        // 253 octets ok
        final RadiusAttribute attribute = dictionary.createAttribute(-1, 2, random.generateSeed(253));
        assertTrue(attribute instanceof OctetsAttribute);
        final byte[] bytes = attribute.toByteArray();

        assertEquals(0xFF, toUnsignedInt(bytes[1]));
        assertEquals(255, toUnsignedInt(bytes[1]));
        assertEquals(255, bytes.length);

        // 254 octets not ok
        final byte[] oversizedArray = random.generateSeed(254);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                dictionary.createAttribute(-1, 2, oversizedArray));

        assertTrue(exception.getMessage().contains("too long"));
    }

    @Test
    void testFlatten() {
        final RadiusAttribute attribute = dictionary.createAttribute(-1, 2, "123456");
        assertTrue(attribute instanceof OctetsAttribute);
        assertEquals("[User-Password: 123456]", attribute.flatten().toString());
    }

    @Test
    void testToString() {
        final RadiusAttribute attribute = dictionary.createAttribute(-1, 2, "123456");
        assertTrue(attribute instanceof OctetsAttribute);
        assertEquals("User-Password: 123456", attribute.toString());
    }

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String secret = "mySecret";
        final String pw = "myPw";

        final byte[] requestAuth = random.generateSeed(16);

        final RadiusAttribute attribute = dictionary.createAttribute(-1, 2, (byte) 0, pw);
        assertTrue(attribute instanceof OctetsAttribute);
        assertFalse(attribute.isEncoded());
        assertEquals(pw, new String(attribute.getValue(), UTF_8));

        // encode
        final RadiusAttribute encode = attribute.encode(requestAuth, secret);
        assertTrue(encode instanceof EncodedAttribute);
        assertTrue(encode.isEncoded());
        assertNotEquals(pw, new String(encode.getValue(), UTF_8));

        // encode again
        final RadiusAttribute encode1 = encode.encode(requestAuth, secret);
        assertEquals(encode1, encode);

        // decode
        final RadiusAttribute decode = encode.decode(requestAuth, secret);
        assertTrue(decode instanceof OctetsAttribute);
        assertFalse(decode.isEncoded());
        assertEquals(pw, new String(decode.getValue(), UTF_8));

        // decode again
        final RadiusAttribute decode1 = decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }


    @Test
    void transformationsTagPersists() {
        final RadiusAttribute attribute = new EncodedAttribute(dictionary.createAttribute(-1, 140, (byte) 0xFF, "123"));
        final RadiusAttribute transformed = attribute.flatten().get(0);
        assertEquals((byte) 0xFF, attribute.getTag().get());
        assertEquals((byte) 0xFF, transformed.getTag().get());
        assertEquals(attribute, transformed);
    }

    @Test
    void encodeDecodeWithTag() throws RadiusPacketException {
        final String secret = "mySecret";
        final String pw = "myPw";
        final byte tag = 123;
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusAttribute attribute = dictionary.createAttribute(-1, 69, tag, pw);
        assertTrue(attribute instanceof OctetsAttribute);
        assertFalse(attribute.isEncoded());
        assertEquals(pw, new String(attribute.getValue(), UTF_8));

        // encode
        final RadiusAttribute encode = attribute.encode(requestAuth, secret);
        assertTrue(encode instanceof EncodedAttribute);
        assertTrue(encode.isEncoded());
        assertEquals(tag, encode.getTag().get());
        assertNotEquals(pw, new String(encode.getValue(), UTF_8));

        // encode again
        final RadiusAttribute encode1 = encode.encode(requestAuth, secret);
        assertEquals(encode1, encode);

        // decode
        final RadiusAttribute decode = encode.decode(requestAuth, secret);
        assertTrue(decode instanceof OctetsAttribute);
        assertFalse(decode.isEncoded());
        assertEquals(tag, decode.getTag().get());
        assertEquals(pw, new String(decode.getValue(), UTF_8));

        // decode again
        final RadiusAttribute decode1 = decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }
}