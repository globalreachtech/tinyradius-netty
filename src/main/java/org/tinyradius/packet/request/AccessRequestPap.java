package org.tinyradius.packet.request;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AccessRequestPap extends AccessRequest<AccessRequestPap> {

    public AccessRequestPap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    protected AccessRequestFactory<AccessRequestPap> factory() {
        return AccessRequestPap::new;
    }

    public AccessRequestPap withPassword(String password) {
        final List<RadiusAttribute> attributes = getAttributes().stream()
                .filter(a -> a.getVendorId() != -1 || a.getType() != USER_PASSWORD)
                .collect(Collectors.toList());

        attributes.add(getDictionary().createAttribute(-1, USER_PASSWORD, password.getBytes(UTF_8)));

        return withAttributes(attributes);
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

    @Override
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        checkUserPassword();
        return super.encodeRequest(sharedSecret);
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        checkUserPassword();
        return super.decodeRequest(sharedSecret);
    }

    private void checkUserPassword() throws RadiusPacketException {
        final int count = filterAttributes(USER_PASSWORD).size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (PAP) should have exactly one User-Password attribute, has " + count);
    }
}
