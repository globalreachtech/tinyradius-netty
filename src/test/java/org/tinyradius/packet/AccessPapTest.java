package org.tinyradius.packet;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
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
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.AccessPap.pad;
import static org.tinyradius.packet.AccessRequest.USER_NAME;
import static org.tinyradius.packet.RadiusPackets.nextPacketId;

class AccessPapTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodePapPassword() throws RadiusPacketException {
        String user = "myUser1";
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessPap request = new AccessPap(dictionary, 2, null, Collections.emptyList());
        request.setAttributeString(USER_NAME, user);
        request.setPlaintextPassword(plaintextPw);
        final AccessPap encoded = (AccessPap) request.encodeRequest(sharedSecret);

        // randomly generated, need to extract
        final byte[] authenticator = encoded.getAuthenticator();
        final byte[] expectedEncodedPassword = RadiusUtils.encodePapPassword(
                request.getPlaintextPassword().getBytes(UTF_8), authenticator, sharedSecret);

        assertNull(request.getAttribute("User-Password"));
        assertNull(request.getAttribute("CHAP-Password"));
        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getIdentifier(), encoded.getIdentifier());
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
        final byte[] authenticator = random16Bytes();

        byte[] encodedPassword = RadiusUtils.encodePapPassword(plaintextPw.getBytes(UTF_8), authenticator, sharedSecret);

        List<RadiusAttribute> attributes = Arrays.asList(
                createAttribute(dictionary, -1, 1, user),
                createAttribute(dictionary, -1, 2, encodedPassword));

        AccessPap request = (AccessPap) AccessRequest.create(dictionary, nextPacketId(), authenticator, attributes);

        assertNull(request.getPlaintextPassword());
        assertEquals(user, request.getAttributeString(USER_NAME));
        assertArrayEquals(encodedPassword, request.getAttribute("User-Password").getValue());

        request.verifyResponse(sharedSecret, null);

        assertEquals(plaintextPw, request.getPlaintextPassword());
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

    private byte[] random16Bytes() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return randomBytes;
    }
}