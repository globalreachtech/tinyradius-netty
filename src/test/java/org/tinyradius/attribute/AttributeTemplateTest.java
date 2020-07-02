package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.type.OctetsAttribute;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class AttributeTemplateTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private static final byte TUNNEL_PASSWORD = 69;
    private static final byte USER_PASSWORD = 2;

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