package org.tinyradius.packet.request;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.List;

class AccessRequestNoAuth extends AccessRequest {

    public AccessRequestNoAuth(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    public AccessRequestNoAuth encodeAuthMechanism(String sharedSecret, byte[] newAuth) {
        return new AccessRequestNoAuth(getDictionary(), getId(), newAuth, getAttributes());
    }

    @Override
    public AccessRequestNoAuth decodeAuthMechanism(String sharedSecret) {
        final List<RadiusAttribute> messageAuthAttr = filterAttributes(MESSAGE_AUTHENTICATOR);
        if (messageAuthAttr.size() != 1) {
            logger.debug("AccessRequest without one of User-Password/CHAP-Password/ARAP-Password/EAP-Message " +
                    "should contain a Message-Authenticator");
        }

        return this;
    }

    @Override
    public AccessRequestNoAuth withAttributes(List<RadiusAttribute> attributes) {
        return new AccessRequestNoAuth(getDictionary(), getId(), getAuthenticator(), attributes);
    }
}
