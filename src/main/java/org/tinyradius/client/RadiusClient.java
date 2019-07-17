package org.tinyradius.client;

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
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPacketEncoder;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static java.lang.Byte.toUnsignedInt;
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
     * @param eventLoopGroup for both channel IO and processing
     * @param factory        to create new Channel
     * @param clientHandler  to log outgoing packets and handle incoming packets/responses
     * @param listenAddress  local address to bind to, will be wildcard address if null
     * @param port           port to bind to, or set to 0 to let system choose
     */
    public RadiusClient(EventLoopGroup eventLoopGroup, ChannelFactory<T> factory, ClientHandler clientHandler, InetAddress listenAddress, int port) {
        this.factory = requireNonNull(factory, "factory cannot be null");
        this.eventLoopGroup = requireNonNull(eventLoopGroup, "eventLoopGroup cannot be null");
        this.clientHandler = clientHandler;
        this.listenAddress = listenAddress;
        this.port = port;
    }

    /**
     * Closes channel socket
     */
    public void stop() {
        if (channel != null)
            channel.close();
    }

    /**
     * Registers the channel and binds to address.
     * <p>
     * Also run implicitly if {@link #communicate(RadiusPacket, RadiusEndpoint)} is called.
     *
     * @return channelFuture of started channel socket
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
     * @return channel that resolves after it is bound to address and registered with eventLoopGroup
     */
    private ChannelFuture listen(final T channel, final InetSocketAddress listenAddress) {

        channel.pipeline().addLast(clientHandler);

        final ChannelPromise promise = channel.newPromise();

        eventLoopGroup.submit(() -> {
            final PromiseCombiner promiseCombiner = new PromiseCombiner(eventLoopGroup.next());
            promiseCombiner.addAll(eventLoopGroup.register(channel), channel.bind(listenAddress));
            promiseCombiner.finish(promise);
        });
        return promise;
    }

    public Future<RadiusPacket> communicate(RadiusPacket originalPacket, RadiusEndpoint endpoint) {
        final Promise<RadiusPacket> promise = eventLoopGroup.next().newPromise();
        promise.addListener(f -> {
            if (!f.isSuccess())
                logger.error("{}", f.cause().getMessage());
        });

        final RadiusPacket request = clientHandler.prepareRequest(originalPacket, endpoint, promise);

        try {
            final DatagramPacket datagram = RadiusPacketEncoder.toDatagram(
                    request.encodeRequest(endpoint.getSharedSecret()),
                    endpoint.getEndpointAddress());
            logger.info("Preparing send: {}", request);

            // todo init channel first, fail fast
            // no point saying request fail if all requests are going to fail cos channel not set up

//        startChannel().addListener((ChannelFuture f) ->
//                send(request, endpoint, 1, maxAttempts, promise));
            send(datagram, 1, promise);

        } catch (RadiusException e) {
            promise.tryFailure(e);
        }

        return promise;
    }

    private void send(DatagramPacket datagram, int attempt, Promise<RadiusPacket> requestPromise) {
        logger.info("Attempt {}, sending packet {} to {}", attempt, toUnsignedInt(datagram.content().getByte(1)), datagram.recipient());
        if (logger.isDebugEnabled())
            logger.debug("\n" + ByteBufUtil.prettyHexDump(datagram.content()));

        startChannel().addListener((ChannelFuture f) ->
                f.channel().writeAndFlush(datagram));

        clientHandler.scheduleRetry(t -> send(datagram, attempt + 1, requestPromise), attempt, requestPromise);
    }
}