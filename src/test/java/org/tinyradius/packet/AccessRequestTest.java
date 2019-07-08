package org.tinyradius.packet;

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
    private byte[] authenticator;

    @BeforeEach
    void setup() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        authenticator = randomBytes;
    }

    @Test
    void encodePapPassword() {
        String user = "user1";
        String pass = "myPassword1";
        String sharedSecret = "sharedSecret1";

        AccessRequest accessRequest = new AccessRequest(dictionary, getNextPacketIdentifier(), authenticator, user, pass);
        accessRequest.setAuthProtocol(AccessRequest.AUTH_PAP);
        final AccessRequest encodedRequest = accessRequest.encodeRequest(sharedSecret);

        final byte[] expectedEncodedPassword = RadiusUtils.encodePapPassword(
                accessRequest.getUserPassword().getBytes(UTF_8), accessRequest.getAuthenticator(), sharedSecret);

        assertArrayEquals(expectedEncodedPassword, encodedRequest.getAttribute("User-Password").getData());
    }

    @Test
    void decodePapPassword() throws RadiusException {
        String user = "user2";
        String pass = "myPassword2";
        String sharedSecret = "sharedSecret2";

        byte[] encodedPassword = RadiusUtils.encodePapPassword(pass.getBytes(UTF_8), authenticator, sharedSecret);

        List<RadiusAttribute> attributes = Arrays.asList(
                new StringAttribute(dictionary, -1, 1, user),
                new RadiusAttribute(dictionary, -1, 2, encodedPassword));

        AccessRequest accessRequest = new AccessRequest(dictionary, getNextPacketIdentifier(), authenticator, attributes);
        accessRequest.decodeAttributes(sharedSecret);

        assertEquals(pass, accessRequest.getUserPassword());
    }

    @Test
    void encodeChapPassword() {
        String user = "user";
        String pass = "password123456789";
        String sharedSecret = "sharedSecret";

        byte[] chapChallenge = new byte[16];
        random.nextBytes(chapChallenge);

        byte chapId = (byte) random.nextInt(256);
        byte[] chapPassword = new byte[17];
        chapPassword[0] = chapId;

        MessageDigest md5 = getMessageDigest();
        md5.update(chapId);
        md5.update(pass.getBytes(UTF_8));
        byte[] chapHash = md5.digest();

        System.arraycopy(chapHash, 0, chapPassword, 1, 16);

        List<RadiusAttribute> radiusAttributes = Arrays.asList(
                new StringAttribute(dictionary, -1, 1, user),
                new RadiusAttribute(dictionary, -1, 60, chapChallenge),
                new RadiusAttribute(dictionary, -1, 3, chapPassword));

        AccessRequest accessRequest = new AccessRequest(dictionary, getNextPacketIdentifier(), authenticator, user, pass);
        accessRequest.setAuthProtocol(AccessRequest.AUTH_CHAP);
        final AccessRequest encodedRequest = accessRequest.encodeRequest(sharedSecret);

        assertEquals(radiusAttributes.size(), encodedRequest.getAttributes().size());
    }

    @Test
    void verifyChapPassword() {
        String user = "user";
        String pass = "password123456789";
        String sharedSecret = "sharedSecret";

        AccessRequest accessRequest = new AccessRequest(dictionary, getNextPacketIdentifier(), authenticator, user, pass);
        accessRequest.setAuthProtocol(AccessRequest.AUTH_CHAP);
        final AccessRequest encodedRequest = accessRequest.encodeRequest(sharedSecret);

        byte[] chapChallenge = encodedRequest.getAttribute("CHAP-Challenge").getData();
        byte[] chapPassword = encodedRequest.getAttribute("CHAP-Password").getData();
        byte chapIdentifier = chapPassword[0];
        MessageDigest md5 = getMessageDigest();
        md5.update(chapIdentifier);
        md5.update(pass.getBytes(UTF_8));
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