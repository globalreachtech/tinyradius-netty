package org.tinyradius.io.client.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketAddress;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultBlacklistManager implements BlacklistManager {
    private static final Logger logger = LogManager.getLogger();
    private final long blacklistTtlMs;
    private final int failCountThreshold;
    private final Clock clock;
    private final Map<SocketAddress, AtomicInteger> failCounts = new ConcurrentHashMap<>();
    private final Map<SocketAddress, Long> blacklist = new ConcurrentHashMap<>();

    public DefaultBlacklistManager(long blacklistTtlMs, int failCountThreshold, Clock clock) {
        this.blacklistTtlMs = blacklistTtlMs;
        this.failCountThreshold = failCountThreshold;
        this.clock = clock;
    }

    @Override
    public boolean isBlacklisted(SocketAddress socketAddress) {
        Long blacklistExpiry = blacklist.get(socketAddress);

        // not blacklisted
        if (blacklistExpiry == null)
            return false;

        // blacklist active
        if (clock.millis() < blacklistExpiry) {
            return true;
        }

        // blacklist expired
        reset(socketAddress);
        logger.info("Endpoint {} removed from blacklist (expired)", socketAddress);
        return false;
    }

    @Override
    public void logFailure(SocketAddress address, Throwable cause) {
        if (cause instanceof TimeoutException) {
            final int failCount = failCounts.computeIfAbsent(address, k -> new AtomicInteger()).incrementAndGet();

            if (failCount >= failCountThreshold && blacklist.get(address) == null) {

                // only set if isn't already blacklisted, to avoid delayed responses extending ttl
                blacklist.put(address, clock.millis() + blacklistTtlMs);
                logger.debug("Endpoint {} added to blacklist", address);
            }
        }
    }

    @Override
    public void reset(SocketAddress socketAddress) {
        blacklist.remove(socketAddress);
        failCounts.remove(socketAddress);
    }
}
