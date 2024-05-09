package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.concurrent.Promise;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.rfc.Rfc2865;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.client.PendingRequestCtx;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * ClientHandler that matches requests/response by appending Proxy-State attribute to
 * outbound packets. This avoids problem with mismatched requests/responses when using
 * packet id, which is limited to 256 unique IDs.
 */
@Log4j2
@RequiredArgsConstructor
public class PromiseAdapter extends MessageToMessageCodec<RadiusResponse, PendingRequestCtx> {

    private static final byte PROXY_STATE = Rfc2865.PROXY_STATE;

    private final Map<String, Request> requests;

    public PromiseAdapter() {
        this(new ConcurrentHashMap<>());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, PendingRequestCtx msg, List<Object> out) {
        final RadiusRequest packet = msg.getRequest();
        final String requestId = UUID.randomUUID().toString();

        try {
            final RadiusRequest encodedRequest = packet
                    .addAttribute(packet.getDictionary().createAttribute(-1, PROXY_STATE, requestId.getBytes(UTF_8)))
                    .encodeRequest(msg.getEndpoint().getSecret());

            msg.getResponse().addListener(f -> {
                requests.remove(requestId);
                log.debug("Removing {} from pending requests", requestId);
            });

            requests.put(requestId, new Request(msg.getEndpoint().getSecret(), encodedRequest.getAuthenticator(), encodedRequest.getId(), msg.getResponse()));
            log.debug("Adding {} to pending requests", requestId);

            out.add(new PendingRequestCtx(encodedRequest, msg.getEndpoint(), msg.getResponse()));
        } catch (RadiusPacketException e) {
            log.warn("Could not encode packet", e);
            msg.getResponse().tryFailure(e);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, RadiusResponse msg, List<Object> out) {

        // retrieve my Proxy-State attribute (the last)
        final List<RadiusAttribute> proxyStates = msg.getAttributes(PROXY_STATE);
        if (proxyStates.isEmpty()) {
            log.warn("Ignoring response - no Proxy-State attribute");
            return;
        }

        final String requestId = new String(proxyStates.get(proxyStates.size() - 1).getValue(), UTF_8);
        final Request request = requests.get(requestId);

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
            final RadiusResponse response = msg.decodeResponse(request.secret, request.auth)
                    .removeLastAttribute(PROXY_STATE);

            log.info("Found request for response identifier {}, proxyState requestId '{}'",
                    response.getId(), requestId);
            request.promise.trySuccess(response);

            // intentionally nothing to pass through - listeners should hook onto promise
        } catch (RadiusPacketException e) {
            log.warn("Could not decode packet", e);
        }
    }

    @RequiredArgsConstructor
    private static class Request {

        private final String secret;
        private final byte[] auth;
        private final int id;
        private final Promise<RadiusResponse> promise;
    }

}
