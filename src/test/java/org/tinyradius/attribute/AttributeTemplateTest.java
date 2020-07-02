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

    private static final int TUNNEL_PASSWORD = 69;

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

        final RadiusAttribute attribute = dictionary.createAttribute("User-Password", pw);
        final AttributeTemplate template = dictionary.getAttributeTemplate("User-Password").get();

        assertArrayEquals(pw.getBytes(UTF_8), attribute.getValue());
        assertFalse(attribute.isEncoded());

        final RadiusAttribute encode = template.encode(attribute, requestAuth, secret);
        assertNotEquals(attribute, encode);
        assertTrue(encode.isEncoded());
        assertFalse(Arrays.equals(pw.getBytes(UTF_8), encode.getValue()));

        // idempotence check
        final RadiusAttribute encode2 = encode.encode(requestAuth, secret);
        assertEquals(encode, encode2);
        assertTrue(encode2.isEncoded());
        assertArrayEquals(encode.getValue(), encode2.getValue());

        final RadiusAttribute decode = encode2.decode(requestAuth, secret);
        assertFalse(decode.isEncoded());
        assertFalse(Arrays.equals(pw.getBytes(UTF_8), decode.getValue()));

        // idempotence check
        final RadiusAttribute decode2 = decode.decode(requestAuth, secret);
        assertFalse(decode2.isEncoded());
        assertFalse(Arrays.equals(pw.getBytes(UTF_8), decode2.getValue()));
        // todo
    }


    @Test
    void encodeDecodeWithTag() {
        final String pw = "myPw";
        final String secret = "secret";
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusAttribute attribute = dictionary.createAttribute("Tunnel-Password", pw);
        final AttributeTemplate template = dictionary.getAttributeTemplate("User-Password").get();
        // todo
    }
}