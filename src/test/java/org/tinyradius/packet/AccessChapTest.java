package org.tinyradius.packet;

import net.jradius.util.CHAP;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.AccessRequest.USER_NAME;
import static org.tinyradius.packet.RadiusPackets.nextPacketId;

class AccessChapTest {

    private static final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;


    @Test
    void encodeChapPassword() throws NoSuchAlgorithmException, RadiusPacketException {
        String user = "user";
        String plaintextPw = "password123456789";
        String sharedSecret = "sharedSecret";

        AccessChap request = new AccessChap(dictionary, nextPacketId(), null, Collections.emptyList());
        request.setAttributeString(USER_NAME, user);
        request.setPlaintextPassword(plaintextPw);
        final AccessChap encoded = (AccessChap) request.encodeRequest(sharedSecret);

        assertNull(request.getAttribute("User-Password"));
        assertNull(request.getAttribute("CHAP-Password"));
        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getIdentifier(), encoded.getIdentifier());
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
        final byte[] challenge = random16Bytes();
        final byte[] password = CHAP.chapResponse((byte) chapId, plaintextPw.getBytes(UTF_8), challenge);

        AccessChap goodRequest = (AccessChap) AccessRequest.create(dictionary, 1, null, Arrays.asList(
                createAttribute(dictionary, -1, 60, challenge),
                createAttribute(dictionary, -1, 3, password)));
        assertTrue(goodRequest.verifyPassword(plaintextPw));

        AccessChap badChallenge = (AccessChap) AccessRequest.create(dictionary, 1, null, Arrays.asList(
                createAttribute(dictionary, -1, 60, random16Bytes()),
                createAttribute(dictionary, -1, 3, password)));
        assertFalse(badChallenge.verifyPassword(plaintextPw));

        password[0] = (byte) ((chapId + 1) % 256);
        AccessChap badPassword = (AccessChap) AccessRequest.create(dictionary, 1, null, Arrays.asList(
                createAttribute(dictionary, -1, 60, challenge),
                createAttribute(dictionary, -1, 3, password)));
        assertFalse(badPassword.verifyPassword(plaintextPw));
    }

    private byte[] random16Bytes() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return randomBytes;
    }

}