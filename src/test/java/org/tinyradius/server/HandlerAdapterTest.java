package org.tinyradius.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
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

import static org.junit.jupiter.api.Assertions.*;

class HandlerAdapterTest {

    private static final HashedWheelTimer timer = new HashedWheelTimer();
    private final ChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);
    private static final NioEventLoopGroup eventExecutors = new NioEventLoopGroup(4);

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @AfterAll
    static void afterAll() {
        eventExecutors.shutdownGracefully().syncUninterruptibly();
        timer.stop();
    }

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

    @Test
    void requestHandlerError() throws RadiusException, InterruptedException {
        final String secret = "mySecret";
        final RadiusPacket radiusPacket = new RadiusPacket(dictionary, 3, 1).encodeRequest(secret);
        final DatagramPacket request = RadiusPacketEncoder.toDatagram(radiusPacket, new InetSocketAddress(0));
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();

        final HandlerAdapter<RadiusPacket> handlerAdapter = new HandlerAdapter<>(
                dictionary, mockRequestHandler, timer, address -> secret, RadiusPacket.class);

        final NioDatagramChannel channel = channelFactory.newChannel();
        eventExecutors.register(channel).syncUninterruptibly();

        final Future<DatagramPacket> response = handlerAdapter.handleRequest(channel, request);
        assertFalse(response.isDone());

        final Exception exception = new Exception("foobar");

        mockRequestHandler.promise.tryFailure(exception);
        Thread.sleep(500);

        assertTrue(response.isDone());
        assertFalse(response.isSuccess());

        assertSame(exception, response.cause());
    }

    @Test
    void requestHandlerSuccess() throws RadiusException, InterruptedException {
        final String secret = "mySecret";
        final RadiusPacket requestPacket = new RadiusPacket(dictionary, 3, 1).encodeRequest(secret);
        final InetSocketAddress serverAddress = new InetSocketAddress(0);
        final InetSocketAddress clientAddress = new InetSocketAddress(1);

        final DatagramPacket request = RadiusPacketEncoder.toDatagram(requestPacket, serverAddress, clientAddress);
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();

        final HandlerAdapter<RadiusPacket> handlerAdapter = new HandlerAdapter<>(
                dictionary, mockRequestHandler, timer, address -> secret, RadiusPacket.class);

        final NioDatagramChannel channel = channelFactory.newChannel();
        eventExecutors.register(channel).syncUninterruptibly();

        final Future<DatagramPacket> response = handlerAdapter.handleRequest(channel, request);
        assertFalse(response.isDone());

        final RadiusPacket responsePacket = new RadiusPacket(dictionary, 4, 1)
                .encodeResponse(secret, requestPacket.getAuthenticator());
        mockRequestHandler.promise.trySuccess(responsePacket);
        Thread.sleep(500);

        assertTrue(response.isSuccess());
        assertArrayEquals(response.getNow().content().array(),
                RadiusPacketEncoder.toDatagram(responsePacket, clientAddress, serverAddress).content().array());
    }

    private static class MockRequestHandler implements RequestHandler<RadiusPacket> {

        private Promise<RadiusPacket> promise;

        @Override
        public Promise<RadiusPacket> handlePacket(Channel channel, RadiusPacket packet, InetSocketAddress remoteAddress, String sharedSecret) {
            return promise = channel.eventLoop().newPromise();
        }
    }
}