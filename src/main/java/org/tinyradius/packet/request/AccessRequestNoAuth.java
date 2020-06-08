package org.tinyradius.packet.request;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.List;

class AccessRequestNoAuth extends AccessRequest<AccessRequestNoAuth> {

    public AccessRequestNoAuth(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    public AccessRequestNoAuth encodeAuthMechanism(String sharedSecret, byte[] newAuth) {
        return new AccessRequestNoAuth(getDictionary(), getId(), newAuth, getAttributes());
    }

    @Override
    public AccessRequestNoAuth decodeAuthMechanism(String sharedSecret) {
        return this;
    }

    @Override
    public AccessRequestNoAuth withAttributes(List<RadiusAttribute> attributes) {
        return new AccessRequestNoAuth(getDictionary(), getId(), getAuthenticator(), attributes);
    }
}
