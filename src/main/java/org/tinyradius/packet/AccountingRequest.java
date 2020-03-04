package org.tinyradius.packet;

import org.tinyradius.attribute.IntegerAttribute;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.List;

import static org.tinyradius.packet.util.PacketType.ACCOUNTING_REQUEST;

/**
 * This class represents a Radius packet of the type Accounting-Request.
 */
public class AccountingRequest extends RadiusRequest {

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
     * @param dictionary     custom dictionary to use
     * @param identifier     packet identifier
     * @param authenticator  authenticator for packet, nullable
     * @param userName       user name
     * @param acctStatusType ACCT_STATUS_TYPE_*
     */
    public AccountingRequest(Dictionary dictionary, int identifier, byte[] authenticator, String userName, int acctStatusType) {
        this(dictionary, identifier, authenticator);
        setAttributeString(USER_NAME, userName);
        setAcctStatusType(acctStatusType);
    }

    /**
     * Constructs an Accounting-Request packet to be sent to a Radius server.
     *
     * @param dictionary    custom dictionary to use
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     */
    public AccountingRequest(Dictionary dictionary, int identifier, byte[] authenticator) {
        this(dictionary, identifier, authenticator, new ArrayList<>());
    }

    /**
     * Constructs an Accounting-Request packet to be sent to a Radius server.
     *
     * @param dictionary    custom dictionary to use
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes
     */
    public AccountingRequest(Dictionary dictionary, int identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        super(dictionary, ACCOUNTING_REQUEST, identifier, authenticator, attributes);
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
     * @return Acct-Status-Type value
     */
    public int getAcctStatusType() {
        RadiusAttribute ra = getAttribute(ACCT_STATUS_TYPE);
        return ra == null ?
                -1 : ((IntegerAttribute) ra).getValueInt();
    }
}
