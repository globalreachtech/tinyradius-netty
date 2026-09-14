package org.tinyradius.core.packet.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.ARAP_PASSWORD;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

class AccessRequestArapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @ValueSource(strings = {"pw123", "password"})
    @ParameterizedTest
    void checkPassword(String password) throws RadiusPacketException {
        var request = (AccessRequestArap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withArapPassword(password);

        assertTrue(request.checkPassword(password));
        assertFalse(request.checkPassword("wrongPassword"));
        assertFalse(request.checkPassword(""));
    }

    @Test
    void checkPasswordTooLongThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                    .withArapPassword("veryLongPassword")
        );
    }

    @Test
    void checkPasswordTooLongReturnsFalse() throws RadiusPacketException {
        var request = (AccessRequestArap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withArapPassword("pw123");

        assertFalse(request.checkPassword("veryLongPassword"));
    }

    @Test
    void challengeResponse() throws RadiusPacketException {
        var password = "testpw";
        var request = (AccessRequestArap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withArapPassword(password);

        byte[] clientChallenge = request.getClientChallenge();
        assertNotNull(clientChallenge);
        assertEquals(8, clientChallenge.length);

        byte[] response = request.getChallengeResponse(password);
        assertNotNull(response);
        assertEquals(8, response.length);

        assertNull(request.getChallengeResponse("veryLongPassword"));
    }

    @Test
    void validateAttributes() throws RadiusPacketException {
        var request = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, new byte[16], Collections.emptyList());
        
        // No ARAP-Password yet, should be AccessRequestNoAuth
        assertInstanceOf(AccessRequestNoAuth.class, request);

        // Add ARAP-Password
        var arapRequest = (AccessRequestArap) request.withArapPassword("testpw");
        arapRequest.validateAttributes();

        // Add another ARAP-Password, should fail validation
        var arapRequest2 = (AccessRequest) arapRequest.addAttribute(dictionary.createAttribute(-1, ARAP_PASSWORD, new byte[16]));
        assertThrows(RadiusPacketException.class, arapRequest2::validateAttributes);
    }

    @Test
    void encodeDecode() throws RadiusPacketException {
        var sharedSecret = "secret";
        var password = "myPw";
        
        var request = (AccessRequestArap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withArapPassword(password);

        var encoded = request.encodeRequest(sharedSecret);
        var decoded = encoded.decodeRequest(sharedSecret);

        assertInstanceOf(AccessRequestArap.class, decoded);
        assertTrue(((AccessRequestArap) decoded).checkPassword(password));
    }
}
