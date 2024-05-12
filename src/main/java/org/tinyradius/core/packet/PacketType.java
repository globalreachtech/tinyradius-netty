package org.tinyradius.core.packet;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Based on official IANA assignments for <a href="https://www.iana.org/assignments/radius-types/radius-types.xhtml#radius-types-27">Radius Packet Type Codes</a>
 */
@UtilityClass
public class PacketType {

    public static final byte ACCESS_REQUEST = 1;
    public static final byte ACCESS_ACCEPT = 2;
    public static final byte ACCESS_REJECT = 3;
    public static final byte ACCOUNTING_REQUEST = 4;
    public static final byte ACCOUNTING_RESPONSE = 5;
    public static final byte ACCOUNTING_STATUS = 6; // aka Interim Accounting
    public static final byte PASSWORD_REQUEST = 7;
    public static final byte PASSWORD_ACCEPT = 8;
    public static final byte PASSWORD_REJECT = 9;
    public static final byte ACCOUNTING_MESSAGE = 10;
    public static final byte ACCESS_CHALLENGE = 11;
    public static final byte STATUS_SERVER = 12;
    public static final byte STATUS_CLIENT = 13;
    public static final byte RESOURCE_FREE_REQUEST = 21;
    public static final byte RESOURCE_FREE_RESPONSE = 22;
    public static final byte RESOURCE_QUERY_REQUEST = 23;
    public static final byte RESOURCE_QUERY_RESPONSE = 24;
    public static final byte ALTERNATE_RESOURCE_RECLAIM_REQUEST = 25;
    public static final byte NAS_REBOOT_REQUEST = 26;
    public static final byte NAS_REBOOT_RESPONSE = 27;
    public static final byte NEXT_PASSCODE = 29;
    public static final byte NEW_PIN = 30;
    public static final byte TERMINATE_SESSION = 31;
    public static final byte PASSWORD_EXPIRED = 32;
    public static final byte EVENT_REQUEST = 33;
    public static final byte EVENT_RESPONSE = 34;
    public static final byte DISCONNECT_REQUEST = 40;
    public static final byte DISCONNECT_ACK = 41;
    public static final byte DISCONNECT_NAK = 42;
    public static final byte COA_REQUEST = 43;
    public static final byte COA_ACK = 44;
    public static final byte COA_NAK = 45;
    public static final byte IP_ADDRESS_ALLOCATE = 50;
    public static final byte IP_ADDRESS_RELEASE = 51;
    public static final byte PROTOCOL_ERROR = 52;


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
        map.put(RESOURCE_FREE_REQUEST, "Resource-Free-Request");
        map.put(RESOURCE_FREE_RESPONSE, "Resource-Free-Response");
        map.put(RESOURCE_QUERY_REQUEST, "Resource-Query-Request");
        map.put(RESOURCE_QUERY_RESPONSE, "Resource-Query-Response");
        map.put(ALTERNATE_RESOURCE_RECLAIM_REQUEST, "Alternate-Resource-Reclaim-Request");
        map.put(NAS_REBOOT_REQUEST, "NAS-Reboot-Request");
        map.put(NAS_REBOOT_RESPONSE, "NAS-Reboot-Response");
        map.put(NEXT_PASSCODE, "Next-Passcode");
        map.put(NEW_PIN, "New-Pin");
        map.put(TERMINATE_SESSION, "Terminate-Session");
        map.put(PASSWORD_EXPIRED, "Password-Expired");
        map.put(EVENT_REQUEST, "Event-Request");
        map.put(EVENT_RESPONSE, "Event-Response");
        map.put(DISCONNECT_REQUEST, "Disconnect-Request");
        map.put(DISCONNECT_ACK, "Disconnect-ACK");
        map.put(DISCONNECT_NAK, "Disconnect-NAK");
        map.put(COA_REQUEST, "CoA-Request");
        map.put(COA_ACK, "CoA-ACK");
        map.put(COA_NAK, "CoA-NAK");
        map.put(IP_ADDRESS_ALLOCATE, "IP-Address-Allocate");
        map.put(IP_ADDRESS_RELEASE, "IP-Address-Release");
        map.put(PROTOCOL_ERROR, "Protocol-Error");
        return map;
    }

    public static String getPacketTypeName(byte code) {
        return typeNames.getOrDefault(code, "Unknown (" + code + ")");
    }
}
