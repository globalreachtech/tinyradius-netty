package org.tinyradius.core.packet.response;

import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.PacketType;
import org.tinyradius.core.packet.util.MessageAuthSupport;
import org.tinyradius.core.RadiusPacketException;

import java.util.List;

public class AccessResponse extends GenericResponse implements MessageAuthSupport<RadiusResponse> {

    private AccessResponse(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    @Override
    public RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final RadiusResponse response = withAttributes(encodeAttributes(requestAuth, sharedSecret))
                .encodeMessageAuth(sharedSecret, requestAuth);

        final byte[] auth = response.genHashedAuth(sharedSecret, requestAuth);
        return new AccessResponse(getDictionary(), getType(), getId(), auth, response.getAttributes());
    }

    @Override
    public RadiusResponse decodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        verifyMessageAuth(sharedSecret, requestAuth);
        return super.decodeResponse(sharedSecret, requestAuth);
    }

    @Override
    public AccessResponse withAttributes(List<RadiusAttribute> attributes) {
        return new AccessResponse(getDictionary(), getType(), getId(), getAuthenticator(), attributes);
    }

    public static class Accept extends AccessResponse {
        public Accept(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
            super(dictionary, PacketType.ACCESS_ACCEPT, identifier, authenticator, attributes);
        }
    }

    public static class Reject extends AccessResponse {
        public Reject(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
            super(dictionary, PacketType.ACCESS_REJECT, identifier, authenticator, attributes);
        }
    }

    public static class Challenge extends AccessResponse {
        public Challenge(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
            super(dictionary, PacketType.ACCESS_CHALLENGE, identifier, authenticator, attributes);
        }
    }
}
