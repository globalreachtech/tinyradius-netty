package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.Attributes;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.AccessRequest.*;

class AccessRequestTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void authenticatorOnlyAddedIfNull() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
        String pw = "myPw";

        AccessRequestPap nullAuthRequest = new AccessRequestPap(dictionary, 2, null, Collections.emptyList(), pw);
        assertNull(nullAuthRequest.getAuthenticator());

        assertNotNull(nullAuthRequest.encodeRequest(sharedSecret).getAuthenticator());

        AccessRequest authRequest = new AccessRequestPap(dictionary, 2, random16Bytes(), Collections.emptyList(), pw);
        assertNotNull(authRequest.getAuthenticator());
        assertArrayEquals(authRequest.getAuthenticator(), authRequest.encodeRequest(sharedSecret).getAuthenticator());
    }

    @Test
    void verifyDecodesPassword() throws RadiusPacketException {
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequestPap request = new AccessRequestPap(dictionary, 2, null, Collections.emptyList(), plaintextPw);
        final AccessRequestPap encoded = (AccessRequestPap) request.encodeRequest(sharedSecret);

        encoded.setPlaintextPassword("set field to something else");
        encoded.verifyRequest(sharedSecret);

        assertEquals(plaintextPw, encoded.getPlaintextPassword());
    }

    private byte[] random16Bytes() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

    @Test
    void testDetectCorrectAuth() {
        final SecureRandom random = new SecureRandom();
        final byte[] encodedPw = random.generateSeed(16);

        final AccessRequest papRequest = AccessRequest.create(dictionary, 1, null,
                Collections.singletonList(Attributes.create(dictionary, -1, USER_PASSWORD, encodedPw)));
        assertTrue(papRequest instanceof AccessRequestPap);

        final AccessRequest chapRequest = AccessRequest.create(dictionary, 1, null,
                Collections.singletonList(Attributes.create(dictionary, -1, CHAP_PASSWORD, encodedPw)));
        assertTrue(chapRequest instanceof AccessRequestChap);

        final AccessRequest eapRequest = AccessRequest.create(dictionary, 1, null,
                Collections.singletonList(Attributes.create(dictionary, -1, EAP_MESSAGE, encodedPw)));
        assertTrue(eapRequest instanceof AccessRequestEap);

        final AccessRequest unknown = AccessRequest.create(dictionary, 1, null, Collections.emptyList());
        assertTrue(unknown instanceof AccessInvalidAuth);
    }
}