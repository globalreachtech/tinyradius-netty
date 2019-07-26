package org.tinyradius.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
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

import static java.lang.Byte.toUnsignedInt;

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

    private final Dictionary dictionary;

    private final Map<RequestKey, Request> contexts = new ConcurrentHashMap<>();

    public SimpleClientHandler(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public RadiusPacket prepareRequest(RadiusPacket original, RadiusEndpoint endpoint, Promise<RadiusPacket> promise) {

        final RadiusPacket encoded = original.encodeRequest(endpoint.getSharedSecret());

        final RequestKey key = new RequestKey(encoded.getIdentifier(), endpoint.getAddress());
        final Request request = new Request(endpoint.getSharedSecret(), encoded, promise);
        contexts.put(key, request);

        promise.addListener(f -> contexts.remove(key));

        return encoded;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        int identifier = toUnsignedInt(packet.content().duplicate().skipBytes(1).readByte());
        final RequestKey key = new RequestKey(identifier, packet.sender());

        final Request request = contexts.get(key);
        if (request == null) {
            logger.info("Request context not found for received packet, ignoring...");
            return;
        }

        try {
            RadiusPacket resp = RadiusPacketEncoder.fromDatagram(dictionary, packet, request.sharedSecret, request.packet);
            logger.info("Found request for response identifier => {}", identifier);

            request.promise.trySuccess(resp);
        } catch (RadiusException e) {
            logger.error("DatagramPacket handle error: ", e);
        } finally {
            packet.release();
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
        private final Promise<RadiusPacket> promise;

        Request(String sharedSecret, RadiusPacket packet, Promise<RadiusPacket> promise) {
            this.sharedSecret = sharedSecret;
            this.packet = packet;
            this.promise = promise;
        }
    }
}
