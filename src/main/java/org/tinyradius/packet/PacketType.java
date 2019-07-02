package org.tinyradius.packet;

/**
 * Packet type codes.
 */
public class PacketType {

    public static final int ACCESS_REQUEST = 1; // RFC 2865
    public static final int ACCESS_ACCEPT = 2;
    public static final int ACCESS_REJECT = 3;
    public static final int ACCESS_CHALLENGE = 11;

    public static final int ACCOUNTING_REQUEST = 4; // RFC 2866
    public static final int ACCOUNTING_RESPONSE = 5;
    public static final int ACCOUNTING_STATUS = 6; // aka Interim Accounting
    public static final int ACCOUNTING_MESSAGE = 10;

    public static final int PASSWORD_REQUEST = 7;
    public static final int PASSWORD_ACCEPT = 8;
    public static final int PASSWORD_REJECT = 9;

    public static final int STATUS_SERVER = 12; // RFC 5997
    public static final int STATUS_CLIENT = 13;

    public static final int DISCONNECT_REQUEST = 40; // RFC 5176
    public static final int DISCONNECT_ACK = 41;
    public static final int DISCONNECT_NAK = 42;
    public static final int COA_REQUEST = 43;
    public static final int COA_ACK = 44;
    public static final int COA_NAK = 45;

    public static final int STATUS_REQUEST = 46;
    public static final int STATUS_ACCEPT = 47;
    public static final int STATUS_REJECT = 48;
    public static final int RESERVED = 255;

    public static String getPacketTypeName(int code) {
        switch (code) {
            case ACCESS_REQUEST:
                return "Access-Request";
            case ACCESS_ACCEPT:
                return "Access-Accept";
            case ACCESS_REJECT:
                return "Access-Reject";
            case ACCOUNTING_REQUEST:
                return "Accounting-Request";
            case ACCOUNTING_RESPONSE:
                return "Accounting-Response";
            case ACCOUNTING_STATUS:
                return "Accounting-Status";
            case PASSWORD_REQUEST:
                return "Password-Request";
            case PASSWORD_ACCEPT:
                return "Password-Accept";
            case PASSWORD_REJECT:
                return "Password-Reject";
            case ACCOUNTING_MESSAGE:
                return "Accounting-Message";
            case ACCESS_CHALLENGE:
                return "Access-Challenge";
            case STATUS_SERVER:
                return "Status-Server";
            case STATUS_CLIENT:
                return "Status-Client";
            case DISCONNECT_REQUEST:
                return "Disconnect-Request";
            case DISCONNECT_ACK:
                return "Disconnect-ACK";
            case DISCONNECT_NAK:
                return "Disconnect-NAK";
            case COA_REQUEST:
                return "CoA-Request";
            case COA_ACK:
                return "CoA-ACK";
            case COA_NAK:
                return "CoA-NAK";
            case STATUS_REQUEST:
                return "Status-Request";
            case STATUS_ACCEPT:
                return "Status-Accept";
            case STATUS_REJECT:
                return "Status-Reject";
            case RESERVED:
                return "Reserved";
            default:
                return "Unknown (" + code + ")";
        }
    }
}
