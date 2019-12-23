package org.tinyradius.packet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.PacketType.ACCESS_REQUEST;

/**
 * This class represents an Access-Request Radius packet.
 */
public class AccessRequest extends RadiusPacket {

    private static final Logger logger = LoggerFactory.getLogger(AccessRequest.class);

    private static final SecureRandom random = new SecureRandom();

    public static final String AUTH_PAP = "pap";
    public static final String AUTH_CHAP = "chap";
    public static final String AUTH_MS_CHAP_V2 = "mschapv2";
    public static final String AUTH_EAP = "eap";

    public static final Set<String> AUTH_PROTOCOLS = new HashSet<>(Arrays.asList(AUTH_PAP, AUTH_CHAP, AUTH_MS_CHAP_V2, AUTH_EAP));

    private String authProtocol = AUTH_PAP;

    // Temporary storage for the unencrypted attributes
    private transient String password;
    private transient byte[] chapPassword;
    private transient byte[] chapChallenge;

    // Attributes
    private static final int USER_NAME = 1;
    private static final int USER_PASSWORD = 2;
    private static final int CHAP_PASSWORD = 3;
    private static final int CHAP_CHALLENGE = 60;
    private static final int EAP_MESSAGE = 79;

    // VendorIds
    private static final int MICROSOFT = 311;

    // Vendor Specific Attributes
    private static final int MS_CHAP_CHALLENGE = 11;
    private static final int MS_CHAP2_RESPONSE = 25;

    /**
     * @param dictionary    custom dictionary to use
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     */
    public AccessRequest(Dictionary dictionary, int identifier, byte[] authenticator) {
        this(dictionary, identifier, authenticator, new ArrayList<>());
    }

