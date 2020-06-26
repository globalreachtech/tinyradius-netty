package org.tinyradius.packet.request;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

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
}
