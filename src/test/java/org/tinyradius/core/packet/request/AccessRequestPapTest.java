package org.tinyradius.core.packet.request;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.USER_PASSWORD;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

class AccessRequestPapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private static final byte USER_NAME = 1;

    @ValueSource(strings = {"", "shortPw", "my16charPassword", "myMuchLongerPassword"})
    @ParameterizedTest
    void encodeDecode(String password) throws RadiusPacketException {
        var sharedSecret = "sharedSecret1";
        var username = "myUsername";

        var request = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withPapPassword(password)
                        .addAttribute(dictionary.createAttribute("User-Name", username));
        assertEquals(password, request.getPassword().get());

        var e = assertThrows(RadiusPacketException.class, () -> request.decodeRequest(sharedSecret));
        assertTrue(e.getMessage().contains("authenticator missing"));

        var encoded = request.encodeRequest(sharedSecret);
        assertNotEquals(password, ((AccessRequestPap) encoded).getPassword().get());
        assertNotNull(encoded.getAuthenticator());

        // idempotence check
        var encoded2 = encoded.encodeRequest(sharedSecret);
        assertArrayEquals(encoded.toBytes(), encoded2.toBytes());

        var decoded = encoded2.decodeRequest(sharedSecret);
        assertEquals(password, ((AccessRequestPap) decoded).getPassword().get());
        assertEquals(username, decoded.getAttribute("User-Name").get().getValueString());

        // idempotence check
        var decoded2 = decoded.decodeRequest(sharedSecret);
        assertArrayEquals(decoded.toBytes(), decoded2.toBytes());
        assertEquals(password, ((AccessRequestPap) decoded2).getPassword().get());
        assertEquals(username, decoded2.getAttribute("User-Name").get().getValueString());
    }

    @Test
    void emptyUserPassword() throws RadiusPacketException {
        var sharedSecret = "sharedSecret1";
        var request = ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                .withPapPassword("")
                .encodeRequest(sharedSecret);

        var decoded = (AccessRequestPap) request.decodeRequest(sharedSecret);
        assertEquals("", decoded.getPassword().orElse(null));
        assertEquals(0, decoded.getAttribute(USER_PASSWORD).orElseThrow().getValue().length);
    }

    @Test
    void decodeChecksAttributeCount() throws RadiusPacketException {
        var sharedSecret = "sharedSecret1";
        var request1 = (AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, new byte[16], Collections.emptyList());
        assertThrows(RadiusPacketException.class, () -> request1.decodeRequest(sharedSecret));

        // add one pw attribute
        var request2 = request1.withPapPassword("myPassword")
                .encodeRequest(sharedSecret)
                .decodeRequest(sharedSecret);

        // add one pw attribute
        var request3 = request2.addAttribute(dictionary.createAttribute(-1, USER_PASSWORD, new byte[16]));
        assertThrows(RadiusPacketException.class, () -> request3.decodeRequest(sharedSecret));
    }

    @ValueSource(strings = {"shortPw", "my16charPassword", "myMuchLongerPassword"})
    @ParameterizedTest
    void encodePapPassword(String password) throws RadiusPacketException {
        var user = "myUser1";
        var sharedSecret = "sharedSecret1";

        var request = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 2, null, Collections.emptyList()))
                        .withPapPassword(password)
                        .addAttribute(USER_NAME, user);

        // encode
        var encoded = (AccessRequestPap) request.encodeRequest(sharedSecret);

        byte[] expectedEncodedPassword = RadiusUtils.encodePapPassword(
                request.getPassword().get().getBytes(UTF_8), encoded.getAuthenticator(), sharedSecret);

        // check correct encode
        assertEquals(1, encoded.getType());
        assertEquals(2, encoded.getId());
        assertEquals(user, encoded.getAttribute(USER_NAME).get().getValueString());

        // check password fields
        assertArrayEquals(expectedEncodedPassword, encoded.getAttribute("User-Password").get().getValue());
        assertNotEquals(password, encoded.getPassword().get());
    }
}