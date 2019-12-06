package org.tinyradius.packet;

import java.util.HashMap;

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

    private static final HashMap<Integer, String> typeNames = new HashMap<Integer, String>() {{
        put(ACCESS_REQUEST, "Access-Request");
        put(ACCESS_ACCEPT, "Access-Accept");
        put(ACCESS_REJECT, "Access-Reject");
        put(ACCOUNTING_REQUEST, "Accounting-Request");
        put(ACCOUNTING_RESPONSE, "Accounting-Response");
        put(ACCOUNTING_STATUS, "Accounting-Status");
        put(PASSWORD_REQUEST, "Password-Request");
        put(PASSWORD_ACCEPT, "Password-Accept");
        put(PASSWORD_REJECT, "Password-Reject");
        put(ACCOUNTING_MESSAGE, "Accounting-Message");
        put(ACCESS_CHALLENGE, "Access-Challenge");
        put(STATUS_SERVER, "Status-Server");
        put(STATUS_CLIENT, "Status-Client");
        put(DISCONNECT_REQUEST, "Disconnect-Request");
        put(DISCONNECT_ACK, "Disconnect-ACK");
        put(DISCONNECT_NAK, "Disconnect-NAK");
        put(COA_REQUEST, "CoA-Request");
        put(COA_ACK, "CoA-ACK");
        put(COA_NAK, "CoA-NAK");
        put(STATUS_REQUEST, "Status-Request");
        put(STATUS_ACCEPT, "Status-Accept");
        put(STATUS_REJECT, "Status-Reject");
        put(RESERVED, "Reserved");
    }};

    public static String getPacketTypeName(int code) {
        return typeNames.getOrDefault(code, "Unknown (" + code + ")");
    }
}
