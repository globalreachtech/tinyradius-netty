package org.tinyradius.packet.request;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tinyradius.packet.util.PacketType.ACCESS_REQUEST;

public class AccessRequestChap extends BaseRadiusPacket<AccessRequestChap> implements AccessRequest<AccessRequestChap> {

    protected static final byte CHAP_CHALLENGE = 60;

    public AccessRequestChap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, ACCESS_REQUEST, identifier, authenticator, attributes);
    }

    /**
     * Set CHAP-Password / CHAP-Challenge attributes with provided password.
     *
     * Will remove existing attributes if exists already
     *
     * @param plaintext password to encode into CHAP-Password
     * @return AccessRequestChap with encoded CHAP-Password and CHAP-Challenge attributes
     * @throws IllegalArgumentException invalid password
     */
    public AccessRequestChap withPassword(String plaintext) throws IllegalArgumentException {
        if (plaintext == null || plaintext.isEmpty())
            throw new IllegalArgumentException("Could not encode CHAP attributes, password not set");

        byte[] challenge = AccessRequest.random16bytes();

        return this
                .removeAttributes(CHAP_PASSWORD)
                .removeAttributes(CHAP_CHALLENGE)
                .addAttribute(Attributes.create(getDictionary(), -1, CHAP_CHALLENGE, challenge))
                .addAttribute(Attributes.create(getDictionary(), -1, CHAP_PASSWORD,
                        computeChapPassword((byte) RANDOM.nextInt(256), plaintext, challenge)));
    }

    /**
     * Sets and encodes the CHAP-Password and CHAP-Challenge attributes.
     *
     * @param sharedSecret shared secret not used to encode
     * @param newAuth      ignored, not used for CHAP
     * @return List of RadiusAttributes to override
     */
    @Override
    public AccessRequestChap encodeAuthMechanism(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
        validateChapAttributes();
        return new AccessRequestChap(getDictionary(), getId(), newAuth, getAttributes());
    }

    /**
     * Encodes a plain-text password using the given CHAP challenge.
     * See RFC 2865 section 2.2
     *
     * @param chapId        CHAP ID associated with request
     * @param plaintextPw   plain-text password
     * @param chapChallenge random 16 octet CHAP challenge
     * @return 17 octet CHAP-encoded password (1 octet for CHAP ID, 16 octets CHAP response)
     */
    private byte[] computeChapPassword(byte chapId, String plaintextPw, byte[] chapChallenge) {
        MessageDigest md5 = RadiusPacket.getMd5Digest();
        md5.update(chapId);
        md5.update(plaintextPw.getBytes(UTF_8));
        md5.update(chapChallenge);

        return ByteBuffer.allocate(17)
                .put(chapId)
                .put(md5.digest())
                .array();
    }

    /**
     * Checks that the passed plain-text password matches the password
     * (hash) send with this Access-Request packet.
     *
     * @param plaintext password to verify packet against
     * @return true if the password is valid, false otherwise
     */
    public boolean checkPassword(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            logger.warn("Plaintext password to check against is empty");
            return false;
        }

        final RadiusAttribute chapChallengeAttr = getAttribute(CHAP_CHALLENGE);
        final byte[] chapChallenge = chapChallengeAttr != null ?
                chapChallengeAttr.getValue() : getAuthenticator();

        final byte[] chapPassword = getAttribute(CHAP_PASSWORD).getValue();
        if (chapPassword == null || chapPassword.length != 17) {
            logger.warn("CHAP-Password must be 17 bytes");
            return false;
        }

        return Arrays.equals(chapPassword, computeChapPassword(chapPassword[0], plaintext, chapChallenge));
    }

    @Override
    public AccessRequestChap verifyAuthMechanism(String sharedSecret) throws RadiusPacketException {
        validateChapAttributes();
        return this;
    }

    @Override
    public AccessRequestChap withAttributes(List<RadiusAttribute> attributes) {
        return new AccessRequestChap(getDictionary(), getId(), getAuthenticator(), attributes);
    }

    private void validateChapAttributes() throws RadiusPacketException {
        final int passwordCount = getAttributes(CHAP_PASSWORD).size();
        if (passwordCount != 1)
            throw new RadiusPacketException("AccessRequest (CHAP) should have exactly one CHAP-Password attribute, has " + passwordCount);
        // CHAP-Challenge can use Request Authenticator instead of attribute
    }
}
