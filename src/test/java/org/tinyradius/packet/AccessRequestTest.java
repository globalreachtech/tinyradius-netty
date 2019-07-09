package org.tinyradius.packet;

import net.jradius.util.CHAP;
import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.StringAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.RadiusPacketEncoder.getNextPacketIdentifier;

class AccessRequestTest {

    private static final SecureRandom random = new SecureRandom();
    private static Dictionary dictionary = DefaultDictionary.INSTANCE;
    private byte[] authenticator = new byte[16];

    @BeforeEach
    void setup() {
        random.nextBytes(authenticator);
    }

    @Test
    void encodePapPassword() {
        String user = "user1";
        String plaintextPw = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequest request = new AccessRequest(dictionary, getNextPacketIdentifier(), null, user, plaintextPw);
        request.setAuthProtocol(AccessRequest.AUTH_PAP);
        final AccessRequest encoded = request.encodeRequest(sharedSecret);

        final byte[] expectedEncodedPassword = RadiusUtils.encodePapPassword(
                request.getUserPassword().getBytes(UTF_8), encoded.getAuthenticator(), sharedSecret);

        assertEquals(request.getPacketType(), encoded.getPacketType());
        assertEquals(request.getPacketIdentifier(), encoded.getPacketIdentifier());
        assertEquals(request.getAttribute("User-Name").getDataString(), encoded.getAttribute("User-Name").getDataString());

        assertNull(request.getAttribute("User-Password"));
        assertArrayEquals(expectedEncodedPassword, encoded.getAttribute("User-Password").getData());
    }

    @Test
    void decodePapPassword() throws RadiusException {
        String user = "user2";
        String plaintextPw = "myPassword2";
        String sharedSecret = "sharedSecret2";

        byte[] encodedPassword = RadiusUtils.encodePapPassword(plaintextPw.getBytes(UTF_8), authenticator, sharedSecret);

        List<RadiusAttribute> attributes = Arrays.asList(
                new StringAttribute(dictionary, -1, 1, user),
                new RadiusAttribute(dictionary, -1, 2, encodedPassword));

        AccessRequest request = new AccessRequest(dictionary, getNextPacketIdentifier(), authenticator, attributes);

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

        AccessRequest request = new AccessRequest(dictionary, getNextPacketIdentifier(), authenticator, user, plaintextPw);
        request.setAuthProtocol(AccessRequest.AUTH_CHAP);
        final AccessRequest encoded = request.encodeRequest(sharedSecret);

        // randomly generated, need to extract
        final byte[] chapChallenge = encoded.getAttribute("CHAP-Challenge").getData();
        final byte[] chapPassword = encoded.getAttribute("CHAP-Password").getData();

        final byte[] expectedChapPassword = CHAP.chapResponse(chapPassword[0], plaintextPw.getBytes(UTF_8), chapChallenge);

        assertArrayEquals(expectedChapPassword, chapPassword);
    }

    @Test
    void verifyChapPassword() {
        String user = "user";
        String plaintextPw = "password123456789";
        String sharedSecret = "sharedSecret";

        AccessRequest request = new AccessRequest(dictionary, 1, authenticator, user, plaintextPw);
        request.setAuthProtocol(AccessRequest.AUTH_CHAP);
        final AccessRequest encodedRequest = request.encodeRequest(sharedSecret);

        byte[] chapChallenge = encodedRequest.getAttribute("CHAP-Challenge").getData();
        byte[] chapPassword = encodedRequest.getAttribute("CHAP-Password").getData();
        byte chapIdentifier = chapPassword[0];
        MessageDigest md5 = getMessageDigest();
        md5.update(chapIdentifier);
        md5.update(plaintextPw.getBytes(UTF_8));
        byte[] chapHash = md5.digest(chapChallenge);

        boolean isTrue = false;
        for (int i = 0; i < 16; i++) {
            if (chapHash[i] == chapPassword[i + 1]) {
                isTrue = true;
            }
        }

        assertTrue(isTrue);
    }

    private MessageDigest getMessageDigest() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md;
    }
}