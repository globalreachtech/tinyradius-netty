package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.util.MessageAuthSupport;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.tinyradius.core.attribute.AttributeTypes.*;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

/**
 * An Access-Request Radius packet.
 */
public abstract class AccessRequest extends GenericRequest implements MessageAuthSupport<RadiusRequest> {

    protected static final Logger logger = LogManager.getLogger();
    protected static final SecureRandom RANDOM = new SecureRandom();

    private static final Set<Integer> AUTH_ATTRS = Set.of(
            (int) USER_PASSWORD, (int) CHAP_PASSWORD, (int) ARAP_PASSWORD, (int) EAP_MESSAGE);

    protected AccessRequest(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
        final byte type = header.getByte(0);
        if (type != ACCESS_REQUEST)
            throw new IllegalArgumentException("First octet must be " + ACCESS_REQUEST + ", actual: " + type);
    }

    /**
     * Create new AccessRequest, tries to identify auth protocol from attributes.
     *
     * @param dictionary custom dictionary to use
     * @param header     packet header (20 octets)
     * @param attributes list of attributes for packet, should not be empty
     *                   or a stub AccessRequest will be returned
     * @return AccessRequest auth mechanism-specific implementation
     */
    static AccessRequest create(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        return lookupAuthType(attributes).newInstance(dictionary, header, attributes);
    }

    private static AccessRequestFactory lookupAuthType(List<RadiusAttribute> attributes) {
        /*
         * An Access-Request that contains either a User-Password or
         * CHAP-Password or ARAP-Password or one or more EAP-Message attributes
         * MUST NOT contain more than one type of those four attributes.
         */
        final Set<Integer> detectedAuth = attributes.stream()
                .map(RadiusAttribute::getType)
                .filter(AUTH_ATTRS::contains)
                .collect(toSet());

        // will occur a lot as PAP/CHAP are generally created by RadiusRequest.create().withPapPassword()
        if (detectedAuth.isEmpty())
            return AccessRequestNoAuth::new;

        if (detectedAuth.size() > 1) {
            logger.warn("Identified multiple auth mechanisms, inferring NoAuth"); // bad packet
            return AccessRequestNoAuth::new;
        }

        final int authType = detectedAuth.iterator().next();
        return switch (authType) {
            case EAP_MESSAGE -> AccessRequestEap::new;
            case CHAP_PASSWORD -> AccessRequestChap::new;
            case USER_PASSWORD -> AccessRequestPap::new;
            case ARAP_PASSWORD -> AccessRequestArap::new;
            default -> {
                // shouldn't happen - if AUTH_ATTRS contains authType, it should be handled
                logger.warn("Cannot process authType {}, defaulting to NoAuth", authType);
                yield AccessRequestNoAuth::new;
            }
        };
    }

    protected static byte[] random16bytes() {
        final byte[] randomBytes = new byte[16];
        RANDOM.nextBytes(randomBytes);
        return randomBytes;
    }

    @Override
    protected byte[] genAuth(String ignored) {
        byte[] auth = getAuthenticator();
        return auth == null ? random16bytes() : auth; // create new auth only if needed - maintain idempotence
    }

    /**
     * Retrieves the username
     *
     * @return username as String
     */
    public Optional<String> getUsername() {
        return getAttribute(-1, USER_NAME)
                .map(RadiusAttribute::getValueString);
    }

    /**
     * Set CHAP-Password attribute with the provided password and initializes
     * CHAP-Challenge with random bytes.
     * <p>
     * Removes existing auth-related attributes if present (User-Password, CHAP-Password, etc).
     *
     * @param password plaintext password to encode into CHAP-Password
     * @return AccessRequestChap with encoded CHAP-Password and CHAP-Challenge attributes
     * @throws IllegalArgumentException invalid password
     * @throws RadiusPacketException    packet validation exceptions
     */
    public AccessRequest withChapPassword(String password) throws RadiusPacketException {
        return AccessRequestChap.withPassword(withoutAuths(), password);
    }

    /**
     * Set User-Password attribute with provided password.
     * <p>
     * Removes existing auth-related attributes if present (User-Password, CHAP-Password etc).
     *
     * @param password plaintext password to encode into User-Password
     * @return AccessRequestPap with the encoded User-Password attribute
     * @throws RadiusPacketException packet validation exceptions
     */
    public AccessRequest withPapPassword(String password) throws RadiusPacketException {
        return AccessRequestPap.withPassword(withoutAuths(), password);
    }

    /**
     * <a href="https://tools.ietf.org/html/rfc2869">RFC 2869</a>
     * <p>
     * An Access-Request that contains either a User-Password or
     * CHAP-Password or ARAP-Password or one or more EAP-Message attributes
     * MUST NOT contain more than one type of those four attributes.
     *
     * @return instance without USER_PASSWORD, CHAP_PASSWORD, ARAP_PASSWORD, EAP_MESSAGE attributes
     */
    private AccessRequest withoutAuths() throws RadiusPacketException {
        return (AccessRequest) withAttributes(getAttributes(a -> !(a.getVendorId() == -1 && AUTH_ATTRS.contains(a.getType()))));
    }

    /**
     * AccessRequest overrides this method to generate a randomized authenticator (RFC 2865)
     * and encode required attributes (e.g. User-Password).
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @return RadiusPacket with new authenticator and encoded attributes
     */
    @Override
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        return ((AccessRequest) super.encodeRequest(sharedSecret))
                .encodeMessageAuth(sharedSecret, null);
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        // authenticator is random, so can't run verifyPacketAuth(), but we can do basic checks
        final byte[] auth = getAuthenticator();
        if (auth == null)
            throw new RadiusPacketException("Authenticator check failed - authenticator missing");

        if (auth.length != 16)
            throw new RadiusPacketException("Authenticator check failed - authenticator must be 16 octets, actual " + auth.length);

        verifyMessageAuth(sharedSecret, null);
        return withAttributes(decodeAttributes(getAuthenticator(), sharedSecret));
    }


    public interface AccessRequestFactory {
        AccessRequest newInstance(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException;
    }
}
