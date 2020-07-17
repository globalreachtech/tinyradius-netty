package org.tinyradius.core.packet.request;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.packet.request.AccessRequest.*;

class AccessRequestTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void authenticatorOnlyAddedIfNull() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
        String pw = "myPw";

        AccessRequestPap nullAuthRequest = ((AccessRequest) RadiusRequest.create(dictionary, (byte) 1, (byte) 2, null, Collections.emptyList()))
                .withPapPassword(pw);
        assertNull(nullAuthRequest.getAuthenticator());

        assertNotNull(nullAuthRequest.encodeRequest(sharedSecret).getAuthenticator());

        RadiusRequest authRequest = ((AccessRequest) RadiusRequest.create(dictionary, (byte) 1, (byte) 2, random.generateSeed(16), Collections.emptyList()))
                .withPapPassword(pw);
        assertNotNull(authRequest.getAuthenticator());
        assertArrayEquals(authRequest.getAuthenticator(), authRequest.encodeRequest(sharedSecret).getAuthenticator());
    }

    @Test
    void testDetectCorrectAuth() throws RadiusPacketException {
        final SecureRandom random = new SecureRandom();
        final byte[] encodedPw = random.generateSeed(16);

        final AccessRequest papRequest = (AccessRequest) RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, USER_PASSWORD, encodedPw)));
        assertTrue(papRequest instanceof AccessRequestPap);

        final AccessRequest chapRequest = (AccessRequest) RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, CHAP_PASSWORD, encodedPw)));
        assertTrue(chapRequest instanceof AccessRequestChap);

        final AccessRequest eapRequest = (AccessRequest) RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, EAP_MESSAGE, encodedPw)));
        assertTrue(eapRequest instanceof AccessRequestEap);

        final AccessRequest unknown = (AccessRequest) RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());
        assertTrue(unknown instanceof AccessRequestNoAuth);

        final AccessRequest invalid = (AccessRequest) RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null,
                Arrays.asList(
                        dictionary.createAttribute(-1, CHAP_PASSWORD, encodedPw),
                        dictionary.createAttribute(-1, EAP_MESSAGE, encodedPw)
                ));
        assertTrue(invalid instanceof AccessRequestNoAuth);
    }
}