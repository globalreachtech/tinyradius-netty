package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class AccessRequestEap extends AccessRequest {

    public AccessRequestEap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    protected AccessRequest encodeAuthMechanism(String sharedSecret, byte[] newAuth) {
        return copy(); // don't care contents of EAP-Message - pass through
    }

    @Override
    protected void verifyAuthMechanism(String sharedSecret) throws RadiusPacketException {
        final List<RadiusAttribute> eapMessageAttr = getAttributes(EAP_MESSAGE);
        if (eapMessageAttr.isEmpty()) {
            throw new RadiusPacketException("EAP-Message expected but not found");
        }

        final List<RadiusAttribute> messageAuthAttr = getAttributes(MESSAGE_AUTHENTICATOR);
        if (messageAuthAttr.size() != 1) {
            throw new RadiusPacketException("AccessRequest (EAP) should have exactly one Message-Authenticator attribute, has " + messageAuthAttr.size());
        }
    }

    @Override
    public AccessRequest copy() {
        return new AccessRequestEap(getDictionary(), getId(), getAuthenticator(), getAttributes());
    }
}
