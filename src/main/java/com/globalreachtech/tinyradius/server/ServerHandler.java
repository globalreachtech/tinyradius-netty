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

public abstract class ServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    protected final Dictionary dictionary;
    private final Deduplicator deduplicator;
    private final Timer timer;
    private final SecretProvider secretProvider;

    protected ServerHandler(Dictionary dictionary, Deduplicator deduplicator, Timer timer, SecretProvider secretProvider) {
        this.dictionary = dictionary;
        this.deduplicator = deduplicator;
        this.timer = timer;
        this.secretProvider = secretProvider;
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
                if (logger.isInfoEnabled())
                    logger.info("ignoring packet from unknown client " + remoteAddress + " received on local address " + localAddress);
                return;
            }

            // parse packet
            RadiusPacket packet = makeRadiusPacket(datagramPacket, secret);
            if (logger.isInfoEnabled())
                logger.info("received packet from " + remoteAddress + " on local address " + localAddress + ": " + packet);

            // check for duplicates
            if (deduplicator.isPacketDuplicate(packet, remoteAddress)) {
                logger.info("ignore duplicate packet");
                return;
            }

            // handle packet
            logger.trace("about to call handlePacket()");
            final Promise<RadiusPacket> promise = handlePacket(ctx.channel(), packet, remoteAddress, secret);

            // so futures don't stay in memory forever if never completed
            Timeout timeout = timer.newTimeout(t -> promise.tryFailure(new RadiusException("timeout while generating client response")),
                    10, SECONDS);

            promise.addListener((Future<RadiusPacket> f) -> {
                timeout.cancel();

                RadiusPacket response = f.getNow();

                // send clientResponse
                if (response != null) {
                    response.setDictionary(dictionary);
                    if (logger.isInfoEnabled()) {
                        logger.info("send clientResponse: " + response);
                        logger.info("sending response packet to " + remoteAddress.toString() + " with secret " + secret);
                    }
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
    protected abstract Promise<RadiusPacket> handlePacket(Channel channel, RadiusPacket request, InetSocketAddress remoteAddress, String sharedSecret) throws RadiusException;
}
