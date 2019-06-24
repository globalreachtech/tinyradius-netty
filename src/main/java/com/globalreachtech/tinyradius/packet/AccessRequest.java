package com.globalreachtech.tinyradius.packet;

import com.globalreachtech.tinyradius.attribute.RadiusAttribute;
import com.globalreachtech.tinyradius.attribute.StringAttribute;
import com.globalreachtech.tinyradius.util.RadiusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * This class represents an Access-Request Radius packet.
 */
public class AccessRequest extends RadiusPacket {

    /**
     * Passphrase Authentication Protocol
     */
    public static final String AUTH_PAP = "pap";

    /**
     * Challenged Handshake Authentication Protocol
     */
    public static final String AUTH_CHAP = "chap";


    /**
     * Temporary storage for the unencrypted User-Password
     * attribute.
     */
    private String password;

    /**
     * Authentication protocol for this access clientRequest.
     */
    private String authProtocol = AUTH_PAP;

    /**
     * CHAP password from a decoded CHAP Access-Request.
     */
    private byte[] chapPassword;

    /**
     * CHAP challenge from a decoded CHAP Access-Request.
     */
    private byte[] chapChallenge;

    /**
     * Random generator
     */
    private static final SecureRandom random = new SecureRandom();

    /**
     * Radius type code for Radius attribute User-Name
     */
    private static final int USER_NAME = 1;

    /**
     * Radius attribute type for User-Password attribute.
     */
    private static final int USER_PASSWORD = 2;

    /**
     * Radius attribute type for CHAP-Password attribute.
     */
    private static final int CHAP_PASSWORD = 3;

    /**
     * Radius attribute type for CHAP-Challenge attribute.
     */
    private static final int CHAP_CHALLENGE = 60;

    /**
     * Logger for logging information about malformed packets
     */
    private static final Logger logger = LoggerFactory.getLogger(AccessRequest.class);

    /**
     * Constructs an empty Access-Request packet.
     */
    public AccessRequest() {
        super();
    }

    /**
     * Constructs an Access-Request packet, sets the
     * code, identifier and adds an User-Name and an
     * User-Password attribute (PAP).
     *
     * @param userName     user name
     * @param userPassword user password
     */
    public AccessRequest(String userName, String userPassword) {
        super(ACCESS_REQUEST, getNextPacketIdentifier());
        setUserName(userName);
        setUserPassword(userPassword);
    }

    /**
     * Sets the User-Name attribute of this Access-Request.
     *
     * @param userName user name to set
     */
    public void setUserName(String userName) {
        requireNonNull(userName, "user name not set");
        if (userName.isEmpty())
            throw new IllegalArgumentException("empty user name not allowed");

        removeAttributes(USER_NAME);
        addAttribute(new StringAttribute(USER_NAME, userName));
    }

    /**
     * Sets the plain-text user password.
     *
     * @param userPassword user password to set
     */
    public void setUserPassword(String userPassword) {
        if (userPassword == null || userPassword.isEmpty())
            throw new IllegalArgumentException("password is empty");
        this.password = userPassword;
    }

    /**
     * Retrieves the plain-text user password.
     * Returns null for CHAP - use verifyPassword().
     *
     * @return user password
     * @see #verifyPassword(String)
     */
    public String getUserPassword() {
        return password;
    }

    /**
     * Retrieves the user name from the User-Name attribute.
     *
     * @return user name
     */
    public String getUserName() {
        List<RadiusAttribute> attrs = getAttributes(USER_NAME);
        if (attrs.size() != 1)
            throw new RuntimeException("exactly one User-Name attribute required");

        RadiusAttribute ra = attrs.get(0);
        return ra.getAttributeValue();
    }

    /**
     * Returns the protocol used for encrypting the passphrase.
     *
     * @return AUTH_PAP or AUTH_CHAP
     */
    public String getAuthProtocol() {
        return authProtocol;
    }

    /**
     * Selects the protocol to use for encrypting the passphrase when
     * encoding this Radius packet.
     *
     * @param authProtocol AUTH_PAP or AUTH_CHAP
     */
    public void setAuthProtocol(String authProtocol) {
        if (authProtocol != null && (authProtocol.equals(AUTH_PAP) || authProtocol.equals(AUTH_CHAP)))
            this.authProtocol = authProtocol;
        else
            throw new IllegalArgumentException("protocol must be pap or chap");
    }

    /**
     * Verifies that the passed plain-text password matches the password
     * (hash) send with this Access-Request packet. Works with both PAP
     * and CHAP.
     *
     * @return true if the password is valid, false otherwise
     */
    public boolean verifyPassword(String plaintext)
            throws RadiusException {
        if (plaintext == null || plaintext.isEmpty())
            throw new IllegalArgumentException("password is empty");
        if (getAuthProtocol().equals(AUTH_CHAP))
            return verifyChapPassword(plaintext);
        else
            return getUserPassword().equals(plaintext);
    }

    /**
     * Decrypts the User-Password attribute.
     *
     * @see RadiusPacket#decodeRequestAttributes(java.lang.String)
     */
    protected void decodeRequestAttributes(String sharedSecret)
            throws RadiusException {
        // detect auth protocol
        RadiusAttribute userPassword = getAttribute(USER_PASSWORD);
        RadiusAttribute chapPassword = getAttribute(CHAP_PASSWORD);
        RadiusAttribute chapChallenge = getAttribute(CHAP_CHALLENGE);

        if (userPassword != null) {
            setAuthProtocol(AUTH_PAP);
            this.password = decodePapPassword(userPassword.getAttributeData(), sharedSecret.getBytes(UTF_8));
            // copy truncated data
            userPassword.setAttributeData(this.password.getBytes(UTF_8));
        } else if (chapPassword != null && chapChallenge != null) {
            setAuthProtocol(AUTH_CHAP);
            this.chapPassword = chapPassword.getAttributeData();
            this.chapChallenge = chapChallenge.getAttributeData();
        } else
            throw new RadiusException("Access-Request: User-Password or CHAP-Password/CHAP-Challenge missing");
    }

