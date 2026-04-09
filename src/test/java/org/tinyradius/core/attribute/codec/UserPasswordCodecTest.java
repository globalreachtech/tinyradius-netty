package org.tinyradius.core.attribute.codec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class UserPasswordCodecTest {

    private final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final UserPasswordCodec codec = new UserPasswordCodec();

    @ValueSource(strings = {"a", "shortPw", "my16charPassword", "myMuchLongerPassword", "myMuchLongerPasswordMyMuchLongerPassword"})
    @ParameterizedTest
    void encodeDecode(String password) throws RadiusPacketException {
        byte[] requestAuth = random.generateSeed(16);
        String sharedSecret = "sharedSecret1";

        RadiusAttribute attribute = dictionary.createAttribute(1, 1, password.getBytes(UTF_8));
        assertEquals(password, new String(attribute.getValue(), UTF_8));

        byte[] encode = codec.encode(attribute.getValue(), requestAuth, sharedSecret);
        assertNotEquals(password, new String(encode, UTF_8));

        byte[] decode = codec.decode(encode, requestAuth, sharedSecret);
        assertEquals(password, new String(decode, UTF_8));
    }

    @Test
    void encodeDecodeNumber() throws RadiusPacketException {
        int pw = 12345;
        byte[] pwByte = ByteBuffer.allocate(Integer.BYTES).putInt(pw).array();
        byte[] requestAuth = random.generateSeed(16);
        String sharedSecret = "sharedSecret1";

        // 529/1 is integer type
        RadiusAttribute attribute = dictionary.createAttribute(529, 1, pwByte);
        assertArrayEquals(pwByte, attribute.getValue());

        byte[] encode = codec.encode(attribute.getValue(), requestAuth, sharedSecret);
        assertNotEquals(Arrays.toString(pwByte), Arrays.toString(encode));

        byte[] decode = codec.decode(encode, requestAuth, sharedSecret);
        assertArrayEquals(pwByte, decode);
    }
}