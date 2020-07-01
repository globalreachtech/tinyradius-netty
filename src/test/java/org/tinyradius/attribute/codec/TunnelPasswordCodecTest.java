package org.tinyradius.attribute.codec;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class TunnelPasswordCodecTest {

    private static final byte TUNNEL_PASSWORD = 69;

    private final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final TunnelPasswordCodec codec = new TunnelPasswordCodec();

    @ValueSource(strings = {"shortPw", "my16charPassword", "myMuchLongerPassword"})
    @ParameterizedTest
    void encodeDecode(String password) throws RadiusPacketException {
        final byte[] requestAuth = random.generateSeed(16);
        final String sharedSecret = "sharedSecret1";

        final RadiusAttribute attribute = dictionary.createAttribute(-1, TUNNEL_PASSWORD, password.getBytes(UTF_8));
        assertEquals(password, new String(attribute.getValue(), UTF_8));
        // todo test encode is idempotent

        final byte[] encode = codec.encode(attribute.getValue(), sharedSecret, requestAuth);
        assertNotEquals(password, new String(encode, UTF_8));

        final byte[] decode = codec.decode(encode, sharedSecret, requestAuth);
        assertEquals(password, new String(decode, UTF_8));

        final RadiusAttribute attribute2 = dictionary.createAttribute(-1, TUNNEL_PASSWORD, password.getBytes(UTF_8));
        assertEquals(password, new String(attribute2.getValue(), UTF_8));
    }

}