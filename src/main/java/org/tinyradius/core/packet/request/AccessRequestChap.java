package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.RadiusPacket;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tinyradius.core.attribute.AttributeTypes.CHAP_CHALLENGE;
import static org.tinyradius.core.attribute.AttributeTypes.CHAP_PASSWORD;

/**
 * CHAP AccessRequest RFC2865
 */
public class AccessRequestChap extends AccessRequest {

    public AccessRequestChap(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    static AccessRequest withPassword(AccessRequest request, String password) throws RadiusPacketException {
        final List<RadiusAttribute> attributes = withPasswordAttribute(request.getDictionary(), request.getAttributes(), password);
        final ByteBuf header = RadiusPacket.buildHeader(request.getType(), request.getId(), request.getAuthenticator(), attributes);
        return create(request.getDictionary(), header, attributes);
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
    private static List<RadiusAttribute> withPasswordAttribute(Dictionary dictionary, List<RadiusAttribute> attributes, String password) {
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("Could not encode CHAP attributes, password not set");

        final byte[] challenge = random16bytes();

        final List<RadiusAttribute> newAttributes = attributes.stream()
                .filter(a -> !(a.getVendorId() == -1 && a.getType() == CHAP_PASSWORD)
                        && !(a.getVendorId() == -1 && a.getType() == CHAP_CHALLENGE))
                .collect(Collectors.toList());

        newAttributes.add(dictionary.createAttribute(-1, CHAP_CHALLENGE, challenge));
        newAttributes.add(dictionary.createAttribute(-1, CHAP_PASSWORD,
                computeChapPassword((byte) RANDOM.nextInt(256), password, challenge)));

        return newAttributes;
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
    private static byte[] computeChapPassword(byte chapId, String plaintextPw, byte[] chapChallenge) {
        final MessageDigest md5 = RadiusPacket.getMd5Digest();
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
        final int count = getAttributes(CHAP_PASSWORD).size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (CHAP) should have exactly one CHAP-Password attribute, has " + count);
        // CHAP-Challenge can use Request Authenticator instead of attribute
    }
}
