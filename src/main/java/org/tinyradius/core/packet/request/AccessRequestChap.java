package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.RadiusPacket;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.tinyradius.core.attribute.AttributeTypes.CHAP_CHALLENGE;
import static org.tinyradius.core.attribute.AttributeTypes.CHAP_PASSWORD;

/**
 * CHAP AccessRequest RFC2865
 */
public class AccessRequestChap extends AccessRequest {

    /**
     * Constructs an AccessRequestChap.
     *
     * @param dictionary the dictionary to use
     * @param header     the packet header
     * @param attributes the packet attributes
     * @throws RadiusPacketException if there is an error creating the request
     */
    public AccessRequestChap(@NonNull Dictionary dictionary, @NonNull ByteBuf header, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    static @NonNull AccessRequest withPassword(@NonNull AccessRequest request, @NonNull String password) throws RadiusPacketException {
        var attributes = withPasswordAttribute(request.getDictionary(), request.getAttributes(), password);
        return (AccessRequest) request.withAttributes(attributes);
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
    private static @NonNull List<RadiusAttribute> withPasswordAttribute(@NonNull Dictionary dictionary, @NonNull List<RadiusAttribute> attributes, @NonNull String password) {
        if (password.isEmpty())
            throw new IllegalArgumentException("Could not encode CHAP attributes, password not set");

        byte[] challenge = random16bytes();

        var newAttributes = attributes.stream()
                .filter(a -> !(a.getVendorId() == -1 && a.getType() == CHAP_PASSWORD)
                        && !(a.getVendorId() == -1 && a.getType() == CHAP_CHALLENGE))
                .collect(toList());

        newAttributes.add(dictionary.createAttribute(-1, CHAP_CHALLENGE, challenge));
        newAttributes.add(dictionary.createAttribute(-1, CHAP_PASSWORD,
                computeChapPassword((byte) RANDOM.nextInt(256), password, challenge)));

        return newAttributes;
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
    private static byte @NonNull [] computeChapPassword(byte chapId, @NonNull String plaintextPw, byte @NonNull [] chapChallenge) {
        var md5 = RadiusPacket.getMd5Digest();
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
     * @param password plaintext password to verify the packet against
     * @return true if the password is valid, false otherwise
     */
    public boolean checkPassword(@NonNull String password) {
        if (password.isEmpty()) {
            logger.warn("Plaintext password to check against is empty");
            return false;
        }

        byte[] chapChallenge = getAttribute(CHAP_CHALLENGE)
                .map(RadiusAttribute::getValue)
                .orElse(getAuthenticator());
        if (chapChallenge == null) {
            logger.warn("CHAP-Challenge not found in attribute or in packet authenticator");
            return false;
        }


        byte[] chapPassword = getAttribute(CHAP_PASSWORD)
                .map(RadiusAttribute::getValue)
                .orElse(null);
        if (chapPassword == null || chapPassword.length != 17) {
            logger.warn("CHAP-Password must be 17 bytes");
            return false;
        }

        return Arrays.equals(chapPassword, computeChapPassword(chapPassword[0], password, chapChallenge));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateAttributes() throws RadiusPacketException {
        int count = getAttributes(CHAP_PASSWORD).size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (CHAP) should have exactly one CHAP-Password attribute, has " + count);
        // CHAP-Challenge can use Request Authenticator instead of attribute
    }
}

