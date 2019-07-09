package org.tinyradius.packet;

import net.jradius.util.CHAP;
import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.StringAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.RadiusPacketEncoder.nextPacketId;

class AccessRequestTest {

    private static final SecureRandom random = new SecureRandom();
    private static Dictionary dictionary = DefaultDictionary.INSTANCE;

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
        assertEquals(request.getPacketType(), encoded.getPacketType());
        assertEquals(request.getPacketIdentifier(), encoded.getPacketIdentifier());
        assertEquals(request.getAttribute("User-Name").getDataString(), encoded.getAttribute("User-Name").getDataString());

        assertNull(encoded.getAttribute("CHAP-Password"));
        assertArrayEquals(expectedEncodedPassword, encoded.getAttribute("User-Password").getData());
    }

    @Test
    void decodePapPassword() throws RadiusException {
        String user = "user2";
        String plaintextPw = "myPassword2";
        String sharedSecret = "sharedSecret2";
        final byte[] authenticator = random16Bytes();

        byte[] encodedPassword = RadiusUtils.encodePapPassword(plaintextPw.getBytes(UTF_8), authenticator, sharedSecret);

        List<RadiusAttribute> attributes = Arrays.asList(
                new StringAttribute(dictionary, -1, 1, user),
                new RadiusAttribute(dictionary, -1, 2, encodedPassword));

        AccessRequest request = new AccessRequest(dictionary, nextPacketId(), authenticator, attributes);

        assertNull(request.getUserPassword());
        assertEquals(user, request.getAttribute("User-Name").getDataString());
        assertArrayEquals(encodedPassword, request.getAttribute("User-Password").getData());

        request.decodeAttributes(sharedSecret);

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
        assertEquals(request.getPacketType(), encoded.getPacketType());
        assertEquals(request.getPacketIdentifier(), encoded.getPacketIdentifier());
        assertEquals(request.getAttribute("User-Name").getDataString(), encoded.getAttribute("User-Name").getDataString());

        // randomly generated, need to extract
        final byte[] chapChallenge = encoded.getAttribute("CHAP-Challenge").getData();
        final byte[] chapPassword = encoded.getAttribute("CHAP-Password").getData();

        final byte[] expectedChapPassword = CHAP.chapResponse(chapPassword[0], plaintextPw.getBytes(UTF_8), chapChallenge);

        assertArrayEquals(expectedChapPassword, chapPassword);
        assertNull(encoded.getAttribute("User-Password"));
    }

    @Test
    void verifyChapPassword() throws NoSuchAlgorithmException, RadiusException {
        String plaintextPw = "password123456789";

        final int chapId = random.nextInt(256);
        final byte[] challenge = random16Bytes();
        final byte[] password = CHAP.chapResponse((byte) chapId, plaintextPw.getBytes(UTF_8), challenge);

        AccessRequest goodRequest = new AccessRequest(dictionary, 1, null, Arrays.asList(
                new RadiusAttribute(dictionary, -1, 60, challenge),
                new RadiusAttribute(dictionary, -1, 3, password)));
        goodRequest.decodeAttributes(null);
        assertTrue(goodRequest.verifyPassword(plaintextPw));

        AccessRequest badChallenge = new AccessRequest(dictionary, 1, null, Arrays.asList(
                new RadiusAttribute(dictionary, -1, 60, random16Bytes()),
                new RadiusAttribute(dictionary, -1, 3, password)));
        badChallenge.decodeAttributes(null);
        assertFalse(badChallenge.verifyPassword(plaintextPw));

        password[0] = (byte) ((chapId + 1) % 256);
        AccessRequest badPassword = new AccessRequest(dictionary, 1, null, Arrays.asList(
                new RadiusAttribute(dictionary, -1, 60, challenge),
                new RadiusAttribute(dictionary, -1, 3, password)));
        badPassword.decodeAttributes(null);
        assertFalse(badPassword.verifyPassword(plaintextPw));
    }

    private byte[] random16Bytes() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return randomBytes;
    }
}