package org.tinyradius.app;

import io.netty.channel.ChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.tinyradius.attribute.IntegerAttribute;
import org.tinyradius.dictionary.*;
import org.tinyradius.netty.*;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

import java.io.FileInputStream;
import java.net.InetSocketAddress;

public class Main {

    private static Log logger = LogFactory.getLog(Main.class);

    public static void main123123(String[] args) throws Exception {

        Logger.getRootLogger().setLevel(Level.DEBUG);

        RadiusQueueImpl queue = new RadiusQueueImpl();
        RadiusEndpoint endpoint = new RadiusEndpoint(
                new InetSocketAddress("127.0.0.1", 1812), "testing123");

        for (int i = 0; i < 100; i++) {
            RadiusPacket packet = RadiusPacket.createRadiusPacket(RadiusPacket.ACCESS_REQUEST);
            RadiusQueueEntry ctx = queue.queue(new RadiusRequestContextImpl(packet, endpoint));
        }

        queue.doit();
        System.out.println("======================================");
        queue.doit();
    }

    public static void main(String[] args) throws Exception {

        BasicConfigurator.configure();

        Logger.getRootLogger().setLevel(Level.DEBUG);

        NioEventLoopGroup eventGroup = new NioEventLoopGroup(4);

        Dictionary dictionary = new MemoryDictionary();
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"),
                (WritableDictionary) dictionary);

        RadiusClient<NioDatagramChannel> client = new RadiusClient<NioDatagramChannel>(dictionary,
                eventGroup, new NioDatagramChannelFactory());

        RadiusEndpoint endpoint = new RadiusEndpoint(
                new InetSocketAddress("127.0.0.1", 1812), "testing123");

        while (!eventGroup.isTerminated()) {

            try {
                AccessRequest request = new AccessRequest("test", "password");

                request.setDictionary(dictionary);
                request.addAttribute(new IntegerAttribute(6, 1));

                final RadiusRequestFuture future =
                        client.communicate(request, endpoint);

                future.addListener(new GenericFutureListener<RadiusRequestFuture>() {
                    public void operationComplete(RadiusRequestFuture future) throws Exception {
                        RadiusRequestContext ctx = future.context();
                        if (future.isSuccess()) {
                            logger.info(String.format("Request %s succeeded: %s",
                                    ctx.request().getPacketIdentifier(), ctx.response().toString()));
                        } else {
                            logger.info(String.format("Request %s failed: %s", ctx.request().getPacketIdentifier(),
                                    future.cause().toString()));
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }

            Thread.sleep(10);
        }
    }

    private static class NioDatagramChannelFactory implements ChannelFactory<NioDatagramChannel> {
        public NioDatagramChannel newChannel() {
             return new NioDatagramChannel();
        }
    }

    private static class RadiusRequestContextImpl implements RadiusRequestContext {

        private RadiusPacket request;
        private RadiusPacket response;
        private RadiusEndpoint endpoint;

        public RadiusRequestContextImpl(RadiusPacket request, RadiusEndpoint endpoint) {
            if (request == null)
                throw new NullPointerException("request cannot be null");
            if (endpoint == null)
                throw new NullPointerException("endpoint cannot be null");
            this.request = request;
            this.endpoint = endpoint;
        }

        public RadiusPacket request() {
            return request;
        }

        public RadiusPacket response() {
            return response;
        }

        public void setResponse(RadiusPacket response) {
            this.response = response;
        }

        public RadiusEndpoint endpoint() {
            return endpoint;
        }
    }
}
