package org.tinyradius.server.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerPacketCodecTest {

    private final HashedWheelTimer timer = new HashedWheelTimer();

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final PacketEncoder packetEncoder = new PacketEncoder(dictionary);

    @Mock
    private ChannelHandler channelHandler;

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void inboundUnknownClientSecret() throws Exception {
        final ServerPacketCodec serverPacketCodec = new ServerPacketCodec(packetEncoder, address -> null);
        final DatagramPacket datagram = new DatagramPacket(Unpooled.buffer(0), new InetSocketAddress(0));

        final ArrayList<Object> out = new ArrayList<>();

        serverPacketCodec.decode(ctx, datagram, out);

        assertEquals(0, out.size());
    }

    @Test
    void requestHandlerSuccess() throws RadiusException, InterruptedException {
        final String secret = "mySecret";
        final RadiusPacket requestPacket = new RadiusPacket(dictionary, 3, 1).encodeRequest(secret);
        final InetSocketAddress serverAddress = new InetSocketAddress(0);
        final InetSocketAddress clientAddress = new InetSocketAddress(1);

        final DatagramPacket request = packetEncoder.toDatagram(requestPacket, serverAddress, clientAddress);

        final ServerPacketCodec codec = new ServerPacketCodec(packetEncoder, address -> secret);

        final Future<DatagramPacket> response = codec.decode(genChannel(), request);
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

        final ServerPacketCodec codec = new ServerPacketCodec(packetEncoder, x -> "");

        when(ctx.channel()).thenReturn(genChannel());

        codec.decode(ctx,
                packetEncoder.toDatagram(request, new InetSocketAddress(0)));

        verify(ctx, never()).write(any());
        verify(ctx, never()).write(any(), any());
        verify(ctx, never()).writeAndFlush(any());
        verify(ctx, never()).writeAndFlush(any(), any());
    }

    @Test
    void handlerSuccessReturnPacket() throws RadiusException {
        final String secret = "mySecret";
        final RadiusPacket request = new RadiusPacket(dictionary, 4, 1).encodeRequest(secret);
        final RadiusPacket response = new RadiusPacket(dictionary, 5, 1)
                .encodeResponse("mySecret", request.getAuthenticator());

        when(channelHandler.handlePacket(any(), any(), any(), any()))
                .thenReturn(eventLoopGroup.next().<RadiusPacket>newPromise().setSuccess(response));

        final ServerPacketCodec handlerWrapper = new ServerPacketCodec(packetEncoder, x -> secret);

        when(ctx.channel()).thenReturn(genChannel());

        handlerWrapper.decode(ctx,
                packetEncoder.toDatagram(request, new InetSocketAddress(0), new InetSocketAddress(1)));

        final ArgumentCaptor<DatagramPacket> captor = ArgumentCaptor.forClass(DatagramPacket.class);
        verify(ctx).writeAndFlush(captor.capture());

        final RadiusPacket expected = packetEncoder.fromDatagram(captor.getValue());
        assertEquals(expected.getIdentifier(), response.getIdentifier());
        assertEquals(expected.getType(), response.getType());
        assertArrayEquals(expected.getAuthenticator(), response.getAuthenticator());
    }
}