package org.tinyradius.packet;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.Attributes;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.AccessRequest.USER_NAME;
import static org.tinyradius.packet.AccessRequestPap.pad;

class AccessRequestPapTest {

    private static final SecureRandom random = new SecureRandom();
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
    void encodePapPassword() throws RadiusPacketException {
        String user = "myUser1";
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequestPap request = new AccessRequestPap(dictionary, (byte) 2, null, Collections.emptyList());
        request.setAttributeString(USER_NAME, user);
        request.setPlaintextPassword(plaintextPw);
        final AccessRequestPap encoded = (AccessRequestPap) request.encodeRequest(sharedSecret);

        // randomly generated, need to extract
        final byte[] authenticator = encoded.getAuthenticator();
        final byte[] expectedEncodedPassword = RadiusUtils.encodePapPassword(
                request.getPlaintextPassword().getBytes(UTF_8), authenticator, sharedSecret);

        assertNull(request.getAttribute("User-Password"));
        assertNull(request.getAttribute("CHAP-Password"));
        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getId(), encoded.getId());
        assertEquals(request.getAttributeString(USER_NAME), encoded.getAttributeString(USER_NAME));

        assertNull(encoded.getAttribute("CHAP-Password"));
        assertArrayEquals(expectedEncodedPassword, encoded.getAttribute("User-Password").getValue());

        // check transient fields copied across
        assertEquals(plaintextPw, encoded.getPlaintextPassword());
        assertEquals(user, encoded.getAttributeString(USER_NAME));
    }

    @Test
    void decodePapPassword() throws RadiusPacketException {
        String user = "user2";
        String plaintextPw = "myPassword2";
        String sharedSecret = "sharedSecret2";
        final byte[] authenticator = random.generateSeed(16);

        byte[] encodedPassword = RadiusUtils.encodePapPassword(plaintextPw.getBytes(UTF_8), authenticator, sharedSecret);

        List<RadiusAttribute> attributes = Arrays.asList(
                Attributes.create(dictionary, -1, (byte) 1, user),
                Attributes.create(dictionary, -1, (byte) 2, encodedPassword));

        AccessRequestPap request = (AccessRequestPap) AccessRequest.create(dictionary, (byte) 1, authenticator, attributes);

        assertNull(request.getPlaintextPassword());
        assertEquals(user, request.getAttributeString(USER_NAME));
        assertArrayEquals(encodedPassword, request.getAttribute("User-Password").getValue());

        request.verifyRequest(sharedSecret);

        assertEquals(plaintextPw, request.getPlaintextPassword());
    }

    @Test
    void verifyDecodesPassword() throws RadiusPacketException {
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequestPap request = new AccessRequestPap(dictionary, (byte) 2, null, Collections.emptyList(), plaintextPw);
        final AccessRequestPap encoded = (AccessRequestPap) request.encodeRequest(sharedSecret);

        encoded.setPlaintextPassword("set field to something else");
        encoded.verifyRequest(sharedSecret);

        assertEquals(plaintextPw, encoded.getPlaintextPassword());
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
        assertEquals(32, pad(new byte[31]).length);
        assertEquals(32, pad(new byte[32]).length);
        assertEquals(48, pad(new byte[33]).length);
    }
}