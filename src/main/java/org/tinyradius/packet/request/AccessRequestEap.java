package org.tinyradius.packet.request;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

import static org.tinyradius.packet.util.PacketType.ACCESS_REQUEST;

public class AccessRequestEap extends BaseRadiusPacket<AccessRequestEap> implements AccessRequest<AccessRequestEap> {

    public AccessRequestEap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, ACCESS_REQUEST, identifier, authenticator, attributes);
    }

    @Override
    public AccessRequestEap encodeAuthMechanism(String sharedSecret, byte[] newAuth) {
        return new AccessRequestEap(getDictionary(), getId(), newAuth, getAttributes());
    }

    @Override
    public AccessRequestEap verifyAuthMechanism(String sharedSecret) throws RadiusPacketException {
        final List<RadiusAttribute> eapMessageAttr = getAttributes(EAP_MESSAGE);
        if (eapMessageAttr.isEmpty()) {
            throw new RadiusPacketException("EAP-Message expected but not found");
        }

        final List<RadiusAttribute> messageAuthAttr = getAttributes(MESSAGE_AUTHENTICATOR);
        if (messageAuthAttr.size() != 1) {
            throw new RadiusPacketException("AccessRequest (EAP) should have exactly one Message-Authenticator attribute, has " + messageAuthAttr.size());
        }
        return this;
    }

    @Override
    public AccessRequestEap withAttributes(List<RadiusAttribute> attributes) {
        return new AccessRequestEap(getDictionary(), getId(), getAuthenticator(), attributes);
    }
}
