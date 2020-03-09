package org.tinyradius.packet;

import net.jradius.util.CHAP;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.util.Attributes.create;
import static org.tinyradius.packet.AccessRequest.CHAP_PASSWORD;
import static org.tinyradius.packet.AccessRequest.USER_NAME;

class AccessRequestChapTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeVerify() throws RadiusPacketException {
        final String plaintextPw = "myPassword";
        String sharedSecret = "sharedSecret1";
        final AccessRequestChap accessRequestChap = new AccessRequestChap(dictionary, (byte) 1, null, Collections.emptyList());
        accessRequestChap.setPlaintextPassword(plaintextPw);

        final AccessRequest encoded = accessRequestChap.encodeRequest(sharedSecret);
        encoded.verifyRequest(sharedSecret);
    }

    @Test
    void verifyAttributeCount() throws RadiusPacketException {
        String sharedSecret = "sharedSecret1";
        final AccessRequestChap request = new AccessRequestChap(dictionary, (byte) 1, new byte[16], Collections.emptyList());
        assertThrows(RadiusPacketException.class, () -> request.verifyRequest(sharedSecret));

        request.addAttribute(Attributes.create(dictionary, -1, CHAP_PASSWORD, new byte[16]));
        request.verifyRequest(sharedSecret); // should have exactly one instance

        request.addAttribute(Attributes.create(dictionary, -1, CHAP_PASSWORD, new byte[16]));
        assertThrows(RadiusPacketException.class, () -> request.verifyRequest(sharedSecret));
    }

    @Test
    void encodeChapPassword() throws NoSuchAlgorithmException, RadiusPacketException {
        String user = "user";
        String plaintextPw = "password123456789";
        String sharedSecret = "sharedSecret";

        AccessRequestChap request = new AccessRequestChap(dictionary, (byte) 1, null, Collections.emptyList());
        request.addAttribute(USER_NAME, user);
        request.setPlaintextPassword(plaintextPw);
        final AccessRequestChap encoded = (AccessRequestChap) request.encodeRequest(sharedSecret);

        assertNull(request.getAttribute("User-Password"));
        assertNull(request.getAttribute("CHAP-Password"));
        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getId(), encoded.getId());
        assertEquals(request.getAttributeString(USER_NAME), encoded.getAttributeString(USER_NAME));

        // randomly generated, need to extract
        final byte[] chapChallenge = encoded.getAttribute("CHAP-Challenge").getValue();
        final byte[] chapPassword = encoded.getAttribute("CHAP-Password").getValue();

        final byte[] expectedChapPassword = CHAP.chapResponse(chapPassword[0], plaintextPw.getBytes(UTF_8), chapChallenge);

        assertArrayEquals(expectedChapPassword, chapPassword);
        assertNull(encoded.getAttribute("User-Password"));

        // check transient fields copied across
        assertEquals(plaintextPw, encoded.getPlaintextPassword());
        assertEquals(user, encoded.getAttributeString(USER_NAME));
    }

    @Test
    void verifyChapPassword() throws NoSuchAlgorithmException {
        String plaintextPw = "password123456789";

        final int chapId = random.nextInt(256);
        final byte[] challenge = random.generateSeed(16);
        final byte[] password = CHAP.chapResponse((byte) chapId, plaintextPw.getBytes(UTF_8), challenge);

        AccessRequestChap goodRequest = (AccessRequestChap) AccessRequest.create(dictionary, (byte) 1, null, Arrays.asList(
                create(dictionary, -1, (byte) 60, challenge),
                create(dictionary, -1, (byte) 3, password)));
        assertTrue(goodRequest.checkPassword(plaintextPw));
        assertFalse(goodRequest.checkPassword("badPw"));

        AccessRequestChap badChallenge = (AccessRequestChap) AccessRequest.create(dictionary, (byte) 1, null, Arrays.asList(
                create(dictionary, -1, (byte) 60, random.generateSeed(16)),
                create(dictionary, -1, (byte) 3, password)));
        assertFalse(badChallenge.checkPassword(plaintextPw));

        password[0] = (byte) ((chapId + 1) % 256);
        AccessRequestChap badPassword = (AccessRequestChap) AccessRequest.create(dictionary, (byte) 1, null, Arrays.asList(
                create(dictionary, -1, (byte) 60, challenge),
                create(dictionary, -1, (byte) 3, password)));
        assertFalse(badPassword.checkPassword(plaintextPw));
    }
}