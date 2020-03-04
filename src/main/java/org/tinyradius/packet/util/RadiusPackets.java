package org.tinyradius.packet.util;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.*;

import java.util.List;

import static org.tinyradius.packet.util.PacketType.*;

/**
 * Utils for creating new RadiusPackets
 */
public class RadiusPackets {

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
    public static RadiusRequest createRequest(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        switch (type) {
            case ACCESS_REQUEST:
                return AccessRequest.create(dictionary, identifier, authenticator, attributes);
            case ACCOUNTING_REQUEST:
                return new AccountingRequest(dictionary, identifier, authenticator, attributes);
            default:
                return new RadiusRequest(dictionary, type, identifier, authenticator, attributes);
        }
    }

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
    public static RadiusResponse createResponse(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        switch (type) {
            case ACCESS_ACCEPT:
            case ACCESS_REJECT:
            case ACCESS_CHALLENGE:
                return new AccessResponse(dictionary, type, identifier, authenticator, attributes);
            default:
                return new RadiusResponse(dictionary, type, identifier, authenticator, attributes);
        }
    }
}
