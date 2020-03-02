package org.tinyradius.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.attribute.Attributes;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.auth.MessageAuthSupport;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;

/**
 * This class represents an Access-Request Radius packet.
 */
public abstract class AccessRequest extends RadiusRequest implements MessageAuthSupport {

    protected static final Logger logger = LogManager.getLogger();

    protected static final SecureRandom RANDOM = new SecureRandom();

    public static final int USER_PASSWORD = 2;
    public static final int CHAP_PASSWORD = 3;
    public static final int EAP_MESSAGE = 79;
    protected static final List<Integer> AUTH_ATTRS = Arrays.asList(USER_PASSWORD, CHAP_PASSWORD, EAP_MESSAGE);

    protected static final int USER_NAME = 1;

    /**
     * Create copy of AccessRequest with new authenticator and encoded attributes
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @param newAuth      authenticator to use to encode PAP password,
     *                     nullable if using different auth protocol
     * @return RadiusPacket with new authenticator and encoded attributes
     */
    protected abstract AccessRequest encodeAuthMechanism(String sharedSecret, byte[] newAuth) throws RadiusPacketException;

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

        return encodeAuthMechanism(sharedSecret, newAuth);
        // todo add Message-Authenticator
    }

    /**
     * Verify packet for specific auth frameworks.
     *
     * @param sharedSecret shared secret
     */
    protected abstract void verifyAuthMechanism(String sharedSecret) throws RadiusPacketException;

    /**
     * AccessRequest cannot verify authenticator as they
     * contain random bytes.
     * <p>
     * It can, however, check the User-Password/Challenge attributes
     * are present and attempt decryption, depending on auth protocol.
     *
     * @param sharedSecret shared secret, only applicable for PAP
     */
    @Override
    public void verifyRequest(String sharedSecret) throws RadiusPacketException {
        verifyMessageAuth(sharedSecret, getAuthenticator());
        verifyAuthMechanism(sharedSecret);
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
                .map(authAttr -> Attributes.filter(attributes, authAttr))
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
        protected AccessRequest encodeAuthMechanism(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
            throw new RadiusPacketException("Cannot encode request for unknown auth protocol");
        }

        @Override
        protected void verifyAuthMechanism(String sharedSecret) throws RadiusPacketException {
            throw new RadiusPacketException("Access-Request auth verify failed - unknown auth protocol");
        }
    }
}
