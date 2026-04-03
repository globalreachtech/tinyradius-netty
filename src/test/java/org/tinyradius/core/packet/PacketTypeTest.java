package org.tinyradius.core.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void testFromCode() {
        assertEquals(ACCESS_REQUEST, PacketType.fromCode((byte) 1));
        assertEquals(ACCESS_ACCEPT, PacketType.fromCode((byte) 2));
        assertThrows(IllegalArgumentException.class, () -> PacketType.fromCode((byte) 0));
    }
}
