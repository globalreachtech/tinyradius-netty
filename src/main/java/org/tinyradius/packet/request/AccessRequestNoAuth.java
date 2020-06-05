package org.tinyradius.packet.request;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;

import java.util.List;

import static org.tinyradius.packet.util.PacketType.ACCESS_REQUEST;

class AccessRequestNoAuth extends BaseRadiusPacket implements AccessRequest {

    public AccessRequestNoAuth(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, ACCESS_REQUEST, identifier, authenticator, attributes);
    }

    @Override
    public AccessRequestNoAuth encodeAuthMechanism(String sharedSecret, byte[] newAuth) {
        return new AccessRequestNoAuth(getDictionary(), getId(), newAuth, getAttributes());
    }

    @Override
    public AccessRequest copy() {
        return new AccessRequestNoAuth(getDictionary(), getId(), getAuthenticator(), getAttributes());
    }

    @Override
    public AccessRequest verifyAuthMechanism(String sharedSecret) {
        return this;
    }
}
