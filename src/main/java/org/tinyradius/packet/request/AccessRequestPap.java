package org.tinyradius.packet.request;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AccessRequestPap extends AccessRequest {

    public AccessRequestPap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    public AccessRequestPap withPassword(String password) {
        return withAttributes(Stream.concat(
                getAttributes().stream().filter(a -> a.getVendorId() != -1 || a.getType() != 2),
                Stream.of(getDictionary().createAttribute(-1, USER_PASSWORD, password.getBytes(UTF_8)))
        ).collect(Collectors.toList()));
    }

    /**
     * Retrieves the plain-text user password.
     *
     * @return user password in plaintext if decoded
     */
    public Optional<String> getPassword() {
        return getAttribute(-1, USER_PASSWORD)
                .map(RadiusAttribute::getValue)
                .map(v -> new String(v, UTF_8));
    }

    /**
     * Sets and encodes the User-Password attribute.
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @param newAuth      authenticator to use to encode PAP password,
     *                     nullable if using different auth protocol
     * @return List of RadiusAttributes to override
     */
    @Override
    public AccessRequestPap encodeAuthMechanism(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
        final Optional<String> password = getPassword();
        if (!password.isPresent() || password.get().isEmpty()) {
            logger.warn("Could not encode PAP attributes, password not set");
            throw new RadiusPacketException("Could not encode PAP attributes, password not set");
        }

        final List<RadiusAttribute> attributes = new ArrayList<>();
        for (RadiusAttribute a : getAttributes()) {
            RadiusAttribute encode = a.encode(sharedSecret, newAuth);
            attributes.add(encode);
        }
        return new AccessRequestPap(getDictionary(), getId(), newAuth, attributes);
    }

    @Override
    public AccessRequestPap decodeAuthMechanism(String sharedSecret) throws RadiusPacketException {
        final List<RadiusAttribute> attrs = filterAttributes(USER_PASSWORD);
        if (attrs.size() != 1) {
            throw new RadiusPacketException("AccessRequest (PAP) should have exactly one User-Password attribute, has " + attrs.size());
        }

        final List<RadiusAttribute> attributes = new ArrayList<>();
        for (RadiusAttribute a : getAttributes()) {
            RadiusAttribute decode = a.decode(sharedSecret, getAuthenticator());
            attributes.add(decode);
        }
        return withAttributes(attributes);
    }

    @Override
    public AccessRequestPap withAttributes(List<RadiusAttribute> attributes) {
        return new AccessRequestPap(getDictionary(), getId(), getAuthenticator(), attributes);
    }
}
