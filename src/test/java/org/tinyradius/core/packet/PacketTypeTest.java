package org.tinyradius.core.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketTypeTest {

    @Test
    void getAccessPacketType() {
        String accessRequest = PacketType.getPacketTypeName(PacketType.ACCESS_REQUEST);
        String accessAccept = PacketType.getPacketTypeName(PacketType.ACCESS_ACCEPT);
        String accessReject = PacketType.getPacketTypeName(PacketType.ACCESS_REJECT);
        String accessChallenge = PacketType.getPacketTypeName(PacketType.ACCESS_CHALLENGE);

        assertEquals("Access-Request", accessRequest);
        assertEquals("Access-Accept", accessAccept);
        assertEquals("Access-Reject", accessReject);
        assertEquals("Access-Challenge", accessChallenge);
    }

    @Test
    void getReservedPacketType() {
        String reserved = PacketType.getPacketTypeName(PacketType.RESERVED);
        assertEquals("Reserved", reserved);
    }

    @Test
    void getDefaultPacketType() {
        String unknown = PacketType.getPacketTypeName((byte) 256);
        assertEquals("Unknown (" + 0 + ")", unknown);
    }
}
