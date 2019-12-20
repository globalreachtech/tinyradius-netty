package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.client.ClientResponseCtx;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.util.RadiusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tinyradius.attribute.Attributes.createAttribute;

/**
 * ClientHandler that matches requests/response by appending Proxy-State attribute to
 * outbound packets. This avoids problem with mismatched requests/responses when using
 * packetIdentifier, which is limited to 256 unique IDs.
 */
public class RequestPromiseHandler extends MessageToMessageCodec<RadiusPacket, ClientResponseCtx> {

    private static final Logger logger = LoggerFactory.getLogger(RequestPromiseHandler.class);

    private static final int PROXY_STATE = 33;

    private final AtomicInteger proxyIndex = new AtomicInteger(1);

    private final Map<String, Request> requests = new ConcurrentHashMap<>();

    private String nextProxyStateId() {
        return Integer.toString(proxyIndex.getAndIncrement());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ClientResponseCtx msg, List<Object> out) {
        final RadiusPacket packet = msg.getRequest().copy();

        final String requestId = nextProxyStateId();
        packet.addAttribute(createAttribute(packet.getDictionary(), -1, PROXY_STATE, requestId.getBytes(UTF_8)));
        final RadiusPacket encodedRequest = packet.encodeRequest(msg.getEndpoint().getSecret());

        final Promise<RadiusPacket> promise = msg.getResponse();

        requests.put(requestId, new Request(msg.getEndpoint().getSecret(), encodedRequest.getAuthenticator(), encodedRequest.getIdentifier(), promise));

        promise.addListener(f -> requests.remove(requestId));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, RadiusPacket msg, List<Object> out) {

        // retrieve my Proxy-State attribute (the last)
        List<RadiusAttribute> proxyStates = msg.getAttributes(PROXY_STATE);
        if (proxyStates.isEmpty()) {
            logger.warn("Ignoring response - no Proxy-State attribute");
            return;
        }

        RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);
        String proxyStateId = new String(proxyState.getValue(), UTF_8);

        final Request request = requests.get(proxyStateId);

        if (request == null) {
            logger.warn("Ignoring response - request context not found");
            return;
        }

        if (msg.getIdentifier() != request.identifier) {
            logger.warn("Ignoring response - identifier mismatch, request ID " + request.identifier +
                    ", response ID " + msg.getIdentifier());
        }

        try {
            msg.verify(request.secret, request.authenticator);
        } catch (RadiusException e) {
            logger.warn(e.getMessage());
            return;
        }

        msg.removeLastAttribute(PROXY_STATE);

        logger.info("Found request for response identifier => {}", msg.getIdentifier());
        request.promise.trySuccess(msg);
    }

    private static class Request {

        private final String secret;
        private final byte[] authenticator;
        private final int identifier;
        private final Promise<RadiusPacket> promise;

        Request(String secret, byte[] authenticator, int identifier, Promise<RadiusPacket> promise) {
            this.secret = secret;
            this.authenticator = authenticator;
            this.identifier = identifier;
            this.promise = promise;
        }
    }
}
