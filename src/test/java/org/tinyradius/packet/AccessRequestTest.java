package org.tinyradius.packet;

import net.jradius.util.CHAP;
import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.AccessRequest.pad;
import static org.tinyradius.packet.RadiusPackets.nextPacketId;

class AccessRequestTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void authenticatorOnlyAddedIfNull() {
        String sharedSecret = "sharedSecret1";

        AccessRequest nullAuthRequest = new AccessRequest(dictionary, 2, null, "myUser", "myPw");
        assertNull(nullAuthRequest.getAuthenticator());

        assertNotNull(nullAuthRequest.encodeRequest(sharedSecret).getAuthenticator());

        AccessRequest authRequest = new AccessRequest(dictionary, 2, random16Bytes(), "myUser", "myPw");
        assertNotNull(authRequest.getAuthenticator());
        assertArrayEquals(authRequest.getAuthenticator(), authRequest.encodeRequest(sharedSecret).getAuthenticator());
    }

    @Test
    void verifyDecodesPassword() throws RadiusException {
        String user = "user1";
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequest request = new AccessRequest(dictionary, 2, null, user, plaintextPw);
        final AccessRequest encoded = request.encodeRequest(sharedSecret);

        encoded.setUserPassword("set field to something else");
        encoded.verify(sharedSecret, null);

        assertEquals(plaintextPw, encoded.getUserPassword());
    }

    @Test
    void encodePapPassword() {
        String user = "user1";
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequest request = new AccessRequest(dictionary, 2, null, user, plaintextPw);
        request.setAuthProtocol(AccessRequest.AUTH_PAP);
        final AccessRequest encoded = request.encodeRequest(sharedSecret);

        // randomly generated, need to extract
        final byte[] authenticator = encoded.getAuthenticator();
        final byte[] expectedEncodedPassword = RadiusUtils.encodePapPassword(
                request.getUserPassword().getBytes(UTF_8), authenticator, sharedSecret);

        assertNull(request.getAttribute("User-Password"));
        assertNull(request.getAttribute("CHAP-Password"));
        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getIdentifier(), encoded.getIdentifier());
        assertEquals(request.getUserName(), encoded.getUserName());

        assertNull(encoded.getAttribute("CHAP-Password"));
        assertArrayEquals(expectedEncodedPassword, encoded.getAttribute("User-Password").getValue());

        // check transient fields copied across
        assertEquals(plaintextPw, encoded.getUserPassword());
        assertEquals(user, encoded.getUserName());
    }

    @Test
    void decodePapPassword() throws RadiusException {
        String user = "user2";
        String plaintextPw = "myPassword2";
        String sharedSecret = "sharedSecret2";
        final byte[] authenticator = random16Bytes();

        byte[] encodedPassword = RadiusUtils.encodePapPassword(plaintextPw.getBytes(UTF_8), authenticator, sharedSecret);

        List<RadiusAttribute> attributes = Arrays.asList(
                createAttribute(dictionary, -1, 1, user),
                createAttribute(dictionary, -1, 2, encodedPassword));

        AccessRequest request = new AccessRequest(dictionary, nextPacketId(), authenticator, attributes);

        assertNull(request.getUserPassword());
        assertEquals(user, request.getUserName());
        assertArrayEquals(encodedPassword, request.getAttribute("User-Password").getValue());

        request.verify(sharedSecret, null);

        assertEquals(plaintextPw, request.getUserPassword());
    }

    @Test
    void encodeChapPassword() throws NoSuchAlgorithmException {
        String user = "user";
        String plaintextPw = "password123456789";
        String sharedSecret = "sharedSecret";

        AccessRequest request = new AccessRequest(dictionary, nextPacketId(), null, user, plaintextPw);
        request.setAuthProtocol(AccessRequest.AUTH_CHAP);
        final AccessRequest encoded = request.encodeRequest(sharedSecret);

        assertNull(request.getAttribute("User-Password"));
        assertNull(request.getAttribute("CHAP-Password"));
        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getIdentifier(), encoded.getIdentifier());
        assertEquals(request.getUserName(), encoded.getUserName());

        // randomly generated, need to extract
        final byte[] chapChallenge = encoded.getAttribute("CHAP-Challenge").getValue();
        final byte[] chapPassword = encoded.getAttribute("CHAP-Password").getValue();

        final byte[] expectedChapPassword = CHAP.chapResponse(chapPassword[0], plaintextPw.getBytes(UTF_8), chapChallenge);

        assertArrayEquals(expectedChapPassword, chapPassword);
        assertNull(encoded.getAttribute("User-Password"));

        // check transient fields copied across
        assertEquals(plaintextPw, encoded.getUserPassword());
        assertEquals(user, encoded.getUserName());
    }

    @Test
    void verifyChapPassword() throws NoSuchAlgorithmException, RadiusException {
        String plaintextPw = "password123456789";

        final int chapId = random.nextInt(256);
        final byte[] challenge = random16Bytes();
        final byte[] password = CHAP.chapResponse((byte) chapId, plaintextPw.getBytes(UTF_8), challenge);

        AccessRequest goodRequest = new AccessRequest(dictionary, 1, null, Arrays.asList(
                createAttribute(dictionary, -1, 60, challenge),
                createAttribute(dictionary, -1, 3, password)));
        goodRequest.verify(null, null);
        assertTrue(goodRequest.verifyPassword(plaintextPw));

        AccessRequest badChallenge = new AccessRequest(dictionary, 1, null, Arrays.asList(
                createAttribute(dictionary, -1, 60, random16Bytes()),
                createAttribute(dictionary, -1, 3, password)));
        badChallenge.verify(null, null);
        assertFalse(badChallenge.verifyPassword(plaintextPw));

        password[0] = (byte) ((chapId + 1) % 256);
        AccessRequest badPassword = new AccessRequest(dictionary, 1, null, Arrays.asList(
                createAttribute(dictionary, -1, 60, challenge),
                createAttribute(dictionary, -1, 3, password)));
        badPassword.verify(null, null);
        assertFalse(badPassword.verifyPassword(plaintextPw));
    }

    private byte[] random16Bytes() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return randomBytes;
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