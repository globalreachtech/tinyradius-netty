package org.tinyradius.core.packet.request;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.*;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

class AccessRequestTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void authenticatorOnlyAddedIfNull() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
        String pw = "myPw";

        final AccessRequestPap nullAuthRequest = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 2, null, Collections.emptyList()))
                        .withPapPassword(pw);
        assertNull(nullAuthRequest.getAuthenticator());

        assertNotNull(nullAuthRequest.encodeRequest(sharedSecret).getAuthenticator());

        final RadiusRequest authRequest = ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 2, random.generateSeed(16), Collections.emptyList()))
                .withPapPassword(pw);
        assertNotNull(authRequest.getAuthenticator());
        assertArrayEquals(authRequest.getAuthenticator(), authRequest.encodeRequest(sharedSecret).getAuthenticator());
    }

    @Test
    void testDetectCorrectAuth() throws RadiusPacketException {
        final SecureRandom random = new SecureRandom();
        final byte[] encodedPw = random.generateSeed(16);

        final AccessRequest papRequest = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, USER_PASSWORD, encodedPw)));
        assertInstanceOf(AccessRequestPap.class, papRequest);

        final AccessRequest chapRequest = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, CHAP_PASSWORD, encodedPw)));
        assertInstanceOf(AccessRequestChap.class, chapRequest);

        final AccessRequest eapRequest = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, EAP_MESSAGE, encodedPw)));
        assertInstanceOf(AccessRequestEap.class, eapRequest);

        final AccessRequest unknown = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());
        assertInstanceOf(AccessRequestNoAuth.class, unknown);

        final AccessRequest invalid = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null,
                Arrays.asList(
                        dictionary.createAttribute(-1, CHAP_PASSWORD, encodedPw),
                        dictionary.createAttribute(-1, EAP_MESSAGE, encodedPw)
                ));
        assertInstanceOf(AccessRequestNoAuth.class, invalid);
    }

    /**
     * <a href="https://tools.ietf.org/html/rfc2869#section-5.19">RFC 2869 Section 5.19</a>
     * <p>
     * An Access-Request that contains either a User-Password or
     * CHAP-Password or ARAP-Password or one or more EAP-Message attributes
     * MUST NOT contain more than one type of those four attributes.  If it
     * does not contain any of those four attributes, it SHOULD contain a
     * Message-Authenticator.
     */
    @Test
    void withPasswordRemovesOtherTypes() throws RadiusPacketException {
        final AccessRequestNoAuth accessRequest = (AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());

        // encode CHAP
        final AccessRequestChap chap = (AccessRequestChap) accessRequest.withChapPassword("abc");
        assertEquals(2, chap.getAttributes().size());
        assertTrue(chap.getAttribute("CHAP-Password").isPresent());
        assertTrue(chap.getAttribute("CHAP-Challenge").isPresent());

        // encode PAP
        final AccessRequestPap pap = (AccessRequestPap) chap.withPapPassword("abc");
        assertEquals(2, pap.getAttributes().size());
        assertTrue(pap.getAttribute("User-Password").isPresent()); // CHAP-Password removed
        assertTrue(pap.getAttribute("CHAP-Challenge").isPresent()); // not removed
        assertEquals(chap.getAttribute("CHAP-Challenge").get(), pap.getAttribute("CHAP-Challenge").get()); // unchanged

        // encode CHAP again
        final AccessRequestChap chap2 = (AccessRequestChap) pap.withChapPassword("abc");
        assertEquals(2, chap2.getAttributes().size());
        assertTrue(chap2.getAttribute("CHAP-Password").isPresent()); // User-Password removed
        assertNotEquals(pap.getAttribute("CHAP-Challenge").get(), chap2.getAttribute("CHAP-Challenge").get()); // newly encoded
    }
}