package org.tinyradius.packet;

import org.tinyradius.attribute.IntegerAttribute;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.StringAttribute;
import org.tinyradius.util.RadiusException;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * This class represents a Radius packet of the type Accounting-Request.
 */
public class AccountingRequest extends RadiusPacket {

    /**
     * Radius User-Name attribute type
     */
    private static final int USER_NAME = 1;

    /**
     * Radius Acct-Status-Type attribute type
     */
    private static final int ACCT_STATUS_TYPE = 40;

    /**
     * Acct-Status-Type: Start
     */
    public static final int ACCT_STATUS_TYPE_START = 1;

    /**
     * Acct-Status-Type: Stop
     */
    public static final int ACCT_STATUS_TYPE_STOP = 2;

    /**
     * Acct-Status-Type: Interim Update/Alive
     */
    public static final int ACCT_STATUS_TYPE_INTERIM_UPDATE = 3;

    /**
     * Acct-Status-Type: Accounting-On
     */
    public static final int ACCT_STATUS_TYPE_ACCOUNTING_ON = 7;

    /**
     * Acct-Status-Type: Accounting-Off
     */
    public static final int ACCT_STATUS_TYPE_ACCOUNTING_OFF = 8;

    /**
     * Constructs an Accounting-Request packet to be sent to a Radius server.
     *
     * @param userName       user name
     * @param acctStatusType ACCT_STATUS_TYPE_*
     */
    public AccountingRequest(String userName, int acctStatusType) {
        this();
        setUserName(userName);
        setAcctStatusType(acctStatusType);
    }

    /**
     * Constructs an empty Accounting-Request to be received by a
     * Radius client.
     */
    public AccountingRequest() {
        super(ACCOUNTING_REQUEST);
    }

    /**
     * Sets the User-Name attribute of this Accounting-Request.
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
     * Retrieves the user name from the User-Name attribute.
     *
     * @return user name
     */
    public String getUserName() {
        List<RadiusAttribute> attrs = getAttributes(USER_NAME);
        if (attrs.size() != 1)
            throw new RuntimeException("exactly one User-Name attribute required");

        return attrs.get(0).getAttributeValue();
    }

    /**
     * Sets the Acct-Status-Type attribute of this Accountnig-Request.
     *
     * @param acctStatusType ACCT_STATUS_TYPE_* to set
     */
    public void setAcctStatusType(int acctStatusType) {
        if (acctStatusType < 1 || acctStatusType > 15)
            throw new IllegalArgumentException("bad Acct-Status-Type");
        removeAttributes(ACCT_STATUS_TYPE);
        addAttribute(new IntegerAttribute(ACCT_STATUS_TYPE, acctStatusType));
    }

    /**
     * Retrieves the user name from the User-Name attribute.
     *
     * @return user name
     */
    public int getAcctStatusType() {
        RadiusAttribute ra = getAttribute(ACCT_STATUS_TYPE);
        return ra == null ?
                -1 : ((IntegerAttribute) ra).getAttributeValueInt();
    }

    /**
     * Calculates the clientRequest authenticator as specified by RFC 2866.
     */
    @Override
    protected byte[] createRequestAuthenticator(String sharedSecret, int packetLength, byte[] attributes) {
        return createHashedAuthenticator(sharedSecret, packetLength, attributes, new byte[16]);
    }

    /**
     * Checks the received clientRequest authenticator as specified by RFC 2866.
     */
    protected void checkRequestAuthenticator(String sharedSecret, int packetLength, byte[] attributes) throws RadiusException {
        byte[] expectedAuthenticator = createHashedAuthenticator(sharedSecret, packetLength, attributes, new byte[16]);
        byte[] receivedAuth = getAuthenticator();
        for (int i = 0; i < 16; i++)
            if (expectedAuthenticator[i] != receivedAuth[i])
                throw new RadiusException("clientRequest authenticator invalid");
    }
}
