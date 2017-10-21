/**
 * $Id: RadiusClient.java,v 1.7 2005/11/10 10:20:21 wuttke Exp $
 * Created on 09.04.2005
 * @author Matthias Wuttke
 * @version $Revision: 1.7 $
 */
package org.tinyradius.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ConcurrentSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This object represents a simple Radius client which communicates with
 * a specified Radius server. You can use a single instance of this object
 * to authenticate or account different users with the same Radius server
 * as long as you authenticate/account one user after the other. This object
 * is thread safe, but only opens a single socket so operations using this
 * socket are synchronized to avoid confusion with the mapping of request
 * and result packets.
 */
public class RadiusClient<T extends DatagramChannel> {

    private int retryCount = 3;
    private static Log logger = LogFactory.getLog(RadiusClient.class);

    private AtomicInteger identifier = new AtomicInteger();
    private ChannelFactory<T> factory;
    private T channel = null;
    private EventLoopGroup eventGroup;
    private Dictionary dictionary;
    private Timer timer;
    private RadiusQueue<RadiusRequestPromise> queue =
            new RadiusQueue<RadiusRequestPromise>();

    private int retransmits = 3;

    /**
     * Creates a new Radius client object for a special Radius server.
     *
     * @param dictionary
     * @param eventGroup
     * @param factory
     * @param timer
     * @param properties
     */
    public RadiusClient(Dictionary dictionary, EventLoopGroup eventGroup, ChannelFactory<T> factory,
                        Timer timer, Properties properties) {
        if (eventGroup == null)
            throw new NullPointerException("eventGroup cannot be null");
        if (factory == null)
            throw new NullPointerException("factory cannot be null");
        if (timer == null)
            throw new NullPointerException("timer cannot be null");
        this.factory = factory;
        this.eventGroup = eventGroup;
        this.timer = timer;
        this.dictionary = dictionary;
    }

    /**
     * Creates a new Radius client object for a special Radius server.
     *
     * @param dictionary
     * @param eventGroup
     * @param factory
     * @param timer
     *
     */
    public RadiusClient(Dictionary dictionary, EventLoopGroup eventGroup, ChannelFactory<T> factory,
                        Timer timer) {
        this(dictionary, eventGroup, factory, timer, new Properties());
    }

    /**
     * Creates a new Radius client object for a special Radius server.
     *
     * @param eventGroup
     * @param factory
     * @param timer
     */
    public RadiusClient(EventLoopGroup eventGroup, ChannelFactory<T> factory, Timer timer) {
        this(DefaultDictionary.getDefaultDictionary(), eventGroup, factory, timer);
    }

    /**
     * Closes the socket of this client.
     */
    public void close() {
        if (channel != null)
            channel.close();
    }

    /**
     * @param request
     * @param endpoint
     * @return
     * @throws IOException
     * @throws RadiusException
     */
    public RadiusRequestFuture communicate(RadiusPacket request, RadiusEndpoint endpoint) {
        if (request == null)
            throw new NullPointerException("request cannot be null");
        if (endpoint == null)
            throw new NullPointerException("endpoint cannot be null");

        return this.communicate(request, endpoint, 3, TimeUnit.SECONDS);
    }

    private void sendRequest(RadiusRequestContextImpl context)
            throws IOException {
        if (context == null)
            throw new NullPointerException("context cannot be null");

        DatagramPacket packetOut = makeDatagramPacket(
                context.request(), context.endpoint());
        T channel = getChannel();
        ChannelFuture f = channel.writeAndFlush(packetOut);

        context.attempts().incrementAndGet();

        if (logger.isInfoEnabled())
            logger.info(context.request().toString());
    }

