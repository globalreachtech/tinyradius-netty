package org.tinyradius.packet.response;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;
import java.util.Collections;

class AccessResponseTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeVerify() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
        final byte[] requestAuth = random.generateSeed(16);

        final AccessResponse accessResponse = new AccessResponse(dictionary, (byte) 2, (byte) 1, null, Collections.emptyList());
        final AccessResponse encodedResponse = accessResponse.encodeResponse(sharedSecret, requestAuth);
        encodedResponse.decodeResponse(sharedSecret, requestAuth);
    }
}