package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.RadiusPacket;
import org.tinyradius.core.packet.util.MessageAuthSupport;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

/**
 * This class represents an Access-Request Radius packet.
 */
public abstract class AccessRequest extends GenericRequest implements MessageAuthSupport<RadiusRequest> {

    protected static final Logger logger = LogManager.getLogger();
    protected static final SecureRandom RANDOM = new SecureRandom();

    protected static final int USER_PASSWORD = 2;
    protected static final int CHAP_PASSWORD = 3;
    protected static final int EAP_MESSAGE = 79;
    protected static final int ARAP_PASSWORD = 70;

    private static final Set<Integer> AUTH_ATTRS = new HashSet<>(
            Arrays.asList(USER_PASSWORD, CHAP_PASSWORD, ARAP_PASSWORD, EAP_MESSAGE));

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

        if (detectedAuth.isEmpty()) {
            logger.warn("AccessRequest no auth mechanism found, inferring NoAuth");
            return AccessRequestNoAuth::new;
        }

        if (detectedAuth.size() > 1) {
            logger.warn("AccessRequest identified multiple auth mechanisms, inferring NoAuth");
            return AccessRequestNoAuth::new;
        }

        switch (detectedAuth.iterator().next()) {
            case EAP_MESSAGE:
                logger.debug("Inferring AccessRequest as EAP");
                return AccessRequestEap::new;
            case CHAP_PASSWORD:
                logger.debug("Inferring AccessRequest as CHAP");
                return AccessRequestChap::new;
            case USER_PASSWORD:
                logger.debug("Inferring AccessRequest as PAP");
                return AccessRequestPap::new;
            case ARAP_PASSWORD:
                logger.debug("Inferring AccessRequest as ARAP");
                return AccessRequestArap::new;
            default:
                logger.debug("Inferring AccessRequest as NoAuth");
                return AccessRequestNoAuth::new;
        }
    }

    protected static byte[] random16bytes() {
        final byte[] randomBytes = new byte[16];
        RANDOM.nextBytes(randomBytes);
        return randomBytes;
    }

    @Override
    protected byte[] genAuth(String sharedSecret) {
        // create authenticator only if needed - maintain idempotence
        return getAuthenticator() == null ? random16bytes() : getAuthenticator();
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
    public AccessRequest withChapPassword(String password) throws RadiusPacketException {
        return AccessRequestChap.withPassword(withoutAuths(), password);
    }

    public AccessRequest withPapPassword(String password) throws RadiusPacketException {
        return AccessRequestPap.withPassword(withoutAuths(), password);
    }

    /**
     * https://tools.ietf.org/html/rfc2869
     * <p>
     * An Access-Request that contains either a User-Password or
     * CHAP-Password or ARAP-Password or one or more EAP-Message attributes
     * MUST NOT contain more than one type of those four attributes.
     */
    private AccessRequest withoutAuths() throws RadiusPacketException {
        return withAttributes(filterAttributes(a -> !(a.getVendorId() == -1 && AUTH_ATTRS.contains(a.getType()))));
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
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        final byte[] auth = genAuth(sharedSecret);

        return withAuthAttributes(auth, encodeAttributes(auth, sharedSecret))
                .encodeMessageAuth(sharedSecret, auth);
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        // authenticator is random, so can't run verifyPacketAuth(), but we can do basic checks
        final byte[] auth = getAuthenticator();
        if (auth == null)
            throw new RadiusPacketException("Authenticator check failed - authenticator missing");

        if (auth.length != 16)
            throw new RadiusPacketException("Authenticator check failed - authenticator must be 16 octets, actual " + auth.length);

        verifyMessageAuth(sharedSecret, getAuthenticator());
        return withAttributes(decodeAttributes(getAuthenticator(), sharedSecret));
    }

    @Override
    public AccessRequest withAttributes(List<RadiusAttribute> attributes) throws RadiusPacketException {
        final ByteBuf newHeader = RadiusPacket.buildHeader(getType(), getId(), getAuthenticator(), attributes);
        return create(getDictionary(), newHeader, attributes);
    }

    @Override
    public AccessRequest withAuthAttributes(byte[] auth, List<RadiusAttribute> attributes) throws RadiusPacketException {
        final ByteBuf header = RadiusPacket.buildHeader(getType(), getId(), auth, attributes);
        return create(getDictionary(), header, attributes);
    }

    public interface AccessRequestFactory {
        AccessRequest newInstance(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException;
    }
}
