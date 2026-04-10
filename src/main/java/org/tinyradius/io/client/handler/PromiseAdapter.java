package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.client.PendingRequestCtx;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.tinyradius.core.attribute.AttributeTypes.PROXY_STATE;

/**
 * ClientHandler that matches requests/response by appending Proxy-State attribute to
 * outbound packets. This avoids the problem with mismatched requests/responses when using
 * packet id, which is limited to 256 unique IDs.
 */
public class PromiseAdapter extends MessageToMessageCodec<RadiusResponse, PendingRequestCtx> {

    private static final Logger log = LogManager.getLogger(PromiseAdapter.class);
    private final Map<String, Request> requests;

    /**
     * Creates a new PromiseAdapter with a custom map supplier for pending requests.
     *
     * @param mapSupplier The supplier that provides the map for storing pending requests.
     */
    public PromiseAdapter(Supplier<Map<String, Request>> mapSupplier) {
        this.requests = mapSupplier.get();
    }

    /**
     * Creates a new PromiseAdapter with a default map (ConcurrentHashMap) for pending requests.
     */
    public PromiseAdapter() {
        this(ConcurrentHashMap::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void encode(@NonNull ChannelHandlerContext ctx, @NonNull PendingRequestCtx msg, @NonNull List<Object> out) {
        var packet = msg.getRequest();
        var requestId = UUID.randomUUID().toString();

        try {
            var encodedRequest = packet
                    .addAttribute(packet.getDictionary().createAttribute(-1, PROXY_STATE, requestId.getBytes(UTF_8)))
                    .encodeRequest(msg.getEndpoint().secret());

            msg.getResponse().addListener(f -> {
                requests.remove(requestId);
                log.debug("Removing {} from pending requests", requestId);
            });

            requests.put(requestId, new Request(msg.getEndpoint().secret(), encodedRequest.getAuthenticator(), encodedRequest.getId(), msg.getResponse()));
            log.debug("Adding {} to pending requests", requestId);

            out.add(new PendingRequestCtx(encodedRequest, msg.getEndpoint(), msg.getResponse()));
        } catch (RadiusPacketException e) {
            log.warn("Could not encode packet", e);
            msg.getResponse().tryFailure(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decode(@NonNull ChannelHandlerContext ctx, @NonNull RadiusResponse msg, @NonNull List<Object> out) {

        // retrieve my Proxy-State attribute (the last)
        var proxyStates = msg.getAttributes(PROXY_STATE);
        if (proxyStates.isEmpty()) {
            log.warn("Ignoring response - no Proxy-State attribute");
            return;
        }

        var requestId = new String(proxyStates.get(proxyStates.size() - 1).getValue(), UTF_8);
        var request = requests.get(requestId);

        if (request == null) {
            log.warn("Ignoring response - request context not found, requestId {}", requestId);
            return;
        }

        if (msg.getId() != request.id) {
            log.warn("Ignoring response - identifier mismatch, requestId {}, responseId {}",
                    request.id, msg.getId());
            return;
        }

        try {
            var response = msg.decodeResponse(request.secret, request.auth)
                    .removeLastAttribute(PROXY_STATE);

            log.debug("Found request for response identifier {}, proxyState requestId '{}'",
                    response.getId(), requestId);
            request.promise.trySuccess(response);

            // intentionally nothing to pass through - listeners should hook onto promise
        } catch (RadiusPacketException e) {
            log.warn("Could not decode packet", e);
        }
    }

    /**
     * Internal record to store request context for matching responses.
     *
     * @param secret  The RADIUS shared secret used for this request.
     * @param auth    The authenticator of the request packet.
     * @param id      The packet identifier.
     * @param promise The promise to be completed when a matching response is received.
     */
    public record Request(String secret, byte[] auth, int id, Promise<RadiusResponse> promise) {
    }

}
