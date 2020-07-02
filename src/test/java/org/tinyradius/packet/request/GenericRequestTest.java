package org.tinyradius.packet.request;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class GenericRequestTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final String username = "myUsername";

        final RadiusRequest response = new GenericRequest(dictionary, (byte) 5, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute("User-Name", username));

        final RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> response.decodeRequest(sharedSecret));
        assertTrue(e.getMessage().contains("authenticator missing"));

        final RadiusRequest encodeRequest = response.encodeRequest(sharedSecret);
        final RadiusRequest decodeRequest = encodeRequest.decodeRequest(sharedSecret);

        final RadiusAttribute attribute = decodeRequest.getAttribute("User-Name").get();
        assertEquals(username, attribute.getValueString());

        // todo idempotence
    }
}
