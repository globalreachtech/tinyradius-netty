package org.tinyradius.core.packet.request;

import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.RadiusPacket;
import org.tinyradius.core.RadiusPacketException;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * CHAP AccessRequest RFC2865
 */
public class AccessRequestChap extends AccessRequest<AccessRequestChap> {

    private static final byte CHAP_CHALLENGE = 60;

    public AccessRequestChap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    protected AccessRequestFactory<AccessRequestChap> factory() {
        return AccessRequestChap::new;
    }

    /**
     * Set CHAP-Password / CHAP-Challenge attributes with provided password.
     * <p>
     * Will remove existing attributes if exists already
     *
     * @param password plaintext password to encode into CHAP-Password
     * @return AccessRequestChap with encoded CHAP-Password and CHAP-Challenge attributes
     * @throws IllegalArgumentException invalid password
     */
    public AccessRequestChap withPassword(String password) {
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("Could not encode CHAP attributes, password not set");

        byte[] challenge = random16bytes();

        final List<RadiusAttribute> attributes = getAttributes().stream()
                .filter(a -> a.getType() != CHAP_PASSWORD && a.getType() != CHAP_CHALLENGE)
                .collect(Collectors.toList());

        attributes.add(getDictionary().createAttribute(-1, CHAP_CHALLENGE, challenge));
        attributes.add(getDictionary().createAttribute(-1, CHAP_PASSWORD,
                computeChapPassword((byte) RANDOM.nextInt(256), password, challenge)));

        return withAttributes(attributes);
    }

    @Override
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        validateChapAttributes();
        return super.encodeRequest(sharedSecret);
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        validateChapAttributes();
        return super.decodeRequest(sharedSecret);
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
     * @param password plaintext password to verify packet against
     * @return true if the password is valid, false otherwise
     */
    public boolean checkPassword(String password) {
        if (password == null || password.isEmpty()) {
            logger.warn("Plaintext password to check against is empty");
            return false;
        }

        final byte[] chapChallenge = getAttribute(CHAP_CHALLENGE)
                .map(RadiusAttribute::getValue)
                .orElse(getAuthenticator());

        final byte[] chapPassword = getAttribute(CHAP_PASSWORD)
                .map(RadiusAttribute::getValue)
                .orElse(null);
        if (chapPassword == null || chapPassword.length != 17) {
            logger.warn("CHAP-Password must be 17 bytes");
            return false;
        }

        return Arrays.equals(chapPassword, computeChapPassword(chapPassword[0], password, chapChallenge));
    }

    private void validateChapAttributes() throws RadiusPacketException {
        final int count = filterAttributes(CHAP_PASSWORD).size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (CHAP) should have exactly one CHAP-Password attribute, has " + count);
        // CHAP-Challenge can use Request Authenticator instead of attribute
    }
}
