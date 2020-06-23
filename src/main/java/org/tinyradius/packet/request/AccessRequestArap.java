package org.tinyradius.packet.request;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.List;

class AccessRequestArap extends AccessRequest {

    public AccessRequestArap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    public AccessRequestArap encodeAuthMechanism(String sharedSecret, byte[] newAuth) {
        return new AccessRequestArap(getDictionary(), getId(), newAuth, getAttributes());
    }

    @Override
    public AccessRequestArap decodeAuthMechanism(String sharedSecret) {
        return this; // todo implement
    }

    @Override
    public AccessRequestArap withAttributes(List<RadiusAttribute> attributes) {
        return new AccessRequestArap(getDictionary(), getId(), getAuthenticator(), attributes);
    }
}
