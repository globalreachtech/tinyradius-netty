package com.globalreachtech.tinyradius.grt;

import com.globalreachtech.tinyradius.netty.RadiusClient;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.Future;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

public class RequestContext {

    private static Log logger = LogFactory.getLog(RequestContext.class);

    private RequestState state;

    private int retransmits = 3;

    private long requestTime;
    private long responseTime;
    private int identifier; //id
    private Timeout timeout;
    private long timeoutNS;
    private AtomicInteger attempts;

    Future<Void> requestFuture;

    private RadiusPacket upstreamRequest;
    private RadiusPacket upstreamResponse;

    private RadiusPacket clientRequest;
    private RadiusPacket clientResponse;

    private RadiusEndpoint endpoint;

    public RequestContext(int identifier, RadiusPacket clientRequest, RadiusEndpoint endpoint, long timeoutNS) {
        this.identifier = identifier;
        this.attempts = new AtomicInteger(0);
        this.clientRequest = requireNonNull(clientRequest, "clientRequest cannot be null");
        this.endpoint = requireNonNull(endpoint, "endpoint cannot be null");
        this.requestTime = System.nanoTime();
        this.timeoutNS = timeoutNS;
        this.clientRequest.setPacketIdentifier(identifier & 0xff);
    }

    public void newTimeout(Timer timer) {
        newTimeout(timer, timeout -> {
            if (this.attempts().intValue() < retransmits) {
                logger.info(String.format("Retransmitting clientRequest for context %d", this.identifier()));
                sendRequest(this);
                this.newTimeout(RadiusClient.this.timer, timeout.task());
            } else {
                if (!promise.isDone()) {
                    promise.setFailure(new RadiusException("Timeout occurred"));
                    RadiusClient.this.dequeue(this);
                }
            }
        });
    }

    public void sendPacket()

    public void newTimeout(Timer timer, TimerTask task) {
        if (this.timeout != null) {
            if (!this.timeout.isExpired())
                this.timeout.cancel();
        }
        this.timeout = timer.newTimeout(task, timeoutNS / retransmits, TimeUnit.NANOSECONDS);
    }



    private RequestContext queue(RequestContext context) {
        requireNonNull(context, "context cannot be null");

        queue.add(context, context.clientRequest().getPacketIdentifier());

        context.newTimeout(timer);

        if (logger.isInfoEnabled())
            logger.info(String.format("Queued clientRequest %d identifier => %d",
                    context.identifier(), context.clientRequest().getPacketIdentifier()));

        return context;
    }

    private boolean dequeue(RequestContext context) {
        requireNonNull(context, "promise cannot be null");

        boolean success = queue.remove(context, context.clientRequest().getPacketIdentifier());
        if (success) {
            if (!context.timeout.isExpired())
                context.timeout.cancel();
        }
        return success;
    }

    public int identifier() {
        return identifier;
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
        return Long.toString(this.identifier);
    }
}
