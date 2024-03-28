package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.io.client.PendingRequestCtx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;

/**
 * ChannelOutboundHandler that adds support for blacklists after multiple failures.
 * <p>
 * Can be placed at any point where PendingRequestCtx is available, as it hooks onto the promise
 * outcomes to catch all failure scenarios (e.g. timeouts). However, the earlier it's hooked, the
 * sooner it can fail fast the request if the endpoint is blacklisted.
 */
public class BlacklistHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger();

    private final BlacklistManager blacklistManager;

    public BlacklistHandler(BlacklistManager blacklistManager) {
        this.blacklistManager = blacklistManager;
    }

    public BlacklistHandler(long blacklistTtlMs, int failCountThreshold, Clock clock) {
        this(new DefaultBlacklistManager(blacklistTtlMs, failCountThreshold, clock));
    }

    public BlacklistHandler(long blacklistTtlMs, int failCountThreshold) {
        this(blacklistTtlMs, failCountThreshold, Clock.systemUTC());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof PendingRequestCtx) {
            final PendingRequestCtx request = (PendingRequestCtx) msg;
            final InetSocketAddress address = request.getEndpoint().getAddress();

            if (blacklistManager.isBlacklisted(address)) {
                logger.debug("Endpoint blacklisted: {}", address);
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
