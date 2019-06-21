package com.globalreachtech.tinyradius.client;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.MAX_PACKET_LENGTH;
import static io.netty.buffer.Unpooled.buffer;
import static java.util.Objects.requireNonNull;

/**
 * This object represents a simple Radius client which communicates with
 * a specified Radius server. You can use a single instance of this object
 * to authenticate or account different users with the same Radius server
 * as long as you authenticate/account one user after the other.
 * <p>
 * This object is thread safe, but requires a packet manager to avoid confusion with the mapping of request
 * and result packets.
 */
public class RadiusClient<T extends DatagramChannel> implements Closeable {

    private static Log logger = LogFactory.getLog(RadiusClient.class);

    private final ChannelFactory<T> factory;
    private final EventLoopGroup eventLoopGroup;
    private final ClientPacketManager packetManager;
    private final InetAddress listenAddress;
    private final int port;

    private T channel = null;

    private ChannelFuture channelFuture;

    /**
     * @param eventLoopGroup
     * @param factory
     * @param packetManager
     * @param port           set to 0 to let system choose
     */
    public RadiusClient(EventLoopGroup eventLoopGroup, ChannelFactory<T> factory, ClientPacketManager packetManager, InetAddress listenAddress, int port) {
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.eventLoopGroup = requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.packetManager = packetManager;
        this.listenAddress = listenAddress;
        this.port = port;
    }

    @Override
    public void close() {
        if (channel != null)
            channel.close();
    }

    /**
     * Registers the channel and binds to address.
     * <p>
     * Run implicitly if {@link #communicate(RadiusPacket, RadiusEndpoint, int)} is called.
     */
    public ChannelFuture startChannel() {
        if (this.channelFuture != null)
            return this.channelFuture;

        if (channel == null)
            channel = factory.newChannel();

        channelFuture = listen(channel, new InetSocketAddress(listenAddress, port));
        return channelFuture;
    }

    /**
     * @param channel       to listen on
     * @param listenAddress the address to bind to
     */
    protected ChannelFuture listen(final T channel, final InetSocketAddress listenAddress) {

        channel.pipeline().addLast(new RadiusChannelHandler());

        final ChannelPromise promise = channel.newPromise();

        final PromiseCombiner promiseCombiner = new PromiseCombiner(eventLoopGroup.next());
        promiseCombiner.addAll(eventLoopGroup.register(channel), channel.bind(listenAddress));
        promiseCombiner.finish(promise);

        return promise;
    }

    public Future<RadiusPacket> communicate(RadiusPacket packet, RadiusEndpoint endpoint, int maxAttempts) {
        return send(packet, endpoint, 1, maxAttempts);
    }

    private Future<RadiusPacket> send(RadiusPacket packet, RadiusEndpoint endpoint, int attempts, int maxAttempts) {
        Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();

        // because netty promises don't support chaining
        sendOnce(packet, endpoint).addListener((Future<RadiusPacket> attempt) -> {
            if (attempt.isSuccess())
                promise.trySuccess(attempt.getNow());
            if (attempts >= maxAttempts)
                promise.tryFailure(new RadiusException("Max retries reached: " + maxAttempts)); //todo check obo error

            logger.info(String.format("Retransmitting clientRequest for context %d", packet.getPacketIdentifier()));
            send(packet, endpoint, attempts + 1, maxAttempts);
        });

        return promise;
    }

    private Future<RadiusPacket> sendOnce(RadiusPacket packet, RadiusEndpoint endpoint) {
        Promise<RadiusPacket> promise = packetManager.logOutbound(packet, endpoint, eventLoopGroup.next());

        try {
            DatagramPacket packetOut = makeDatagramPacket(packet, endpoint);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Sending packet to %s", endpoint.getEndpointAddress()));
                logger.debug("\n" + ByteBufUtil.prettyHexDump(packetOut.content()));
            }

            startChannel().addListener((ChannelFuture f) -> {
                f.channel().writeAndFlush(packetOut);

                if (logger.isInfoEnabled())
                    logger.info(packet.toString());
            });

        } catch (Exception e) {
            promise.tryFailure(e);
        }
        return promise;
    }

    /**
     * Creates a datagram packet from a RadiusPacket to be send.
     *
     * @param packet   RadiusPacket
     * @param endpoint destination port number
     * @return new datagram packet
     */
    protected DatagramPacket makeDatagramPacket(RadiusPacket packet, RadiusEndpoint endpoint) throws IOException, RadiusException {
        ByteBuf buf = buffer(MAX_PACKET_LENGTH, MAX_PACKET_LENGTH);
        packet.encodeRequestPacket(new ByteBufOutputStream(buf), endpoint.getSharedSecret());

        return new DatagramPacket(buf, endpoint.getEndpointAddress());
    }

    private class RadiusChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            packetManager.handleInbound(packet);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }
    }

}