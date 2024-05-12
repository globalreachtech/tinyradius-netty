package org.tinyradius.core.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.core.packet.PacketType.*;

class PacketTypeTest {

    @Test
    void getAccessPacketType() {
        String accessRequest = PacketType.getPacketTypeName(ACCESS_REQUEST);
        String accessAccept = PacketType.getPacketTypeName(ACCESS_ACCEPT);
        String accessReject = PacketType.getPacketTypeName(ACCESS_REJECT);
        String accessChallenge = PacketType.getPacketTypeName(ACCESS_CHALLENGE);

        assertEquals("Access-Request", accessRequest);
        assertEquals("Access-Accept", accessAccept);
        assertEquals("Access-Reject", accessReject);
        assertEquals("Access-Challenge", accessChallenge);
    }

    @Test
    void getDefaultPacketType() {
        String unknown = PacketType.getPacketTypeName((byte) 256);
        assertEquals("Unknown (" + 0 + ")", unknown);
    }
}
