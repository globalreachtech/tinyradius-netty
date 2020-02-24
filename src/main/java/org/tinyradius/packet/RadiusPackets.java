package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.packet.PacketType.ACCOUNTING_REQUEST;

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
     * @param dictionary custom dictionary to use
     * @param type       packet type
     * @param identifier packet identifier
     * @return RadiusPacket object
     */
    public static RadiusPacket create(Dictionary dictionary, int type, int identifier) {
        return create(dictionary, type, identifier, null, new ArrayList<>());
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
     * @return RadiusPacket object
     */
    public static RadiusPacket create(Dictionary dictionary, int type, int identifier, byte[] authenticator) {
        return create(dictionary, type, identifier, authenticator, new ArrayList<>());
    }

    /**
     * Creates a RadiusPacket object. Depending on the passed type, an
     * appropriate packet is created. Also sets the type, and the
     * the packet identifier.
     *
     * @param dictionary custom dictionary to use
     * @param type       packet type
     * @param identifier packet identifier
     * @param attributes list of attributes for packet
     * @return RadiusPacket object
     */
    public static RadiusPacket create(Dictionary dictionary, int type, int identifier, List<RadiusAttribute> attributes) {
        return create(dictionary, type, identifier, null, attributes);
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
    public static RadiusPacket create(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        switch (type) {
            case ACCESS_REQUEST:
                return AccessRequest.create(dictionary, identifier, authenticator, attributes);
            case ACCOUNTING_REQUEST:
                return new AccountingRequest(dictionary, identifier, authenticator, attributes);
            default:
                return new RadiusPacket(dictionary, type, identifier, authenticator, attributes);
        }
    }
}
