package org.tinyradius.packet.request;

import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

import static org.tinyradius.packet.util.PacketType.ACCESS_REQUEST;
import static org.tinyradius.packet.util.PacketType.ACCOUNTING_REQUEST;

public interface RadiusRequest extends RadiusPacket<RadiusRequest> {

    /**
     * Creates a RadiusPacket object. Depending on the passed type, an
     * appropriate packet is created. Also sets the type, and the
     * the packet identifier.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes for packet
     * @return RadiusPacket object
     */
    static RadiusRequest create(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        switch (type) {
            case ACCESS_REQUEST:
                return AccessRequest.create(dictionary, identifier, authenticator, attributes);
            case ACCOUNTING_REQUEST:
                return new AccountingRequest(dictionary, identifier, authenticator, attributes);
            default:
                return new GenericRequest(dictionary, type, identifier, authenticator, attributes);
        }
    }

    /**
     * Encode request and generate authenticator. Should be idempotent.
     * <p>
     * Base implementation generates hashed authenticator.
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @return RadiusRequest with new authenticator and/or encoded attributes
     * @throws RadiusPacketException if invalid or missing attributes
     */
    RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException;

    /**
     * Checks the request authenticator against the supplied shared secret.
     *
     * @param sharedSecret shared secret
     * @return verified RadiusRequest with decoded attributes if appropriate
     * @throws RadiusPacketException if authenticator check fails
     */
    default RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        verifyPacketAuth(sharedSecret, new byte[16]);
        return this;
    }
}
