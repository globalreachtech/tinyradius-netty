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

import static com.globalreachtech.tinyradius.packet.RadiusPacket.decodeResponsePacket;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ProxyPacketManager implements RadiusClient.PacketManager {

    private static Log logger = LogFactory.getLog(ProxyPacketManager.class);

    private final AtomicInteger proxyIndex = new AtomicInteger(1);

    private final Timer timer;
    private final Dictionary dictionary;
    private final int timeoutMs;

    private final Map<ContextKey, Context> contexts = new ConcurrentHashMap<>();

    public ProxyPacketManager(Timer timer, Dictionary dictionary, int timeoutMs) {
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
        final Context context = new Context(endpoint.getSharedSecret(), packet, eventExecutor.newPromise());
        contexts.put(key, context);

        final Timeout timeout = timer.newTimeout(
                t -> context.response.tryFailure(new RadiusException("Timeout occurred")), timeoutMs, MILLISECONDS);

        context.response.addListener(f -> {
            contexts.remove(key);
            timeout.cancel();
        });

        return context.response;
    }

    /**
     * Creates a RadiusPacket for a Radius request from a received
     * datagram packet.
     * @param packet received datagram
     * @return RadiusPacket object
     * @exception RadiusException malformed packet
     * @exception IOException communication error (after getRetryCount()
     * retries)
     */
    protected RadiusPacket makeRadiusPacket(DatagramPacket packet, String sharedSecret)
            throws IOException, RadiusException {
        ByteArrayInputStream in = new ByteArrayInputStream(packet.getData());
        return RadiusPacket.decodeRequestPacket(in, sharedSecret);
    }

    @Override
    public void handleInbound(DatagramPacket packetIn) {

        RadiusPacket packet = makeRadiusPacket(packetIn, secret);



        // retrieve my Proxy-State attribute (the last)
        List<RadiusAttribute> proxyStates = packet.getAttributes(33);
        if (proxyStates == null || proxyStates.size() == 0)
            throw new RadiusException("proxy packet without Proxy-State attribute");
        RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);

        // todo move this logic to proxy implementation of RadiusClient.PacketManager
        String state = new String(proxyState.getAttributeData());
        // use state as the key to distinguish between connections





        int identifier = packet.content().duplicate().skipBytes(1).readByte() & 0xff;
        final ContextKey key = new ContextKey(identifier, packet.sender());

        final Context context = contexts.get(key);
        if (context == null) {
            logger.info("Request context not found for received packet, ignoring...");
            return;
        }

        try {
            RadiusPacket resp = decodeResponsePacket(dictionary,
                    new ByteBufInputStream(packet.content().duplicate()),
                    context.sharedSecret, context.request);

            if (logger.isInfoEnabled())
                logger.info(String.format("Found context %d for clientResponse identifier => %d",
                        key.packetIdentifier, resp.getPacketIdentifier()));

            context.response.trySuccess(resp);
        } catch (IOException | RadiusException ignored) {
        }
    }

    private static class ContextKey {
        private final int packetIdentifier;
        private final InetSocketAddress address;
        private final String proxyState;

        public ContextKey(int packetIdentifier, InetSocketAddress address, String proxyState) {
            this.packetIdentifier = packetIdentifier;
            this.address = address;
            this.proxyState = proxyState;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ContextKey that = (ContextKey) o;
            return packetIdentifier == that.packetIdentifier &&
                    address.equals(that.address) &&
                    proxyState.equals(that.proxyState);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packetIdentifier, address, proxyState);
        }
    }

    private static class Context {

        private final String sharedSecret;
        private final RadiusPacket request;
        private final Promise<RadiusPacket> response;

        Context(String sharedSecret, RadiusPacket request, Promise<RadiusPacket> response) {
            this.sharedSecret = sharedSecret;
            this.request = request;
            this.response = response;
        }
    }
}
