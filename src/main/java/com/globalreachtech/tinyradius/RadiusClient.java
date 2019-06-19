package com.globalreachtech.tinyradius;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.MAX_PACKET_LENGTH;
import static io.netty.buffer.Unpooled.buffer;
import static java.util.Objects.requireNonNull;

/**
 * This object represents a simple Radius client which communicates with
 * a specified Radius server. You can use a single INSTANCE of this object
 * to authenticate or account different users with the same Radius server
 * as long as you authenticate/account one user after the other. This object
 * is thread safe, but requires a packet manager to avoid confusion with the mapping of request
 * and result packets.
 */
public class RadiusClient<T extends DatagramChannel> implements Closeable {

    private static Log logger = LogFactory.getLog(RadiusClient.class);

    private final ChannelFactory<T> factory;
    private final EventLoopGroup eventLoopGroup;
    private final PacketManager packetManager;

    private T channel = null;

    /**
     * Creates a new Radius client object for a special Radius server.
     *
     * @param eventLoopGroup
     * @param factory
     * @param packetManager
     */
    public RadiusClient(EventLoopGroup eventLoopGroup, ChannelFactory<T> factory, PacketManager packetManager) {
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.eventLoopGroup = requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.packetManager = packetManager;
    }

    @Override
    public void close() {
        if (channel != null)
            channel.close();
    }

    public Future<RadiusPacket> communicate(RadiusPacket packet, RadiusEndpoint endpoint, int maxAttempts) {
        return send(packet, endpoint, 1, maxAttempts);
    }

    private Future<RadiusPacket> send(RadiusPacket packet, RadiusEndpoint endpoint, int attempts, int maxAttempts) {
        Promise<RadiusPacket> promise = new DefaultPromise<>();

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
        Promise<RadiusPacket> promise = packetManager.logOutbound(packet, endpoint);

        try {
            DatagramPacket packetOut = makeDatagramPacket(packet, endpoint);

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Sending packet to %s", endpoint.getEndpointAddress()));
                logger.debug("\n" + ByteBufUtil.prettyHexDump(packetOut.content()));
            }

            T channel = getChannel();
            channel.writeAndFlush(packetOut);

            if (logger.isInfoEnabled())
                logger.info(packet.toString());
        } catch (Exception e) {
            promise.tryFailure(e);
        }
        return promise;
    }

    /**
     * Returns the socket used for the server communication. It is
     * bound to an arbitrary free local port number.
     *
     * @return local socket
     * @throws ChannelException
     */
    private T getChannel() throws ChannelException {
        if (channel == null)
            channel = createChannel();
        return channel;
    }

    private T createChannel() {
        final T channel = factory.newChannel();

        final ChannelPromise promise = channel.newPromise()
                .addListener(f -> channel.pipeline().addLast(new RadiusChannelHandler()));

        final PromiseCombiner promiseCombiner = new PromiseCombiner(eventLoopGroup.next());
        promiseCombiner.addAll(eventLoopGroup.register(channel), channel.bind(new InetSocketAddress(0)));
        promiseCombiner.finish(promise);

        final ChannelPromise future = promise.syncUninterruptibly();
        if (future.cause() != null)
            throw new ChannelException(future.cause());

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

    private class RadiusChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            packetManager.logInbound(packet);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }
    }

    public interface PacketManager {

        Promise<RadiusPacket> logOutbound(RadiusPacket packet, RadiusEndpoint endpoint);

        /**
         * Process packet received
         *
         * @param packet
         */
        void logInbound(DatagramPacket packet);


    }
}