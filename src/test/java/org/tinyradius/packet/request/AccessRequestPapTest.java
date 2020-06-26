package org.tinyradius.packet.request;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.Arrays;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.request.AccessRequest.USER_PASSWORD;

class AccessRequestPapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private static final byte USER_NAME = 1;

    @ValueSource(strings = {"shortPw", "my16charPassword", "myMuchLongerPassword"})
    @ParameterizedTest
    void encodeVerify(String password) throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";

        final RadiusRequest encoded = new AccessRequestPap(dictionary, (byte) 1, null, Collections.emptyList())
                .withPassword(password)
                .encodeRequest(sharedSecret);

        assertNotEquals(password, ((AccessRequestPap) encoded).getPassword().get());

        assertNotNull(encoded.getAuthenticator());
        final RadiusRequest decoded = encoded.decodeRequest(sharedSecret);
        assertEquals(password, ((AccessRequestPap) decoded).getPassword().get());
    }

    @Test
    void verifyAttributeCount() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
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

    @Test
    void encodePapPassword() throws RadiusPacketException {
        String user = "myUser1";
        String password1 = "myPw1"; // todo test longer passwords
        String password2 = "myPw2";
        String sharedSecret = "sharedSecret1";

        RadiusRequest request = new AccessRequestPap(dictionary, (byte) 2, null, Collections.emptyList())
                .withPassword(password1)
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
        System.out.println(Arrays.toString(expectedEncodedPassword));
        System.out.println(Arrays.toString(encoded.getAttribute("User-Password").get().getValue()));
//        assertFalse(request.getAttribute("User-Password").isPresent());
        assertArrayEquals(expectedEncodedPassword, encoded.getAttribute("User-Password").get().getValue());
        System.out.println(expectedEncodedPassword);
        System.out.println(encoded.getAttribute("User-Password").get().getValue());
        assertEquals(password1, encoded.getPassword().get());

        // set password to something else
        final AccessRequestPap encoded2 = encoded.withPassword(password2);
        assertEquals(password2, encoded2.getPassword().get());

        // check decodes password
        final AccessRequestPap encoded3 = (AccessRequestPap) encoded2.decodeRequest(sharedSecret);
        assertEquals(password1, encoded3.getPassword().get());
    }

    // todo test encode is idempotent
}