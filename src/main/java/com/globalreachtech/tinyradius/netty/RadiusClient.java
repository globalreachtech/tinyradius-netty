package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.grt.RequestContext;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.MAX_PACKET_LENGTH;
import static io.netty.buffer.Unpooled.buffer;
import static java.util.Objects.requireNonNull;

/**
 * This object represents a simple Radius client which communicates with
 * a specified Radius server. You can use a single instance of this object
 * to authenticate or account different users with the same Radius server
 * as long as you authenticate/account one user after the other. This object
 * is thread safe, but only opens a single socket so operations using this
 * socket are synchronized to avoid confusion with the mapping of clientRequest
 * and result packets.
 */
public class RadiusClient<T extends DatagramChannel> implements Closeable {

    private static Log logger = LogFactory.getLog(RadiusClient.class);

    private final AtomicInteger identifier = new AtomicInteger();
    private final ChannelFactory<T> factory;
    private final EventLoopGroup eventGroup;
    private final Consumer<DatagramPacket> consumer;

    private T channel = null;

    /**
     * Creates a new Radius client object for a special Radius server.
     *
     * @param eventGroup
     * @param factory
     */
    public RadiusClient(EventLoopGroup eventGroup, ChannelFactory<T> factory, Consumer<DatagramPacket> consumer) {
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.eventGroup = requireNonNull(eventGroup, "eventGroup cannot be null");
        this.consumer = consumer;
    }

    /**
     * Closes the socket of this client.
     */
    @Override
    public void close() {
        if (channel != null)
            channel.close();
    }

    public RequestContext communicate(RadiusPacket request, RadiusEndpoint endpoint, long timeout, TimeUnit unit) {
        if (timeout < 0)
            throw new IllegalArgumentException("timeout is invalid");

        RequestContext context = new RequestContext(
                identifier.getAndIncrement(), request, endpoint, unit.toNanos(timeout));

        try {
            sendRequest(context.clientRequest(), context.endpoint());
        } catch (Exception e) {
        }

        return context;
    }

    public void sendRequest(RadiusPacket packet, RadiusEndpoint endpoint) throws IOException, RadiusException {

        DatagramPacket packetOut = makeDatagramPacket(packet, endpoint);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Sending packet to %s", endpoint.getEndpointAddress()));
            logger.debug("\n" + ByteBufUtil.prettyHexDump(packetOut.content()));
        }

        T channel = getChannel();
        channel.writeAndFlush(packetOut);

        if (logger.isInfoEnabled())
            logger.info(packet.toString());
    }

    /**
     * Binds the channel to a specific InetSocketAddress
     *
     * @param channel
     * @return
     */
    protected ChannelFuture doBind(Channel channel) {
        return channel.bind(new InetSocketAddress(0));
    }

    /**
     * Returns the socket used for the server communication. It is
     * bound to an arbitrary free local port number.
     *
     * @return local socket
     * @throws ChannelException
     */
    protected T getChannel() throws ChannelException {
        if (channel == null)
            channel = createChannel();
        return channel;
    }

    private T createChannel() {
        final T channel = factory.newChannel();
        ChannelFuture future = eventGroup.register(channel).syncUninterruptibly();
        if (future.cause() != null)
            throw new ChannelException(future.cause());

        doBind(channel).syncUninterruptibly();

        channel.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
            public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
                consumer.accept(packet);
            }
        });
        return channel;
    }

    /**
     * Creates a datagram packet from a RadiusPacket to be send.
     *
     * @param packet   RadiusPacket
     * @param endpoint destination port number
     * @return new datagram packet
     * @throws IOException
     */
    protected DatagramPacket makeDatagramPacket(RadiusPacket packet, RadiusEndpoint endpoint) throws IOException, RadiusException {

        ByteBufOutputStream bos = new ByteBufOutputStream(buffer(MAX_PACKET_LENGTH, MAX_PACKET_LENGTH));
        packet.encodeRequestPacket(bos, endpoint.getSharedSecret());

        return new DatagramPacket(bos.buffer(), endpoint.getEndpointAddress());
    }

}
