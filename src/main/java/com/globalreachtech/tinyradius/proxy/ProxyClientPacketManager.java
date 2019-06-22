package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.attribute.RadiusAttribute;
import com.globalreachtech.tinyradius.client.RadiusClient;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.decodeRequestPacket;
import static com.globalreachtech.tinyradius.packet.RadiusPacket.decodeResponsePacket;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class ProxyClientPacketManager implements RadiusClient.PacketManager {

    private static Log logger = LogFactory.getLog(ProxyClientPacketManager.class);

    private final AtomicInteger proxyIndex = new AtomicInteger(1);

    private final Timer timer;
    private final Dictionary dictionary;
    private final int timeoutMs;

    private final Map<ContextKey, Context> contexts = new ConcurrentHashMap<>();

    private Map<String, Context> proxyConnections = new ConcurrentHashMap<>();

    public ProxyClientPacketManager(Timer timer, Dictionary dictionary, int timeoutMs) {
        this.timer = timer;
        this.dictionary = dictionary;
        this.timeoutMs = timeoutMs;
    }

    private String genProxyState() {
        return Integer.toString(proxyIndex.getAndIncrement());
    }

    @Override
    public Promise<RadiusPacket> handleOutbound(RadiusPacket packet, RadiusEndpoint endpoint, EventExecutor eventExecutor) {

        // add Proxy-State attribute
        String proxyState = genProxyState();
        packet.addAttribute(new RadiusAttribute(33, proxyState.getBytes()));

        final ContextKey key = new ContextKey(packet.getPacketIdentifier(), endpoint.getEndpointAddress(), proxyState);
        final Context context = new Context(eventExecutor.newPromise());
        proxyConnections.put(proxyState, context);

        final Timeout timeout = timer.newTimeout(
                t -> context.response.tryFailure(new RadiusException("Timeout occurred")), timeoutMs, MILLISECONDS);

        context.response.addListener(f -> {
            proxyConnections.remove(proxyState);
            timeout.cancel();
        });

        return context.response;
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

    public abstract String getSharedSecret(InetSocketAddress client);


    @Override
    public void handleInbound(DatagramPacket packetIn) {

        try {
            String secret = getSharedSecret(packetIn.sender());
            if (secret == null) {
                if (logger.isInfoEnabled())
                    logger.info("ignoring packet from unknown server " + packetIn.sender() + " received on local address " + packetIn.recipient());
                return;
            }


            RadiusPacket packet = makeRadiusPacket(packetIn, secret);


            // retrieve my Proxy-State attribute (the last)
            List<RadiusAttribute> proxyStates = packet.getAttributes(33);
            if (proxyStates == null || proxyStates.size() == 0)
                throw new RadiusException("proxy packet without Proxy-State attribute");
            RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);

            String connectionId = new String(proxyState.getAttributeData());

            final Context context = proxyConnections.get(connectionId);

            if (context == null) {
                logger.info("Request context not found for received packet, ignoring...");
                return;
            }

            if (logger.isInfoEnabled())
                logger.info(String.format("Found context %s for response identifier => %d",
                        connectionId, packet.getPacketIdentifier()));

            context.response.trySuccess(packet);
        } catch (IOException ioe) {
            // error while reading/writing socket
            logger.error("communication error", ioe);
        } catch (RadiusException re) {
            logger.error("malformed Radius packet", re);
        }
    }

    private static class ContextKey {
        private final int packetIdentifier;
        private final InetSocketAddress address;
        private final String proxyState;

        ContextKey(int packetIdentifier, InetSocketAddress address, String proxyState) {
            this.packetIdentifier = packetIdentifier;
            this.address = address;
            this.proxyState = proxyState;
        }

    }

    private static class Context {

        private final Promise<RadiusPacket> response;

        Context(Promise<RadiusPacket> response) {
            this.response = response;
        }
    }
}
