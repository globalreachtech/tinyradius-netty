package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.client.PendingRequestCtx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * ClientHandler that matches requests/response by appending Proxy-State attribute to
 * outbound packets. This avoids problem with mismatched requests/responses when using
 * packet id, which is limited to 256 unique IDs.
 */
public class PromiseAdapter extends MessageToMessageCodec<RadiusResponse, PendingRequestCtx> {

    private static final Logger logger = LogManager.getLogger();

    public static final byte PROXY_STATE = 33;

    private final AtomicInteger proxyIndex = new AtomicInteger(1);

    private final Map<String, Request> requests = new ConcurrentHashMap<>();

    private String nextProxyState() {
        return Integer.toString(proxyIndex.getAndIncrement());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, PendingRequestCtx msg, List<Object> out) {
        final RadiusRequest packet = msg.getRequest();
        final String requestId = nextProxyState();

        try {
            final RadiusRequest encodedRequest = packet
                    .addAttribute(packet.getDictionary().createAttribute(-1, PROXY_STATE, requestId.getBytes(UTF_8)))
                    .encodeRequest(msg.getEndpoint().getSecret());

            msg.getResponse().addListener(f -> {
                requests.remove(requestId);
                logger.debug("Removing {} from pending requests", requestId);
            });

            requests.put(requestId, new Request(msg.getEndpoint().getSecret(), encodedRequest.getAuthenticator(), encodedRequest.getId(), msg.getResponse()));
            logger.debug("Adding {} to pending requests", requestId);

            out.add(new PendingRequestCtx(encodedRequest, msg.getEndpoint(), msg.getResponse()));
        } catch (RadiusPacketException e) {
            logger.warn("Could not encode packet", e);
            msg.getResponse().tryFailure(e);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, RadiusResponse msg, List<Object> out) {

        // retrieve my Proxy-State attribute (the last)
        final List<RadiusAttribute> proxyStates = msg.filterAttributes(PROXY_STATE);
        if (proxyStates.isEmpty()) {
            logger.warn("Ignoring response - no Proxy-State attribute");
            return;
        }

        final String requestId = new String(proxyStates.get(proxyStates.size() - 1).getValue(), UTF_8);
        final Request request = requests.get(requestId);

        if (request == null) {
            logger.warn("Ignoring response - request context not found, requestId {}", requestId);
            return;
        }

        if (msg.getId() != request.id) {
            logger.warn("Ignoring response - identifier mismatch, requestId {}, responseId {}",
                    request.id, msg.getId());
            return;
        }

        try {
            final RadiusResponse response = msg.decodeResponse(request.secret, request.auth)
                    .removeLastAttribute(PROXY_STATE);

            logger.info("Found request for response identifier {}, proxyState requestId '{}'",
                    response.getId(), requestId);
            request.promise.trySuccess(response);

            // intentionally nothing to pass through - listeners should hook onto promise
        } catch (RadiusPacketException e) {
            logger.warn("Could not decode packet", e);
        }
    }

    private static class Request {

        private final String secret;
        private final byte[] auth;
        private final int id;
        private final Promise<RadiusResponse> promise;

        Request(String secret, byte[] auth, int id, Promise<RadiusResponse> promise) {
            this.secret = secret;
            this.auth = auth;
            this.id = id;
            this.promise = promise;
        }
    }

}
