package org.tinyradius.app;

import io.netty.channel.ChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static Log logger = LogFactory.getLog(Main.class);

    public static void main12312399(String[] args) throws Exception {

        BasicConfigurator.configure();

        Logger.getRootLogger().setLevel(Level.DEBUG);

        Dictionary dictionary = new MemoryDictionary();
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"),
                (WritableDictionary) dictionary);

        NioEventLoopGroup eventGroup = new NioEventLoopGroup(4);
        RadiusClient<NioDatagramChannel> client = new RadiusClient<NioDatagramChannel>(dictionary,
                eventGroup, new NioDatagramChannelFactory(), new HashedWheelTimer());


        AccessRequest request1 = new AccessRequest("test", "password");
        request1.setDictionary(dictionary);
        request1.addAttribute(new IntegerAttribute(6, 1));

        RadiusEndpoint endpoint1 = new RadiusEndpoint(
                new InetSocketAddress("127.0.0.1", 1812), "testing123");

        RadiusRequestFuture future1 =
                client.communicate(request1, endpoint1, 3000, TimeUnit.MILLISECONDS);

        future1.addListener(new GenericFutureListener<RadiusRequestFuture>() {
            public void operationComplete(RadiusRequestFuture future) throws Exception {
                RadiusRequestContext ctx = future.context();
                if (future.isSuccess()) {
                    logger.info(String.format("Request %s succeeded, took %d.%d ms: %s",
                            ctx.request().getPacketIdentifier(),
                            ctx.responseTime() / 1000000,
                            ctx.responseTime() % 1000000 / 10000, ctx.response().toString()));
                } else {
                    logger.info(String.format("Request %s failed: %s", ctx.request().getPacketIdentifier(),
                            future.cause().toString()));
                }
            }
        });

        Thread.sleep(1000);

        AccessRequest request2 = new AccessRequest("test", "password");
        request2.setDictionary(dictionary);
        request2.addAttribute(new IntegerAttribute(6, 1));

        RadiusEndpoint endpoint2 = new RadiusEndpoint(
                new InetSocketAddress("127.0.0.1", 1813), "testing123");

        final RadiusRequestFuture future2 =
                client.communicate(request2, endpoint2, 1000, TimeUnit.MILLISECONDS);

        future2.addListener(new GenericFutureListener<RadiusRequestFuture>() {
            public void operationComplete(RadiusRequestFuture future) throws Exception {
                RadiusRequestContext ctx = future.context();
                if (future.isSuccess()) {
                    if (logger.isInfoEnabled())
                        logger.info(String.format("Request %s succeeded, took %d.%d ms: %s",
                            ctx.request().getPacketIdentifier(),
                            ctx.responseTime() / 1000000,
                            ctx.responseTime() % 1000000 / 10000, ctx.response().toString()));
                } else {
                    if (logger.isInfoEnabled())
                        logger.info(String.format("Request %s failed: %s", ctx.request().getPacketIdentifier(),
                            future.cause().toString()));
                }
            }
        });
    }

    public static void main1231232388(String[] args) throws Exception {

        BasicConfigurator.configure();

        Logger.getRootLogger().setLevel(Level.DEBUG);

        Dictionary dictionary = new MemoryDictionary();
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"),
                (WritableDictionary) dictionary);

        NioEventLoopGroup eventGroup = new NioEventLoopGroup(4);
        RadiusClient<NioDatagramChannel> client = new RadiusClient<NioDatagramChannel>(dictionary,
                eventGroup, new NioDatagramChannelFactory(), new HashedWheelTimer());

        AccessRequest request = new AccessRequest("test", "password");
        request.setDictionary(dictionary);
        request.addAttribute(new IntegerAttribute(6, 1));

        RadiusEndpoint endpoint1 = new RadiusEndpoint(
                new InetSocketAddress("127.0.0.1", 1812), "testing123");

        long start = System.currentTimeMillis();

        for (int i = 0; i < 10000; i++) {
            RadiusRequestFuture future =
                    client.communicate(request, endpoint1, 1000, TimeUnit.MILLISECONDS);
        }

        System.out.println("Took " + (System.currentTimeMillis() - start) + " ms");
    }

    public static void main(String[] args) throws Exception {

        //BasicConfigurator.configure();
        //Logger.getRootLogger().setLevel(Level.DEBUG);

        final Dictionary dictionary = new MemoryDictionary();
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"),
                (WritableDictionary) dictionary);

        final NioEventLoopGroup eventGroup = new NioEventLoopGroup(8);
        final RadiusClient<NioDatagramChannel> client = new RadiusClient<NioDatagramChannel>(dictionary,
                eventGroup, new NioDatagramChannelFactory(), new HashedWheelTimer());

        final RadiusEndpoint[] endpoints = new RadiusEndpoint[5];

        endpoints[0] = new RadiusEndpoint(new InetSocketAddress("127.0.0.1", 1812), "testing123");
        endpoints[1] = new RadiusEndpoint(new InetSocketAddress("127.0.0.1", 1812), "testing123");
        endpoints[2] = new RadiusEndpoint(new InetSocketAddress("127.0.0.1", 1812), "testing123");
        endpoints[3] = new RadiusEndpoint(new InetSocketAddress("127.0.0.1", 1812), "testing123");
        endpoints[4] = new RadiusEndpoint(new InetSocketAddress("127.0.0.1", 1812), "testing123");

        final RadiusEndpoint endpoint =
                new RadiusEndpoint(new InetSocketAddress("127.0.0.1", 1812), "testing123");

        final AtomicInteger requests = new AtomicInteger(0);
        final AtomicInteger completed = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);

