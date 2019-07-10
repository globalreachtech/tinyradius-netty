package org.tinyradius.server;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.packet.PacketType.ACCOUNTING_REQUEST;
import static org.tinyradius.packet.PacketType.ACCOUNTING_RESPONSE;

class AcctHandlerTest {

    private Dictionary dictionary = DefaultDictionary.INSTANCE;
    private SecureRandom random = new SecureRandom();

    @Test
    void handlePacket() {
        final int id = random.nextInt(256);

        final NioDatagramChannel datagramChannel = new NioDatagramChannel();
        new NioEventLoopGroup(4).register(datagramChannel).syncUninterruptibly();

        final AccountingRequest request = new AccountingRequest(dictionary, id, null);
        request.addAttribute(new RadiusAttribute(dictionary, -1, 33, "state1".getBytes(UTF_8)));
        request.addAttribute(new RadiusAttribute(dictionary, -1, 33, "state2".getBytes(UTF_8)));
        assertEquals(ACCOUNTING_REQUEST, request.getPacketType());
        assertEquals(Arrays.asList("state1", "state2"), request.getAttributes().stream()
                .map(RadiusAttribute::getData)
                .map(String::new)
                .collect(Collectors.toList()));

        final RadiusPacket response = new AcctHandler().handlePacket(datagramChannel, request, null, "")
                .syncUninterruptibly().getNow();

        assertEquals(id, response.getPacketIdentifier());
        assertEquals(ACCOUNTING_RESPONSE, response.getPacketType());
        assertEquals(Arrays.asList("state1", "state2"), response.getAttributes().stream()
                .map(RadiusAttribute::getData)
                .map(String::new)
                .collect(Collectors.toList()));
    }
}