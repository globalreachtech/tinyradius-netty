package org.tinyradius.core;

/**
 * An exception which occurs on Radius protocol errors like
 * invalid packets or malformed attributes.
 */
public class RadiusPacketException extends Exception {

    public RadiusPacketException(String message) {
        super(message);
    }

    public RadiusPacketException(String message, Throwable cause) {
        super(message, cause);
    }
}
