package org.tinyradius.packet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.StringAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.max;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tinyradius.packet.RadiusPacketEncoder.getNextPacketIdentifier;
import static org.tinyradius.packet.Util.getStringFromUtf8;

class AccessRequestPacketTest {

    private static final SecureRandom random = new SecureRandom();
    private static byte[] authenticator;

    @BeforeEach
    void setup() throws IOException {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        authenticator = randomBytes;
    }

    @Test
    void encodePapPassword() throws RadiusException {
        String user = "user";
        String pass = "password123456789";
        String sharedSecret = "sharedSecret";

        byte[] data = encodePapPassword(pass, sharedSecret);

        List<RadiusAttribute> radiusAttributes = Arrays.asList(new StringAttribute(DefaultDictionary.INSTANCE, -1, 1, user),
                new RadiusAttribute(DefaultDictionary.INSTANCE, -1, 2, data));

        AccessRequest accessRequest = new AccessRequest(DefaultDictionary.INSTANCE, getNextPacketIdentifier(), authenticator, user, pass);
        accessRequest.setAuthProtocol(AccessRequest.AUTH_PAP);
        accessRequest.encodeRequest(sharedSecret);
        String userPassword = accessRequest.getAttribute("User-Password").getDataString();

        assertEquals(radiusAttributes.get(1).getDataString(), userPassword);
    }

    @Test
    void decodePapPassword() throws RadiusException {
        String user = "user";
        String pass = "password123456789";
        String sharedSecret = "sharedSecret";

        byte[] encryptedPass = encodePapPassword(pass, sharedSecret);
        byte[] result = new byte[encryptedPass.length];
        byte[] C = authenticator;

        for (int i = 0; i < encryptedPass.length; i += C.length) {
            C = compute(sharedSecret.getBytes(), C);
            C = xor(encryptedPass, i, C.length, C, C.length);
            System.arraycopy(C, 0, result, i, C.length);
            System.arraycopy(encryptedPass, i, C, 0, C.length);
        }

        String decodedPassword = getStringFromUtf8(result);

        AccessRequest accessRequest = new AccessRequest(DefaultDictionary.INSTANCE, getNextPacketIdentifier(), authenticator, user, pass);
        accessRequest.setAuthProtocol(AccessRequest.AUTH_PAP);
        accessRequest.encodeRequest(sharedSecret);
        accessRequest.decodeAttributes(sharedSecret);

        assertEquals(decodedPassword, accessRequest.getUserPassword());
    }

    @Test
    void encodeChapPassword() throws RadiusException {
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

        List<RadiusAttribute> radiusAttributes = Arrays.asList(new StringAttribute(DefaultDictionary.INSTANCE, -1, 1, user),
                new RadiusAttribute(DefaultDictionary.INSTANCE, -1, 60, chapChallenge),
                new RadiusAttribute(DefaultDictionary.INSTANCE, -1, 3, chapPassword));

        AccessRequest accessRequest = new AccessRequest(DefaultDictionary.INSTANCE, getNextPacketIdentifier(), authenticator, user, pass);
        accessRequest.setAuthProtocol(AccessRequest.AUTH_CHAP);
        accessRequest.encodeRequest(sharedSecret);

        assertEquals(radiusAttributes.size(), accessRequest.getAttributes().size());
    }

    @Test
    void verifyChapPassword() throws RadiusException {
        String user = "user";
        String pass = "password123456789";
        String sharedSecret = "sharedSecret";

        AccessRequest accessRequest = new AccessRequest(DefaultDictionary.INSTANCE, getNextPacketIdentifier(), authenticator, user, pass);
        accessRequest.setAuthProtocol(AccessRequest.AUTH_CHAP);
        accessRequest.encodeRequest(sharedSecret);

        byte[] chapChallenge = accessRequest.getAttribute("CHAP-Challenge").getData();
        byte[] chapPassword = accessRequest.getAttribute("CHAP-Password").getData();
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

    private byte[] encodePapPassword(String pass, String sharedSecret) {
        byte[] C = authenticator;
        byte[] padded = getPadded(pass.getBytes());
        byte[] data = new byte[padded.length];

        for (int i = 0; i < padded.length; i += C.length) {
            C = compute(sharedSecret.getBytes(), C);
            C = xor(padded, i, C.length, C, C.length);
            System.arraycopy(C, 0, data, i, C.length);
        }
        return data;
    }

    private byte[] getPadded(byte[] pass) {
        int length = Math.max((int) (Math.ceil((double) pass.length / authenticator.length) * authenticator.length), authenticator.length);
        byte[] padded = new byte[length];
        System.arraycopy(pass, 0, padded, 0, pass.length);
        return padded;
    }

    private byte[] compute(byte[]... values) {
        MessageDigest md = getMessageDigest();
        assert md != null;

        for (byte[] b : values)
            md.update(b);

        return md.digest();
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


    private byte[] xor(byte[] src1, int src1offset, int src1length,
                       byte[] src2, int src2length) {
        byte[] dst = new byte[max(max(src1length, src2length), 0)];

        int length = Math.min(src1length, src2length);

        for (int i = 0; i < length; i++) {
            dst[i] = (byte) (src1[i + src1offset] ^ src2[i]);
        }

        return dst;
    }

}