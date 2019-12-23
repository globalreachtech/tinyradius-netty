package org.tinyradius.server.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerPacketCodecTest {


    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final PacketEncoder packetEncoder = new PacketEncoder(dictionary);

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void inboundUnknownClientSecret() {
        final ServerPacketCodec serverPacketCodec = new ServerPacketCodec(packetEncoder, address -> null);
        final DatagramPacket datagram = new DatagramPacket(Unpooled.buffer(0), new InetSocketAddress(0));

        final List<Object> out = new ArrayList<>();
        serverPacketCodec.decode(ctx, datagram, out);

        assertEquals(0, out.size());
    }

    @Test
    void requestHandlerSuccess() throws RadiusException {
        final String secret = "mySecret";
        final RadiusPacket requestPacket = new RadiusPacket(dictionary, 3, 1).encodeRequest(secret);
        final InetSocketAddress localAddress = new InetSocketAddress(0);
        final InetSocketAddress remoteAddress = new InetSocketAddress(1);

        final ServerPacketCodec codec = new ServerPacketCodec(packetEncoder, address -> secret);

        final DatagramPacket request = packetEncoder.toDatagram(requestPacket, localAddress, remoteAddress);

        final ArrayList<Object> out1 = new ArrayList<>();
        codec.decode(ctx, request, out1);
        assertEquals(1, out1.size());

        final RequestCtx requestCtx = (RequestCtx) out1.get(0);
        assertEquals(remoteAddress, requestCtx.getEndpoint().getAddress());
        assertEquals(secret, requestCtx.getEndpoint().getSecret());
        assertEquals(requestPacket, requestCtx.getRequest());

        final RadiusPacket responsePacket = new RadiusPacket(dictionary, 4, 1)
                .encodeResponse(secret, requestPacket.getAuthenticator());

        final List<Object> out2 = new ArrayList<>();
        codec.encode(ctx, requestCtx.withResponse(responsePacket), out2);
        assertEquals(1, out2.size());

        final DatagramPacket response = (DatagramPacket) out2.get(0);
        assertArrayEquals(response.content().array(),
                packetEncoder.toDatagram(responsePacket, remoteAddress, localAddress).content().array());
    }

    @Test
    void exceptionDropPacket() throws RadiusException {
        final RadiusPacket request = new RadiusPacket(dictionary, 4, 1).encodeRequest("mySecret");

        final ServerPacketCodec codec = new ServerPacketCodec(packetEncoder, x -> "");

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, packetEncoder.toDatagram(request, new InetSocketAddress(0)), out1);
        assertEquals(0, out1.size());

        verify(ctx, never()).write(any());
        verify(ctx, never()).write(any(), any());
        verify(ctx, never()).writeAndFlush(any());
        verify(ctx, never()).writeAndFlush(any(), any());
    }
}