    /**
     * @param dictionary    custom dictionary to use
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes for packet
     */
    public AccessRequest(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, ACCESS_REQUEST, identifier, authenticator, attributes);
    }

    /**
     * Constructs an Access-Request packet, sets the
     * code, identifier and adds an User-Name and an
     * User-Password attribute (PAP).
     *
     * @param dictionary    custom dictionary to use
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     * @param userName      user name
     * @param userPassword  user password
     */
    public AccessRequest(Dictionary dictionary, int identifier, byte[] authenticator, String userName, String userPassword) {
        this(dictionary, identifier, authenticator);
        setUserName(userName);
        setUserPassword(userPassword);
    }

    /**
     * Sets the User-Name attribute of this Access-Request.
     *
     * @param userName user name to set
     */
    public void setUserName(String userName) {
        requireNonNull(userName, "User name not set");
        if (userName.isEmpty())
            throw new IllegalArgumentException("Empty user name not allowed");

        removeAttributes(USER_NAME);
        addAttribute(createAttribute(getDictionary(), -1, USER_NAME, userName));
    }

    /**
     * Sets the plain-text user password.
     *
     * @param userPassword user password to set
     */
    public void setUserPassword(String userPassword) {
        requireNonNull(userPassword, "User password not set");
        if (userPassword.isEmpty())
            throw new IllegalArgumentException("Password is empty");
        this.password = userPassword;
    }

    /**
     * Retrieves the plain-text user password.
     * Returns null for CHAP - use verifyPassword().
     *
     * @return user password in plaintext if decoded and using PAP
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
        final RadiusAttribute attribute = getAttribute(USER_NAME);
        return attribute == null ?
                null : attribute.getValueString();
    }

    /**
     * Returns the protocol used for encrypting the passphrase.
     *
     * @return one of {@link #AUTH_PROTOCOLS}
     */
    public String getAuthProtocol() {
        return authProtocol;
    }

    /**
     * Selects the protocol to use for encrypting the passphrase when
     * encoding this Radius packet.
     *
     * @param authProtocol {@link #AUTH_PROTOCOLS}
     */
    public void setAuthProtocol(String authProtocol) {
        if (authProtocol != null && AUTH_PROTOCOLS.contains(authProtocol))
            this.authProtocol = authProtocol;
        else
            throw new IllegalArgumentException("protocol must be in " + AUTH_PROTOCOLS);
    }

    /**
     * AccessRequest cannot verify authenticator as they
     * contain random bytes.
     * <p>
     * Instead it checks the User-Password/Challenge attributes
     * are present and attempts decryption.
     *
     * @param sharedSecret shared secret, only applicable for PAP
     * @param ignored      ignored, not applicable for AccessRequest
     */
    @Override
    public void verify(String sharedSecret, byte[] ignored) throws RadiusException {
        if (!decryptPasswords(sharedSecret))
            throw new RadiusException("Access-Request: User-Password or CHAP-Password/CHAP-Challenge missing");
    }

    public boolean decryptPasswords(String sharedSecret) throws RadiusException {
        RadiusAttribute userPassword = getAttribute(USER_PASSWORD);
        if (userPassword != null) {
            setAuthProtocol(AUTH_PAP);
            this.password = decodePapPassword(userPassword.getValue(), sharedSecret.getBytes(UTF_8));
            return true;
        }

        RadiusAttribute chapPassword = getAttribute(CHAP_PASSWORD);
        RadiusAttribute chapChallenge = getAttribute(CHAP_CHALLENGE);
        if (chapPassword != null) {
            setAuthProtocol(AUTH_CHAP);
            this.chapPassword = chapPassword.getValue();
            this.chapChallenge = chapChallenge != null ?
                    chapChallenge.getValue() : getAuthenticator();
            return true;
        }

        RadiusAttribute msChapChallenge = getAttribute(MICROSOFT, MS_CHAP_CHALLENGE);
        RadiusAttribute msChap2Response = getAttribute(MICROSOFT, MS_CHAP2_RESPONSE);
        if (msChapChallenge != null && msChap2Response != null) {
            setAuthProtocol(AUTH_MS_CHAP_V2);
            this.chapPassword = msChap2Response.getValue();
            this.chapChallenge = msChapChallenge.getValue();
            return true;
        }

        List<RadiusAttribute> eapMessage = getAttributes(EAP_MESSAGE);
        if (eapMessage.size() > 0) {
            setAuthProtocol(AUTH_EAP);
            return true;
        }

        return false;
    }

    /**
     * Verifies that the passed plain-text password matches the password
     * (hash) send with this Access-Request packet. Works with both PAP
     * and CHAP.
     *
     * @param plaintext password to verify packet against
     * @return true if the password is valid, false otherwise
     */
    public boolean verifyPassword(String plaintext) throws UnsupportedOperationException {
        if (plaintext == null || plaintext.isEmpty())
            throw new IllegalArgumentException("password is empty");
        switch (getAuthProtocol()) {
            case AUTH_CHAP:
                return verifyChapPassword(plaintext);
            case AUTH_MS_CHAP_V2:
                throw new UnsupportedOperationException(AUTH_MS_CHAP_V2 + " verification not supported yet");
            case AUTH_EAP:
                throw new UnsupportedOperationException(AUTH_EAP + " verification not supported yet");
            case AUTH_PAP:
            default:
                return getUserPassword().equals(plaintext);
        }
    }

    /**
     * AccessRequest overrides this method to generate a randomized authenticator as per RFC 2865
     * and encode required attributes (i.e. User-Password).
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @return RadiusPacket with new authenticator and/or encoded attributes
     * @throws UnsupportedOperationException auth type not supported
     */
    @Override
    public AccessRequest encodeRequest(String sharedSecret) throws UnsupportedOperationException {
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new IllegalArgumentException("shared secret cannot be null/empty");

        // create authenticator only if needed to maintain idempotence
        byte[] newAuthenticator = getAuthenticator() == null ? random16bytes() : getAuthenticator();

        final AccessRequest accessRequest = new AccessRequest(getDictionary(), getIdentifier(), newAuthenticator, new ArrayList<>(getAttributes()));
        copyTransientFields(accessRequest);

        // encode attributes (User-Password attribute needs the new authenticator)
        encodeRequestAttributes(newAuthenticator, sharedSecret).forEach(a -> {
            accessRequest.removeAttributes(a.getType());
            accessRequest.addAttribute(a);
        });
        return accessRequest;
    }

    @Override
    public RadiusPacket encodeResponse(String sharedSecret, byte[] requestAuthenticator) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets and encrypts the User-Password attribute.
     *
     * @param authenticator authenticator to use to encode PAP password,
     *                      nullable if using different auth protocol
     * @param sharedSecret  shared secret that secures the communication
     *                      with the other Radius server/client
     * @return List of RadiusAttributes to override
     * @throws UnsupportedOperationException auth protocol not supported
     */
    protected List<RadiusAttribute> encodeRequestAttributes(byte[] authenticator, String sharedSecret) throws UnsupportedOperationException {
        if (password != null && !password.isEmpty())
            switch (getAuthProtocol()) {
                case AUTH_PAP:
                    return Collections.singletonList(
                            createAttribute(getDictionary(), -1, USER_PASSWORD,
                                    encodePapPassword(authenticator, password.getBytes(UTF_8), sharedSecret.getBytes(UTF_8))));
                case AUTH_CHAP:
                    byte[] challenge = random16bytes();
                    return Arrays.asList(
                            createAttribute(getDictionary(), -1, CHAP_CHALLENGE, challenge),
                            createAttribute(getDictionary(), -1, CHAP_PASSWORD,
                                    computeChapPassword((byte) random.nextInt(256), password, challenge)));
                case AUTH_MS_CHAP_V2:
                    throw new UnsupportedOperationException("Encoding not supported for " + AUTH_MS_CHAP_V2);
                case AUTH_EAP:
                    throw new UnsupportedOperationException("Encoding not supported for " + AUTH_EAP);
            }

        return Collections.emptyList();
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
     * Decodes the passed encrypted password and returns the clear-text form.
     *
     * @param encryptedPass encrypted password
     * @param sharedSecret  shared secret
     * @return decrypted password
     */
    private String decodePapPassword(byte[] encryptedPass, byte[] sharedSecret) throws RadiusException {
        if (encryptedPass.length < 16) {
            // PAP passwords require at least 16 bytes, or multiples thereof
            logger.warn("Malformed packet: User-Password attribute length must be greater than 15, actual {}", encryptedPass.length);
            throw new RadiusException("Malformed User-Password attribute");
        }

        final ByteBuffer buffer = ByteBuffer.allocate(encryptedPass.length);
        byte[] ciphertext = this.getAuthenticator();

        for (int i = 0; i < encryptedPass.length; i += 16) {
            buffer.put(xor16(encryptedPass, i, md5(sharedSecret, ciphertext)));
            ciphertext = Arrays.copyOfRange(encryptedPass, i, 16);
        }

        return stripNullPadding(new String(buffer.array(), UTF_8));
    }

    /**
     * Encodes a plain-text password using the given CHAP challenge.
     * See RFC 2865 section 2.2
     *
     * @param chapId        CHAP ID associated with request
     * @param plaintextPw   plain-text password
     * @param chapChallenge random 16 octet CHAP challenge
     * @return 17 octet CHAP-encoded password (1 octet for CHAP ID, 16 octets CHAP response)
     */
    private byte[] computeChapPassword(byte chapId, String plaintextPw, byte[] chapChallenge) {
        MessageDigest md5 = getMd5Digest();
        md5.update(chapId);
        md5.update(plaintextPw.getBytes(UTF_8));
        md5.update(chapChallenge);

        return ByteBuffer.allocate(17)
                .put(chapId)
                .put(md5.digest())
                .array();
    }

    /**
     * Verifies a CHAP password against the given plaintext password.
     *
     * @param plaintext plaintext password
     * @return true if plaintext password matches encoded chap password
     */
    private boolean verifyChapPassword(String plaintext) {
        if (plaintext == null || plaintext.isEmpty())
            logger.warn("plaintext must not be empty");
        else if (chapChallenge == null)
            logger.warn("CHAP challenge is null");
        else if (chapPassword == null || chapPassword.length != 17)
            logger.warn("CHAP password must be 17 bytes");
        else
            return Arrays.equals(chapPassword, computeChapPassword(chapPassword[0], plaintext, chapChallenge));
        return false;
    }

    private byte[] md5(byte[] a, byte[] b) {
        MessageDigest md = getMd5Digest();
        md.update(a);
        return md.digest(b);
    }

    private byte[] random16bytes() {
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);
        return randomBytes;
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

    private static String stripNullPadding(String s) {
        int i = s.indexOf('\0');
        return (i > 0) ? s.substring(0, i) : s;
    }

    private AccessRequest copyTransientFields(AccessRequest target) {
        target.password = password;
        target.chapPassword = chapPassword;
        target.chapChallenge = chapChallenge;
        return target;
    }

    @Override
    public AccessRequest copy() {
        return copyTransientFields((AccessRequest) super.copy());
    }
}
