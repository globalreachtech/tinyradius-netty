package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.PacketType.*;
import static org.tinyradius.packet.PacketType.ACCOUNTING_REQUEST;

class RadiusPacketsTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createPacket() {
        GenericRadiusPacket accessRequest = RadiusPackets.create(dictionary, ACCESS_REQUEST, 1, null, Collections.emptyList());
        GenericRadiusPacket coaRequest = RadiusPackets.create(dictionary, COA_REQUEST, 2, null, Collections.emptyList());
        GenericRadiusPacket accountingRequest = RadiusPackets.create(dictionary, ACCOUNTING_REQUEST, 3, null, Collections.emptyList());

        assertEquals(ACCESS_REQUEST, accessRequest.getType());
        assertTrue(accessRequest instanceof AccessRequest); // don't care about subclass

        assertEquals(COA_REQUEST, coaRequest.getType());
        assertEquals(GenericRadiusPacket.class, coaRequest.getClass());

        assertEquals(ACCOUNTING_REQUEST, accountingRequest.getType());
        assertEquals(AccountingRequest.class, accountingRequest.getClass());
    }
}