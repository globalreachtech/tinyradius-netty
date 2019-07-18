package org.tinyradius.packet;

import org.tinyradius.attribute.IntegerAttribute;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.PacketType.ACCOUNTING_REQUEST;

/**
 * This class represents a Radius packet of the type Accounting-Request.
 */
public class AccountingRequest extends RadiusPacket {

    // Attributes
    private static final int USER_NAME = 1;
    private static final int ACCT_STATUS_TYPE = 40;

    // Acct-Status-Type values
    public static final int ACCT_STATUS_TYPE_START = 1;
    public static final int ACCT_STATUS_TYPE_STOP = 2;
    public static final int ACCT_STATUS_TYPE_INTERIM_UPDATE = 3;
    public static final int ACCT_STATUS_TYPE_ACCOUNTING_ON = 7;
    public static final int ACCT_STATUS_TYPE_ACCOUNTING_OFF = 8;

    /**
     * Constructs an Accounting-Request packet to be sent to a Radius server.
     *
     * @param userName       user name
     * @param acctStatusType ACCT_STATUS_TYPE_*
     */
    public AccountingRequest(Dictionary dictionary, int identifier, byte[] authenticator, String userName, int acctStatusType) {
        this(dictionary, identifier, authenticator);
        setUserName(userName);
        setAcctStatusType(acctStatusType);
    }

    public AccountingRequest(Dictionary dictionary, int identifier, byte[] authenticator) {
        this(dictionary, identifier, authenticator, new ArrayList<>());
    }

    public AccountingRequest(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, ACCOUNTING_REQUEST, identifier, authenticator, attributes);
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
        addAttribute(createAttribute(getDictionary(), -1, USER_NAME, userName));
    }

    /**
     * Retrieves the user name from the User-Name attribute.
     *
     * @return user name
     */
    public String getUserName() {
        final RadiusAttribute attribute = getAttribute(USER_NAME);
        return attribute == null ?
                null : attribute.getDataString();
    }

    /**
     * Sets the Acct-Status-Type attribute of this Accounting-Request.
     *
     * @param acctStatusType ACCT_STATUS_TYPE_* to set
     */
    public void setAcctStatusType(int acctStatusType) {
        if (acctStatusType < 1 || acctStatusType > 15)
            throw new IllegalArgumentException("bad Acct-Status-Type");
        removeAttributes(ACCT_STATUS_TYPE);
        addAttribute(new IntegerAttribute(getDictionary(), -1, ACCT_STATUS_TYPE, acctStatusType));
    }

    /**
     * @return
     */
    public int getAcctStatusType() {
        RadiusAttribute ra = getAttribute(ACCT_STATUS_TYPE);
        return ra == null ?
                -1 : (int) ((IntegerAttribute) ra).getAttributeValueInt();
    }
}
