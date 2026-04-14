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

    @ValueSource(strings = {"pw123", "password", "veryLongPassword"})
    @ParameterizedTest
    void checkPassword(String password) throws RadiusPacketException {
        AccessRequestArap request = (AccessRequestArap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withArapPassword(password);

        assertTrue(request.checkPassword(password));
        assertFalse(request.checkPassword("wrongPassword"));
        assertFalse(request.checkPassword(""));
    }

    @Test
    void validateAttributes() throws RadiusPacketException {
        AccessRequest request = (AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, new byte[16], Collections.emptyList());
        
        // No ARAP-Password yet, should be AccessRequestNoAuth
        assertInstanceOf(AccessRequestNoAuth.class, request);

        // Add ARAP-Password
        AccessRequestArap arapRequest = (AccessRequestArap) request.withArapPassword("testpw");
        arapRequest.validateAttributes();

        // Add another ARAP-Password, should fail validation
        AccessRequest arapRequest2 = (AccessRequest) arapRequest.addAttribute(dictionary.createAttribute(-1, ARAP_PASSWORD, new byte[16]));
        assertThrows(RadiusPacketException.class, arapRequest2::validateAttributes);
    }

    @Test
    void encodeDecode() throws RadiusPacketException {
        String sharedSecret = "secret";
        String password = "myPassword";
        
        AccessRequestArap request = (AccessRequestArap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withArapPassword(password);

        RadiusRequest encoded = request.encodeRequest(sharedSecret);
        RadiusRequest decoded = encoded.decodeRequest(sharedSecret);

        assertInstanceOf(AccessRequestArap.class, decoded);
        assertTrue(((AccessRequestArap) decoded).checkPassword(password));
    }
}
