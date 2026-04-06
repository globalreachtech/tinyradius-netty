package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.*;

class OctetsAttributeTest {

    private static final RadiusAttributeFactory<OctetsAttribute> FACTORY = OctetsAttribute.FACTORY;

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createMaxSizeAttribute() {
        // 255 octets ok
        OctetsAttribute attribute = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, random.generateSeed(253));
        byte[] bytes = attribute.toByteArray();

        assertEquals(0xFF, toUnsignedInt(bytes[1]));
        assertEquals(255, toUnsignedInt(bytes[1]));
        assertEquals(255, bytes.length);

        // 256 octets not ok
        byte[] oversizedArray = random.generateSeed(254);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                FACTORY.create(dictionary, -1, 3, (byte) 0, oversizedArray)); // CHAP-Password

        assertThat(exception).hasMessageContaining("too long");
    }

    @Test
    void headerBadDeclaredSize() {
        ByteBuf byteBuf = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, random.generateSeed(253))
                .toByteBuf()
                .setByte(1, 123);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                FACTORY.create(dictionary, -1, byteBuf)); // CHAP-Password

        assertEquals("Attribute declared length is 123, actual length: 255", exception.getCause().getMessage());
    }

    @Test
    void testFlatten() {
        OctetsAttribute attribute = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, "FFFF0000");
        assertEquals("[CHAP-Password=0xFFFF0000]", attribute.flatten().toString());
    }

    @Test
    void testToString() {
        OctetsAttribute attribute = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, "FFFF0000");
        assertEquals("CHAP-Password=0xFFFF0000", attribute.toString());
    }

    @Test
    void encodeDecode() throws RadiusPacketException {
        String secret = "mySecret";
        byte[] value = ByteBuffer.allocate(4).putInt(10000).array();

        byte[] requestAuth = random.generateSeed(16);

        // User-Password (we need something with encrypt)
        OctetsAttribute attribute = FACTORY.create(dictionary, -1, USER_PASSWORD, (byte) 0, value);
        assertTrue(attribute.isDecoded());
        assertArrayEquals(value, attribute.getValue());

        // encode
        EncodedAttribute encode = (EncodedAttribute) attribute.encode(requestAuth, secret);
        assertTrue(encode.isEncoded());
        assertFalse(Arrays.equals(value, encode.getValue()));

        // encode again
        RadiusAttribute encode1 = encode.encode(requestAuth, secret);
        assertEquals(encode1, encode);

        // decode
        OctetsAttribute decode = (OctetsAttribute) encode.decode(requestAuth, secret);
        assertTrue(decode.isDecoded());
        assertArrayEquals(value, decode.getValue());

        // decode again
        RadiusAttribute decode1 = decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }

    @Test
    void encodeDecodeWithTag() throws RadiusPacketException, IOException {
        Dictionary testDictionary = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/test_dictionary");

        String secret = "mySecret";
        byte[] value = ByteBuffer.allocate(4).putInt(10000).array();
        byte tag = 123;
        byte[] requestAuth = random.generateSeed(16);

        // Tunnel-Password (actually a StringAttribute, we need something with encrypt and tag )
        OctetsAttribute attribute = FACTORY.create(testDictionary, -1, TUNNEL_PASSWORD, tag, value);
        assertTrue(attribute.isDecoded());
        assertEquals(tag, attribute.getTag().get());
        assertArrayEquals(value, attribute.getValue());
        assertEquals("Tunnel-Password:123=0x00002710", attribute.toString());

        // encode
        EncodedAttribute encode = (EncodedAttribute) attribute.encode(requestAuth, secret);
        assertTrue(encode.isEncoded());
        assertEquals(tag, encode.getTag().get());
        assertFalse(Arrays.equals(value, encode.getValue()));

        // encode again
        RadiusAttribute encode1 = encode.encode(requestAuth, secret);
        assertEquals(encode1, encode);

        // decode
        OctetsAttribute decode = (OctetsAttribute) encode.decode(requestAuth, secret);
        assertTrue(decode.isDecoded());
        assertEquals(tag, decode.getTag().get());
        assertArrayEquals(value, decode.getValue());

        // decode again
        RadiusAttribute decode1 = decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }
}