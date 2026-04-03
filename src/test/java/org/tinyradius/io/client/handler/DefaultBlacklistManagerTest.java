package org.tinyradius.io.client.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Clock;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBlacklistManagerTest {

    @Mock
    private Clock clock;

    private final SocketAddress address = new InetSocketAddress("127.0.0.1", 1812);

    @Test
    void testBlacklist() {
        DefaultBlacklistManager manager = new DefaultBlacklistManager(1000, 3, clock);

        when(clock.millis()).thenReturn(1000L);

        // not blacklisted initially
        assertFalse(manager.isBlacklisted(address));

        // log 2 failures (less than threshold 3)
        manager.logFailure(address, new TimeoutException());
        manager.logFailure(address, new TimeoutException());
        assertFalse(manager.isBlacklisted(address));

        // 3rd failure - blacklisted
        manager.logFailure(address, new TimeoutException());
        assertTrue(manager.isBlacklisted(address));

        // check expiry (still active at 1500)
        when(clock.millis()).thenReturn(1500L);
        assertTrue(manager.isBlacklisted(address));

        // check expiry (expired at 2100)
        when(clock.millis()).thenReturn(2100L);
        assertFalse(manager.isBlacklisted(address));

        // should be removed from maps after expiry check
        assertFalse(manager.isBlacklisted(address));
    }

    @Test
    void testReset() {
        DefaultBlacklistManager manager = new DefaultBlacklistManager(1000, 1, clock);
        when(clock.millis()).thenReturn(1000L);

        manager.logFailure(address, new TimeoutException());
        assertTrue(manager.isBlacklisted(address));

        manager.reset(address);
        assertFalse(manager.isBlacklisted(address));
    }

    @Test
    void testNonTimeoutFailure() {
        DefaultBlacklistManager manager = new DefaultBlacklistManager(1000, 1, clock);
        manager.logFailure(address, new RuntimeException());
        assertFalse(manager.isBlacklisted(address));
    }
}
