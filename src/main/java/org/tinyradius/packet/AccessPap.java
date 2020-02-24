package org.tinyradius.packet;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.Attributes.createAttribute;

public class AccessPap extends AccessRequest {

    private transient String password;

    public AccessPap(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes, String plaintextPw) {
        this(dictionary, identifier, authenticator, attributes);
        setPlaintextPassword(plaintextPw);
    }

    public AccessPap(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, identifier, authenticator, attributes);
    }

    /**
     * Sets the plain-text user password.
     *
     * @param userPassword user password to set
     */
    public void setPlaintextPassword(String userPassword) {
        requireNonNull(userPassword, "User password not set");
        if (userPassword.isEmpty())
            throw new IllegalArgumentException("Password is empty");
        this.password = userPassword;
    }

    /**
     * Retrieves the plain-text user password.
     *
     * @return user password in plaintext if decoded
     */
    public String getUserPassword() {
        return password;
    }

    /**
     * Sets and encrypts the User-Password attribute.
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @param newAuth      authenticator to use to encode PAP password,
     *                     nullable if using different auth protocol
     * @return List of RadiusAttributes to override
     */
    @Override
    protected AccessRequest encodeRequest(String sharedSecret, byte[] newAuth) throws RadiusPacketException {
        if (password == null || password.isEmpty()) {
            logger.warn("Could not encode PAP attributes, password not set");
            throw new RadiusPacketException("Could not encode PAP attributes, password not set");
        }
        final AccessPap accessPap = new AccessPap(getDictionary(), getIdentifier(), newAuth, new ArrayList<>(getAttributes()), password);
        accessPap.removeAttributes(USER_PASSWORD);
        accessPap.addAttribute(createAttribute(getDictionary(), -1, USER_PASSWORD,
                encodePapPassword(newAuth, password.getBytes(UTF_8), sharedSecret.getBytes(UTF_8))));

        return accessPap;
    }

    /**
     * Verifies that the passed plain-text password matches the password
     * (hash) send with this Access-Request packet.
     *
     * @param plaintext password to verify packet against
     * @return true if the password is valid, false otherwise
     */
    public boolean verifyPassword(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            logger.warn("Plaintext password to check against is empty");
            return false;
        }

        final String userPassword = getUserPassword();
        if (userPassword == null || userPassword.isEmpty()) {
            logger.warn("Password to check is empty - verify and decode with shared secret first");
            return false;
        }

        return userPassword.equals(plaintext);
    }

    /**
     * AccessRequest cannot verify authenticator as they
     * contain random bytes.
     * <p>
     * Instead it checks the User-Password attributes
     * are present and attempts decryption for PAP.
     *
     * @param sharedSecret shared secret
     * @param requestAuth  ignored, not applicable for AccessRequest
     */
    @Override
    public void verify(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        this.password = decodePapPassword(sharedSecret.getBytes(UTF_8));
    }

    /**
     * This method encodes the plaintext user password according to RFC 2865.
     *
     * @param userPass     the password to encrypt
     * @param sharedSecret shared secret
     * @return the byte array containing the encrypted password
     */
    private byte[] encodePapPassword(byte[] authenticator, byte[] userPass, byte[] sharedSecret) {
        requireNonNull(userPass, "userPass cannot be null");
        requireNonNull(sharedSecret, "sharedSecret cannot be null");

        byte[] ciphertext = authenticator;
        byte[] P = pad(userPass);
        final ByteBuffer buffer = ByteBuffer.allocate(P.length);

        for (int i = 0; i < P.length; i += 16) {
            ciphertext = xor16(P, i, md5(sharedSecret, ciphertext));
            buffer.put(ciphertext);
        }

        return buffer.array();
    }

    /**
     * Decodes the passed encrypted password and returns the cleartext form.
     *
     * @param sharedSecret shared secret
     * @return decrypted password
     */
    private String decodePapPassword(byte[] sharedSecret) throws RadiusPacketException {
        final byte[] encryptedPass = getAttribute(USER_PASSWORD).getValue();
        if (encryptedPass.length < 16) {
            // PAP passwords require at least 16 bytes, or multiples thereof
            logger.warn("Malformed packet: User-Password attribute length must be greater than 15, actual {}", encryptedPass.length);
            throw new RadiusPacketException("Malformed User-Password attribute");
        }

        final ByteBuffer buffer = ByteBuffer.allocate(encryptedPass.length);
        byte[] ciphertext = this.getAuthenticator();

        for (int i = 0; i < encryptedPass.length; i += 16) {
            buffer.put(xor16(encryptedPass, i, md5(sharedSecret, ciphertext)));
            ciphertext = Arrays.copyOfRange(encryptedPass, i, 16);
        }

        return stripNullPadding(new String(buffer.array(), UTF_8));
    }

    private static String stripNullPadding(String s) {
        int i = s.indexOf('\0');
        return (i > 0) ? s.substring(0, i) : s;
    }


    private byte[] md5(byte[] a, byte[] b) {
        MessageDigest md = getMd5Digest();
        md.update(a);
        return md.digest(b);
    }

    private static byte[] xor16(byte[] src1, int src1offset, byte[] src2) {

        byte[] dst = new byte[16];

        requireNonNull(src1, "src1 is null");
        requireNonNull(src2, "src2 is null");

        if (src1offset < 0)
            throw new IndexOutOfBoundsException("src1offset is less than 0");
        if ((src1offset + 16) > src1.length)
            throw new IndexOutOfBoundsException("bytes in src1 is less than src1offset plus 16");
        if (16 > src2.length)
            throw new IndexOutOfBoundsException("bytes in src2 is less than 16");

        for (int i = 0; i < 16; i++) {
            dst[i] = (byte) (src1[i + src1offset] ^ src2[i]);
        }

        return dst;
    }

    static byte[] pad(byte[] val) {
        requireNonNull(val, "value cannot be null");

        int length = Math.max(
                (int) (Math.ceil((double) val.length / 16) * 16), 16);

        byte[] padded = new byte[length];

        System.arraycopy(val, 0, padded, 0, val.length);

        return padded;
    }

    @Override
    public AccessRequest copy() {
        return new AccessPap(getDictionary(), getIdentifier(), getAuthenticator(), new ArrayList<>(getAttributes()), password);
    }

}
