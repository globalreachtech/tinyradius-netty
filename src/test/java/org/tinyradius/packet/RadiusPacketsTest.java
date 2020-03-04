package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.util.RadiusPackets;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tinyradius.packet.util.PacketType.*;

class RadiusPacketsTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void createRequest() {
        RadiusRequest accessRequest = RadiusPackets.createRequest(dictionary, ACCESS_REQUEST, 1, null, Collections.emptyList());
        assertEquals(ACCESS_REQUEST, accessRequest.getType());
        assertTrue(accessRequest instanceof AccessRequest); // don't care about subclass

        RadiusRequest coaRequest = RadiusPackets.createRequest(dictionary, COA_REQUEST, 2, null, Collections.emptyList());
        assertEquals(COA_REQUEST, coaRequest.getType());
        assertEquals(RadiusRequest.class, coaRequest.getClass());

        RadiusRequest accountingRequest = RadiusPackets.createRequest(dictionary, ACCOUNTING_REQUEST, 3, null, Collections.emptyList());
        assertEquals(ACCOUNTING_REQUEST, accountingRequest.getType());
        assertEquals(AccountingRequest.class, accountingRequest.getClass());
    }

    @Test
    void createResponse() {
        RadiusResponse accessAccept = RadiusPackets.createResponse(dictionary, ACCESS_ACCEPT, 1, null, Collections.emptyList());
        assertEquals(ACCESS_ACCEPT, accessAccept.getType());
        assertTrue(accessAccept instanceof AccessResponse); // don't care about subclass

        RadiusResponse accountingResponse = RadiusPackets.createResponse(dictionary, ACCOUNTING_RESPONSE, 2, null, Collections.emptyList());
        assertEquals(ACCOUNTING_RESPONSE, accountingResponse.getType());
        assertEquals(RadiusResponse.class, accountingResponse.getClass());
    }
}