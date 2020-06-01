package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.List;

class AccessRequestNoAuth extends AccessRequest {

    public AccessRequestNoAuth(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    protected AccessRequest encodeAuthMechanism(String sharedSecret, byte[] newAuth) {
        return copy();
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
