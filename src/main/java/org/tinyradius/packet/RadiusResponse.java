package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.util.RadiusPackets;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class RadiusResponse extends BaseRadiusPacket {

    /**
     * Builds a Radius packet with the given type, identifier and attributes.
     * <p>
     * Use {@link RadiusPackets#createResponse(Dictionary, int, int, byte[], List)}
     * where possible as that automatically creates Access/Accounting
     * variants as required.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param identifier    packet identifier
     * @param authenticator can be null if creating manually
     * @param attributes    list of RadiusAttribute objects
     */
    public RadiusResponse(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    /**
     * Encode and generate authenticator. Should be idempotent.
     * <p>
     * Requires request authenticator to generator response authenticator.
     *
     * @param sharedSecret shared secret to be used to encode this packet
     * @param requestAuth  request packet authenticator
     * @return new RadiusPacket instance with same properties and valid authenticator
     */
    public RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) {
        final byte[] newAuth = createHashedAuthenticator(sharedSecret, requestAuth);
        return RadiusPackets.createResponse(getDictionary(), getType(), getIdentifier(), newAuth, getAttributes());
    }

    /**
     * Checks the response authenticator against the supplied shared secret.
     *
     * @param sharedSecret shared secret
     * @param requestAuth  authenticator for corresponding request
     * @throws RadiusPacketException if authenticator check fails
     */
    public void verifyResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        verifyPacketAuth(sharedSecret, requestAuth);
    }
}
