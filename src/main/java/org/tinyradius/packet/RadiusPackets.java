package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.auth.RadiusRequest;
import org.tinyradius.packet.auth.RadiusResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.tinyradius.packet.PacketType.*;

/**
 * Utils for creating new RadiusPackets
 */
public class RadiusPackets {

    private static final AtomicInteger nextPacketId = new AtomicInteger();

    /**
     * Increment and return the next packet identifier, between 0 and 255
     *
     * @return the next packet identifier to use
     */
    public static int nextPacketId() {
        return nextPacketId.updateAndGet(i -> i >= 255 ? 0 : i + 1);
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
    public static RadiusRequest createRequest(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
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
    public static RadiusResponse createResponse(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
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
