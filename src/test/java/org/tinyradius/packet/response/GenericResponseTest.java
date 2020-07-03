package org.tinyradius.packet.response;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class GenericResponseTest {

    private static final byte USER_PASSWORD = 2;

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final String username = "myUsername";
        final String password = "myPassword";

        final byte[] requestAuth = random.generateSeed(16);

        final RadiusResponse response = new GenericResponse(dictionary, (byte) 5, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute("User-Name", username))
                .addAttribute(dictionary.createAttribute(-1, USER_PASSWORD, password.getBytes(UTF_8)));

        final RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> response.decodeResponse(sharedSecret, requestAuth));
        assertTrue(e.getMessage().contains("authenticator missing"));

        final RadiusResponse encoded = response.encodeResponse(sharedSecret, requestAuth);
        assertNotNull(encoded.getAuthenticator());
        assertEquals(username, encoded.getAttribute("User-Name").get().getValueString());
        assertNotEquals(password, new String(encoded.getAttribute("User-Password").get().getValue(), UTF_8));

        // idempotence check
        final RadiusResponse encoded2 = encoded.encodeResponse(sharedSecret, requestAuth);
        assertArrayEquals(encoded.getAuthenticator(), encoded2.getAuthenticator());
        assertArrayEquals(encoded.getAttributeBytes(), encoded2.getAttributeBytes());

        final RadiusResponse decoded = encoded2.decodeResponse(sharedSecret, requestAuth);
        assertEquals(username, decoded.getAttribute("User-Name").get().getValueString());
        assertEquals(password, new String(decoded.getAttribute("User-Password").get().getValue(), UTF_8));

        // idempotence check
        final RadiusResponse decoded2 = decoded.decodeResponse(sharedSecret, requestAuth);
        assertArrayEquals(decoded.getAttributeBytes(), decoded2.getAttributeBytes());
        assertEquals(username, decoded2.getAttribute("User-Name").get().getValueString());
        assertEquals(password, new String(decoded2.getAttribute("User-Password").get().getValue(), UTF_8));
    }
}
