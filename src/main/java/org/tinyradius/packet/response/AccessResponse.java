package org.tinyradius.packet.response;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.util.MessageAuthSupport;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

import static org.tinyradius.packet.util.PacketType.*;

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
            super(dictionary, ACCESS_ACCEPT, identifier, authenticator, attributes);
        }
    }

    public static class Reject extends AccessResponse {
        public Reject(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
            super(dictionary, ACCESS_REJECT, identifier, authenticator, attributes);
        }
    }

    public static class Challenge extends AccessResponse {
        public Challenge(Dictionary dictionary, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
            super(dictionary, ACCESS_CHALLENGE, identifier, authenticator, attributes);
        }
    }
}
