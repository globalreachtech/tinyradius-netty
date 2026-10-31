package org.tinyradius.core.attribute.type;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.MemoryDictionary;
import org.tinyradius.core.dictionary.Vendor;
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
        var attribute = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, random.generateSeed(253));
        byte[] bytes = attribute.toByteArray();

        assertEquals(0xFF, toUnsignedInt(bytes[1]));
        assertEquals(255, toUnsignedInt(bytes[1]));
        assertEquals(255, bytes.length);

        // 256 octets not ok
        byte[] oversizedArray = random.generateSeed(254);
        var exception = assertThrows(IllegalArgumentException.class, () ->
                FACTORY.create(dictionary, -1, 3, (byte) 0, oversizedArray)); // CHAP-Password

        assertThat(exception).hasMessageContaining("too long");
    }

    @Test
    void headerBadDeclaredSize() {
        var byteBuf = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, random.generateSeed(253))
                .toByteBuf()
                .setByte(1, 123);

        var exception = assertThrows(IllegalArgumentException.class, () ->
                FACTORY.create(dictionary, -1, byteBuf)); // CHAP-Password

        assertEquals("Attribute declared length is 123, actual length: 255", exception.getCause().getMessage());
    }

    @Test
    void testFlatten() {
        var attribute = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, "FFFF0000");
        assertEquals("[CHAP-Password=0xFFFF0000]", attribute.flatten().toString());
    }

    @Test
    void testToString() {
        var attribute = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, "FFFF0000");
        assertEquals("CHAP-Password=0xFFFF0000", attribute.toString());
    }

    @Test
    void encodeDecode() throws RadiusPacketException {
        var secret = "mySecret";
        byte[] value = ByteBuffer.allocate(4).putInt(10000).array();

        byte[] requestAuth = random.generateSeed(16);

        // User-Password (we need something with encrypt)
        var attribute = FACTORY.create(dictionary, -1, USER_PASSWORD, (byte) 0, value);
        assertTrue(attribute.isDecoded());
        assertArrayEquals(value, attribute.getValue());

        // encode
        var encode = (EncodedAttribute) attribute.encode(requestAuth, secret);
        assertTrue(encode.isEncoded());
        assertFalse(Arrays.equals(value, encode.getValue()));

        // encode again
        var encode1 = encode.encode(requestAuth, secret);
        assertEquals(encode1, encode);

        // decode
        var decode = (OctetsAttribute) encode.decode(requestAuth, secret);
        assertTrue(decode.isDecoded());
        assertArrayEquals(value, decode.getValue());

        // decode again
        var decode1 = decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }

    @Test
    void encodeDecodeWithTag() throws RadiusPacketException, IOException {
        var testDictionary = DictionaryParser.newClasspathParser()
                .parseDictionary("org/tinyradius/core/dictionary/test_dictionary");

        var secret = "mySecret";
        byte[] value = ByteBuffer.allocate(4).putInt(10000).array();
        byte tag = 123;
        byte[] requestAuth = random.generateSeed(16);

        // Tunnel-Password (actually a StringAttribute, we need something with encrypt and tag )
        var attribute = FACTORY.create(testDictionary, -1, TUNNEL_PASSWORD, tag, value);
        assertTrue(attribute.isDecoded());
        assertEquals(tag, attribute.getTag().get());
        assertArrayEquals(value, attribute.getValue());
        assertEquals("Tunnel-Password:123=0x00002710", attribute.toString());

        // encode
        var encode = (EncodedAttribute) attribute.encode(requestAuth, secret);
        assertTrue(encode.isEncoded());
        assertEquals(tag, encode.getTag().get());
        assertFalse(Arrays.equals(value, encode.getValue()));

        // encode again
        var encode1 = encode.encode(requestAuth, secret);
        assertEquals(encode1, encode);

        // decode
        var decode = (OctetsAttribute) encode.decode(requestAuth, secret);
        assertTrue(decode.isDecoded());
        assertEquals(tag, decode.getTag().get());
        assertArrayEquals(value, decode.getValue());

        // decode again
        var decode1 = decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }

    @Test
    void hashCodeLengthAndUntemplatedEncodeDecode() throws RadiusPacketException {
        var attr1 = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, "FFFF0000");
        var attr2 = FACTORY.create(dictionary, -1, CHAP_PASSWORD, (byte) 0, "FFFF0000");
        assertEquals(attr1, attr2);
        assertEquals(attr1.hashCode(), attr2.hashCode());
        assertEquals(6, attr1.getLength());

        var customDict = new MemoryDictionary();
        customDict.addVendor(new Vendor(100, "Len0Vendor", 1, 0));
        customDict.addVendor(new Vendor(200, "Len2Vendor", 1, 2));

        var len0Attr = FACTORY.create(customDict, 100, 1, (byte) 0, new byte[]{1, 2, 3});
        assertEquals(4, len0Attr.getLength());

        var len2Attr = FACTORY.create(customDict, 200, 1, (byte) 0, new byte[]{1, 2, 3});
        assertEquals(6, len2Attr.getLength());

        // untemplated attribute encode/decode returns self
        byte[] auth = new byte[16];
        assertSame(len0Attr, len0Attr.encode(auth, "secret"));
        assertSame(len0Attr, len0Attr.decode(auth, "secret"));
    }
}