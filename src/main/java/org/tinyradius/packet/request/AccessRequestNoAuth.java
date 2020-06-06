package org.tinyradius.packet.request;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;

import java.util.List;

import static org.tinyradius.packet.util.PacketType.ACCESS_REQUEST;

class AccessRequestNoAuth extends BaseRadiusPacket<AccessRequestNoAuth> implements AccessRequest<AccessRequestNoAuth> {

    public AccessRequestNoAuth(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, ACCESS_REQUEST, identifier, authenticator, attributes);
    }

    @Override
    public AccessRequestNoAuth encodeAuthMechanism(String sharedSecret, byte[] newAuth) {
        return new AccessRequestNoAuth(getDictionary(), getId(), newAuth, getAttributes());
    }

    @Override
    public AccessRequestNoAuth verifyAuthMechanism(String sharedSecret) {
        return this;
    }

    @Override
    public AccessRequestNoAuth withAttributes(List<RadiusAttribute> attributes) {
        return this;
    }
}
