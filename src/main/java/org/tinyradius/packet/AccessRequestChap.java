package org.tinyradius.packet;

import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class AccessRequestChap extends AccessRequest {

    protected static final byte CHAP_CHALLENGE = 60;

    private transient String password;

    public AccessRequestChap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes, String plaintextPw) {
        this(dictionary, identifier, authenticator, attributes);
        setPlaintextPassword(plaintextPw);
    }

    public AccessRequestChap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    /**
     * Sets the plain-text user password.
     *
     * @param userPassword user password to set
     */
    public void setPlaintextPassword(String userPassword) {
        requireNonNull(userPassword, "User password not set");
        if (userPassword.isEmpty())
            throw new IllegalArgumentException("Password is empty");
        this.password = userPassword;
    }

    /**
     * Retrieves the plain-text user password.
     *
     * @return user password in plaintext, only available if set in memory,
     * cannot be extracted from packet
     */
    public String getPlaintextPassword() {
        return password;
    }

    /**
     * Sets and encodes the CHAP-Password and CHAP-Challenge attributes.
     *
     * @param sharedSecret shared secret not used to encode
     * @param newAuth      ignored, not used for CHAP
     * @return List of RadiusAttributes to override
     */
    @Override
    protected AccessRequestChap encodeAuthMechanism(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
        if (password == null || password.isEmpty()) {
            logger.warn("Could not encode CHAP attributes, password not set");
            throw new RadiusPacketException("Could not encode CHAP attributes, password not set");
        }

        final AccessRequestChap encoded = new AccessRequestChap(getDictionary(), getId(), newAuth, getAttributes(), password);
        encoded.removeAttributes(CHAP_PASSWORD);
        encoded.removeAttributes(CHAP_CHALLENGE);

        byte[] challenge = random16bytes();

        encoded.addAttribute(Attributes.create(getDictionary(), -1, CHAP_CHALLENGE, challenge));
        encoded.addAttribute(Attributes.create(getDictionary(), -1, CHAP_PASSWORD,
                computeChapPassword((byte) RANDOM.nextInt(256), password, challenge)));

        return encoded;
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
        MessageDigest md5 = getMd5Digest();
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
    protected void verifyAuthMechanism(String sharedSecret) throws RadiusPacketException {
        final List<RadiusAttribute> attrs = getAttributes(CHAP_PASSWORD);
        if (attrs.size() != 1) {
            throw new RadiusPacketException("AccessRequest (CHAP) should have exactly one CHAP-Password attribute, has " + attrs.size());
        }
    }

    @Override
    public AccessRequestChap copy() {
        return new AccessRequestChap(getDictionary(), getId(), getAuthenticator(), getAttributes(), password);
    }
}
