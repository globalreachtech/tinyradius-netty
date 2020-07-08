package org.tinyradius.core.attribute.type.decorator;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.security.SecureRandom;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class TaggedAttributeTest {

    private final SecureRandom random = new SecureRandom();
    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String secret = "mySecret";
        final String pw = "myPw";
        final byte tag = 123;
        final byte[] requestAuth = random.generateSeed(16);

        final RadiusAttribute attribute = dictionary.createAttribute(-1, (byte) 69, tag, pw);
        assertTrue(attribute instanceof TaggedAttribute);
        assertFalse(attribute.isEncoded());
        assertEquals(pw, new String(attribute.getValue(), UTF_8));

        // encode
        final RadiusAttribute encode = attribute.encode(requestAuth, secret);
        assertTrue(encode instanceof EncodedAttribute);
        assertTrue(encode.isEncoded());
        assertEquals(tag, encode.getTag());
        assertNotEquals(pw, new String(encode.getValue(), UTF_8));

        // encode again
        final RadiusAttribute encode1 = encode.encode(requestAuth, secret);
        assertEquals(encode1, encode);

        // decode
        final RadiusAttribute decode = encode.decode(requestAuth, secret);
        assertTrue(decode instanceof TaggedAttribute);
        assertFalse(decode.isEncoded());
        assertEquals(tag, decode.getTag());
        assertEquals(pw, new String(decode.getValue(), UTF_8));

        // decode again
        final RadiusAttribute decode1 = decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }
}
