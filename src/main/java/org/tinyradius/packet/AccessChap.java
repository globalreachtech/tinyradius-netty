package org.tinyradius.packet;

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
import static org.tinyradius.attribute.Attributes.createAttribute;

public class AccessChap extends AccessRequest {

    protected static final int CHAP_CHALLENGE = 60;

    private transient String password;

    public AccessChap(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes, String plaintextPw) {
        this(dictionary, identifier, authenticator, attributes);
        setPlaintextPassword(plaintextPw);
    }

    public AccessChap(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
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
    public String getUserPassword() {
        return password;
    }

    /**
     * Sets and encrypts the User-Password attribute.
     *
     * @param sharedSecret shared secret not used to encode
     * @param newAuth      ignored, not used for CHAP
     * @return List of RadiusAttributes to override
     */
    @Override
    protected AccessChap encodeRequest(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
        if (password == null || password.isEmpty()) {
            logger.warn("Could not encode CHAP attributes, password not set");
            throw new RadiusPacketException("Could not encode CHAP attributes, password not set");
        }

        final AccessChap accessChap = new AccessChap(getDictionary(), getIdentifier(), newAuth, new ArrayList<>(getAttributes()), password);
        accessChap.removeAttributes(CHAP_PASSWORD);
        accessChap.removeAttributes(CHAP_CHALLENGE);

        byte[] challenge = random16bytes();

        accessChap.addAttribute(createAttribute(getDictionary(), -1, CHAP_CHALLENGE, challenge));
        accessChap.addAttribute(createAttribute(getDictionary(), -1, CHAP_PASSWORD,
                computeChapPassword((byte) RANDOM.nextInt(256), password, challenge)));

        return accessChap;
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
     * Verifies that the passed plain-text password matches the password
     * (hash) send with this Access-Request packet.
     *
     * @param plaintext password to verify packet against
     * @return true if the password is valid, false otherwise
     */
    public boolean verifyPassword(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            logger.warn("Plaintext password to check against is empty");
            return false;
        }

        final RadiusAttribute chapChallengeAttr = getAttribute(CHAP_CHALLENGE);
        final byte[] chapChallenge = chapChallengeAttr != null ?
                chapChallengeAttr.getValue() : getAuthenticator();

        if (chapChallenge == null) {
            logger.warn("CHAP challenge is null");
            return false;
        }

        final byte[] chapPassword = getAttribute(CHAP_PASSWORD).getValue();
        if (chapPassword == null || chapPassword.length != 17) {
            logger.warn("CHAP-Password must be 17 bytes");
            return false;
        }

        return Arrays.equals(chapPassword, computeChapPassword(chapPassword[0], plaintext, chapChallenge));
    }

    /**
     * AccessRequest (CHAP) cannot verify authenticator as they
     * contain random bytes.
     *
     * @param sharedSecret ignored, not applicable for CHAP
     * @param requestAuth  ignored, not applicable for AccessRequest
     */
    @Override
    public void verify(String sharedSecret, byte[] requestAuth) {
    }

    @Override
    public AccessChap copy() {
        return new AccessChap(getDictionary(), getIdentifier(), getAuthenticator(), new ArrayList<>(getAttributes()), password);
    }
}
