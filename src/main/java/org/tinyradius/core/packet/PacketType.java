package org.tinyradius.core.packet;

import lombok.experimental.UtilityClass;

import java.util.Map;

import static java.util.Map.entry;

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


    private static final Map<Byte, String> typeNames = Map.ofEntries(
            entry(ACCESS_REQUEST, "Access-Request"),
            entry(ACCESS_ACCEPT, "Access-Accept"),
            entry(ACCESS_REJECT, "Access-Reject"),
            entry(ACCOUNTING_REQUEST, "Accounting-Request"),
            entry(ACCOUNTING_RESPONSE, "Accounting-Response"),
            entry(ACCOUNTING_STATUS, "Accounting-Status"),
            entry(PASSWORD_REQUEST, "Password-Request"),
            entry(PASSWORD_ACCEPT, "Password-Accept"),
            entry(PASSWORD_REJECT, "Password-Reject"),
            entry(ACCOUNTING_MESSAGE, "Accounting-Message"),
            entry(ACCESS_CHALLENGE, "Access-Challenge"),
            entry(STATUS_SERVER, "Status-Server"),
            entry(STATUS_CLIENT, "Status-Client"),
            entry(RESOURCE_FREE_REQUEST, "Resource-Free-Request"),
            entry(RESOURCE_FREE_RESPONSE, "Resource-Free-Response"),
            entry(RESOURCE_QUERY_REQUEST, "Resource-Query-Request"),
            entry(RESOURCE_QUERY_RESPONSE, "Resource-Query-Response"),
            entry(ALTERNATE_RESOURCE_RECLAIM_REQUEST, "Alternate-Resource-Reclaim-Request"),
            entry(NAS_REBOOT_REQUEST, "NAS-Reboot-Request"),
            entry(NAS_REBOOT_RESPONSE, "NAS-Reboot-Response"),
            entry(NEXT_PASSCODE, "Next-Passcode"),
            entry(NEW_PIN, "New-Pin"),
            entry(TERMINATE_SESSION, "Terminate-Session"),
            entry(PASSWORD_EXPIRED, "Password-Expired"),
            entry(EVENT_REQUEST, "Event-Request"),
            entry(EVENT_RESPONSE, "Event-Response"),
            entry(DISCONNECT_REQUEST, "Disconnect-Request"),
            entry(DISCONNECT_ACK, "Disconnect-ACK"),
            entry(DISCONNECT_NAK, "Disconnect-NAK"),
            entry(COA_REQUEST, "CoA-Request"),
            entry(COA_ACK, "CoA-ACK"),
            entry(COA_NAK, "CoA-NAK"),
            entry(IP_ADDRESS_ALLOCATE, "IP-Address-Allocate"),
            entry(IP_ADDRESS_RELEASE, "IP-Address-Release"),
            entry(PROTOCOL_ERROR, "Protocol-Error")
    );

    public static String getPacketTypeName(byte code) {
        return typeNames.getOrDefault(code, "Unknown (" + code + ")");
    }
}
