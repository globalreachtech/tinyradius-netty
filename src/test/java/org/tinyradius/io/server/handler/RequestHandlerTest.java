package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandlerContext;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.server.RequestCtx;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

class RequestHandlerTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void acceptRejectMsg() throws Exception {
        var requestHandler = new RequestHandler() {

            @Override
            protected @NonNull Class<? extends RadiusRequest> acceptedPacketType() {
                return AccessRequest.class;
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
            }
        };

        var acctRequest = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 1, null, Collections.emptyList());
        var authRequest = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());

        assertFalse(requestHandler.acceptInboundMessage("not a RequestCtx"));
        assertFalse(requestHandler.acceptInboundMessage(new RequestCtx(acctRequest, new RadiusEndpoint(null, null))));
        assertTrue(requestHandler.acceptInboundMessage(new RequestCtx(authRequest, new RadiusEndpoint(null, null))));
    }
}