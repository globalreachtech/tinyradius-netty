package com.globalreachtech.pas;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import io.netty.util.Timeout;
import io.netty.util.concurrent.Future;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

public class RequestContext {

    private static Log logger = LogFactory.getLog(RequestContext.class);

    private RequestState state;

    private final long requestTime;
    private long responseTime;
    private final int id;
    private Timeout timeout;
    private final AtomicInteger attempts;

    Future<Void> requestFuture;

    private RadiusPacket upstreamRequest;
    private RadiusPacket upstreamResponse;

    private final RadiusPacket clientRequest;
    private RadiusPacket clientResponse;

    private final RadiusEndpoint endpoint;

    public RequestContext(int id, RadiusPacket clientRequest, RadiusEndpoint endpoint, long timeoutNS) {
        this.id = id;
        this.attempts = new AtomicInteger(0);
        this.clientRequest = requireNonNull(clientRequest, "clientRequest cannot be null");
        this.endpoint = requireNonNull(endpoint, "endpoint cannot be null");
        this.requestTime = System.nanoTime();
        this.clientRequest.setPacketIdentifier(id & 0xff);
    }

    public int identifier() {
        return id;
    }

    public RadiusPacket clientRequest() {
        return clientRequest;
    }

    public RadiusPacket clientResponse() {
        return clientResponse;
    }

    public long calculateResponseTime() {
        if (responseTime == 0)
            responseTime = System.nanoTime() - requestTime;
        return responseTime;
    }

    private AtomicInteger attempts() {
        return this.attempts;
    }

    public long requestTime() {
        return requestTime;
    }

    public long responseTime() {
        return responseTime;
    }

    public void setClientResponse(RadiusPacket clientResponse) {
        this.clientResponse = clientResponse;
    }

    public RadiusEndpoint endpoint() {
        return endpoint;
    }

    public String toString() {
        return Long.toString(this.id);
    }
}
