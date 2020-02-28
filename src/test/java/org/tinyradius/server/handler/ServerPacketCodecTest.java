package org.tinyradius.server.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.tinyradius.packet.PacketCodec.toDatagram;

@ExtendWith(MockitoExtension.class)
class ServerPacketCodecTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final InetSocketAddress address = new InetSocketAddress(0);

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void decodeUnknownSecret() {
        final ServerPacketCodec codec = new ServerPacketCodec(dictionary, address -> null);
        final DatagramPacket datagram = new DatagramPacket(Unpooled.buffer(0), address);

        final List<Object> out = new ArrayList<>();
        codec.decode(ctx, datagram, out);

        assertEquals(0, out.size());
    }

    @Test
    void decodeExceptionDropPacket() throws RadiusPacketException {
        final BaseRadiusPacket request = new BaseRadiusPacket(dictionary, 4, 1).encodeRequest("mySecret");
        final DatagramPacket datagram = toDatagram(request, address);
        final ServerPacketCodec codec = new ServerPacketCodec(dictionary, x -> "bad secret");

        final List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, datagram, out1);

        assertEquals(0, out1.size());
    }

    @Test
    void decodeEncodeSuccess() throws RadiusPacketException {
        final String secret = "mySecret";
        final ServerPacketCodec codec = new ServerPacketCodec(dictionary, address -> secret);
        when(ctx.channel()).thenReturn(mock(Channel.class));

        // create datagram
        final BaseRadiusPacket requestPacket = new BaseRadiusPacket(dictionary, 3, 1).encodeRequest(secret);
        final InetSocketAddress remoteAddress = new InetSocketAddress(123);
        final DatagramPacket request = toDatagram(requestPacket, address, remoteAddress);

        // decode
        final ArrayList<Object> out1 = new ArrayList<>();
        codec.decode(ctx, request, out1);
        assertEquals(1, out1.size());

        // check decoded
        final RequestCtx requestCtx = (RequestCtx) out1.get(0);
        assertEquals(remoteAddress, requestCtx.getEndpoint().getAddress());
        assertEquals(secret, requestCtx.getEndpoint().getSecret());
        assertEquals(requestPacket, requestCtx.getRequest());

        final BaseRadiusPacket responsePacket = new BaseRadiusPacket(dictionary, 4, 1);

        // encode
        final List<Object> out2 = new ArrayList<>();
        codec.encode(ctx, requestCtx.withResponse(responsePacket), out2);
        assertEquals(1, out2.size());

        // check encoded
        final DatagramPacket response = (DatagramPacket) out2.get(0);
        assertArrayEquals(response.content().array(),
                toDatagram(responsePacket.encodeResponse(secret, requestPacket.getAuthenticator()), remoteAddress, address).content().array());
    }
}