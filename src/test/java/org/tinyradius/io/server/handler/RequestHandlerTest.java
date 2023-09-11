package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.AccessRequestNoAuth;
import org.tinyradius.core.packet.request.AccountingRequest;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.io.server.RequestCtx;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

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

        final AccountingRequest acctRequest = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 1, null, Collections.emptyList());
        final AccessRequestNoAuth authRequest = (AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());

        assertFalse(requestHandler.acceptInboundMessage(new RequestCtx(acctRequest, null)));
        assertTrue(requestHandler.acceptInboundMessage(new RequestCtx(authRequest, null)));
    }
}