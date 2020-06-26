package org.tinyradius.attribute.encrypt;

import org.tinyradius.util.RadiusPacketException;

public interface AttributeCodec {

    /**
     * Encodes plaintext data
     *
     * @param data          the data to encrypt
     * @param sharedSecret  shared secret
     * @param authenticator packet authenticator
     * @return the byte array containing the encrypted data
     */
    byte[] encode(byte[] data, String sharedSecret, byte[] authenticator) throws RadiusPacketException;

    /**
     * Decodes the passed encoded attribute data and returns the cleartext form as bytes
     *
     * @param data          data to decrypt
     * @param sharedSecret  shared secret
     * @param authenticator packet authenticator
     * @return decrypted data
     */
    byte[] decode(byte[] data, String sharedSecret, byte[] authenticator) throws RadiusPacketException;
}
