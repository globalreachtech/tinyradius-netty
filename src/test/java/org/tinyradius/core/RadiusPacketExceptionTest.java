package org.tinyradius.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RadiusPacketExceptionTest {

    @Test
    void testRadiusPacketException() {
        RadiusPacketException e1 = new RadiusPacketException("message");
        assertEquals("message", e1.getMessage());

        RadiusPacketException e2 = new RadiusPacketException("message", new RuntimeException("cause"));
        assertEquals("message", e2.getMessage());
        assertEquals("cause", e2.getCause().getMessage());
    }
}
