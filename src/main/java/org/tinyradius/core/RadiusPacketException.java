package org.tinyradius.core;

import org.jspecify.annotations.NonNull;

/**
 * An exception which occurs on Radius protocol errors like
 * invalid packets or malformed attributes.
 */
public class RadiusPacketException extends Exception {

    public RadiusPacketException(@NonNull String message) {
        super(message);
    }

    public RadiusPacketException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }
}
