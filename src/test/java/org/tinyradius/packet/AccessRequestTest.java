package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class AccessRequestTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void authenticatorOnlyAddedIfNull() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";

        AccessPap nullAuthRequest = new AccessPap(dictionary, 2, null, Collections.emptyList());
        nullAuthRequest.setPlaintextPassword("myPw");
        assertNull(nullAuthRequest.getAuthenticator());

        assertNotNull(nullAuthRequest.encodeRequest(sharedSecret).getAuthenticator());

        AccessRequest authRequest = new AccessPap(dictionary, 2, random16Bytes(), Collections.emptyList());
        assertNotNull(authRequest.getAuthenticator());
        assertArrayEquals(authRequest.getAuthenticator(), authRequest.encodeRequest(sharedSecret).getAuthenticator());
    }

    @Test
    void verifyDecodesPassword() throws RadiusPacketException {
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessPap request = new AccessPap(dictionary, 2, null, Collections.emptyList(), plaintextPw);
        final AccessPap encoded = (AccessPap) request.encodeRequest(sharedSecret);

        encoded.setPlaintextPassword("set field to something else");
        encoded.verify(sharedSecret, null);

        assertEquals(plaintextPw, encoded.getUserPassword());
    }

    private byte[] random16Bytes() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    @Test
    void testCreateCorrectSubclass() {
        throw new RuntimeException();
    }
}