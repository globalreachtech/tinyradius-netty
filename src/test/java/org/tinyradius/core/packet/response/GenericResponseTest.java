package org.tinyradius.core.packet.response;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

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
        String sharedSecret = "sharedSecret1";
        String username = "myUsername";
        String password = "myPassword";

        byte[] requestAuth = random.generateSeed(16);

        GenericResponse response = (GenericResponse) RadiusResponse.create(dictionary, (byte) 5, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute("User-Name", username))
                .addAttribute(dictionary.createAttribute(-1, USER_PASSWORD, password.getBytes(UTF_8)));

        RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> response.decodeResponse(sharedSecret, requestAuth));
        assertTrue(e.getMessage().contains("authenticator missing"));

        RadiusResponse encoded = response.encodeResponse(sharedSecret, requestAuth);
        assertNotNull(encoded.getAuthenticator());
        assertEquals(username, encoded.getAttribute("User-Name").get().getValueString());
        assertNotEquals(password, new String(encoded.getAttribute("User-Password").get().getValue(), UTF_8));

        // idempotence check
        RadiusResponse encoded2 = encoded.encodeResponse(sharedSecret, requestAuth);
        assertArrayEquals(encoded.toBytes(), encoded2.toBytes());

        RadiusResponse decoded = encoded2.decodeResponse(sharedSecret, requestAuth);
        assertEquals(username, decoded.getAttribute("User-Name").get().getValueString());
        assertEquals(password, new String(decoded.getAttribute("User-Password").get().getValue(), UTF_8));

        // idempotence check
        RadiusResponse decoded2 = decoded.decodeResponse(sharedSecret, requestAuth);
        assertArrayEquals(decoded.toBytes(), decoded2.toBytes());
        assertEquals(username, decoded2.getAttribute("User-Name").get().getValueString());
        assertEquals(password, new String(decoded2.getAttribute("User-Password").get().getValue(), UTF_8));
    }
}