    /**
     *
     * @param request
     * @param endpoint
     * @return
     * @throws IOException
     * @throws RadiusException
     */
    @SuppressWarnings("unchecked")
    public RadiusRequestFuture communicate(RadiusPacket request, RadiusEndpoint endpoint, long timeout, TimeUnit unit) {
        if (request == null)
            throw new NullPointerException("request cannot be null");
        if (endpoint == null)
            throw new NullPointerException("endpoint cannot be null");
        if (timeout < 0)
            throw new IllegalArgumentException("timeout is invalid");
        if (unit == null)
            throw new NullPointerException("unit cannot be null");

        RadiusRequestContextImpl context =
                new RadiusRequestContextImpl(
                        identifier.getAndIncrement(), request,
                        endpoint, unit.toNanos(timeout));

        RadiusRequestPromise promise = queue(context);

        try {
            sendRequest((RadiusRequestContextImpl)promise.context());
        } catch (Exception e) {
            promise.setFailure(e);
            dequeue(promise);
        }

        return promise;
    }

    @SuppressWarnings("unchecked")
    private RadiusRequestPromise queue(RadiusRequestContextImpl context) {
        if (context == null)
            throw new NullPointerException("context cannot be null");

        final RadiusRequestPromise promise =
            new DefaultRadiusRequestPromise(context, GlobalEventExecutor.INSTANCE /* XXX: use a dedicated executor? */) {
                public boolean cancel(boolean mayInterruptIfRunning) {
                    RadiusClient.this.dequeue(this);
                    return super.cancel(mayInterruptIfRunning);
                }
            };

        queue.add(promise, context.request().getPacketIdentifier());

        context.newTimeout(timer, new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                RadiusRequestContextImpl ctx =
                        (RadiusRequestContextImpl)promise.context();
                if (ctx.attempts().intValue() < retransmits) {
                    logger.info(String.format("Retransmitting request for context %d", ctx.identifier()));
                    RadiusClient.this.sendRequest(ctx);
                    ctx.newTimeout(RadiusClient.this.timer, timeout.task());
                } else {
                    if (!promise.isDone()) {
                        promise.setFailure(new RadiusException("Timeout occurred"));
                        RadiusClient.this.dequeue(promise);
                    }
                }
            }
        });

        if (logger.isInfoEnabled())
            logger.info(String.format("Queued request %d identifier => %d",
                    context.identifier(), context.request().getPacketIdentifier()));

        return promise;
    }

    @SuppressWarnings("unchecked")
    private boolean dequeue(RadiusRequestPromise promise) {
        if (promise == null)
            throw new NullPointerException("promise cannot be null");

        RadiusRequestContextImpl context =
                (RadiusRequestContextImpl)promise.context();

        boolean success = queue.remove(promise, context.request().getPacketIdentifier());
        if (success) {
            if (!context.timeout.isExpired())
                 context.timeout.cancel();
        }
        return success;
    }

    @SuppressWarnings("unchecked")
    private RadiusRequestPromise lookup(DatagramPacket response) {
        if (response == null)
            throw new NullPointerException("response cannot be null");

        ByteBuf buf = response.content()
                .duplicate().skipBytes(1);

        int identifier = buf.readByte() & 0xff;

        for (RadiusRequestPromise promise : queue.get(identifier)) {
            RadiusRequestContextImpl context =
                    (RadiusRequestContextImpl)promise.context();
            if (identifier != context.request().getPacketIdentifier())
                continue;
            if (!(response.sender().equals(
                    context.endpoint().getEndpointAddress())))
                continue;
            try {
                RadiusPacket resp = RadiusPacket.decodeResponsePacket(dictionary,
                        new ByteBufInputStream(response.content().duplicate()),
                        context.endpoint().getSharedSecret(), context.request());

                if (logger.isInfoEnabled())
                    logger.info(String.format("Found context %d for response identifier => %d",
                        context.identifier(), resp.getPacketIdentifier()));

                context.setResponse(resp);

                return promise;

            } catch (IOException ioe) {
            } catch (RadiusException e) {
            }
        }

        return null;
    }

    /**
     * Returns the socket used for the server communication. It is
     * bound to an arbitrary free local port number.
     * @return local socket
     * @throws ChannelException
     */
    @SuppressWarnings("unchecked")
    protected T getChannel() throws ChannelException {
        if (channel == null) {
            channel = factory.newChannel();
            ChannelFuture future = eventGroup.register(channel);
            if (future.cause() != null)
                throw new ChannelException(future.cause());
            future.syncUninterruptibly();
            channel.bind(new InetSocketAddress(0));
            channel.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
                    RadiusRequestPromise promise = lookup(packet);
                    if (promise == null) {
                        logger.info("Request context not found for received packet, ignoring...");
                    } else {
                        RadiusRequestContextImpl context =
                                (RadiusRequestContextImpl)promise.context();

                        context.calculateResponseTime();

                        if (logger.isInfoEnabled()) {
                            logger.info(String.format("Received response in %d.%dms: %s\nFor request: %s",
                                    context.responseTime() / 1000000,
                                    context.responseTime() % 1000000 / 10000,
                                    context.response().toString(),
                                    context.request().toString()));
                        }

                        promise.trySuccess(null);
                        dequeue(promise);
                    }
                }

                public void channelReadComplete(ChannelHandlerContext ctx) {
                    ctx.flush();
                }

            });
        }

        return channel;
    }
    
    /**
     * Creates a datagram packet from a RadiusPacket to be send. 
     * @param packet RadiusPacket
     * @param endpoint destination port number
     * @return new datagram packet
     * @throws IOException
     */
    protected DatagramPacket makeDatagramPacket(RadiusPacket packet, RadiusEndpoint endpoint)
    throws IOException {

        ByteBufOutputStream bos = new ByteBufOutputStream(Unpooled.buffer(
                RadiusPacket.MAX_PACKET_LENGTH, RadiusPacket.MAX_PACKET_LENGTH));
        packet.encodeRequestPacket(bos, endpoint.getSharedSecret());

        return new DatagramPacket(bos.buffer(), endpoint.getEndpointAddress());
    }

    private class RadiusRequestContextImpl implements RadiusRequestContext {

        private long requestTime;
        private long responseTime;
        private int identifier;
        private Timeout timeout;
        private long timeoutNS;
        private AtomicInteger attempts;
        private RadiusPacket request;
        private RadiusPacket response;
        private RadiusEndpoint endpoint;

        public RadiusRequestContextImpl(int identifier, RadiusPacket request, RadiusEndpoint endpoint, long timeoutNS) {
            if (request == null)
                throw new NullPointerException("request cannot be null");
            if (endpoint == null)
                throw new NullPointerException("endpoint cannot be null");
            this.identifier = identifier;
            this.attempts = new AtomicInteger(0);
            this.request = request;
            //this.request.setPacketIdentifier(identifier & 0xff);
            this.endpoint = endpoint;
            this.requestTime = System.nanoTime();
            this.timeoutNS = timeoutNS;
        }

        public Timeout newTimeout(Timer timeout, TimerTask task) {
            if (this.timeout != null) {
                if (!this.timeout.isExpired())
                     this.timeout.cancel();
            }
            this.timeout = timer.newTimeout(task, timeoutNS / retransmits, TimeUnit.NANOSECONDS);
            return this.timeout;
        }

        public int identifier() {
            return identifier;
        }

        public RadiusPacket request() {
            return request;
        }

        public RadiusPacket response() {
            return response;
        }

        public long calculateResponseTime() {
            if (responseTime == 0)
                responseTime = System.nanoTime() - requestTime;
            return responseTime;
        }

        private AtomicInteger attempts() {
            return this.attempts;
        }

        public long requestTime() {
            return requestTime;
        }

        public long responseTime() {
            return responseTime;
        }

        public void setResponse(RadiusPacket response) {
            this.response = response;
        }

        public RadiusEndpoint endpoint() {
            return endpoint;
        }

        public String toString() {
            return Long.toString(this.identifier);
        }
    }
}
