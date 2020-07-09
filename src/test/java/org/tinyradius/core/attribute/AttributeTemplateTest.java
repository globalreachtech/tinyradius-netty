package org.tinyradius.core.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.IntegerAttribute;
import org.tinyradius.core.attribute.type.OctetsAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.decorator.EncodedAttribute;
import org.tinyradius.core.attribute.type.decorator.TaggedAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.security.SecureRandom;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.*;

class AttributeTemplateTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private static final int TUNNEL_PASSWORD = 69;
    private static final int USER_PASSWORD = 2;

    @Test
    void testFlagDetection() {
        final AttributeTemplate template = new AttributeTemplate(-1, 1, "TestAttr", "integer", (byte) 0, false);
        assertEquals(NO_ENCRYPT, template.getCodecType());
        assertEquals(IntegerAttribute.class, template.create(dictionary, (byte) 1, new byte[4]).getClass());
        assertEquals(IntegerAttribute.class, template.createEncoded(dictionary, (byte) 1, new byte[4]).getClass());

        final AttributeTemplate userPassword = new AttributeTemplate(-1, 2, "Test-User-Password", "integer", (byte) 0, false);
        assertEquals(RFC2865_USER_PASSWORD, userPassword.getCodecType());
        assertEquals(IntegerAttribute.class, userPassword.create(dictionary, (byte) 1, new byte[4]).getClass());
        assertEquals(EncodedAttribute.class, userPassword.createEncoded(dictionary, (byte) 1, new byte[4]).getClass());

        final AttributeTemplate tunnelPassword = new AttributeTemplate(-1, 69, "Test-Tunnel-Password", "integer", (byte) 0, false);
        assertEquals(RFC2868_TUNNEL_PASSWORD, tunnelPassword.getCodecType());
        assertEquals(TaggedAttribute.class, tunnelPassword.create(dictionary, (byte) 1, new byte[4]).getClass());
        assertEquals(EncodedAttribute.class, tunnelPassword.createEncoded(dictionary, (byte) 1, new byte[4]).getClass());

        final AttributeTemplate ascendSend = new AttributeTemplate(529, 214, "Test-Ascend-Send-Secret", "integer", (byte) 0, false);
        assertEquals(ASCENT_SEND_SECRET, ascendSend.getCodecType());
        assertEquals(IntegerAttribute.class, ascendSend.create(dictionary, (byte) 1, new byte[4]).getClass());
        assertEquals(EncodedAttribute.class, ascendSend.createEncoded(dictionary, (byte) 1, new byte[4]).getClass());

        final AttributeTemplate custom = new AttributeTemplate(123, (byte) 123, "Test-Custom", "integer", (byte) 1, true);
        assertEquals(RFC2865_USER_PASSWORD, custom.getCodecType());
        assertEquals(TaggedAttribute.class, custom.create(dictionary, (byte) 1, new byte[4]).getClass());
        assertEquals(EncodedAttribute.class, custom.createEncoded(dictionary, (byte) 1, new byte[4]).getClass());
    }

    @Test
    void encodeNonEncryptAttribute() throws RadiusPacketException {
        final String username = "myUsername";
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusAttribute attribute = dictionary.createAttribute("User-Name", username);
        final AttributeTemplate template = dictionary.getAttributeTemplate("User-Name").get();

        final RadiusAttribute encode = template.encode(attribute, requestAuth, "secret");
        assertEquals(attribute, encode);
        assertTrue(encode instanceof OctetsAttribute);
        assertArrayEquals(username.getBytes(UTF_8), encode.getValue());
    }

    @Test
    void encodeDecodeNoTag() throws RadiusPacketException {
        final String pw = "myPw";
        final String secret = "secret";
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusAttribute attribute = dictionary.createAttribute(-1, USER_PASSWORD, pw.getBytes(UTF_8));
        final AttributeTemplate template = dictionary.getAttributeTemplate(USER_PASSWORD).get();

        assertArrayEquals(pw.getBytes(UTF_8), attribute.getValue());
        assertFalse(attribute.isEncoded());

        final RadiusAttribute encode1 = template.encode(attribute, requestAuth, secret);
        assertNotEquals(attribute, encode1);
        assertTrue(encode1.isEncoded());
        assertFalse(Arrays.equals(pw.getBytes(UTF_8), encode1.getValue()));

        // idempotence check
        final RadiusAttribute encode2 = template.encode(encode1, requestAuth, secret);
        assertEquals(encode1, encode2);
        assertTrue(encode2.isEncoded());
        assertArrayEquals(encode1.getValue(), encode2.getValue());

        final RadiusAttribute decode1 = template.decode(encode2, requestAuth, secret);
        assertEquals(attribute, decode1);
        assertFalse(decode1.isEncoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode1.getValue());

        // idempotence check
        final RadiusAttribute decode2 = template.decode(decode1, requestAuth, secret);
        assertEquals(decode1, decode2);
        assertFalse(decode2.isEncoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode2.getValue());
    }

    @Test
    void encodeDecodeWithTag() throws RadiusPacketException {
        final String pw = "myPw";
        final byte tag = random.generateSeed(1)[0];
        final String secret = "secret";
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusAttribute attribute = dictionary.createAttribute(-1, TUNNEL_PASSWORD, tag, pw.getBytes(UTF_8));
        final AttributeTemplate template = dictionary.getAttributeTemplate(TUNNEL_PASSWORD).get();

        assertEquals(tag, attribute.getTag());
        assertArrayEquals(pw.getBytes(UTF_8), attribute.getValue());
        assertFalse(attribute.isEncoded());

        final RadiusAttribute encode1 = template.encode(attribute, requestAuth, secret);
        assertNotEquals(attribute, encode1);
        assertTrue(encode1.isEncoded());
        assertFalse(Arrays.equals(pw.getBytes(UTF_8), encode1.getValue()));
        assertEquals(tag, attribute.getTag());
        assertEquals(tag, attribute.toByteArray()[2]);

        // idempotence check
        final RadiusAttribute encode2 = template.encode(encode1, requestAuth, secret);
        assertEquals(encode1, encode2);
        assertTrue(encode2.isEncoded());
        assertArrayEquals(encode1.getValue(), encode2.getValue());
        assertEquals(tag, attribute.getTag());
        assertEquals(tag, attribute.toByteArray()[2]);

        final RadiusAttribute decode1 = template.decode(encode2, requestAuth, secret);
        assertEquals(attribute, decode1);
        assertFalse(decode1.isEncoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode1.getValue());
        assertEquals(tag, attribute.getTag());
        assertEquals(tag, attribute.toByteArray()[2]);

        // idempotence check
        final RadiusAttribute decode2 = template.decode(decode1, requestAuth, secret);
        assertEquals(decode1, decode2);
        assertFalse(decode2.isEncoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode2.getValue());
        assertEquals(tag, attribute.getTag());
        assertEquals(tag, attribute.toByteArray()[2]);
    }
}