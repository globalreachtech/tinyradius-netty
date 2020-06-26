package org.tinyradius.packet.request;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class GenericRequest extends BaseRadiusPacket<RadiusRequest> implements RadiusRequest {

    /**
     * Builds a Radius packet with the given type, identifier and attributes.
     * <p>
     * Use {@link RadiusRequest#create(Dictionary, byte, byte, byte[], List)}
     * where possible as that automatically creates Access/Accounting
     * variants as required.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param identifier    packet identifier
     * @param authenticator can be null if creating manually
     * @param attributes    list of RadiusAttribute objects
     */
    public GenericRequest(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    /**
     * @param sharedSecret to generate authenticator
     * @return new authenticator, must be idempotent
     */
    protected byte[] genAuth(String sharedSecret) {
        return genHashedAuth(sharedSecret, new byte[16]);
    }

    @Override
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        final byte[] auth = genAuth(sharedSecret);
        return new GenericRequest(getDictionary(), getType(), getId(), auth, encodeAttributes(sharedSecret, auth));
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        verifyPacketAuth(sharedSecret, new byte[16]);
        return withAttributes(decodeAttributes(sharedSecret));
    }

    @Override
    public RadiusRequest withAttributes(List<RadiusAttribute> attributes) {
        return new GenericRequest(getDictionary(), getType(), getId(), getAuthenticator(), attributes);
    }
}
