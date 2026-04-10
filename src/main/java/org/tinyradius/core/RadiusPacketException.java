package org.tinyradius.core;

import org.jspecify.annotations.NonNull;

/**
 * An exception which occurs on Radius protocol errors like
 * invalid packets or malformed attributes.
 */
public class RadiusPacketException extends Exception {

    /**
     * Constructs a new RadiusPacketException with the specified detail message.
     *
     * @param message the detail message
     */
    public RadiusPacketException(@NonNull String message) {
        super(message);
    }

    /**
     * Constructs a new RadiusPacketException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public RadiusPacketException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }
}
