package org.tinyradius.packet.response;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;

import java.util.List;

public class GenericRadiusResponse extends BaseRadiusPacket<GenericRadiusResponse> implements RadiusResponse {

    /**
     * Builds a Radius packet with the given type, identifier and attributes.
     * <p>
     * Use {@link RadiusResponse#create(Dictionary, byte, byte, byte[], List)}
     * where possible as that automatically creates Access/Accounting
     * variants as required.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param identifier    packet identifier
     * @param authenticator can be null if creating manually
     * @param attributes    list of RadiusAttribute objects
     */
    public GenericRadiusResponse(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    @Override
    public GenericRadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) {
        final byte[] newAuth = createHashedAuthenticator(sharedSecret, requestAuth);
        return new GenericRadiusResponse(getDictionary(), getType(), getId(), newAuth, getAttributes());
    }

    @Override
    public GenericRadiusResponse withAttributes(List<RadiusAttribute> attributes) {
        return new GenericRadiusResponse(getDictionary(), getType(), getId(), getAuthenticator(), attributes);
    }
}