/*
        final ScheduledFuture<?> scheduledFuture =
                eventGroup.next().scheduleAtFixedRate(new Runnable() {
            public void run() {

                //for (int i = 0; i < 6; i++) {

                    AccessRequest request = new AccessRequest("test", "password");
                    request.setDictionary(dictionary);
                    request.addAttribute(new IntegerAttribute(6, 1));

                    RadiusRequestFuture future =
                            client.communicate(request, endpoints[requests.getAndIncrement() % endpoints.length]);

                    future.addListener(new GenericFutureListener<RadiusRequestFuture>() {
                        public void operationComplete(RadiusRequestFuture future) throws Exception {
                            RadiusRequestContext ctx = future.context();
                            if (future.isSuccess()) {
                                logger.info(String.format("Request(%s) %s succeeded, took %d.%d ms: %s",
                                        ctx.toString(),
                                        ctx.request().getPacketIdentifier(),
                                        ctx.responseTime() / 1000000,
                                        ctx.responseTime() % 1000000 / 10000, ctx.response().toString()));
                            } else {
                                logger.info(String.format("Request(%s) %s failed: %s",
                                        ctx.toString(),
                                        ctx.request().getPacketIdentifier(),
                                        future.cause().toString()));
                            }
                        }
                    });

                    Thread.yield();
                //}
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        */

/*
        eventGroup.next().schedule(new Runnable() {
            public void run() {
                scheduledFuture.cancel(false);
            }
        }, 30, TimeUnit.SECONDS);

        scheduledFuture.wait();
        */

        final long start = System.currentTimeMillis();

        final long numberOfRequests = 100000;

        for (int i = 0; i < numberOfRequests; i++) {
            AccessRequest request = new AccessRequest("test", "password");
            request.setDictionary(dictionary);
            request.addAttribute(new IntegerAttribute(6, 1));

            RadiusRequestFuture future =
                    client.communicate(request, endpoints[requests.getAndIncrement() % endpoints.length]);

            future.addListener(new GenericFutureListener<RadiusRequestFuture>() {
                public void operationComplete(RadiusRequestFuture future) throws Exception {

                    RadiusRequestContext ctx = future.context();
                    if (future.isSuccess()) {
                        logger.info(String.format("Request(%s) %s succeeded, took %d.%d ms: %s",
                                ctx.toString(),
                                ctx.request().getPacketIdentifier(),
                                ctx.responseTime() / 1000000,
                                ctx.responseTime() % 1000000 / 10000, ctx.response().toString()));
                    } else {
                        failed.incrementAndGet();
                        logger.info(String.format("Request(%s) %s failed: %s",
                                ctx.toString(),
                                ctx.request().getPacketIdentifier(),
                                future.cause().toString()));
                    }

                    if (completed.incrementAndGet() >= numberOfRequests)
                        System.out.println( completed.get() + " Completed in " + (System.currentTimeMillis() - start) + "ms => " + future.context() + " failed => " + failed.get());

                }


            });

            Thread.yield();
        }

        System.in.read();

        eventGroup.shutdownGracefully();
    }

    private static class NioDatagramChannelFactory implements ChannelFactory<NioDatagramChannel> {
        public NioDatagramChannel newChannel() {
             return new NioDatagramChannel();
        }
    }
}
