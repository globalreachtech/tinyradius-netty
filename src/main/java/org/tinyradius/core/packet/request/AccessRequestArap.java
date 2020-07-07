package org.tinyradius.core.packet.request;

import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.RadiusPacketException;

import java.util.List;

/**
 * ARAP AccessRequest RFC2869
 * Stub TODO
 */
public class AccessRequestArap extends AccessRequest<AccessRequestArap> {

    public AccessRequestArap(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    @Override
    protected AccessRequestFactory<AccessRequestArap> factory() {
        return AccessRequestArap::new;
    }

    @Override
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        validateArapAttributes();
        return super.encodeRequest(sharedSecret);
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        validateArapAttributes();
        return super.decodeRequest(sharedSecret);
    }

    private void validateArapAttributes() throws RadiusPacketException {
        final int count = filterAttributes(ARAP_PASSWORD).size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (ARAP) should have exactly one ARAP-Password attribute, has " + count);
    }
}
