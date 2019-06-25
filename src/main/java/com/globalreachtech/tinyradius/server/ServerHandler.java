package com.globalreachtech.tinyradius.server;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import com.globalreachtech.tinyradius.util.SecretProvider;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.MAX_PACKET_LENGTH;
import static com.globalreachtech.tinyradius.packet.RadiusPacket.decodeRequestPacket;
import static io.netty.buffer.Unpooled.buffer;
import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class ServerHandler<T extends RadiusPacket> extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    protected final Dictionary dictionary;
    private final Deduplicator deduplicator;
    private final Timer timer;
    private final SecretProvider secretProvider;
    private final Class<? extends RadiusPacket> packetClass;

    /**
     * @param dictionary     for encoding/decoding RadiusPackets
     * @param deduplicator   handle duplicate client requests
     * @param timer          handle timeouts if requests take too long to be processed
     * @param secretProvider lookup sharedSecret given remote address
     * @param packetClass    restrict RadiusPacket subtypes that can be processed by handler, otherwise will be dropped.
     *                       If all types of RadiusPackets are allowed, use {@link RadiusPacket}
     */
    protected ServerHandler(Dictionary dictionary, Deduplicator deduplicator, Timer timer, SecretProvider secretProvider, Class<T> packetClass) {
        this.dictionary = dictionary;
        this.deduplicator = deduplicator;
        this.timer = timer;
        this.secretProvider = secretProvider;
        this.packetClass = packetClass;
    }

    /**
     * Creates a Radius clientResponse datagram packet from a RadiusPacket to be send.
     *
     * @param packet  RadiusPacket
     * @param secret  shared secret to encode packet
     * @param address where to send the packet
     * @param request clientRequest packet
     * @return new datagram packet
     */
    protected DatagramPacket makeDatagramPacket(RadiusPacket packet, String secret, InetSocketAddress address, RadiusPacket request)
            throws IOException, RadiusException {

        ByteBuf buf = buffer(MAX_PACKET_LENGTH, MAX_PACKET_LENGTH);
        packet.setDictionary(dictionary);
        packet.encodeResponsePacket(new ByteBufOutputStream(buf), secret, request);

        return new DatagramPacket(buf, address);
    }

    /**
     * Creates a RadiusPacket for a Radius clientRequest from a received
     * datagram packet.
     *
     * @param packet received datagram
     * @return RadiusPacket object
     * @throws RadiusException malformed packet
     * @throws IOException     communication error (after getRetryCount()
     *                         retries)
     */
    protected RadiusPacket makeRadiusPacket(DatagramPacket packet, String sharedSecret) throws IOException, RadiusException {
        ByteBufInputStream in = new ByteBufInputStream(packet.content());
        return decodeRequestPacket(dictionary, in, sharedSecret);
    }

    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagramPacket) {
        try {
            InetSocketAddress localAddress = datagramPacket.recipient();
            InetSocketAddress remoteAddress = datagramPacket.sender();

            String secret = secretProvider.getSharedSecret(remoteAddress);
            if (secret == null) {
                logger.info("ignoring packet from unknown client {} received on local address {}", remoteAddress, localAddress);
                return;
            }

            // parse packet
            RadiusPacket packet = makeRadiusPacket(datagramPacket, secret);
            logger.info("received packet from {} on local address {}: {}", remoteAddress, localAddress, packet);

            // check for duplicates
            if (deduplicator.isPacketDuplicate(packet, remoteAddress)) {
                logger.info("ignore duplicate packet");
                return;
            }

            // check channelHandler packet type restrictions
            if (!packetClass.isInstance(packet)) {
                logger.info("handler only accepts {}, unknown Radius packet type: {}", packetClass.getName(), packet.getPacketType());
                return;
            }

            logger.trace("about to call handlePacket()");
            final Promise<RadiusPacket> promise = handlePacket(ctx.channel(), (T) packet, remoteAddress, secret);

            // so futures don't stay in memory forever if never completed
            Timeout timeout = timer.newTimeout(t -> promise.tryFailure(new RadiusException("timeout while generating client response")),
                    10, SECONDS);

            promise.addListener((Future<RadiusPacket> f) -> {
                timeout.cancel();

                RadiusPacket response = f.getNow();

                // send clientResponse
                if (response != null) {
                    response.setDictionary(dictionary);
                    logger.info("send clientResponse: {}", response);
                    logger.info("sending response packet to {} with secret {}", remoteAddress, secret);
                    DatagramPacket packetOut = makeDatagramPacket(response, secret, remoteAddress, packet);
                    ctx.writeAndFlush(packetOut);
                } else {
                    logger.info("no clientResponse sent");
                    Throwable e = f.cause();
                    if (e != null)
                        logger.error("exception while handling packet", e);
                }
            });


        } catch (IOException ioe) {
            // error while reading/writing socket
            logger.error("communication error", ioe);
        } catch (RadiusException re) {
            logger.error("malformed Radius packet", re);
        }
    }

    /**
     * Handles the received Radius packet and constructs a clientResponse.
     *
     * @param channel       socket which received packet
     * @param request       the packet
     * @param remoteAddress remote address the packet was sent by
     * @param sharedSecret  shared secret associated with remoteAddress
     * @return Promise of RadiusPacket or null for no clientResponse. Uses Promise instead Future,
     * to allow requests to be timed out or cancelled by the caller
     */
    protected abstract Promise<RadiusPacket> handlePacket(Channel channel, T request, InetSocketAddress remoteAddress, String sharedSecret) throws RadiusException;
}
