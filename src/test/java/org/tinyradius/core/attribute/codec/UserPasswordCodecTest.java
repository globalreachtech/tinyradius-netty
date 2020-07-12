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

    private static final byte USER_PASSWORD = 2;

    private final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final UserPasswordCodec codec = new UserPasswordCodec();

    @ValueSource(strings = {"shortPw", "my16charPassword", "myMuchLongerPassword", "myMuchLongerPasswordMyMuchLongerPassword"})
    @ParameterizedTest
    void encodeDecode(String password) throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);
        final String sharedSecret = "sharedSecret1";

        final RadiusAttribute attribute = dictionary.createAttribute(-1, USER_PASSWORD, password.getBytes(UTF_8));
        assertEquals(password, new String(attribute.getValue(), UTF_8));

        final byte[] encode = codec.encode(attribute.getValue(), requestAuth, sharedSecret);
        assertNotEquals(password, new String(encode, UTF_8));

        final byte[] decode = codec.decode(encode, requestAuth, sharedSecret);
        assertEquals(password, new String(decode, UTF_8));
    }

    @Test
    void encodeDecodeNumber() throws RadiusPacketException {
        final int pw = 12345;
        final byte[] pwByte = ByteBuffer.allocate(Integer.BYTES).putInt(pw).array();
        final byte[] requestAuth = random.generateSeed(16);
        final String sharedSecret = "sharedSecret1";

        // 529/1 is integer type
        final RadiusAttribute attribute = dictionary.createAttribute(529, 1, pwByte);
        assertArrayEquals(pwByte, attribute.getValue());

        final byte[] encode = codec.encode(attribute.getValue(), requestAuth, sharedSecret);
        assertNotEquals(Arrays.toString(pwByte), Arrays.toString(encode));

        final byte[] decode = codec.decode(encode, requestAuth, sharedSecret);
        assertArrayEquals(pwByte, decode);
    }
}