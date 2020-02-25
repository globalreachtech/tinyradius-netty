package org.tinyradius.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.attribute.AttributeHolder;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;

/**
 * This class represents an Access-Request Radius packet.
 */
public abstract class AccessRequest extends RadiusPacket {

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

        // create authenticator only if needed to maintain idempotence
        byte[] newAuth = getAuthenticator() == null ? random16bytes() : getAuthenticator();

        return encodeRequest(sharedSecret, newAuth);
    }


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
    public abstract void verify(String sharedSecret, byte[] requestAuth) throws RadiusPacketException;

    protected static final Logger logger = LogManager.getLogger();

    protected static final SecureRandom RANDOM = new SecureRandom();

    private static final int USER_NAME = 1;
    protected static final int USER_PASSWORD = 2;
    protected static final int CHAP_PASSWORD = 3;
    protected static final int CHAP_CHALLENGE = 60;
    protected static final int EAP_MESSAGE = 79;

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
     * Create new AccessRequest, setting protocol explicitly.
     *
     * @param dictionary    custom dictionary to use
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     * @param protocol      auth protocol
     */
    public static AccessRequest create(Dictionary dictionary, int identifier, byte[] authenticator, String protocol) {
        switch (protocol) {
            case "eap":
                return new AccessEap(dictionary, identifier, authenticator, new ArrayList<>());
            case "pap":
                return new AccessPap(dictionary, identifier, authenticator, new ArrayList<>());
            case "chap":
                return new AccessChap(dictionary, identifier, authenticator, new ArrayList<>());
            default:
                return new AccessRequestUnknownProtocol(dictionary, identifier, authenticator, new ArrayList<>());
        }
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
        return detectType(attributes).newInstance(dictionary, identifier, authenticator, attributes);
    }

    private static AccessRequestConstructor<? extends AccessRequest> detectType(List<RadiusAttribute> attributes) {
        final List<RadiusAttribute> eapMessage = AttributeHolder.getAttributes(attributes, EAP_MESSAGE);
        if (!eapMessage.isEmpty()) {
            return AccessEap::new;
        }

        final List<RadiusAttribute> userPassword = AttributeHolder.getAttributes(attributes, USER_PASSWORD);
        if (!userPassword.isEmpty()) {
            return AccessPap::new;
        }

        final List<RadiusAttribute> chapPassword = AttributeHolder.getAttributes(attributes, CHAP_PASSWORD);
        if (!chapPassword.isEmpty()) {
            return AccessChap::new;
        }

        return AccessRequestUnknownProtocol::new;
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

    interface AccessRequestConstructor<T extends AccessRequest> {
        T newInstance(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes);
    }

    private static class AccessRequestUnknownProtocol extends AccessRequest {

        public AccessRequestUnknownProtocol(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
            super(dictionary, identifier, authenticator, attributes);
        }

        @Override
        protected AccessRequest encodeRequest(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
            throw new RadiusPacketException("Cannot encode request for unsupported auth protocol");
        }

        @Override
        public void verify(String sharedSecret, byte[] ignored) throws RadiusPacketException {
            throw new RadiusPacketException("Access-Request auth verify failed - could not identify auth protocol");
        }
    }
}
