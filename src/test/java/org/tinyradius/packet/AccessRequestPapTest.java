package org.tinyradius.packet;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.tinyradius.attribute.Attributes;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.AccessRequest.USER_NAME;
import static org.tinyradius.packet.AccessRequest.USER_PASSWORD;
import static org.tinyradius.packet.AccessRequestPap.pad;

class AccessRequestPapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeVerify() throws RadiusPacketException {
        final String plaintextPw = "myPassword";
        String sharedSecret = "sharedSecret1";
        final AccessRequestPap accessRequestPap = new AccessRequestPap(dictionary, (byte) 1, null, Collections.emptyList());
        accessRequestPap.setPlaintextPassword(plaintextPw);

        final AccessRequest encoded = accessRequestPap.encodeRequest(sharedSecret);
        encoded.verifyRequest(sharedSecret);
    }

    @Test
    void verifyAttributeCount() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
        final AccessRequestPap request = new AccessRequestPap(dictionary, (byte) 1, new byte[16], Collections.emptyList());
        assertThrows(RadiusPacketException.class, () -> request.verifyRequest(sharedSecret));

        request.addAttribute(Attributes.create(dictionary, -1, USER_PASSWORD, new byte[16]));
        request.verifyRequest(sharedSecret); // should have exactly one instance

        request.addAttribute(Attributes.create(dictionary, -1, USER_PASSWORD, new byte[16]));
        assertThrows(RadiusPacketException.class, () -> request.verifyRequest(sharedSecret));
    }

    @Test
    void encodePapPassword() throws RadiusPacketException {
        String user = "myUser1";
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequestPap request = new AccessRequestPap(dictionary, (byte) 2, null, Collections.emptyList());
        request.addAttribute(USER_NAME, user);
        request.setPlaintextPassword(plaintextPw);

        // encode
        final AccessRequestPap encoded = (AccessRequestPap) request.encodeRequest(sharedSecret);

        final byte[] expectedEncodedPassword = RadiusUtils.encodePapPassword(
                request.getPlaintextPassword().getBytes(UTF_8), encoded.getAuthenticator(), sharedSecret);

        // check correct encode
        assertEquals(1, encoded.getType());
        assertEquals(2, encoded.getId());
        assertEquals(user, encoded.getAttributeString(USER_NAME));

        // check password fields
        assertNull(request.getAttribute("User-Password"));
        assertArrayEquals(expectedEncodedPassword, encoded.getAttribute("User-Password").getValue());
        assertEquals(plaintextPw, encoded.getPlaintextPassword());

        // flush transient fields
        encoded.setPlaintextPassword("something else");

        // check verify decodes password
        request.verifyRequest(sharedSecret);
        assertEquals(plaintextPw, request.getPlaintextPassword());
    }

    @Test
    void checkPassword() throws RadiusPacketException {
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequestPap request = new AccessRequestPap(dictionary, (byte) 2, null, Collections.emptyList(), plaintextPw);
        final AccessRequestPap encoded = (AccessRequestPap) request.encodeRequest(sharedSecret);

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