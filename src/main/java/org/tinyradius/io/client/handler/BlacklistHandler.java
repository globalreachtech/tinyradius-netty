package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.io.client.PendingRequestCtx;

import java.io.IOException;
import java.time.Clock;

/**
 * ChannelOutboundHandler that adds support for blacklists after multiple failures.
 * <p>
 * Can be placed at any point where PendingRequestCtx is available, as it hooks onto the promise
 * outcomes to catch all failure scenarios (e.g. timeouts). However, the earlier it's hooked, the
 * sooner it can fail fast the request if the endpoint is blacklisted.
 */
public class BlacklistHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(BlacklistHandler.class);
    private final BlacklistManager blacklistManager;

    /**
     * Constructs a {@code BlacklistHandler} with the specified {@link BlacklistManager}.
     *
     * @param blacklistManager the manager to use for blacklisting endpoints
     */
    public BlacklistHandler(BlacklistManager blacklistManager) {
        this.blacklistManager = blacklistManager;
    }

    /**
     * Creates a new BlacklistHandler with a {@link DefaultBlacklistManager}.
     *
     * @param blacklistTtlMs     time-to-live for blacklist entries in milliseconds
     * @param failCountThreshold number of failures before blacklisting an endpoint
     * @param clock              clock for timestamp operations
     */
    public BlacklistHandler(long blacklistTtlMs, int failCountThreshold, @NonNull Clock clock) {
        this(new DefaultBlacklistManager(blacklistTtlMs, failCountThreshold, clock));
    }

    /**
     * Creates a new BlacklistHandler with a {@link DefaultBlacklistManager} using UTC clock.
     *
     * @param blacklistTtlMs     time-to-live for blacklist entries in milliseconds
     * @param failCountThreshold number of failures before blacklisting an endpoint
     */
    public BlacklistHandler(long blacklistTtlMs, int failCountThreshold) {
        this(blacklistTtlMs, failCountThreshold, Clock.systemUTC());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(@NonNull ChannelHandlerContext ctx, @NonNull Object msg, @NonNull ChannelPromise promise) {
        if (msg instanceof PendingRequestCtx request) {
            var address = request.getEndpoint().address();

            if (blacklistManager.isBlacklisted(address)) {
                log.debug("Endpoint blacklisted: {}", address);
                request.getRequest().toByteBuf().release();
                request.getResponse().tryFailure(new IOException("Client send failed - endpoint blacklisted: " + address));
                return;
            }

            request.getResponse().addListener(f -> {
                if (f.isSuccess()) {
                    blacklistManager.reset(address);
                } else {
                    blacklistManager.logFailure(address, f.cause());
                }
            });
        }

        ctx.write(msg, promise);
    }
}
