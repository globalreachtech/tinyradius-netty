package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.AccessRequest;
import org.tinyradius.packet.request.AccessRequestNoAuth;
import org.tinyradius.packet.request.AccountingRequest;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.server.RequestCtx;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestHandlerTest {

    final private static Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void acceptRejectMsg() throws Exception {
        final RequestHandler requestHandler = new RequestHandler() {

            @Override
            protected Class<? extends RadiusRequest> acceptedPacketType() {
                return AccessRequest.class;
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
            }
        };

        final RadiusRequest acctRequest = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        final RadiusRequest authRequest = new AccessRequestNoAuth(dictionary, (byte) 1, null, Collections.emptyList());

        assertFalse(requestHandler.acceptInboundMessage(new RequestCtx(acctRequest, null)));
        assertTrue(requestHandler.acceptInboundMessage(new RequestCtx(authRequest, null)));
    }
}