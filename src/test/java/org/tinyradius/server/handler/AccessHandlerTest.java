package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.PacketType.*;

@ExtendWith(MockitoExtension.class)
class AccessHandlerTest {

    private static final NioEventLoopGroup eventExecutors = new NioEventLoopGroup(4);

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();

    @Mock
    private ChannelHandlerContext ctx;

    @AfterAll
    static void afterAll() {
        eventExecutors.shutdownGracefully().syncUninterruptibly();
    }

    private final AccessHandler authHandler = new AccessHandler() {
        @Override
        public String getUserPassword(String userName) {
            return userName + "-pw";
        }
    };

    @Test
    void accessAccept() {
        final int id = random.nextInt(256);

        final NioDatagramChannel datagramChannel = new NioDatagramChannel();
        eventExecutors.register(datagramChannel).syncUninterruptibly();

        final AccessRequest request = new AccessRequest(dictionary, id, null, "user1", "user1-pw");
        request.addAttribute(createAttribute(dictionary, -1, 33, "state1".getBytes(UTF_8)));
        request.addAttribute(createAttribute(dictionary, -1, 33, "state2".getBytes(UTF_8)));

        assertEquals(ACCESS_REQUEST, request.getType());
        assertEquals(Arrays.asList("user1", "state1", "state2"), request.getAttributes().stream()
                .map(RadiusAttribute::getValue)
                .map(String::new)
                .collect(Collectors.toList()));

        final RadiusPacket response = authHandler.channelRead0(datagramChannel, request, null, a -> "");

        assertEquals(id, response.getIdentifier());
        assertEquals(ACCESS_ACCEPT, response.getType());
        assertEquals(Arrays.asList("state1", "state2"), response.getAttributes().stream()
                .map(RadiusAttribute::getValue)
                .map(String::new)
                .collect(Collectors.toList()));
    }

    @Test
    void accessReject() {
        final int id = random.nextInt(256);

        final NioDatagramChannel datagramChannel = new NioDatagramChannel();
        eventExecutors.register(datagramChannel).syncUninterruptibly();

        final AccessRequest request = new AccessRequest(dictionary, id, null, "user1", "user1-badPw");
        request.addAttribute(createAttribute(dictionary, -1, 33, "state1".getBytes(UTF_8)));
        request.addAttribute(createAttribute(dictionary, -1, 33, "state2".getBytes(UTF_8)));

        assertEquals(ACCESS_REQUEST, request.getType());
        assertEquals(Arrays.asList("user1", "state1", "state2"), request.getAttributes().stream()
                .map(RadiusAttribute::getValue)
                .map(String::new)
                .collect(Collectors.toList()));

        final RadiusPacket response = authHandler.handlePacket(datagramChannel, request, null, a -> "")
                .syncUninterruptibly().getNow();

        assertEquals(id, response.getIdentifier());
        assertEquals(ACCESS_REJECT, response.getType());
        assertEquals(Arrays.asList("state1", "state2"), response.getAttributes().stream()
                .map(RadiusAttribute::getValue)
                .map(String::new)
                .collect(Collectors.toList()));
    }
}