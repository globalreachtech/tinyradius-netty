package org.tinyradius.core.packet.request;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.packet.request.AccessRequest.EAP_MESSAGE;
import static org.tinyradius.core.packet.request.AccessRequest.random16bytes;
import static org.tinyradius.core.packet.util.MessageAuthSupport.MESSAGE_AUTHENTICATOR;

class AccessRequestEapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final byte[] message = random16bytes();

        final AccessRequestEap request = (AccessRequestEap) RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute(-1, EAP_MESSAGE, message));

        final RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> request.decodeRequest(sharedSecret));
        assertTrue(e.getMessage().contains("should have exactly one Message-Authenticator attribute"));

        final RadiusRequest encoded = request.encodeRequest(sharedSecret);
        assertNotNull(encoded.getAuthenticator());
        assertArrayEquals(message, encoded.getAttribute(EAP_MESSAGE).get().getValue());

        // idempotence check
        final RadiusRequest encoded2 = encoded.encodeRequest(sharedSecret);
        assertArrayEquals(encoded.getAuthenticator(), encoded2.getAuthenticator());
        assertArrayEquals(encoded.getAttributeBytes(), encoded2.getAttributeBytes());

        final RadiusRequest decoded = encoded2.decodeRequest(sharedSecret);
        assertArrayEquals(message, decoded.getAttribute(EAP_MESSAGE).get().getValue());

        // idempotence check
        final RadiusRequest decoded2 = decoded.decodeRequest(sharedSecret);
        assertArrayEquals(decoded.getAttributeBytes(), decoded2.getAttributeBytes());
        assertArrayEquals(message, decoded2.getAttribute(EAP_MESSAGE).get().getValue());
    }

    @Test
    void verifyAttributeCount() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final AccessRequestEap request = (AccessRequestEap) RadiusRequest.create(dictionary, (byte) 1, (byte) 1, new byte[16], Collections.emptyList());
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
