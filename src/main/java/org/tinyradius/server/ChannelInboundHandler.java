package org.tinyradius.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPacketEncoder;
import org.tinyradius.util.RadiusException;
import org.tinyradius.util.SecretProvider;

import java.net.InetSocketAddress;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * SimpleChannelInboundHandler implementation that converts between RadiusPackets
 * and DatagramPackets. Acts as an adapter so RequestHandlers dont have to be
 * concerned with Datagrams.
 *
 * @param <T> RadiusPacket types that this Channel can accept
 */
public class ChannelInboundHandler<T extends RadiusPacket> extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(ChannelInboundHandler.class);

    private final Dictionary dictionary;
    private final RequestHandler<T> requestHandler;
    private final Timer timer;
    private final SecretProvider secretProvider;
    private final Class<T> packetClass;

    /**
     * @param dictionary     for encoding/decoding RadiusPackets
     * @param requestHandler handle requests
     * @param timer          handle timeouts if requests take too long to be processed
     * @param secretProvider lookup sharedSecret given remote address
     * @param packetClass    restrict RadiusPacket subtypes that can be processed by handler, otherwise will be dropped.
     *                       If all types of RadiusPackets are allowed, use {@link RadiusPacket}
     */
    public ChannelInboundHandler(Dictionary dictionary, RequestHandler<T> requestHandler, Timer timer, SecretProvider secretProvider, Class<T> packetClass) {
        this.dictionary = dictionary;
        this.requestHandler = requestHandler;
        this.timer = timer;
        this.secretProvider = secretProvider;
        this.packetClass = packetClass;
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
            RadiusPacket request = RadiusPacketEncoder.fromRequestDatagram(dictionary, datagramPacket, secret);
            logger.info("received packet from {} on local address {}: {}", remoteAddress, localAddress, request);

            // check channelHandler packet type restrictions
            if (!packetClass.isInstance(request)) {
                logger.info("handler only accepts {}, unknown Radius packet type: {}", packetClass.getName(), request.getPacketType());
                return;
            }

            final byte[] requestAuthenticator = request.getAuthenticator(); // save ref in case request is mutated

            logger.trace("about to call handlePacket()");
            final Promise<RadiusPacket> promise = requestHandler.handlePacket(ctx.channel(), packetClass.cast(request), remoteAddress, secret);

            // so futures don't stay in memory forever if never completed
            Timeout timeout = timer.newTimeout(t -> promise.tryFailure(new RadiusException("timeout while generating client response")),
                    10, SECONDS);

            promise.addListener((Future<RadiusPacket> f) -> {
                timeout.cancel();

                RadiusPacket response = f.getNow();

                // todo exception handling?
                // send response
                if (response != null) {
                    logger.info("sending response {} to {} with secret {}", response, remoteAddress, secret);
                    DatagramPacket packetOut = RadiusPacketEncoder.toDatagram(
                            response.encodeResponse(secret, requestAuthenticator),
                            remoteAddress);
                    ctx.writeAndFlush(packetOut);
                } else {
                    logger.info("no response sent");
                    Throwable e = f.cause();
                    if (e != null)
                        logger.error("exception while handling packet", e);
                }
            });
        } catch (RadiusException e) {
            logger.error("DatagramPacket handle error: ", e);
        }
    }
}
