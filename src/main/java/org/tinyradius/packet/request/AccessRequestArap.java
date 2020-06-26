package org.tinyradius.packet.request;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

/**
 * Stub ARAP AccessRequest
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
