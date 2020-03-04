package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.Attributes;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.tinyradius.packet.AccessRequest.EAP_MESSAGE;
import static org.tinyradius.packet.util.MessageAuthSupport.MESSAGE_AUTHENTICATOR;

public class AccessRequestEapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeVerify() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
        final AccessRequestEap accessRequestEap = new AccessRequestEap(dictionary, (byte) 1, null, Collections.emptyList());

        final AccessRequest encoded = accessRequestEap.encodeRequest(sharedSecret);

        encoded.verifyRequest(sharedSecret);
        throw new NotImplementedException();
    }

    @Test
    void testVerify() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
        final AccessRequestEap request = new AccessRequestEap(dictionary, (byte) 1, new byte[16], Collections.emptyList());
        assertThrows(RadiusPacketException.class, () -> request.verifyRequest(sharedSecret));

        request.addAttribute(Attributes.create(dictionary, -1, EAP_MESSAGE, new byte[16]));
        assertThrows(RadiusPacketException.class, () -> request.verifyRequest(sharedSecret));

        final AccessRequest encoded = request.encodeRequest(sharedSecret); // adds one messageAuth
        encoded.verifyRequest(sharedSecret); // should have exactly one instance

        encoded.addAttribute(Attributes.create(dictionary, -1, MESSAGE_AUTHENTICATOR, new byte[16]));
        assertThrows(RadiusPacketException.class, () -> request.verifyRequest(sharedSecret));
    }
}
