package org.tinyradius.packet.util;

import java.util.HashMap;

/**
 * Packet type codes.
 */
public class PacketType {

    public static final byte ACCESS_REQUEST = 1; // RFC 2865
    public static final byte ACCESS_ACCEPT = 2;
    public static final byte ACCESS_REJECT = 3;
    public static final byte ACCESS_CHALLENGE = 11;

    public static final byte ACCOUNTING_REQUEST = 4; // RFC 2866
    public static final byte ACCOUNTING_RESPONSE = 5;
    public static final byte ACCOUNTING_STATUS = 6; // aka Interim Accounting
    public static final byte ACCOUNTING_MESSAGE = 10;

    public static final byte PASSWORD_REQUEST = 7;
    public static final byte PASSWORD_ACCEPT = 8;
    public static final byte PASSWORD_REJECT = 9;

    public static final byte STATUS_SERVER = 12; // RFC 5997
    public static final byte STATUS_CLIENT = 13;

    public static final byte DISCONNECT_REQUEST = 40; // RFC 5176
    public static final byte DISCONNECT_ACK = 41;
    public static final byte DISCONNECT_NAK = 42;
    public static final byte COA_REQUEST = 43;
    public static final byte COA_ACK = 44;
    public static final byte COA_NAK = 45;

    public static final byte STATUS_REQUEST = 46;
    public static final byte STATUS_ACCEPT = 47;
    public static final byte STATUS_REJECT = 48;
    public static final byte RESERVED = (byte) 255;

    private static final HashMap<Byte, String> typeNames = new HashMap<>();

    static {
        typeNames.put(ACCESS_REQUEST, "Access-Request");
        typeNames.put(ACCESS_ACCEPT, "Access-Accept");
        typeNames.put(ACCESS_REJECT, "Access-Reject");
        typeNames.put(ACCOUNTING_REQUEST, "Accounting-Request");
        typeNames.put(ACCOUNTING_RESPONSE, "Accounting-Response");
        typeNames.put(ACCOUNTING_STATUS, "Accounting-Status");
        typeNames.put(PASSWORD_REQUEST, "Password-Request");
        typeNames.put(PASSWORD_ACCEPT, "Password-Accept");
        typeNames.put(PASSWORD_REJECT, "Password-Reject");
        typeNames.put(ACCOUNTING_MESSAGE, "Accounting-Message");
        typeNames.put(ACCESS_CHALLENGE, "Access-Challenge");
        typeNames.put(STATUS_SERVER, "Status-Server");
        typeNames.put(STATUS_CLIENT, "Status-Client");
        typeNames.put(DISCONNECT_REQUEST, "Disconnect-Request");
        typeNames.put(DISCONNECT_ACK, "Disconnect-ACK");
        typeNames.put(DISCONNECT_NAK, "Disconnect-NAK");
        typeNames.put(COA_REQUEST, "CoA-Request");
        typeNames.put(COA_ACK, "CoA-ACK");
        typeNames.put(COA_NAK, "CoA-NAK");
        typeNames.put(STATUS_REQUEST, "Status-Request");
        typeNames.put(STATUS_ACCEPT, "Status-Accept");
        typeNames.put(STATUS_REJECT, "Status-Reject");
        typeNames.put(RESERVED, "Reserved");
    }

    private PacketType() {
    }

    public static String getPacketTypeName(byte code) {
        return typeNames.getOrDefault(code, "Unknown (" + code + ")");
    }
}
