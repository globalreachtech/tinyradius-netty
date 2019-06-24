package com.globalreachtech.tinyradius.test;

import com.globalreachtech.tinyradius.dictionary.DictionaryParser;
import com.globalreachtech.tinyradius.dictionary.MemoryDictionary;
import com.globalreachtech.tinyradius.dictionary.WritableDictionary;
import com.globalreachtech.tinyradius.packet.AccessRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.server.*;
import com.globalreachtech.tinyradius.util.RadiusException;
import com.globalreachtech.tinyradius.util.SecretProvider;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.io.FileInputStream;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.ACCESS_ACCEPT;

/**
 * Test server which terminates after 30 s.
 * Knows only the client "localhost" with secret "testing123" and
 * the user "mw" with the password "test".
 * <p>
 * TestServer can answer both to Access-Request and Access-Response
 * packets with Access-Accept/Reject or Accounting-Response, respectively.
 */
public class TestServer {

    public static void main(String[] args) throws Exception {

        WritableDictionary dictionary = new MemoryDictionary();
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"), dictionary);

        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

        final Timer timer = new HashedWheelTimer();
        final Deduplicator deduplicator = new DefaultDeduplicator(timer, 30000);

        final SecretProvider secretProvider = remote ->
                remote.getAddress().getHostAddress().equals("127.0.0.1") ? "testing123" : null;

        final AuthHandler authHandler = new AuthHandler(dictionary, deduplicator, timer, secretProvider) {
            @Override
            public String getUserPassword(String userName) {
                return userName.equals("test") ? "password" : null;
            }

            // Adds an attribute to the Access-Accept packet
            @Override
            public Promise<RadiusPacket> accessRequestReceived(EventExecutor eventExecutor, AccessRequest accessRequest) throws RadiusException {
                System.out.println("Received Access-Request:\n" + accessRequest);
                final Promise<RadiusPacket> promise = eventExecutor.newPromise();
                super.accessRequestReceived(eventExecutor, accessRequest).addListener((Future<RadiusPacket> f) -> {
                    final RadiusPacket packet = f.getNow();
                    if (packet == null) {
                        System.out.println("Ignore packet.");
                        promise.tryFailure(f.cause());
                    } else {
                        if (packet.getPacketType() == ACCESS_ACCEPT)
                            packet.addAttribute("Reply-Message", "Welcome " + accessRequest.getUserName() + "!");
                        System.out.println("Answer:\n" + packet);
                        promise.trySuccess(packet);
                    }
                });

                return promise;
            }
        };

        final AcctHandler acctHandler = new AcctHandler(dictionary, deduplicator, timer, secretProvider);

        final RadiusServer<NioDatagramChannel> server = new RadiusServer<>(
                eventLoopGroup,
                new ReflectiveChannelFactory<>(NioDatagramChannel.class),
                null,
                authHandler, acctHandler,
                11812, 11813);

        final Future<Void> future = server.start();
        future.addListener(future1 -> {
            if (future1.isSuccess()) {
                System.out.println("Server started");
            } else {
                System.out.println("Failed to start server: " + future1.cause());
                server.stop();
                eventLoopGroup.shutdownGracefully();
            }
        });

        System.in.read();

        server.stop();

        eventLoopGroup.shutdownGracefully()
                .awaitUninterruptibly();
    }

}
