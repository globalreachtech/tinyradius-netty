package org.tinyradius.packet.request;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.List;

class AccessRequestNoAuth extends AccessRequest {

    public AccessRequestNoAuth(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    protected AccessRequestNoAuth encodeAuthMechanism(String sharedSecret, byte[] newAuth) {
        return new AccessRequestNoAuth(getDictionary(), getId(), newAuth, getAttributes());
    }

    @Override
    public AccessRequest copy() {
        return new AccessRequestNoAuth(getDictionary(), getId(), getAuthenticator(), getAttributes());
    }

    @Override
    protected void verifyAuthMechanism(String sharedSecret) {
        // no auth - nothing to verify
    }
}