    /**
     * Sets and encrypts the User-Password attribute.
     *
     * @see RadiusPacket#encodeRequestAttributes(java.lang.String)
     */
    protected void encodeRequestAttributes(String sharedSecret) throws RadiusException {
        if (password == null || password.isEmpty())
            return;
        // ok for proxied packets whose CHAP password is already encrypted
        //throw new RuntimeException("no password set");

        if (getAuthProtocol().equals(AUTH_PAP)) {
            byte[] pass = encodePapPassword(this.password.getBytes(UTF_8), sharedSecret.getBytes(UTF_8));
            removeAttributes(USER_PASSWORD);
            addAttribute(new RadiusAttribute(USER_PASSWORD, pass));
        } else if (getAuthProtocol().equals(AUTH_CHAP)) {
            byte[] challenge = createChapChallenge();
            byte[] pass = encodeChapPassword(password, challenge);
            removeAttributes(CHAP_PASSWORD);
            removeAttributes(CHAP_CHALLENGE);
            addAttribute(new RadiusAttribute(CHAP_PASSWORD, pass));
            addAttribute(new RadiusAttribute(CHAP_CHALLENGE, challenge));
        }
    }

    /**
     * This method encodes the plaintext user password according to RFC 2865.
     *
     * @param userPass     the password to encrypt
     * @param sharedSecret shared secret
     * @return the byte array containing the encrypted password
     */
    private byte[] encodePapPassword(final byte[] userPass, byte[] sharedSecret) throws RadiusException {
        requireNonNull(userPass, "userPass cannot be null");
        requireNonNull(sharedSecret, "sharedSecret cannot be null");

        try {
            byte[] C = this.getAuthenticator();
            byte[] P = Util.pad(userPass, C.length);
            byte[] result = new byte[P.length];

            for (int i = 0; i < P.length; i += C.length) {
                C = Util.compute(sharedSecret, C);
                C = Util.xor(P, i, C.length, C, 0, C.length);
                System.arraycopy(C, 0, result, i, C.length);
            }

            return result;

        } catch (GeneralSecurityException e) {
            throw new RadiusException(e);
        }
    }

    /**
     * Decodes the passed encrypted password and returns the clear-text form.
     *
     * @param encryptedPass encrypted password
     * @param sharedSecret  shared secret
     * @return decrypted password
     */
    private String decodePapPassword(byte[] encryptedPass, byte[] sharedSecret) throws RadiusException {
        if (encryptedPass == null || encryptedPass.length < 16) {
            // PAP passwords require at least 16 bytes
            logger.warn("invalid Radius packet: User-Password attribute with malformed PAP password, length = " +
                    encryptedPass.length + ", but length must be greater than 15");
            throw new RadiusException("malformed User-Password attribute");
        }

        try {
            byte[] result = new byte[encryptedPass.length];
            byte[] C = this.getAuthenticator();

            for (int i = 0; i < encryptedPass.length; i += C.length) {
                C = Util.compute(sharedSecret, C);
                C = Util.xor(encryptedPass, i, C.length, C, 0, C.length);
                System.arraycopy(C, 0, result, i, C.length);
                System.arraycopy(encryptedPass, i, C, 0, C.length);
            }

            return Util.getStringFromUtf8(result);

        } catch (GeneralSecurityException e) {
            throw new RadiusException(e);
        }
    }

    /**
     * Creates a random CHAP challenge using a secure random algorithm.
     *
     * @return 16 byte CHAP challenge
     */
    private byte[] createChapChallenge() {
        byte[] challenge = new byte[16];
        random.nextBytes(challenge);
        return challenge;
    }

    /**
     * Encodes a plain-text password using the given CHAP challenge.
     *
     * @param plaintext     plain-text password
     * @param chapChallenge CHAP challenge
     * @return CHAP-encoded password
     */
    private byte[] encodeChapPassword(String plaintext, byte[] chapChallenge) {
        // see RFC 2865 section 2.2
        byte chapIdentifier = (byte) random.nextInt(256);
        byte[] chapPassword = new byte[17];
        chapPassword[0] = chapIdentifier;

        MessageDigest md5 = getMd5Digest();
        md5.reset();
        md5.update(chapIdentifier);
        md5.update(plaintext.getBytes(UTF_8));
        byte[] chapHash = md5.digest(chapChallenge);

        System.arraycopy(chapHash, 0, chapPassword, 1, 16);
        return chapPassword;
    }

    /**
     * Verifies a CHAP password against the given plaintext password.
     *
     * @return plain-text password
     */
    private boolean verifyChapPassword(String plaintext)
            throws RadiusException {
        if (plaintext == null || plaintext.isEmpty())
            throw new IllegalArgumentException("plaintext must not be empty");
        if (chapChallenge == null || chapChallenge.length != 16)
            throw new RadiusException("CHAP challenge must be 16 bytes");
        if (chapPassword == null || chapPassword.length != 17)
            throw new RadiusException("CHAP password must be 17 bytes");

        byte chapIdentifier = chapPassword[0];
        MessageDigest md5 = getMd5Digest();
        md5.reset();
        md5.update(chapIdentifier);
        md5.update(plaintext.getBytes(UTF_8));
        byte[] chapHash = md5.digest(chapChallenge);

        // compare
        for (int i = 0; i < 16; i++)
            if (chapHash[i] != chapPassword[i + 1])
                return false;
        return true;
    }
}
