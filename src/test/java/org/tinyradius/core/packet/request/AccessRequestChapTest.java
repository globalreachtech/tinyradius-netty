package org.tinyradius.core.packet.request;

import net.jradius.util.CHAP;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.rfc.Rfc2865.USER_NAME;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.request.AccessRequest.CHAP_PASSWORD;

class AccessRequestChapTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final String username = "myUsername";

        final AccessRequestChap request = (AccessRequestChap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withChapPassword("myPw")
                        .addAttribute(dictionary.createAttribute("User-Name", username));

        final RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> request.decodeRequest(sharedSecret));
        assertTrue(e.getMessage().contains("authenticator missing"));

        final RadiusRequest encoded = request.encodeRequest(sharedSecret);
        assertNotNull(encoded.getAuthenticator());
        assertEquals(username, encoded.getAttribute("User-Name").get().getValueString());

        // idempotence check
        final RadiusRequest encoded2 = encoded.encodeRequest(sharedSecret);
        assertArrayEquals(encoded.toBytes(), encoded2.toBytes());

        final RadiusRequest decoded = encoded2.decodeRequest(sharedSecret);
        assertEquals(username, decoded.getAttribute("User-Name").get().getValueString());

        // idempotence check
        final RadiusRequest decoded2 = decoded.decodeRequest(sharedSecret);
        assertArrayEquals(decoded.toBytes(), decoded2.toBytes());
        assertEquals(username, decoded2.getAttribute("User-Name").get().getValueString());
    }

    @Test
    void verifyAttributeCount() throws RadiusPacketException {
        final byte[] auth = new byte[16];
        auth[1] = 1; // set to non-zero/null
        final String sharedSecret = "sharedSecret1";
        final AccessRequestNoAuth request1 = (AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, auth, Collections.emptyList());
        request1.decodeRequest(sharedSecret); // nothing thrown, NoAuth doesn't check anything

        // add one pw attribute
        final AccessRequestChap request2 = (AccessRequestChap) request1.withChapPassword("myPw");
        request2.decodeRequest(sharedSecret); // nothing thrown, chap password exists

        // add one more pw attribute
        final RadiusRequest request3 = request2.addAttribute(dictionary.createAttribute(-1, CHAP_PASSWORD, auth));
        final RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> request3.decodeRequest(sharedSecret));
        assertTrue(e.getMessage().contains("should have exactly one CHAP-Password"));
    }

    @Test
    void encodeChapPassword() throws NoSuchAlgorithmException, RadiusPacketException {
        final String user = "user";
        final String plaintextPw = "password123456789";
        final String sharedSecret = "sharedSecret";

        final AccessRequestNoAuth emptyRequest = (AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());

        assertFalse(emptyRequest.getAttribute("User-Password").isPresent());
        assertFalse(emptyRequest.getAttribute("CHAP-Password").isPresent());

        final AccessRequestNoAuth request = (AccessRequestNoAuth) emptyRequest.addAttribute(USER_NAME, user);

        final AccessRequestChap encoded = (AccessRequestChap) request.withChapPassword(plaintextPw).encodeRequest(sharedSecret);
        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getId(), encoded.getId());
        assertEquals(user, encoded.getAttribute(USER_NAME).get().getValueString());
        assertEquals(request.getAttribute(USER_NAME).get().getValueString(), encoded.getAttribute(USER_NAME).get().getValueString());
        assertFalse(encoded.getAttribute("User-Password").isPresent());

        // randomly generated, need to extract
        final byte[] chapChallenge = encoded.getAttribute("CHAP-Challenge").get().getValue();
        final byte[] chapPassword = encoded.getAttribute("CHAP-Password").get().getValue();
        final byte[] expectedChapPassword = CHAP.chapResponse(chapPassword[0], plaintextPw.getBytes(UTF_8), chapChallenge);

        assertArrayEquals(expectedChapPassword, chapPassword);
    }

    @Test
    void verifyChapPassword() throws NoSuchAlgorithmException, RadiusPacketException {
        final String plaintextPw = "password123456789";

        final int chapId = random.nextInt(256);
        final byte[] challenge = random.generateSeed(16);
        final byte[] password = CHAP.chapResponse((byte) chapId, plaintextPw.getBytes(UTF_8), challenge);

        AccessRequestChap goodRequest = (AccessRequestChap) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Arrays.asList(
                dictionary.createAttribute(-1, 60, challenge),
                dictionary.createAttribute(-1, 3, password)));
        assertTrue(goodRequest.checkPassword(plaintextPw));
        assertFalse(goodRequest.checkPassword("badPw"));

        AccessRequestChap badChallenge = (AccessRequestChap) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Arrays.asList(
                dictionary.createAttribute(-1, 60, random.generateSeed(16)),
                dictionary.createAttribute(-1, 3, password)));
        assertFalse(badChallenge.checkPassword(plaintextPw));

        password[0] = (byte) ((chapId + 1) % 256);
        AccessRequestChap badPassword = (AccessRequestChap) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Arrays.asList(
                dictionary.createAttribute(-1, 60, challenge),
                dictionary.createAttribute(-1, 3, password)));
        assertFalse(badPassword.checkPassword(plaintextPw));
    }
}