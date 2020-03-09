package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;
import java.util.Arrays;
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

        AccessRequestPap nullAuthRequest = new AccessRequestPap(dictionary, (byte) 2, null, Collections.emptyList(), pw);
        assertNull(nullAuthRequest.getAuthenticator());

        assertNotNull(nullAuthRequest.encodeRequest(sharedSecret).getAuthenticator());

        AccessRequest authRequest = new AccessRequestPap(dictionary, (byte) 2, random.generateSeed(16), Collections.emptyList(), pw);
        assertNotNull(authRequest.getAuthenticator());
        assertArrayEquals(authRequest.getAuthenticator(), authRequest.encodeRequest(sharedSecret).getAuthenticator());
    }

    @Test
    void testDetectCorrectAuth() {
        final SecureRandom random = new SecureRandom();
        final byte[] encodedPw = random.generateSeed(16);

        final AccessRequest papRequest = AccessRequest.create(dictionary, (byte) 1, null,
                Collections.singletonList(Attributes.create(dictionary, -1, USER_PASSWORD, encodedPw)));
        assertTrue(papRequest instanceof AccessRequestPap);

        final AccessRequest chapRequest = AccessRequest.create(dictionary, (byte) 1, null,
                Collections.singletonList(Attributes.create(dictionary, -1, CHAP_PASSWORD, encodedPw)));
        assertTrue(chapRequest instanceof AccessRequestChap);

        final AccessRequest eapRequest = AccessRequest.create(dictionary, (byte) 1, null,
                Collections.singletonList(Attributes.create(dictionary, -1, EAP_MESSAGE, encodedPw)));
        assertTrue(eapRequest instanceof AccessRequestEap);

        final AccessRequest unknown = AccessRequest.create(dictionary, (byte) 1, null, Collections.emptyList());
        assertTrue(unknown instanceof AccessRequestNoAuth);

        final AccessRequest invalid = AccessRequest.create(dictionary, (byte) 1, null,
                Arrays.asList(
                        Attributes.create(dictionary, -1, CHAP_PASSWORD, encodedPw),
                        Attributes.create(dictionary, -1, EAP_MESSAGE, encodedPw)
                ));
        assertTrue(invalid instanceof AccessRequestNoAuth);
    }
}