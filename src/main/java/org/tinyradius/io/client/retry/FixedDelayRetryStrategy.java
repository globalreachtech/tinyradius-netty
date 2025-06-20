package org.tinyradius.io.client.retry;

import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
public class FixedDelayRetryStrategy implements RetryStrategy {

    private final int maxAttempts;

    private final Duration retryDelay;


    @Override
    public int maxAttempts() {
        return maxAttempts;
    }

    @Override
    public Duration delay(int attempts) {
        return retryDelay;
    }

}
