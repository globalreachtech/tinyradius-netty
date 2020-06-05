package org.tinyradius.packet.request;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.packet.util.RadiusPackets;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

public class RadiusRequest extends BaseRadiusPacket {

    /**
     * Builds a Radius packet with the given type, identifier and attributes.
     * <p>
     * Use {@link RadiusPackets#createRequest(Dictionary, byte, byte, byte[], List)}
     * where possible as that automatically creates Access/Accounting
     * variants as required.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param identifier    packet identifier
     * @param authenticator can be null if creating manually
     * @param attributes    list of RadiusAttribute objects
     */
    public RadiusRequest(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, type, identifier, authenticator, attributes);
    }

    public RadiusRequest copy() {
        return RadiusPackets.createRequest(getDictionary(), getType(), getId(), getAuthenticator(), getAttributes());
    }

    /**
     * Encode request and generate authenticator. Should be idempotent.
     * <p>
     * Base implementation generates hashed authenticator.
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @return RadiusPacket with new authenticator and/or encoded attributes
     * @throws RadiusPacketException if invalid or missing attributes
     */
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        final byte[] authenticator = createHashedAuthenticator(sharedSecret, new byte[16]);
        return RadiusPackets.createRequest(getDictionary(), getType(), getId(), authenticator, getAttributes());
    }

    /**
     * Checks the request authenticator against the supplied shared secret.
     *
     * @param sharedSecret shared secret
     * @throws RadiusPacketException if authenticator check fails
     */
    public void verifyRequest(String sharedSecret) throws RadiusPacketException {
        verifyPacketAuth(sharedSecret, new byte[16]);
    }
}
