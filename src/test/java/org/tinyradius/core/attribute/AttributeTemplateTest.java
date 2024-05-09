package org.tinyradius.core.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.EncodedAttribute;
import org.tinyradius.core.attribute.type.IntegerAttribute;
import org.tinyradius.core.attribute.type.OctetsAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.MemoryDictionary;
import org.tinyradius.core.dictionary.WritableDictionary;

import java.security.SecureRandom;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.*;
import static org.tinyradius.core.attribute.rfc.Rfc2865.USER_PASSWORD;
import static org.tinyradius.core.attribute.rfc.Rfc2868.TUNNEL_PASSWORD;

class AttributeTemplateTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    /**
     * User-Password / Tunnel-Password / Ascent-Send-Secret should ignore encrypt/tag flags
     */
    @Test
    void testFlagDetection() {
        final AttributeTemplate template = new AttributeTemplate(
                -1, 1, "TestAttr", "integer", IntegerAttribute.FACTORY, (byte) 0, false);
        final AttributeTemplate userPassword = new AttributeTemplate(
                -1, 2, "Test-User-Password", "integer", IntegerAttribute.FACTORY, (byte) 0, false);
        final AttributeTemplate tunnelPassword = new AttributeTemplate(
                -1, 69, "Test-Tunnel-Password", "integer", IntegerAttribute.FACTORY, (byte) 0, false);
        final AttributeTemplate ascendSend = new AttributeTemplate(
                529, 214, "Test-Ascend-Send-Secret", "integer", IntegerAttribute.FACTORY, (byte) 0, false);
        final AttributeTemplate custom = new AttributeTemplate(
                123, (byte) 123, "Test-Custom", "integer", IntegerAttribute.FACTORY, (byte) 1, true);

        final WritableDictionary customDict = new MemoryDictionary()
                .addAttributeTemplate(template)
                .addAttributeTemplate(userPassword)
                .addAttributeTemplate(tunnelPassword)
                .addAttributeTemplate(ascendSend)
                .addAttributeTemplate(custom);

        assertEquals(NO_ENCRYPT, template.getCodecType());
        final IntegerAttribute templateDecoded = (IntegerAttribute) template.create(customDict, (byte) 1, new byte[4]);
        assertFalse(templateDecoded.getTag().isPresent());
        final IntegerAttribute templateEncoded = (IntegerAttribute) template.createEncoded(customDict, (byte) 1, new byte[4]);
        assertFalse(templateEncoded.getTag().isPresent());

        assertEquals(RFC2865_USER_PASSWORD, userPassword.getCodecType());
        final IntegerAttribute userPasswordDecoded = (IntegerAttribute) userPassword.create(customDict, (byte) 1, new byte[4]);
        assertFalse(userPasswordDecoded.getTag().isPresent());
        final EncodedAttribute userPasswordEncoded = (EncodedAttribute) userPassword.createEncoded(customDict, (byte) 1, new byte[4]);
        assertFalse(userPasswordEncoded.getTag().isPresent());

        assertEquals(RFC2868_TUNNEL_PASSWORD, tunnelPassword.getCodecType());
        final IntegerAttribute tunnelPasswordDecoded = (IntegerAttribute) tunnelPassword.create(customDict, (byte) 1, new byte[3]);  // int length 3 if has_tag
        assertTrue(tunnelPasswordDecoded.getTag().isPresent());
        final EncodedAttribute tunnelPasswordEncoded = (EncodedAttribute) tunnelPassword.createEncoded(customDict, (byte) 1, new byte[4]);
        assertTrue(tunnelPasswordEncoded.getTag().isPresent());

        assertEquals(ASCEND_SEND_SECRET, ascendSend.getCodecType());
        final IntegerAttribute ascendSendDecoded = (IntegerAttribute) ascendSend.create(customDict, (byte) 1, new byte[4]);
        assertFalse(ascendSendDecoded.getTag().isPresent());
        final EncodedAttribute ascendSendEncoded = (EncodedAttribute) ascendSend.createEncoded(customDict, (byte) 1, new byte[4]);
        assertFalse(ascendSendEncoded.getTag().isPresent());

        assertEquals(RFC2865_USER_PASSWORD, custom.getCodecType());
        final IntegerAttribute customDecoded = (IntegerAttribute) custom.create(customDict, (byte) 1, new byte[3]); // int length 3 if has_tag
        assertTrue(customDecoded.getTag().isPresent());
        final EncodedAttribute customEncoded = (EncodedAttribute) custom.createEncoded(customDict, (byte) 1, new byte[4]);
        assertTrue(customEncoded.getTag().isPresent());
    }

    @Test
    void encodeNonEncryptAttribute() throws RadiusPacketException {
        final String username = "myUsername";
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusAttribute attribute = dictionary.createAttribute("User-Name", username);
        final AttributeTemplate template = dictionary.getAttributeTemplate("User-Name").get();

        final OctetsAttribute encode = (OctetsAttribute) template.encode(attribute, requestAuth, "secret");
        assertEquals(attribute, encode);
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

        assertEquals(tag, attribute.getTag().get());
        assertArrayEquals(pw.getBytes(UTF_8), attribute.getValue());
        assertFalse(attribute.isEncoded());

        final RadiusAttribute encode1 = template.encode(attribute, requestAuth, secret);
        assertNotEquals(attribute, encode1);
        assertTrue(encode1.isEncoded());
        assertFalse(Arrays.equals(pw.getBytes(UTF_8), encode1.getValue()));
        assertEquals(tag, attribute.getTag().get());
        assertEquals(tag, attribute.toByteArray()[2]);

        // idempotence check
        final RadiusAttribute encode2 = template.encode(encode1, requestAuth, secret);
        assertEquals(encode1, encode2);
        assertTrue(encode2.isEncoded());
        assertArrayEquals(encode1.getValue(), encode2.getValue());
        assertEquals(tag, attribute.getTag().get());
        assertEquals(tag, attribute.toByteArray()[2]);

        final RadiusAttribute decode1 = template.decode(encode2, requestAuth, secret);
        assertEquals(attribute, decode1);
        assertFalse(decode1.isEncoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode1.getValue());
        assertEquals(tag, attribute.getTag().get());
        assertEquals(tag, attribute.toByteArray()[2]);

        // idempotence check
        final RadiusAttribute decode2 = template.decode(decode1, requestAuth, secret);
        assertEquals(decode1, decode2);
        assertFalse(decode2.isEncoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode2.getValue());
        assertEquals(tag, attribute.getTag().get());
        assertEquals(tag, attribute.toByteArray()[2]);
    }
}