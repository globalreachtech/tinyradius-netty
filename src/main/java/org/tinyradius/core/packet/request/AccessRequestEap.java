package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.List;

import static org.tinyradius.core.attribute.RfcAttributeTypes.EAP_MESSAGE;
import static org.tinyradius.core.attribute.RfcAttributeTypes.MESSAGE_AUTHENTICATOR;

/**
 * EAP AccessRequest RFC3579
 */
public class AccessRequestEap extends AccessRequest {

    public AccessRequestEap(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
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

        final List<RadiusAttribute> messageAuthAttr = getAttributes(MESSAGE_AUTHENTICATOR);
        if (messageAuthAttr.size() != 1)
            throw new RadiusPacketException("AccessRequest (EAP) should have exactly one Message-Authenticator attribute, has " + messageAuthAttr.size());

        return super.decodeRequest(sharedSecret);
    }

    private void validateEapAttributes() throws RadiusPacketException {
        final List<RadiusAttribute> eapMessageAttr = getAttributes(EAP_MESSAGE);
        if (eapMessageAttr.isEmpty())
            throw new RadiusPacketException("AccessRequest (EAP) must have at least one EAP-Message attribute");
    }
}
