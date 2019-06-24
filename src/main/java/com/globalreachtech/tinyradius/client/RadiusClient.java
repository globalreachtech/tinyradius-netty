package com.globalreachtech.tinyradius.client;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class RadiusClient<T extends DatagramChannel> {

    private static final Logger logger = LoggerFactory.getLogger(RadiusClient.class);

    private final ChannelFactory<T> factory;
    private final EventLoopGroup eventLoopGroup;
    private final ClientHandler clientHandler;
    private final InetAddress listenAddress;
    private final int port;

    private T channel = null;

    private ChannelFuture channelFuture;

    /**
     * @param port set to 0 to let system choose
     */
    public RadiusClient(EventLoopGroup eventLoopGroup, ChannelFactory<T> factory, ClientHandler clientHandler, InetAddress listenAddress, int port) {
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.eventLoopGroup = requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.clientHandler = clientHandler;
        this.listenAddress = listenAddress;
        this.port = port;
    }

    public void stop() {
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

        channel.pipeline().addLast(clientHandler);

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
        try {
            // run first to add any identifiers/attributes needed
            Future<RadiusPacket> promise = clientHandler.processRequest(packet, endpoint, eventLoopGroup.next());

            // will mutate packet (regenerate authenticator)
            final DatagramPacket packetOut = makeDatagramPacket(packet, endpoint);

            logger.debug("Sending packet to {}", endpoint.getEndpointAddress());
            if (logger.isDebugEnabled())
                logger.debug("\n" + ByteBufUtil.prettyHexDump(packetOut.content()));

            startChannel().addListener((ChannelFuture f) -> {
                f.channel().writeAndFlush(packetOut);

                logger.info("{}", packet);
            });

            return promise;
        } catch (Exception e) {
            return eventLoopGroup.next().newFailedFuture(e);
        }
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

}