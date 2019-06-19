package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.RadiusClient;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ClientPacketManager implements RadiusClient.PacketManager {

    private static Log logger = LogFactory.getLog(ClientPacketManager.class);

    private final Timer timer;
    private final Dictionary dictionary;
    private final int timeoutMs;

    private final Map<ContextKey, Context> contexts = new ConcurrentHashMap<>();

    public ClientPacketManager(Timer timer, Dictionary dictionary, int timeoutMs) {
        this.timer = timer;
        this.dictionary = dictionary;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Promise<RadiusPacket> logOutbound(RadiusPacket packet, RadiusEndpoint endpoint) {

        final ContextKey key = new ContextKey(packet.getPacketIdentifier(), endpoint.getEndpointAddress());
        final Context context = new Context(endpoint.getSharedSecret(), packet);
        contexts.put(key, context);

        final Timeout timeout = timer.newTimeout(
                t -> context.response.tryFailure(new RadiusException("Timeout occurred")), timeoutMs, MILLISECONDS);

        context.response.addListener(f -> {
            contexts.remove(key);
            timeout.cancel();
        });

        return context.response;
    }

    @Override
    public void logInbound(DatagramPacket packet) {
        int identifier = packet.content().duplicate().skipBytes(1).readByte() & 0xff;
        final ContextKey key = new ContextKey(identifier, packet.sender());

        final Context context = contexts.get(key);
        if (context == null) {
            logger.info("Request context not found for received packet, ignoring...");
            return;
        }

        try {
            RadiusPacket resp = RadiusPacket.decodeResponsePacket(dictionary,
                    new ByteBufInputStream(packet.content().duplicate()),
                    context.sharedSecret, context.request);

            if (logger.isInfoEnabled())
                logger.info(String.format("Found context %d for clientResponse identifier => %d",
                        key.packetIdentifier, resp.getPacketIdentifier()));

            context.response.trySuccess(resp);
        } catch (IOException | RadiusException ignored) {
        }
    }

    private class ContextKey {
        // todo can all packets be uniquely identified by this?
        private final int packetIdentifier;
        private final InetSocketAddress address;

        ContextKey(int packetIdentifier, InetSocketAddress address) {
            this.packetIdentifier = packetIdentifier;
            this.address = address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ContextKey that = (ContextKey) o;
            return packetIdentifier == that.packetIdentifier &&
                    address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packetIdentifier, address);
        }
    }

    private class Context {

        private final String sharedSecret;
        private final RadiusPacket request;
        private final Promise<RadiusPacket> response = new DefaultPromise<>();

        Context(String sharedSecret, RadiusPacket request) {
            this.sharedSecret = sharedSecret;
            this.request = request;
        }
    }
}
