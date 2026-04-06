package org.tinyradius.core.packet.request;

import static org.tinyradius.core.attribute.AttributeTypes.ARAP_PASSWORD;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

/**
 * ARAP AccessRequest RFC2869
 * Stub TODO
 */
public class AccessRequestArap extends AccessRequest {

    public AccessRequestArap(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    @Override
    public @NonNull RadiusRequest encodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        validateArapAttributes();
        return super.encodeRequest(sharedSecret);
    }

    @Override
    public @NonNull RadiusRequest decodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        validateArapAttributes();
        return super.decodeRequest(sharedSecret);
    }

    private void validateArapAttributes() throws RadiusPacketException {
        int count = getAttributes(ARAP_PASSWORD).size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (ARAP) should have exactly one ARAP-Password attribute, has " + count);
    }
}
