package org.tinyradius.core.packet.request;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.*;
import static org.tinyradius.core.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class AccessRequestTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void authenticatorOnlyAddedIfNull() throws RadiusPacketException {
        var sharedSecret = "sharedSecret1";
        var pw = "myPw";

        var nullAuthRequest = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 2, null, Collections.emptyList()))
                        .withPapPassword(pw);
        assertNull(nullAuthRequest.getAuthenticator());

        assertNotNull(nullAuthRequest.encodeRequest(sharedSecret).getAuthenticator());

        var authRequest = ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 2, random.generateSeed(16), Collections.emptyList()))
                .withPapPassword(pw);
        assertNotNull(authRequest.getAuthenticator());
        assertArrayEquals(authRequest.getAuthenticator(), authRequest.encodeRequest(sharedSecret).getAuthenticator());
    }

    @Test
    void testDetectCorrectAuth() throws RadiusPacketException {
        byte[] encodedPw = random.generateSeed(16);

        var papRequest = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, USER_PASSWORD, encodedPw)));
        assertInstanceOf(AccessRequestPap.class, papRequest);

        var chapRequest = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, CHAP_PASSWORD, encodedPw)));
        assertInstanceOf(AccessRequestChap.class, chapRequest);

        var eapRequest = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, EAP_MESSAGE, encodedPw)));
        assertInstanceOf(AccessRequestEap.class, eapRequest);

        var unknown = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());
        assertInstanceOf(AccessRequestNoAuth.class, unknown);

        var invalid = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null,
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
        var vendorSubAttr = dictionary.createAttribute(14122, USER_PASSWORD, new byte[4]);
        var accessRequest = (AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of(vendorSubAttr));

        // encode CHAP
        var chap = (AccessRequestChap) accessRequest.withChapPassword("abc");
        assertTrue(chap.getAttribute("CHAP-Password").isPresent());
        assertTrue(chap.getAttribute("CHAP-Challenge").isPresent());

        // encode PAP
        var pap = (AccessRequestPap) chap.withPapPassword("abc");
        assertTrue(pap.getAttribute("User-Password").isPresent()); // CHAP-Password removed
        assertTrue(pap.getAttribute("CHAP-Challenge").isPresent()); // not removed
        assertEquals(chap.getAttribute("CHAP-Challenge").get(), pap.getAttribute("CHAP-Challenge").get()); // unchanged

        // encode CHAP again
        var chap2 = (AccessRequestChap) pap.withChapPassword("abc");
        assertTrue(chap2.getAttribute("CHAP-Password").isPresent()); // User-Password removed
        assertNotEquals(pap.getAttribute("CHAP-Challenge").get(), chap2.getAttribute("CHAP-Challenge").get()); // newly encoded
    }

    @Test
    void invalidHeaderAndNoAuthMessageAuthenticator() throws RadiusPacketException {
        var badHeader = Unpooled.buffer(20).writeByte(ACCESS_ACCEPT).writeByte(1).writeShort(20).writeZero(16);
        assertThrows(IllegalArgumentException.class, () -> AccessRequest.create(dictionary, badHeader, List.of()));

        var validNoAuth = (AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
        var encodedWithMsgAuth = (AccessRequestNoAuth) validNoAuth.encodeRequest("secret");
        assertNotNull(encodedWithMsgAuth.decodeRequest("secret"));
    }
}