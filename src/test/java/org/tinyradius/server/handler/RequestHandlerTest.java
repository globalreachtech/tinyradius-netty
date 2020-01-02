package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestHandlerTest {

    private final RadiusPacket accessRequest = new AccessRequest(DefaultDictionary.INSTANCE, 1, null);

    @Test
    void acceptMsg() throws Exception {

        final boolean b = new RequestHandler() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
            }
        }.acceptInboundMessage(new RequestCtx(accessRequest, null));
        assertTrue(b);
    }

    @Test
    void rejectMsg() throws Exception {

        final boolean b = new RequestHandler() {
            @Override
            protected Class<? extends RadiusPacket> acceptedPacketType() {
                return AccountingRequest.class;
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
            }
        }.acceptInboundMessage(new RequestCtx(accessRequest, null));
        assertFalse(b);
    }
}