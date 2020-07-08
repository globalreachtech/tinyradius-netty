package org.tinyradius.core.attribute.codec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.RadiusPacketException;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

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

        final byte[] encode = codec.encode(attribute.getValue(), requestAuth, sharedSecret);
        assertNotEquals(password, new String(encode, UTF_8));

        final byte[] decode = codec.decode(encode, requestAuth, sharedSecret);
        assertEquals(password, new String(decode, UTF_8));
    }

    @Test
    void encodeDecodeNumber() throws RadiusPacketException {
        final int pw = 12345;
        final byte[] pwByte = ByteBuffer.allocate(4).putInt(pw).array();
        final byte[] requestAuth = random.generateSeed(16);
        final String sharedSecret = "sharedSecret1";

        final RadiusAttribute attribute = dictionary.createAttribute(-1, (byte) 141, pwByte);
        assertArrayEquals(pwByte, attribute.getValue());

        final byte[] encode = codec.encode(attribute.getValue(), requestAuth, sharedSecret);
        assertNotEquals(Arrays.toString(pwByte), Arrays.toString(encode));

        final byte[] decode = codec.decode(encode, requestAuth, sharedSecret);
        assertArrayEquals(pwByte, decode);
    }
}