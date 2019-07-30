package org.tinyradius.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPacketEncoder;
import org.tinyradius.server.handler.RequestHandler;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandlerAdapterTest {

    private static final HashedWheelTimer timer = new HashedWheelTimer();

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void unknownClient() {
        final HandlerAdapter<AccountingRequest> handlerAdapter = new HandlerAdapter<>(
                dictionary, null, timer, address -> null, AccountingRequest.class);
        final DatagramPacket datagramPacket = new DatagramPacket(Unpooled.buffer(0), new InetSocketAddress(0));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handlerAdapter.handleRequest(null, datagramPacket));

        assertTrue(exception.getMessage().toLowerCase().contains("unknown client"));

        datagramPacket.release();
    }

    @Test
    void unhandledPacketType() throws RadiusException {
        final String secret = "mySecret";
        final RadiusPacket radiusPacket = new AccountingRequest(dictionary, 1, null).encodeRequest(secret);
        final DatagramPacket datagramPacket = RadiusPacketEncoder.toDatagram(radiusPacket, new InetSocketAddress(0));

        final HandlerAdapter<AccessRequest> handlerAdapter = new HandlerAdapter<>(
                dictionary, null, timer, address -> secret, AccessRequest.class);

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handlerAdapter.handleRequest(null, datagramPacket));

        assertTrue(exception.getMessage().toLowerCase().contains("handler only accepts accessrequest"));

        datagramPacket.release();
    }

    private static class MockRequestHandler implements RequestHandler<RadiusPacket> {

        @Override
        public Promise<RadiusPacket> handlePacket(Channel channel, RadiusPacket packet, InetSocketAddress remoteAddress, String sharedSecret) {
            return channel.eventLoop().newPromise();
        }
    }
}