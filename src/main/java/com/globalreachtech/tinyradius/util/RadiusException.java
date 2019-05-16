/**
 * $Id: RadiusException.java,v 1.2 2005/10/15 11:35:30 wuttke Exp $
 * Created on 10.04.2005
 * @author Matthias Wuttke
 * @version $Revision: 1.2 $
 */
package com.globalreachtech.tinyradius.util;

/**
 * An exception which occurs on Radius protocol errors like
 * invalid packets or malformed attributes.
 */
public class RadiusException extends Exception {

	/**
	 *
	 */
	public RadiusException() {
		super();
	}

	/**
	 * @param message
	 */
	public RadiusException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RadiusException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param cause
	 */
	public RadiusException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 2201204523946051388L;

}
