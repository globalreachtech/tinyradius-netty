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
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.handler.RequestHandler;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class HandlerAdapterTest {

    private static final HashedWheelTimer timer = new HashedWheelTimer();
    private final ChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);
    private static final NioEventLoopGroup eventExecutors = new NioEventLoopGroup(4);

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final PacketEncoder packetEncoder = new PacketEncoder(dictionary);

    @AfterAll
    static void afterAll() {
        eventExecutors.shutdownGracefully().syncUninterruptibly();
        timer.stop();
    }

    @Test
    void unknownClient() {
        final HandlerAdapter<AccountingRequest> handlerAdapter = new HandlerAdapter<>(
                packetEncoder, null, timer, address -> null, AccountingRequest.class);
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
        final DatagramPacket datagramPacket = packetEncoder.toDatagram(radiusPacket, new InetSocketAddress(0));

        final HandlerAdapter<AccessRequest> handlerAdapter = new HandlerAdapter<>(
                packetEncoder, null, timer, address -> secret, AccessRequest.class);

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handlerAdapter.handleRequest(null, datagramPacket));

        assertTrue(exception.getMessage().toLowerCase().contains("handler only accepts accessrequest"));

        datagramPacket.release();
    }

    @Test
    void requestHandlerError() throws RadiusException, InterruptedException {
        final String secret = "mySecret";
        final RadiusPacket radiusPacket = new RadiusPacket(dictionary, 3, 1).encodeRequest(secret);
        final DatagramPacket request = packetEncoder.toDatagram(radiusPacket, new InetSocketAddress(0));
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();

        final HandlerAdapter<RadiusPacket> handlerAdapter = new HandlerAdapter<>(
                packetEncoder, mockRequestHandler, timer, address -> secret, RadiusPacket.class);

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

        final DatagramPacket request = packetEncoder.toDatagram(requestPacket, serverAddress, clientAddress);
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();

        final HandlerAdapter<RadiusPacket> handlerAdapter = new HandlerAdapter<>(
                packetEncoder, mockRequestHandler, timer, address -> secret, RadiusPacket.class);

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
                packetEncoder.toDatagram(responsePacket, clientAddress, serverAddress).content().array());
    }

    @Test
    void lifecycleCommands() {
        final HashedWheelTimer timer = new HashedWheelTimer();
        final MockRequestHandler mockProxyRequestHandler = new MockRequestHandler();

        final HandlerAdapter proxyHandlerAdapter =
                new HandlerAdapter<>(packetEncoder, mockProxyRequestHandler, timer, a -> "mysecret", RadiusPacket.class);

        assertFalse(mockProxyRequestHandler.isStarted);

        proxyHandlerAdapter.start().syncUninterruptibly();
        assertTrue(mockProxyRequestHandler.isStarted);

        proxyHandlerAdapter.stop().syncUninterruptibly();
        assertFalse(mockProxyRequestHandler.isStarted);

        timer.stop();
    }

    private static class MockRequestHandler implements RequestHandler<RadiusPacket> {

        private Promise<RadiusPacket> promise;
        private boolean isStarted = false;

        @Override
        public Promise<RadiusPacket> handlePacket(Channel channel, RadiusPacket request, InetSocketAddress remoteAddress, String sharedSecret) {
            return promise = channel.eventLoop().newPromise();
        }


        @Override
        public Future<Void> start() {
            isStarted = true;
            return GlobalEventExecutor.INSTANCE.newSucceededFuture(null);
        }

        @Override
        public Future<Void> stop() {
            isStarted = false;
            return GlobalEventExecutor.INSTANCE.newSucceededFuture(null);
        }
    }
}