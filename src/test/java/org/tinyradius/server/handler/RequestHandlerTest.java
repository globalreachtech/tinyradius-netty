package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RequestHandlerTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final RequestHandler handler = new RequestHandler() {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {

        }
    };

    @Test
    void acceptInboundMessage() throws Exception {
        final RadiusPacket packet = new AccessRequest(dictionary, 1, null);

        final boolean b = handler.acceptInboundMessage(new RequestCtx(packet, null));
        assertFalse(b);
    }
}