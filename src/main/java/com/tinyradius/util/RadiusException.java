package com.tinyradius.util;

/**
 * An exception which occurs on Radius protocol errors like
 * invalid packets or malformed attributes.
 */
public class RadiusException extends Exception {

    public RadiusException() {
        super();
    }

    public RadiusException(String message) {
        super(message);
    }

    public RadiusException(String message, Throwable cause) {
        super(message, cause);
    }

    public RadiusException(Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = 2201204523946051388L;

}
