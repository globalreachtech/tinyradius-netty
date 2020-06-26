package org.tinyradius.packet.request;

import net.jradius.util.CHAP;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.request.AccessRequest.CHAP_PASSWORD;

class AccessRequestChapTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private static final byte USER_NAME = 1;

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final String username = "myUsername";

        final RadiusRequest accessRequestChap = new AccessRequestChap(dictionary, (byte) 1, null, Collections.emptyList())
                .withPassword("myPw")
                .addAttribute(dictionary.createAttribute("User-Name", username));

        final RadiusRequest encoded = accessRequestChap.encodeRequest(sharedSecret);
        assertNotNull(encoded.getAuthenticator());

        final RadiusRequest decodeRequest = encoded.decodeRequest(sharedSecret);
        final RadiusAttribute attribute = decodeRequest.getAttribute("User-Name").get();
        assertEquals(username, attribute.getValueString());
    }

    @Test
    void verifyAttributeCount() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final AccessRequestChap request1 = new AccessRequestChap(dictionary, (byte) 1, new byte[16], Collections.emptyList());
        assertThrows(RadiusPacketException.class, () -> request1.decodeRequest(sharedSecret));

        // add one pw attribute
        final AccessRequestChap request2 = request1.withPassword("myPw");
        request2.decodeRequest(sharedSecret);

        // add one more pw attribute
        final RadiusRequest request3 = request2.addAttribute(dictionary.createAttribute(-1, CHAP_PASSWORD, new byte[16]));
        final RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> request3.decodeRequest(sharedSecret));
        assertTrue(e.getMessage().contains("should have exactly one CHAP-Password"));
    }

    @Test
    void encodeChapPassword() throws NoSuchAlgorithmException, RadiusPacketException {
        final String user = "user";
        final String plaintextPw = "password123456789";
        final String sharedSecret = "sharedSecret";

        final AccessRequestChap emptyRequest = new AccessRequestChap(dictionary, (byte) 1, null, Collections.emptyList());

        assertFalse(emptyRequest.getAttribute("User-Password").isPresent());
        assertFalse(emptyRequest.getAttribute("CHAP-Password").isPresent());

        final AccessRequestChap request = (AccessRequestChap) emptyRequest.addAttribute(USER_NAME, user);

        final RadiusRequest encoded = request.withPassword(plaintextPw).encodeRequest(sharedSecret);
        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getId(), encoded.getId());
        assertEquals(user, encoded.getAttribute(USER_NAME).get().getValueString());
        assertEquals(request.getAttribute(USER_NAME), encoded.getAttribute(USER_NAME));
        assertFalse(encoded.getAttribute("User-Password").isPresent());

        // randomly generated, need to extract
        final byte[] chapChallenge = encoded.getAttribute("CHAP-Challenge").get().getValue();
        final byte[] chapPassword = encoded.getAttribute("CHAP-Password").get().getValue();
        final byte[] expectedChapPassword = CHAP.chapResponse(chapPassword[0], plaintextPw.getBytes(UTF_8), chapChallenge);

        assertArrayEquals(expectedChapPassword, chapPassword);
    }

    @Test
    void verifyChapPassword() throws NoSuchAlgorithmException {
        final String plaintextPw = "password123456789";

        final int chapId = random.nextInt(256);
        final byte[] challenge = random.generateSeed(16);
        final byte[] password = CHAP.chapResponse((byte) chapId, plaintextPw.getBytes(UTF_8), challenge);

        AccessRequestChap goodRequest = (AccessRequestChap) AccessRequest.create(dictionary, (byte) 1, null, Arrays.asList(
                dictionary.createAttribute(-1, (byte) 60, challenge),
                dictionary.createAttribute(-1, (byte) 3, password)));
        assertTrue(goodRequest.checkPassword(plaintextPw));
        assertFalse(goodRequest.checkPassword("badPw"));

        AccessRequestChap badChallenge = (AccessRequestChap) AccessRequest.create(dictionary, (byte) 1, null, Arrays.asList(
                dictionary.createAttribute(-1, (byte) 60, random.generateSeed(16)),
                dictionary.createAttribute(-1, (byte) 3, password)));
        assertFalse(badChallenge.checkPassword(plaintextPw));

        password[0] = (byte) ((chapId + 1) % 256);
        AccessRequestChap badPassword = (AccessRequestChap) AccessRequest.create(dictionary, (byte) 1, null, Arrays.asList(
                dictionary.createAttribute(-1, (byte) 60, challenge),
                dictionary.createAttribute(-1, (byte) 3, password)));
        assertFalse(badPassword.checkPassword(plaintextPw));
    }
}