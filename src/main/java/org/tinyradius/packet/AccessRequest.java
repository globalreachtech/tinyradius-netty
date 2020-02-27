package org.tinyradius.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.attribute.AttributeHolder;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;

/**
 * This class represents an Access-Request Radius packet.
 */
public abstract class AccessRequest extends RadiusPacket {

    protected static final Logger logger = LogManager.getLogger();

    private static final String HMAC_MD5 = "HmacMD5";
    protected static final SecureRandom RANDOM = new SecureRandom();

    public static final int USER_PASSWORD = 2;
    public static final int CHAP_PASSWORD = 3;
    public static final int EAP_MESSAGE = 79;
    protected static final List<Integer> AUTH_ATTRS = Arrays.asList(USER_PASSWORD, CHAP_PASSWORD, EAP_MESSAGE);

    protected static final int USER_NAME = 1;
    protected static final int MESSAGE_AUTHENTICATOR = 80;

    /**
     * Create copy of AccessRequest with new authenticator and encoded attributes
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @param newAuth      authenticator to use to encode PAP password,
     *                     nullable if using different auth protocol
     * @return RadiusPacket with new authenticator and encoded attributes
     */
    protected abstract AccessRequest encodeRequest(String sharedSecret, byte[] newAuth) throws RadiusPacketException;

    /**
     * AccessRequest overrides this method to generate a randomized authenticator as per RFC 2865
     * and encode required attributes (i.e. User-Password).
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @return RadiusPacket with new authenticator and encoded attributes
     */
    @Override
    public AccessRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("shared secret cannot be null/empty");

        // create authenticator only if needed - maintain idempotence
        byte[] newAuth = getAuthenticator() == null ? random16bytes() : getAuthenticator();

