package org.tinyradius.packet.request;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.request.AccessRequest.EAP_MESSAGE;
import static org.tinyradius.packet.util.MessageAuthSupport.MESSAGE_AUTHENTICATOR;

class AccessRequestEapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final AccessRequestEap accessRequestEap = new AccessRequestEap(dictionary, (byte) 1, null,
                Collections.singletonList(dictionary.createAttribute(-1, EAP_MESSAGE, new byte[16])));

        final RadiusRequest encoded = accessRequestEap.encodeRequest(sharedSecret);

        assertNotNull(encoded.getAuthenticator());
        encoded.decodeRequest(sharedSecret);
    }

    @Test
    void verifyAttributeCount() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final AccessRequestEap request = new AccessRequestEap(dictionary, (byte) 1, new byte[16], Collections.emptyList());
        assertThrows(RadiusPacketException.class, () -> request.decodeRequest(sharedSecret));

        final RadiusRequest request1 = request.addAttribute(dictionary.createAttribute(-1, EAP_MESSAGE, new byte[16]));
        assertThrows(RadiusPacketException.class, () -> request1.decodeRequest(sharedSecret));

        // add one messageAuth
        final RadiusRequest request2 = request1.encodeRequest(sharedSecret);
        request2.decodeRequest(sharedSecret);

        final RadiusRequest request3 = request2.addAttribute(dictionary.createAttribute(-1, MESSAGE_AUTHENTICATOR, new byte[16]));
        final RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> request3.decodeRequest(sharedSecret));
        assertEquals("AccessRequest (EAP) should have exactly one Message-Authenticator attribute, has 2", e.getMessage());
    }
}
