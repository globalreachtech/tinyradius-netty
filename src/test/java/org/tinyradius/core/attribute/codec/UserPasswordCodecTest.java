package org.tinyradius.core.attribute.codec;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.RadiusPacketException;

import java.security.SecureRandom;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UserPasswordCodecTest {

    private static final byte USER_PASSWORD = 2;

    private final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final UserPasswordCodec codec = new UserPasswordCodec();

    @ValueSource(strings = {"shortPw", "my16charPassword", "myMuchLongerPassword"})
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

        final RadiusAttribute attribute2 = dictionary.createAttribute(-1, USER_PASSWORD, password.getBytes(UTF_8));
        assertEquals(password, new String(attribute2.getValue(), UTF_8));
    }
}