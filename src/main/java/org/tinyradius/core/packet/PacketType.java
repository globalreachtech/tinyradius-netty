package org.tinyradius.core.packet;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet type codes.
 */
@UtilityClass
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

    private static final Map<Byte, String> typeNames = setupTypeNames();

    private static Map<Byte, String> setupTypeNames() {
        final Map<Byte, String> map = new HashMap<>();
        map.put(ACCESS_REQUEST, "Access-Request");
        map.put(ACCESS_ACCEPT, "Access-Accept");
        map.put(ACCESS_REJECT, "Access-Reject");
        map.put(ACCOUNTING_REQUEST, "Accounting-Request");
        map.put(ACCOUNTING_RESPONSE, "Accounting-Response");
        map.put(ACCOUNTING_STATUS, "Accounting-Status");
        map.put(PASSWORD_REQUEST, "Password-Request");
        map.put(PASSWORD_ACCEPT, "Password-Accept");
        map.put(PASSWORD_REJECT, "Password-Reject");
        map.put(ACCOUNTING_MESSAGE, "Accounting-Message");
        map.put(ACCESS_CHALLENGE, "Access-Challenge");
        map.put(STATUS_SERVER, "Status-Server");
        map.put(STATUS_CLIENT, "Status-Client");
        map.put(DISCONNECT_REQUEST, "Disconnect-Request");
        map.put(DISCONNECT_ACK, "Disconnect-ACK");
        map.put(DISCONNECT_NAK, "Disconnect-NAK");
        map.put(COA_REQUEST, "CoA-Request");
        map.put(COA_ACK, "CoA-ACK");
        map.put(COA_NAK, "CoA-NAK");
        map.put(STATUS_REQUEST, "Status-Request");
        map.put(STATUS_ACCEPT, "Status-Accept");
        map.put(STATUS_REJECT, "Status-Reject");
        map.put(RESERVED, "Reserved");
        return map;
    }

    public static String getPacketTypeName(byte code) {
        return typeNames.getOrDefault(code, "Unknown (" + code + ")");
    }
}
