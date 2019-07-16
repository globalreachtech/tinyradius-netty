package org.tinyradius.client;

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
import org.tinyradius.packet.RadiusPacketEncoder;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;
import org.tinyradius.util.SecretProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.tinyradius.attribute.Attributes.createAttribute;

/**
 * ClientHandler that matches requests/response by appending Proxy-State attribute to
 * outbound packets. This avoids problem with mismatched requests/responses when using
 * packetIdentifier, which is limited to 256 unique IDs.
 */
public class ProxyStateClientHandler extends ClientHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyStateClientHandler.class);

    private static final int PROXY_STATE = 33;

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

    private String nextProxyStateId() {
        return Integer.toString(proxyIndex.getAndIncrement());
    }

    @Override
    public Promise<RadiusPacket> processRequest(RadiusPacket packet, RadiusEndpoint endpoint, EventExecutor eventExecutor) {
        // add Proxy-State attribute
        String requestId = nextProxyStateId();
        packet.addAttribute(createAttribute(packet.getDictionary(), -1, PROXY_STATE, requestId.getBytes()));

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

        try {
            RadiusPacket packet = RadiusPacketEncoder.fromDatagram(dictionary, datagramPacket, secret);

            // retrieve my Proxy-State attribute (the last)
            List<RadiusAttribute> proxyStates = packet.getAttributes(PROXY_STATE);
            if (proxyStates.isEmpty())
                throw new RadiusException("proxy packet without Proxy-State attribute");

            RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);
            String proxyStateId = new String(proxyState.getData());

            final Promise<RadiusPacket> request = requests.get(proxyStateId);

            if (request == null) {
                logger.info("Request context not found for received packet, ignoring...");
                return;
            }

            logger.info("Found connection (proxyState) {} for packet => {}", proxyStateId, packet);

            packet.removeLastAttribute(PROXY_STATE);

            request.trySuccess(packet);
        } catch (RadiusException e) {
            logger.error("DatagramPacket handle error: ", e);
        }
    }
}
