package org.tinyradius.io.client.retry;

import java.time.Duration;

public interface RetryStrategy {

    int maxAttempts();

    Duration delay(int attempts);

}
