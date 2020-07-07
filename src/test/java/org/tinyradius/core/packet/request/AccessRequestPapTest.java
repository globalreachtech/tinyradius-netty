package org.tinyradius.core.packet.request;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.RadiusPacketException;

import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.packet.request.AccessRequest.USER_PASSWORD;

class AccessRequestPapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private static final byte USER_NAME = 1;

    @ValueSource(strings = {"shortPw", "my16charPassword", "myMuchLongerPassword"})
    @ParameterizedTest
    void encodeDecode(String password) throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final String username = "myUsername";

        final RadiusRequest request = new AccessRequestPap(dictionary, (byte) 1, null, Collections.emptyList())
                .withPassword(password)
                .addAttribute(dictionary.createAttribute("User-Name", username));
        assertEquals(password, ((AccessRequestPap) request).getPassword().get());

        final RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> request.decodeRequest(sharedSecret));
        assertTrue(e.getMessage().contains("authenticator missing"));

        final RadiusRequest encoded = request.encodeRequest(sharedSecret);
        assertNotEquals(password, ((AccessRequestPap) encoded).getPassword().get());
        assertNotNull(encoded.getAuthenticator());

        // idempotence check
        final RadiusRequest encoded2 = encoded.encodeRequest(sharedSecret);
        assertArrayEquals(encoded.getAuthenticator(), encoded2.getAuthenticator());
        assertArrayEquals(encoded.getAttributeBytes(), encoded2.getAttributeBytes());

        final RadiusRequest decoded = encoded2.decodeRequest(sharedSecret);
        assertEquals(password, ((AccessRequestPap) decoded).getPassword().get());
        assertEquals(username, decoded.getAttribute("User-Name").get().getValueString());

        // idempotence check
        final RadiusRequest decoded2 = decoded.decodeRequest(sharedSecret);
        assertArrayEquals(decoded.getAttributeBytes(), decoded2.getAttributeBytes());
        assertEquals(password, ((AccessRequestPap) decoded2).getPassword().get());
        assertEquals(username, decoded2.getAttribute("User-Name").get().getValueString());
    }

    @Test
    void decodeChecksAttributeCount() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final AccessRequestPap request1 = new AccessRequestPap(dictionary, (byte) 1, new byte[16], Collections.emptyList());
        assertThrows(RadiusPacketException.class, () -> request1.decodeRequest(sharedSecret));

        // add one pw attribute
        final RadiusRequest request2 = request1.withPassword("myPassword")
                .encodeRequest(sharedSecret);
        request2.decodeRequest(sharedSecret);

        // add one pw attribute
        final RadiusRequest request3 = request2.addAttribute(dictionary.createAttribute(-1, USER_PASSWORD, new byte[16]));
        assertThrows(RadiusPacketException.class, () -> request3.decodeRequest(sharedSecret));
    }

    @ValueSource(strings = {"shortPw", "my16charPassword", "myMuchLongerPassword"})
    @ParameterizedTest
    void encodePapPassword(String password) throws RadiusPacketException {
        final String user = "myUser1";
        final String sharedSecret = "sharedSecret1";

        RadiusRequest request = new AccessRequestPap(dictionary, (byte) 2, null, Collections.emptyList())
                .withPassword(password)
                .addAttribute(USER_NAME, user);

        // encode
        final AccessRequestPap encoded = (AccessRequestPap) request.encodeRequest(sharedSecret);

        final byte[] expectedEncodedPassword = RadiusUtils.encodePapPassword(
                ((AccessRequestPap) request).getPassword().get().getBytes(UTF_8), encoded.getAuthenticator(), sharedSecret);

        // check correct encode
        assertEquals(1, encoded.getType());
        assertEquals(2, encoded.getId());
        assertEquals(user, encoded.getAttribute(USER_NAME).get().getValueString());

        // check password fields
        assertArrayEquals(expectedEncodedPassword, encoded.getAttribute("User-Password").get().getValue());
        assertNotEquals(password, encoded.getPassword().get());
    }
}