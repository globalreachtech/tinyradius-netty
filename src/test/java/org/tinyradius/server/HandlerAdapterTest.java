package org.tinyradius.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandlerAdapterTest {

    private final HashedWheelTimer timer = new HashedWheelTimer();
    private final ChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final PacketEncoder packetEncoder = new PacketEncoder(dictionary);

    @Mock
    private RequestHandler<RadiusPacket, SecretProvider> requestHandler;

    @Mock
    private ChannelHandlerContext channelHandlerContext;

    @Test
    void unknownClient() {
        final HandlerAdapter<AccountingRequest, SecretProvider> handlerAdapter = new HandlerAdapter<>(
                address -> null, AccountingRequest.class);
        final DatagramPacket datagramPacket = new DatagramPacket(Unpooled.buffer(0), new InetSocketAddress(0));

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handlerAdapter.handleRequest(null, datagramPacket));

        assertTrue(exception.getMessage().toLowerCase().contains("unknown client"));
    }

    @Test
    void unhandledPacketType() throws RadiusException {
        final String secret = "mySecret";
        final RadiusPacket radiusPacket = new AccountingRequest(dictionary, 1, null).encodeRequest(secret);
        final DatagramPacket datagramPacket = packetEncoder.toDatagram(radiusPacket, new InetSocketAddress(0));

        final HandlerAdapter<AccessRequest, SecretProvider> handlerAdapter = new HandlerAdapter<>(
                address -> secret, AccessRequest.class);

        final RadiusException exception = assertThrows(RadiusException.class,
                () -> handlerAdapter.handleRequest(null, datagramPacket));

        assertTrue(exception.getMessage().toLowerCase().contains("handler only accepts accessrequest"));
    }

    @Test
    void requestHandlerErrorPropagates() throws RadiusException, InterruptedException {
        final String secret = "mySecret";
        final RadiusPacket radiusPacket = new RadiusPacket(dictionary, 3, 1).encodeRequest(secret);
        final DatagramPacket request = packetEncoder.toDatagram(radiusPacket, new InetSocketAddress(0));
        final MockRequestHandler mockRequestHandler = new MockRequestHandler();

        final HandlerAdapter<RadiusPacket, SecretProvider> handlerAdapter = new HandlerAdapter<>(
                address -> secret, RadiusPacket.class);

        final Future<DatagramPacket> response = handlerAdapter.handleRequest(genChannel(), request);
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

        final HandlerAdapter<RadiusPacket, SecretProvider> handlerAdapter = new HandlerAdapter<>(
                address -> secret, RadiusPacket.class);

        final Future<DatagramPacket> response = handlerAdapter.handleRequest(genChannel(), request);
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
    void exceptionDropPacket() throws RadiusException {
        final RadiusPacket request = new RadiusPacket(dictionary, 4, 1).encodeRequest("mySecret");

        final HandlerAdapter<RadiusPacket, SecretProvider> handlerWrapper = new HandlerAdapter<>(
                x -> "", RadiusPacket.class);

        when(channelHandlerContext.channel()).thenReturn(genChannel());

        handlerWrapper.channelRead0(channelHandlerContext,
                packetEncoder.toDatagram(request, new InetSocketAddress(0)));

        verify(channelHandlerContext, never()).write(any());
        verify(channelHandlerContext, never()).write(any(), any());
        verify(channelHandlerContext, never()).writeAndFlush(any());
        verify(channelHandlerContext, never()).writeAndFlush(any(), any());
    }

    @Test
    void handlerSuccessReturnPacket() throws RadiusException {
        final String secret = "mySecret";
        final RadiusPacket request = new RadiusPacket(dictionary, 4, 1).encodeRequest(secret);
        final RadiusPacket response = new RadiusPacket(dictionary, 5, 1)
                .encodeResponse("mySecret", request.getAuthenticator());

        when(requestHandler.handlePacket(any(), any(), any(), any()))
                .thenReturn(eventLoopGroup.next().<RadiusPacket>newPromise().setSuccess(response));

        final HandlerAdapter<RadiusPacket, SecretProvider> handlerWrapper = new HandlerAdapter<>(
                x -> secret, RadiusPacket.class);

        when(channelHandlerContext.channel()).thenReturn(genChannel());

        handlerWrapper.channelRead0(channelHandlerContext,
                packetEncoder.toDatagram(request, new InetSocketAddress(0), new InetSocketAddress(1)));

        final ArgumentCaptor<DatagramPacket> captor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(channelHandlerContext).writeAndFlush(captor.capture());

        final RadiusPacket expected = packetEncoder.fromDatagram(captor.getValue());
        assertEquals(expected.getIdentifier(), response.getIdentifier());
        assertEquals(expected.getType(), response.getType());
        assertArrayEquals(expected.getAuthenticator(), response.getAuthenticator());
    }

    private static class MockRequestHandler implements RequestHandler<RadiusPacket, SecretProvider> {

        private Promise<RadiusPacket> promise;

        @Override
        public Promise<RadiusPacket> handlePacket(Channel channel, RadiusPacket request, InetSocketAddress remoteAddress, SecretProvider secretProvider) {
            return promise = channel.eventLoop().newPromise();
        }
    }

    private Channel genChannel() {
        final DatagramChannel datagramChannel = channelFactory.newChannel();
        eventLoopGroup.register(datagramChannel).syncUninterruptibly();
        return datagramChannel;
    }
}