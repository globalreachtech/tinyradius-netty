package org.tinyradius.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPacketEncoder;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * ClientHandler that uses packetIdentifier and remote address to uniquely identify request/responses.
 * <p>
 * Note that packetIdentifier is limited to 1 byte, i.e. only 256 unique IDs. A large number of requests
 * in short period of time may cause requests/responses to be mismatched.
 * <p>
 * When response is received, it also verifies against the authenticator of the original request to decode.
 */
public class SimpleClientHandler extends ClientHandler {

    private static final Logger logger = LoggerFactory.getLogger(SimpleClientHandler.class);

    private final Timer timer;
    private final Dictionary dictionary;
    private final int timeoutMs;

    private final Map<RequestKey, Request> contexts = new ConcurrentHashMap<>();

    public SimpleClientHandler(Timer timer, Dictionary dictionary, int timeoutMs) {
        this.timer = timer;
        this.dictionary = dictionary;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Promise<RadiusPacket> processRequest(RadiusPacket packet, RadiusEndpoint endpoint, EventExecutor eventExecutor) {

        final RequestKey key = new RequestKey(packet.getPacketIdentifier(), endpoint.getEndpointAddress());
        final Request request = new Request(endpoint.getSharedSecret(), packet, eventExecutor.newPromise());
        contexts.put(key, request);

        final Timeout timeout = timer.newTimeout(
                t -> request.response.tryFailure(new RadiusException("Timeout occurred")), timeoutMs, MILLISECONDS);

        request.response.addListener(f -> {
            contexts.remove(key);
            timeout.cancel();
        });

        return request.response;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        int identifier = packet.content().duplicate().skipBytes(1).readByte() & 0xff;
        final RequestKey key = new RequestKey(identifier, packet.sender());

        final Request request = contexts.get(key);
        if (request == null) {
            logger.info("Request context not found for received packet, ignoring...");
            return;
        }

        try {
            RadiusPacket resp = RadiusPacketEncoder.fromResponseDatagram(dictionary, packet, request.sharedSecret, request.packet);
            logger.info("Found request for response identifier => {}", identifier);

            request.response.trySuccess(resp);
        } catch (RadiusException e) {
            logger.error("DatagramPacket handle error: ", e);
            // let timeout complete the future, we may get correct reply later
        }
    }

    private static class RequestKey {
        private final int packetIdentifier;
        private final InetSocketAddress address;

        RequestKey(int packetIdentifier, InetSocketAddress address) {
            this.packetIdentifier = packetIdentifier;
            this.address = address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RequestKey that = (RequestKey) o;
            return packetIdentifier == that.packetIdentifier &&
                    address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packetIdentifier, address);
        }
    }

    private static class Request {

        private final String sharedSecret;
        private final RadiusPacket packet;
        private final Promise<RadiusPacket> response;

        Request(String sharedSecret, RadiusPacket packet, Promise<RadiusPacket> response) {
            this.sharedSecret = sharedSecret;
            this.packet = packet;
            this.response = response;
        }
    }
}
