package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.packet.PacketType.ACCOUNTING_REQUEST;

public class RadiusPackets {
    private static AtomicInteger nextPacketId = new AtomicInteger();

    /**
     * Increment the next packet identifier.
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
     * @param type packet type
     * @return RadiusPacket object
     */
    public static RadiusPacket createRadiusPacket(Dictionary dictionary, int type, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        switch (type) {
            case ACCESS_REQUEST:
                return new AccessRequest(dictionary, identifier, authenticator, attributes);
            case ACCOUNTING_REQUEST:
                return new AccountingRequest(dictionary, identifier, authenticator, attributes);
            default:
                return new RadiusPacket(dictionary, type, identifier, authenticator, attributes);
        }
    }
}
