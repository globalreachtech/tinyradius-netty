package org.tinyradius.core.attribute;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.EncodedAttribute;
import org.tinyradius.core.attribute.type.IntegerAttribute;
import org.tinyradius.core.attribute.type.OctetsAttribute;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.VendorSpecificAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.MemoryDictionary;
import org.tinyradius.core.dictionary.WritableDictionary;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.TUNNEL_PASSWORD;
import static org.tinyradius.core.attribute.AttributeTypes.USER_PASSWORD;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.*;

class AttributeTemplateTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    /**
     * User-Password / Tunnel-Password / Ascent-Send-Secret should ignore encrypt/tag flags
     */
    @Test
    void testFlagDetection() {
        AttributeTemplate template = new AttributeTemplate(
                -1, 1, "TestAttr", "integer", IntegerAttribute.FACTORY, NO_ENCRYPT, false);
        AttributeTemplate userPassword = new AttributeTemplate(
                -1, 2, "Test-User-Password", "integer", IntegerAttribute.FACTORY, NO_ENCRYPT, false);
        AttributeTemplate tunnelPassword = new AttributeTemplate(
                -1, 69, "Test-Tunnel-Password", "integer", IntegerAttribute.FACTORY, NO_ENCRYPT, false);
        AttributeTemplate custom = new AttributeTemplate(
                123, (byte) 123, "Test-Custom", "integer", IntegerAttribute.FACTORY, RFC2865_USER_PASSWORD, true);

        WritableDictionary customDict = new MemoryDictionary()
                .addAttributeTemplate(template)
                .addAttributeTemplate(userPassword)
                .addAttributeTemplate(tunnelPassword)
                .addAttributeTemplate(custom);

        assertEquals(NO_ENCRYPT, template.codecType());
        IntegerAttribute templateDecoded = (IntegerAttribute) template.create(customDict, (byte) 1, new byte[4]);
        assertFalse(templateDecoded.getTag().isPresent());
        IntegerAttribute templateEncoded = (IntegerAttribute) template.createEncoded(customDict, (byte) 1, new byte[4]);
        assertFalse(templateEncoded.getTag().isPresent());

        assertEquals(RFC2865_USER_PASSWORD, userPassword.codecType());
        IntegerAttribute userPasswordDecoded = (IntegerAttribute) userPassword.create(customDict, (byte) 1, new byte[4]);
        assertFalse(userPasswordDecoded.getTag().isPresent());
        EncodedAttribute userPasswordEncoded = (EncodedAttribute) userPassword.createEncoded(customDict, (byte) 1, new byte[4]);
        assertFalse(userPasswordEncoded.getTag().isPresent());

        assertEquals(RFC2868_TUNNEL_PASSWORD, tunnelPassword.codecType());
        IntegerAttribute tunnelPasswordDecoded = (IntegerAttribute) tunnelPassword.create(customDict, (byte) 1, new byte[3]);  // int length 3 if has_tag
        assertTrue(tunnelPasswordDecoded.getTag().isPresent());
        EncodedAttribute tunnelPasswordEncoded = (EncodedAttribute) tunnelPassword.createEncoded(customDict, (byte) 1, new byte[4]);
        assertTrue(tunnelPasswordEncoded.getTag().isPresent());

        assertEquals(RFC2865_USER_PASSWORD, custom.codecType());
        IntegerAttribute customDecoded = (IntegerAttribute) custom.create(customDict, (byte) 1, new byte[3]); // int length 3 if has_tag
        assertTrue(customDecoded.getTag().isPresent());
        EncodedAttribute customEncoded = (EncodedAttribute) custom.createEncoded(customDict, (byte) 1, new byte[4]);
        assertTrue(customEncoded.getTag().isPresent());
    }

    @Test
    void encodeNonEncryptAttribute() throws RadiusPacketException {
        String username = "myUsername";
        byte[] requestAuth = random.generateSeed(16);

        RadiusAttribute attribute = dictionary.createAttribute("User-Name", username);
        AttributeTemplate template = dictionary.getAttributeTemplate("User-Name").get();

        OctetsAttribute encode = (OctetsAttribute) template.encode(attribute, requestAuth, "secret");
        assertEquals(attribute, encode);
        assertArrayEquals(username.getBytes(UTF_8), encode.getValue());
    }

    @Test
    void encodeDecodeNoTag() throws RadiusPacketException {
        String pw = "myPw";
        String secret = "secret";
        byte[] requestAuth = random.generateSeed(16);

        RadiusAttribute attribute = dictionary.createAttribute(-1, USER_PASSWORD, pw.getBytes(UTF_8));
        AttributeTemplate template = dictionary.getAttributeTemplate(USER_PASSWORD).get();

        assertArrayEquals(pw.getBytes(UTF_8), attribute.getValue());
        assertTrue(attribute.isDecoded());

        RadiusAttribute encode1 = template.encode(attribute, requestAuth, secret);
        assertNotEquals(attribute, encode1);
        assertTrue(encode1.isEncoded());
        assertFalse(Arrays.equals(pw.getBytes(UTF_8), encode1.getValue()));

        // idempotence check
        RadiusAttribute encode2 = template.encode(encode1, requestAuth, secret);
        assertEquals(encode1, encode2);
        assertTrue(encode2.isEncoded());
        assertArrayEquals(encode1.getValue(), encode2.getValue());

        RadiusAttribute decode1 = template.decode(encode2, requestAuth, secret);
        assertEquals(attribute, decode1);
        assertTrue(decode1.isDecoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode1.getValue());

        // idempotence check
        RadiusAttribute decode2 = template.decode(decode1, requestAuth, secret);
        assertEquals(decode1, decode2);
        assertTrue(decode2.isDecoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode2.getValue());
    }

    @Test
    void encodeDecodeWithTag() throws RadiusPacketException {
        String pw = "myPw";
        byte tag = random.generateSeed(1)[0];
        String secret = "secret";
        byte[] requestAuth = random.generateSeed(16);

        RadiusAttribute attribute = dictionary.createAttribute(-1, TUNNEL_PASSWORD, tag, pw.getBytes(UTF_8));
        AttributeTemplate template = dictionary.getAttributeTemplate(TUNNEL_PASSWORD).get();

        assertEquals(tag, attribute.getTag().get());
        assertArrayEquals(pw.getBytes(UTF_8), attribute.getValue());
        assertTrue(attribute.isDecoded());

        RadiusAttribute encode1 = template.encode(attribute, requestAuth, secret);
        assertNotEquals(attribute, encode1);
        assertTrue(encode1.isEncoded());
        assertFalse(Arrays.equals(pw.getBytes(UTF_8), encode1.getValue()));
        assertEquals(tag, attribute.getTag().get());
        assertEquals(tag, attribute.toByteArray()[2]);

        // idempotence check
        RadiusAttribute encode2 = template.encode(encode1, requestAuth, secret);
        assertEquals(encode1, encode2);
        assertTrue(encode2.isEncoded());
        assertArrayEquals(encode1.getValue(), encode2.getValue());
        assertEquals(tag, attribute.getTag().get());
        assertEquals(tag, attribute.toByteArray()[2]);

        RadiusAttribute decode1 = template.decode(encode2, requestAuth, secret);
        assertEquals(attribute, decode1);
        assertTrue(decode1.isDecoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode1.getValue());
        assertEquals(tag, attribute.getTag().get());
        assertEquals(tag, attribute.toByteArray()[2]);

        // idempotence check
        RadiusAttribute decode2 = template.decode(decode1, requestAuth, secret);
        assertEquals(decode1, decode2);
        assertTrue(decode2.isDecoded());
        assertArrayEquals(pw.getBytes(UTF_8), decode2.getValue());
        assertEquals(tag, attribute.getTag().get());
        assertEquals(tag, attribute.toByteArray()[2]);
    }

    @Test
    void templateValidationToStringAndErrors() {
        assertThrows(IllegalArgumentException.class, () ->
                new AttributeTemplate(-1, 1, "", "string", OctetsAttribute.FACTORY, NO_ENCRYPT, false));

        var template = new AttributeTemplate(-1, 1, "Test-Attr", "string", OctetsAttribute.FACTORY, RFC2865_USER_PASSWORD, false);
        assertThrows(IllegalArgumentException.class, () -> template.addEnumerationValue(1, ""));
        assertEquals("1/Test-Attr: string", template.toString());

        var vendorTemplate = new AttributeTemplate(14122, 1, "Vendor-Attr", "string", OctetsAttribute.FACTORY, NO_ENCRYPT, false);
        assertEquals("1/Vendor-Attr: string (Vendor 14122)", vendorTemplate.toString());

        // encode error when requestAuth is wrong length (not 16)
        var attr = template.create(dictionary, (byte) 0, new byte[]{1, 2, 3});
        assertThrows(RadiusPacketException.class, () -> template.encode(attr, new byte[5], "secret"));

        // decode error when encoded attribute has invalid length or short requestAuth
        var encodedAttr = template.createEncoded(dictionary, (byte) 0, new byte[]{1, 2, 3});
        assertThrows(RadiusPacketException.class, () -> template.decode(encodedAttr, new byte[16], "secret"));
    }

    @Test
    void attributeHolderEdgeCases() {
        // 1 trailing byte after reading attributes
        var buf1Byte = Unpooled.wrappedBuffer(new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> AttributeHolder.readAttributes(dictionary, -1, buf1Byte));

        // attribute length < typeSize + lengthSize (length = 1)
        var bufShortAttr = Unpooled.wrappedBuffer(new byte[]{1, 1});
        assertThrows(IllegalArgumentException.class, () -> AttributeHolder.readAttributes(dictionary, -1, bufShortAttr));

        // attribute length > readableBytes (length = 10, readable = 3)
        var bufLongAttr = Unpooled.wrappedBuffer(new byte[]{1, 10, 0});
        assertThrows(IllegalArgumentException.class, () -> AttributeHolder.readAttributes(dictionary, -1, bufLongAttr));

        // getAttributes(AttributeTemplate) on VendorSpecificAttribute
        var subAttr = dictionary.createAttribute(14122, 1, "0102");
        var vsa = new VendorSpecificAttribute(dictionary, 14122, List.of(subAttr));
        var matchingTemplate = new AttributeTemplate(14122, 1, "Vendor-Attr", "octets", OctetsAttribute.FACTORY, NO_ENCRYPT, false);
        var nonMatchingTemplate = dictionary.getAttributeTemplate(-1, 1).orElseThrow();
        assertEquals(1, vsa.getAttributes(matchingTemplate).size());
        assertTrue(vsa.getAttributes(nonMatchingTemplate).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> vsa.getAttributes("Non-Existent-Attr"));
    }
}