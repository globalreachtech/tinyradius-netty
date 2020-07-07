package org.tinyradius.core.packet.request;

import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.RadiusPacketException;

import java.util.List;

/**
 * EAP AccessRequest RFC3579
 */
public class AccessRequestEap extends AccessRequest<AccessRequestEap> {

    public AccessRequestEap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    protected AccessRequestFactory<AccessRequestEap> factory() {
        return AccessRequestEap::new;
    }

    @Override
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        validateEapAttributes();
        // Message-Auth added at end of encoding
        return super.encodeRequest(sharedSecret);
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        validateEapAttributes();

        final List<RadiusAttribute> messageAuthAttr = filterAttributes(MESSAGE_AUTHENTICATOR);
        if (messageAuthAttr.size() != 1)
            throw new RadiusPacketException("AccessRequest (EAP) should have exactly one Message-Authenticator attribute, has " + messageAuthAttr.size());

        return super.decodeRequest(sharedSecret);
    }

    private void validateEapAttributes() throws RadiusPacketException {
        final List<RadiusAttribute> eapMessageAttr = filterAttributes(EAP_MESSAGE);
        if (eapMessageAttr.isEmpty())
            throw new RadiusPacketException("AccessRequest (EAP) must have at least one EAP-Message attribute");
    }
}
