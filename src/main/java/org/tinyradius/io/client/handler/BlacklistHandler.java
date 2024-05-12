package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
@RequiredArgsConstructor
public class BlacklistHandler extends ChannelOutboundHandlerAdapter {

    private final BlacklistManager blacklistManager;

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
