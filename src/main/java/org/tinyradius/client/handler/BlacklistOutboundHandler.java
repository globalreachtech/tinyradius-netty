package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.client.PendingRequestCtx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ChannelOutboundHandler that adds support for blacklists after multiple failures.
 * <p>
 * Can be placed at any point where PendingRequestCtx is available, as it hooks onto the promise
 * outcomes to catch all failure scenarios (e.g. timeouts). However, the earlier it's hooked, the
 * sooner it can fail fast the request if the endpoint is blacklisted.
 */
public class BlacklistOutboundHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger();

    private final long blacklistTtlMs;
    private final int failCountThreshold;

    private final Map<SocketAddress, AtomicInteger> failureCounts = new ConcurrentHashMap<>();
    private final Map<SocketAddress, Long> blacklisted = new ConcurrentHashMap<>();

    public BlacklistOutboundHandler(long blacklistTtlMs, int failCountThreshold) {
        this.blacklistTtlMs = blacklistTtlMs;
        this.failCountThreshold = failCountThreshold;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof PendingRequestCtx) {
            final PendingRequestCtx request = (PendingRequestCtx) msg;
            final InetSocketAddress address = request.getEndpoint().getAddress();

            if (isBlacklisted(address)) {
                request.getResponse().tryFailure(new IOException("Client send failed, endpoint blacklisted: " + address));
                return;
            }

            request.getResponse().addListener(f -> logResult(address, f.isSuccess()));
        }

        ctx.write(msg, promise);
    }


    boolean isBlacklisted(SocketAddress socketAddress) {
        final Long blacklistExpiry = blacklisted.get(socketAddress);

        if (blacklistExpiry == null)
            return false;

        if (System.currentTimeMillis() < blacklistExpiry) {
            logger.debug("Endpoint blacklisted while proxying packet to {}", socketAddress);
            return true;
        }

        // expired
        blacklisted.remove(socketAddress);
        failureCounts.remove(socketAddress);
        logger.info("Endpoint {} removed from blacklist (expired)", socketAddress);
        return false;
    }

    void logResult(SocketAddress socketAddress, boolean isSuccess) {
        if (isSuccess) {
            failureCounts.remove(socketAddress);
        } else {
            final int i = failureCounts.computeIfAbsent(socketAddress, d -> new AtomicInteger()).incrementAndGet();
            if (i >= failCountThreshold) {
                // dont reset countdown time if already blacklisted
                final Long prevBlacklist = blacklisted.putIfAbsent(socketAddress, System.currentTimeMillis() + blacklistTtlMs);
                if (prevBlacklist == null)
                    logger.debug("Endpoint {} added to blacklist", socketAddress);
            }
        }
    }
}
