package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.client.PendingRequestCtx;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * ClientHandler that matches requests/response by appending Proxy-State attribute to
 * outbound packets. This avoids problem with mismatched requests/responses when using
 * packetIdentifier, which is limited to 256 unique IDs.
 */
public class PromiseAdapter extends MessageToMessageCodec<RadiusResponse, PendingRequestCtx> {

    private static final Logger logger = LogManager.getLogger();

    private static final byte PROXY_STATE = 33;

    private final AtomicInteger proxyIndex = new AtomicInteger(1);

    private final Map<String, Request> requests = new ConcurrentHashMap<>();

    private String nextProxyStateId() {
        return Integer.toString(proxyIndex.getAndIncrement());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, PendingRequestCtx msg, List<Object> out) {
        final RadiusRequest packet = msg.getRequest();
        final String requestId = nextProxyStateId();

        try {
            /*
             * ideally encode as late as possible, just before convert to datagram
             * however we need to generate a copy of the authenticator now for later lookups
             * encodeRequest() should be idempotent anyway
             */
            final RadiusRequest encodedRequest = packet
                    .addAttribute(packet.getDictionary().createAttribute(-1, PROXY_STATE, requestId.getBytes(UTF_8)))
                    .encodeRequest(msg.getEndpoint().getSecret());

            msg.getResponse().addListener(f -> requests.remove(requestId));
            requests.put(requestId, new Request(msg.getEndpoint().getSecret(), encodedRequest.getAuthenticator(), encodedRequest.getId(), msg.getResponse()));

            out.add(new PendingRequestCtx(encodedRequest, msg.getEndpoint(), msg.getResponse()));
        } catch (RadiusPacketException e) {
            logger.warn("Could not encode Radius packet: {}", e.getMessage());
            msg.getResponse().tryFailure(e);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, RadiusResponse msg, List<Object> out) {

        // retrieve my Proxy-State attribute (the last)
        List<RadiusAttribute> proxyStates = msg.filterAttributes(PROXY_STATE);
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

        if (msg.getId() != request.identifier) {
            logger.warn("Ignoring response - identifier mismatch, request ID {}, response ID {}", request.identifier, msg.getId());
            return;
        }

        try {
            final RadiusResponse response = msg.decodeResponse(request.secret, request.authenticator)
                    .removeLastAttribute(PROXY_STATE);

            logger.info("Found request for response identifier => {}", response.getId());
            request.promise.trySuccess(response);

            // intentionally nothing to pass through - listeners should hook onto promise
        } catch (RadiusPacketException e) {
            logger.warn(e.getMessage());
        }
    }

    private static class Request {

        private final String secret;
        private final byte[] authenticator;
        private final int identifier;
        private final Promise<RadiusResponse> promise;

        Request(String secret, byte[] authenticator, int identifier, Promise<RadiusResponse> promise) {
            this.secret = secret;
            this.authenticator = authenticator;
            this.identifier = identifier;
            this.promise = promise;
        }
    }

}
