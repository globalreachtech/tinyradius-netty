package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.List;

import static org.tinyradius.core.attribute.RfcAttributeTypes.MESSAGE_AUTHENTICATOR;

public class AccessRequestNoAuth extends AccessRequest {

    public AccessRequestNoAuth(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    @Override
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        final RadiusRequest radiusRequest = super.encodeRequest(sharedSecret);
        checkMessageAuth(radiusRequest.getAttributes()); // MessageAuthSupport should append Message-Auth during encoding
        return radiusRequest;
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        checkMessageAuth(getAttributes());
        return super.decodeRequest(sharedSecret);
    }

    private static void checkMessageAuth(List<RadiusAttribute> attributes) {
        final long count = attributes.stream()
                .filter(a -> a.getType() == MESSAGE_AUTHENTICATOR)
                .count();
        if (count != 1)
            logger.warn("AccessRequest without one of User-Password/CHAP-Password/ARAP-Password/EAP-Message " +
                    "should contain a Message-Authenticator");
    }
}
