package org.tinyradius.core.attribute.type;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.MemoryDictionary;

import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.TUNNEL_PASSWORD;
import static org.tinyradius.core.attribute.AttributeTypes.USER_NAME;
import static org.tinyradius.core.attribute.AttributeTypes.USER_PASSWORD;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

class StringAttributeTest {

    private static final RadiusAttributeFactory<StringAttribute> FACTORY = StringAttribute.FACTORY;

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void standardNoEncryptMinLength() {
        // Standard unencrypted attribute (min length: header(2) + tag(0) + 1 = 3)
        var exception = assertThrows(IllegalArgumentException.class,
                () -> FACTORY.create(dictionary, -1, USER_NAME, (byte) 0, ""));
        assertEquals("String attribute value should be min 3 octets, actual: 2", exception.getCause().getMessage());

        var valid = FACTORY.create(dictionary, -1, USER_NAME, (byte) 0, "a");
        assertEquals("a", valid.getValueString());
        assertEquals(3, valid.getData().readableBytes());
    }

    @Test
    void taggedNoEncryptMinLength() {
        // Tagged unencrypted string attribute (min length: header(2) + tag(1) + 1 = 4)
        var taggedTemplate = new AttributeTemplate(
                -1, 100, "Test-Tagged-String", "string", FACTORY, NO_ENCRYPT, true);
        var customDict = new MemoryDictionary().addAttributeTemplate(taggedTemplate);

        // Empty string value produces Type(1) + Length(1) + Tag(1) = 3 octets, which is less than minLength 4
        var exception = assertThrows(IllegalArgumentException.class,
                () -> FACTORY.create(customDict, -1, 100, (byte) 1, ""));
        assertEquals("String attribute value should be min 4 octets, actual: 3", exception.getCause().getMessage());

        var valid = FACTORY.create(customDict, -1, 100, (byte) 1, "a");
        assertEquals("a", valid.getValueString());
        assertEquals(4, valid.getData().readableBytes());
    }

    @Test
    void userPasswordMinLength() {
        // Untagged encrypted attribute (min length: header(2) + tag(0) + 0 = 2)
        // Decoded empty password produces 2 octets (Type + Length) and is allowed
        var userPassword = FACTORY.create(dictionary, -1, USER_PASSWORD, (byte) 0, "");
        assertEquals("", userPassword.getValueString());
        assertEquals("User-Password=", userPassword.toString());
        assertEquals(0, userPassword.getValue().length);
        assertEquals(2, userPassword.getData().readableBytes());
    }

    @Test
    void tunnelPasswordMinLength() {
        // Tagged encrypted attribute (min length: header(2) + tag(1) + 0 = 3)
        // Decoded empty password produces 3 octets (Type + Length + Tag) and is allowed
        var tunnelPassword = FACTORY.create(dictionary, -1, TUNNEL_PASSWORD, (byte) 1, "");
        assertEquals("", tunnelPassword.getValueString());
        assertEquals(0, tunnelPassword.getValue().length);
        assertEquals(3, tunnelPassword.getData().readableBytes());

        // Malformed buffer of 2 octets (missing required tag byte) fails minLength 3
        var malformedBuffer = Unpooled.wrappedBuffer(new byte[]{TUNNEL_PASSWORD, 2});
        var exception = assertThrows(IllegalArgumentException.class,
                () -> FACTORY.create(dictionary, -1, malformedBuffer));
        assertEquals("String attribute value should be min 3 octets, actual: 2", exception.getCause().getMessage());
    }

    @Test
    void getDataValue() {
        var s = new Date().toString();
        var stringAttribute = FACTORY.create(dictionary, -1, USER_NAME, (byte) 0, s);
        assertEquals(s, stringAttribute.getValueString());
        assertArrayEquals(s.getBytes(UTF_8), stringAttribute.getValue());
    }
}
