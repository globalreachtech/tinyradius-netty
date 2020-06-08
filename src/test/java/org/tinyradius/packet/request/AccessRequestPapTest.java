package org.tinyradius.packet.request;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.request.AccessRequest.USER_NAME;
import static org.tinyradius.packet.request.AccessRequest.USER_PASSWORD;
import static org.tinyradius.packet.request.AccessRequestPap.pad;

class AccessRequestPapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeVerify() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";

        final AccessRequestPap encoded = new AccessRequestPap(dictionary, (byte) 1, null, Collections.emptyList(), "myPassword")
                .encodeRequest(sharedSecret);

        assertNotNull(encoded.getAuthenticator());
        encoded.decodeRequest(sharedSecret);
    }

    @Test
    void verifyAttributeCount() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
        final AccessRequestPap request1 = new AccessRequestPap(dictionary, (byte) 1, new byte[16], Collections.emptyList());
        assertThrows(RadiusPacketException.class, () -> request1.decodeRequest(sharedSecret));

        // add one pw attribute
        final AccessRequestPap request2 = request1.withPassword("myPassword")
                .encodeRequest(sharedSecret);
        request2.decodeRequest(sharedSecret);

        // add one pw attribute
        final AccessRequestPap request3 = request2.addAttribute(Attributes.create(dictionary, -1, USER_PASSWORD, new byte[16]));
        assertThrows(RadiusPacketException.class, () -> request3.decodeRequest(sharedSecret));
    }

    @Test
    void encodePapPassword() throws RadiusPacketException {
        String user = "myUser1";
        String password1 = "myPw1";
        String password2 = "myPw2";
        String sharedSecret = "sharedSecret1";

        AccessRequestPap request = new AccessRequestPap(dictionary, (byte) 2, null, Collections.emptyList(), password1)
                .addAttribute(USER_NAME, user);

        // encode
        final AccessRequestPap encoded = request.encodeRequest(sharedSecret);

        final byte[] expectedEncodedPassword = RadiusUtils.encodePapPassword(
                request.getPassword().getBytes(UTF_8), encoded.getAuthenticator(), sharedSecret);

        // check correct encode
        assertEquals(1, encoded.getType());
        assertEquals(2, encoded.getId());
        assertEquals(user, encoded.getAttribute(USER_NAME).get().getValueString());

        // check password fields
        assertFalse(request.getAttribute("User-Password").isPresent());
        assertArrayEquals(expectedEncodedPassword, encoded.getAttribute("User-Password").get().getValue());
        assertEquals(password1, encoded.getPassword());

        // set password to something else
        final AccessRequestPap encoded2 = encoded.withPassword(password2);
        assertEquals(password2, encoded2.getPassword());

        // check decodes password
        final AccessRequestPap encoded3 = encoded2.decodeRequest(sharedSecret);
        assertEquals(password1, encoded3.getPassword());
    }

    @Test
    void checkPassword() throws RadiusPacketException {
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequestPap request = new AccessRequestPap(dictionary, (byte) 2, null, Collections.emptyList(), plaintextPw);
        final AccessRequestPap encoded = request.encodeRequest(sharedSecret);

        assertTrue(encoded.checkPassword(plaintextPw));
        assertFalse(encoded.checkPassword("badPw"));
    }

    @Test
    void testPad() {
        assertEquals(16, pad(new byte[0]).length);
        assertEquals(16, pad(new byte[1]).length);
        assertEquals(16, pad(new byte[2]).length);
        assertEquals(16, pad(new byte[15]).length);
        assertEquals(16, pad(new byte[16]).length);
        assertEquals(32, pad(new byte[17]).length);
        assertEquals(32, pad(new byte[18]).length);
        assertEquals(32, pad(new byte[32]).length);
        assertEquals(48, pad(new byte[33]).length);
    }
}