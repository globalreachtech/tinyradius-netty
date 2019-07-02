package org.tinyradius.client;

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPacketDecoder;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;
import org.tinyradius.util.SecretProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * ClientHandler that matches requests/response by appending Proxy-State attribute to
 * outbound packets. This avoids problem with mismatched requests/responses when using
 * packetIdentifier, which is limited to 256 unique IDs.
 */
public class ProxyStateClientHandler extends ClientHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyStateClientHandler.class);

    private final AtomicInteger proxyIndex = new AtomicInteger(1);

    private final Dictionary dictionary;
    private final Timer timer;
    private final int timeoutMs;
    private final SecretProvider secretProvider;

    private final Map<String, Promise<RadiusPacket>> requests = new ConcurrentHashMap<>();

    /**
     * @param dictionary     to decode packet incoming DatagramPackets to RadiusPackets
     * @param timer          set timeout handlers if no responses received after timeout
     * @param timeoutMs      time to wait for responses in MS
     * @param secretProvider lookup shared secret for decoding response for upstream server.
     *                       Unlike packetIdentifier, Proxy-State is stored in attribute rather than the second octet,
     *                       so requires decoding first before we can lookup any context.
     */
    public ProxyStateClientHandler(Dictionary dictionary, Timer timer, int timeoutMs, SecretProvider secretProvider) {
        this.dictionary = dictionary;
        this.timer = timer;
        this.timeoutMs = timeoutMs;
        this.secretProvider = secretProvider;
    }

    private String genProxyState() {
        return Integer.toString(proxyIndex.getAndIncrement());
    }

    @Override
    public Promise<RadiusPacket> processRequest(RadiusPacket packet, RadiusEndpoint endpoint, EventExecutor eventExecutor) {
        // add Proxy-State attribute
        String requestId = genProxyState();
        packet.addAttribute(new RadiusAttribute(33, requestId.getBytes()));

        Promise<RadiusPacket> response = eventExecutor.newPromise();
        requests.put(requestId, response);

        final Timeout timeout = timer.newTimeout(
                t -> response.tryFailure(new RadiusException("Timeout occurred")), timeoutMs, MILLISECONDS);

        response.addListener(f -> {
            requests.remove(requestId);
            timeout.cancel();
        });

        return response;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagramPacket) {
        String secret = secretProvider.getSharedSecret(datagramPacket.sender());
        if (secret == null) {
            logger.info("ignoring packet from unknown server {} received on local address {}",
                    datagramPacket.sender(), datagramPacket.recipient());
            return;
        }

        try (ByteBufInputStream in = new ByteBufInputStream(datagramPacket.content())) {
            RadiusPacket packet = RadiusPacketDecoder.decodeRequestPacket(dictionary, in, secret);

            // retrieve my Proxy-State attribute (the last)
            List<RadiusAttribute> proxyStates = packet.getAttributes(33);
            if (proxyStates == null || proxyStates.size() == 0)
                throw new RadiusException("proxy packet without Proxy-State attribute");
            RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);

            String requestId = new String(proxyState.getAttributeData());

            final Promise<RadiusPacket> request = requests.get(requestId);

            if (request == null) {
                logger.info("Request context not found for received packet, ignoring...");
                return;
            }

            logger.info("Found connection {} for request identifier => {}", requestId, packet.getPacketIdentifier());
            logger.info("received proxy packet: {}", packet);

            // remove only own Proxy-State (last attribute)
            packet.removeLastAttribute(33);

            request.trySuccess(packet);
        } catch (IOException ioe) {
            logger.error("communication/socket io error", ioe);
        } catch (RadiusException re) {
            logger.error("malformed Radius packet", re);
        }
    }
}
