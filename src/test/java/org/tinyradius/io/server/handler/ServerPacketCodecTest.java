package org.tinyradius.io.server.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.server.RequestCtx;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(MockitoExtension.class)
class ServerPacketCodecTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final InetSocketAddress address = new InetSocketAddress(0);

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void decodeUnknownSecret() throws RadiusPacketException {
        ServerPacketCodec codec = new ServerPacketCodec(dictionary, address -> null);
        List<Object> out = new ArrayList<>();

        // malformed packet drops before secret lookup
        DatagramPacket datagram = new DatagramPacket(Unpooled.buffer(0), address);
        codec.decode(ctx, datagram, out);
        assertEquals(0, out.size());

        // valid packet drops when secretProvider returns null
        var validEncodedReq = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList())
                .encodeRequest("mySecret");
        codec.decode(ctx, new DatagramPacket(validEncodedReq.toByteBuf(), address, address), out);
        assertEquals(0, out.size());
    }

    @Test
    void decodeExceptionDropPacket() throws RadiusPacketException {
        RadiusRequest request = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 1, null, Collections.emptyList()).encodeRequest("mySecret");
        DatagramPacket datagram = new DatagramPacket(request.toByteBuf(), address);
        ServerPacketCodec codec = new ServerPacketCodec(dictionary, addr -> "bad secret");

        List<Object> out1 = new ArrayList<>();
        codec.decode(ctx, datagram, out1);

        assertEquals(0, out1.size());
    }

    @Test
    void decodeEncodeSuccess() throws RadiusPacketException {
        String secret = "mySecret";
        String password = "myPw";
        ServerPacketCodec codec = new ServerPacketCodec(dictionary, address -> secret);
        when(ctx.channel()).thenReturn(mock(Channel.class));

        // create datagram
        RadiusRequest request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList())
                .addAttribute("User-Password", password)
                .encodeRequest(secret);
        InetSocketAddress remoteAddress = new InetSocketAddress(123);
        DatagramPacket datagram = new DatagramPacket(request.toByteBuf(), address, remoteAddress);

        // decode
        ArrayList<Object> out1 = new ArrayList<>();
        codec.decode(ctx, datagram, out1);
        assertEquals(1, out1.size());

        // check decoded
        RequestCtx requestCtx = (RequestCtx) out1.get(0);
        assertEquals(remoteAddress, requestCtx.getEndpoint().address());
        assertEquals(secret, requestCtx.getEndpoint().secret());
        AccessRequestPap decodedRequest = (AccessRequestPap) requestCtx.getRequest();
        assertEquals(password, decodedRequest.getPassword().get());
        assertEquals(1, decodedRequest.getId());

        RadiusResponse responsePacket = RadiusResponse.create(dictionary, (byte) 4, (byte) 1, null, Collections.emptyList());

        // encode
        List<Object> out2 = new ArrayList<>();
        codec.encode(ctx, requestCtx.withResponse(responsePacket), out2);
        assertEquals(1, out2.size());

        // check encoded
        DatagramPacket response = (DatagramPacket) out2.get(0);
        assertArrayEquals(response.content().copy().array(),
                new DatagramPacket(responsePacket.encodeResponse(secret, request.getAuthenticator()).toByteBuf(), remoteAddress, address).content().copy().array());
    }

    @Test
    void encodeNullRequestAuthAndException() throws RadiusPacketException {
        var secret = "mySecret";
        var codec = new ServerPacketCodec(dictionary, addr -> secret);
        var channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);

        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());
        var endpoint = new RadiusEndpoint(address, secret);
        var reqCtx = new RequestCtx(request, endpoint);

        var respPacket = RadiusResponse.create(dictionary, (byte) 2, (byte) 1, null, Collections.emptyList());
        var out = new ArrayList<>();
        codec.encode(ctx, reqCtx.withResponse(respPacket), out);
        assertEquals(1, out.size());

        // encode logs and drops packet on RadiusPacketException
        var badResp = mock(RadiusResponse.class);
        when(badResp.encodeResponse(anyString(), any()))
                .thenThrow(new RadiusPacketException("encode error"));
        var outErr = new ArrayList<>();
        codec.encode(ctx, reqCtx.withResponse(badResp), outErr);
        assertEquals(0, outErr.size());
    }
}