        return encodeRequest(sharedSecret, newAuth);
        // todo add Message-Authenticator
    }

    /**
     * AccessRequest cannot verify authenticator as they
     * contain random bytes.
     * <p>
     * Instead it checks the User-Password/Challenge attributes
     * are present and attempts decryption.
     *
     * @param sharedSecret shared secret, only applicable for PAP
     */
    protected abstract void verify(String sharedSecret) throws RadiusPacketException;

    /**
     * AccessRequest cannot verify authenticator as they
     * contain random bytes.
     * <p>
     * Instead it checks the User-Password/Challenge attributes
     * are present and attempts decryption.
     *
     * @param sharedSecret shared secret, only applicable for PAP
     * @param requestAuth  ignored, not applicable for AccessRequest
     */
    @Override
    public void verify(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final List<RadiusAttribute> msgAuthAttr = getAttributes(MESSAGE_AUTHENTICATOR);
        if (msgAuthAttr.size() > 1)
            throw new RadiusPacketException("AccessRequest should have at most one Message-Authenticator attribute, has " + msgAuthAttr.size());

        if (msgAuthAttr.size() == 1) {
            final byte[] messageAuth = msgAuthAttr.get(0).getValue();
            // todo tests

            if (!Arrays.equals(messageAuth, calcMessageAuth(sharedSecret)))
                throw new RadiusPacketException("AccessRequest Message-Authenticator check failed");
        }
        // todo
        /*
         * For Access-Challenge, Access-Accept, and Access-Reject packets,
         * the Message-Authenticator is calculated as follows, using the
         * Request-Authenticator from the Access-Request this packet is in
         * reply to:
         *
         * Message-Authenticator = HMAC-MD5 (Type, Identifier, Length,
         * Request Authenticator, Attributes)
         */

        verify(sharedSecret);
    }

    private Mac getHmacMd5(String key) {
        try {
            final SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_MD5);
            final Mac mac = Mac.getInstance(HMAC_MD5);
            mac.init(secretKeySpec);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e); // never happen
        }
    }

    private byte[] calcMessageAuth(String sharedSecret) {
        final Mac mac = getHmacMd5(sharedSecret);

        final ByteBuf buf = Unpooled.buffer()
                .writeByte(getType())
                .writeByte(getIdentifier())
                .writeShort(0) // placeholder
                .writeBytes(getAuthenticator());

        for (RadiusAttribute attribute : getAttributes()) {
            if (attribute.getVendorId() == -1 && attribute.getType() == MESSAGE_AUTHENTICATOR)
                buf.writeBytes(new byte[18]);
            else
                buf.writeBytes(attribute.toByteArray());
        }

        buf.setShort(2, buf.readableBytes());
        return mac.doFinal(buf.array());
    }

    /**
     * @param dictionary    custom dictionary to use
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes for packet
     */
    protected AccessRequest(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, ACCESS_REQUEST, identifier, authenticator, attributes);
    }

    /**
     * Create new AccessRequest, tries to identify auth protocol from attributes.
     *
     * @param dictionary    custom dictionary to use
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes for packet, should not be empty
     *                      or a stub AccessRequest will be returned
     */
    public static AccessRequest create(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        return lookupAuthType(attributes).newInstance(dictionary, identifier, authenticator, attributes);
    }

    private static AccessRequestConstructor lookupAuthType(int authAttribute) {
        switch (authAttribute) {
            case EAP_MESSAGE:
                return AccessEap::new;
            case CHAP_PASSWORD:
                return AccessChap::new;
            case USER_PASSWORD:
                return AccessPap::new;
            default:
                return AccessInvalidAuth::new;
        }
    }

    private static AccessRequestConstructor lookupAuthType(List<RadiusAttribute> attributes) {
        /*
         * An Access-Request that contains either a User-Password or
         * CHAP-Password or ARAP-Password or one or more EAP-Message attributes
         * MUST NOT contain more than one type of those four attributes.
         */
        final Set<Integer> detectedAuth = AUTH_ATTRS.stream()
                .map(authAttr -> AttributeHolder.getAttributes(attributes, authAttr))
                .filter(a -> !a.isEmpty())
                .map(a -> a.get(0).getType())
                .collect(Collectors.toSet());

        if (detectedAuth.size() == 0) {
            logger.warn("AccessRequest could not identify auth protocol");
            return AccessInvalidAuth::new;
        }

        if (detectedAuth.size() > 1) {
            logger.warn("AccessRequest identified multiple possible auth protocols");
            return AccessInvalidAuth::new;
        }

        return lookupAuthType(detectedAuth.iterator().next());
    }

    /**
     * Retrieves the user name from the User-Name attribute.
     *
     * @return user name
     */
    public String getUserName() {
        final RadiusAttribute attribute = getAttribute(USER_NAME);
        return attribute == null ?
                null : attribute.getValueString();
    }

    /**
     * Sets the User-Name attribute of this Access-Request.
     *
     * @param userName user name to set
     */
    public void setUserName(String userName) {
        requireNonNull(userName, "User name not set");
        if (userName.isEmpty())
            throw new IllegalArgumentException("Empty user name not allowed");

        removeAttributes(USER_NAME);
        addAttribute(createAttribute(getDictionary(), -1, USER_NAME, userName));
    }

    @Override
    public RadiusPacket encodeResponse(String sharedSecret, byte[] requestAuthenticator) {
        throw new UnsupportedOperationException();
    }

    protected byte[] random16bytes() {
        byte[] randomBytes = new byte[16];
        RANDOM.nextBytes(randomBytes);
        return randomBytes;
    }

    interface AccessRequestConstructor {
        AccessRequest newInstance(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes);
    }

    static class AccessInvalidAuth extends AccessRequest {

        public AccessInvalidAuth(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
            super(dictionary, identifier, authenticator, attributes);
        }

        @Override
        protected AccessRequest encodeRequest(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
            throw new RadiusPacketException("Cannot encode request for unknown auth protocol");
        }

        @Override
        protected void verify(String sharedSecret) throws RadiusPacketException {
            throw new RadiusPacketException("Access-Request auth verify failed - unknown auth protocol");
        }
    }
}
