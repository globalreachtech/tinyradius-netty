package org.tinyradius.core.packet;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

import static java.util.Map.entry;

/**
 * Based on official IANA assignments for <a href="https://www.iana.org/assignments/radius-types/radius-types.xhtml#radius-types-27">Radius Packet Type Codes</a>
 */
@UtilityClass
public class PacketType {

    /**
     * Access-Request [RFC2865]
     * <p>
     * Sent by a client (NAS) to a RADIUS server to request authentication and authorization for a user.
     */
    public static final byte ACCESS_REQUEST = 1;

    /**
     * Access-Accept [RFC2865]
     * <p>
     * Sent by the RADIUS server to the client to indicate that the user's credentials are valid.
     */
    public static final byte ACCESS_ACCEPT = 2;

    /**
     * Access-Reject [RFC2865]
     * <p>
     * Sent by the RADIUS server to the client to indicate that the user's credentials are invalid or access is denied.
     */
    public static final byte ACCESS_REJECT = 3;

    /**
     * Accounting-Request [RFC2866]
     * <p>
     * Sent by the client to the RADIUS server to convey accounting information for a user session.
     */
    public static final byte ACCOUNTING_REQUEST = 4;

    /**
     * Accounting-Response [RFC2866]
     * <p>
     * Sent by the RADIUS server to the client to acknowledge receipt of an Accounting-Request.
     */
    public static final byte ACCOUNTING_RESPONSE = 5;

    /**
     * Interim-Accounting [RFC2866]
     * <p>
     * Used for interim updates during an active session (often mapped to Accounting-Request with Status-Type = Interim-Update).
     */
    public static final byte ACCOUNTING_STATUS = 6;

    /**
     * Password-Request [RFC2865]
     * <p>
     * Used in some authentication sequences to request a password from the user.
     */
    public static final byte PASSWORD_REQUEST = 7;

    /**
     * Password-Accept [RFC2865]
     * <p>
     * Indicates that the password provided in a Password-Request has been accepted.
     */
    public static final byte PASSWORD_ACCEPT = 8;

    /**
     * Password-Reject [RFC2865]
     * <p>
     * Indicates that the password provided in a Password-Request has been rejected.
     */
    public static final byte PASSWORD_REJECT = 9;

    /**
     * Accounting-Message [RFC2866]
     * <p>
     * Used to send accounting messages between the client and server.
     */
    public static final byte ACCOUNTING_MESSAGE = 10;

    /**
     * Access-Challenge [RFC2865]
     * <p>
     * Sent by the RADIUS server to the client to request further information from the user (e.g., token or secondary password).
     */
    public static final byte ACCESS_CHALLENGE = 11;

    /**
     * Status-Server [RFC2865]
     * <p>
     * Sent by the client to the RADIUS server to inquire about its status or availability.
     */
    public static final byte STATUS_SERVER = 12;

    /**
     * Status-Client [RFC2865]
     * <p>
     * Sent by the RADIUS server to the client to inquire about its status.
     */
    public static final byte STATUS_CLIENT = 13;

    /**
     * Resource-Free-Request [RFC2882]
     * <p>
     * Sent by a client to request the release of a managed resource.
     */
    public static final byte RESOURCE_FREE_REQUEST = 21;

    /**
     * Resource-Free-Response [RFC2882]
     * <p>
     * Sent by a server to acknowledge the release of a managed resource.
     */
    public static final byte RESOURCE_FREE_RESPONSE = 22;

    /**
     * Resource-Query-Request [RFC2882]
     * <p>
     * Sent by a client to query the status or availability of a managed resource.
     */
    public static final byte RESOURCE_QUERY_REQUEST = 23;

    /**
     * Resource-Query-Response [RFC2882]
     * <p>
     * Sent by a server to provide the requested resource status information.
     */
    public static final byte RESOURCE_QUERY_RESPONSE = 24;

    /**
     * Alternate-Resource-Reclaim-Request [RFC2882]
     * <p>
     * Sent by a server to proactively reclaim a managed resource from a client.
     */
    public static final byte ALTERNATE_RESOURCE_RECLAIM_REQUEST = 25;

    /**
     * NAS-Reboot-Request [RFC2882]
     * <p>
     * Sent by a client to indicate that it has rebooted and all its active sessions are terminated.
     */
    public static final byte NAS_REBOOT_REQUEST = 26;

    /**
     * NAS-Reboot-Response [RFC2882]
     * <p>
     * Sent by a server to acknowledge the NAS reboot notification.
     */
    public static final byte NAS_REBOOT_RESPONSE = 27;

    /**
     * Next-Passcode [RFC2869]
     * <p>
     * Sent by the server to indicate that the next passcode in a sequence is required.
     */
    public static final byte NEXT_PASSCODE = 29;

    /**
     * New-Pin [RFC2869]
     * <p>
     * Sent by the server to request that the user define a new PIN.
     */
    public static final byte NEW_PIN = 30;

    /**
     * Terminate-Session [RFC2869]
     * <p>
     * Sent by the server to request the immediate termination of a user session.
     */
    public static final byte TERMINATE_SESSION = 31;

    /**
     * Password-Expired [RFC2869]
     * <p>
     * Sent by the server to indicate that the user's password has expired and must be changed.
     */
    public static final byte PASSWORD_EXPIRED = 32;

    /**
     * Event-Request [RFC2869]
     * <p>
     * Sent by the client to record a specific event on the RADIUS server.
     */
    public static final byte EVENT_REQUEST = 33;

    /**
     * Event-Response [RFC2869]
     * <p>
     * Sent by the server to acknowledge receipt and recording of an event.
     */
    public static final byte EVENT_RESPONSE = 34;

    /**
     * Disconnect-Request [RFC3576]
     * <p>
     * Sent by the RADIUS server to the NAS to request that a specific user session be terminated.
     */
    public static final byte DISCONNECT_REQUEST = 40;

    /**
     * Disconnect-ACK [RFC3576]
     * <p>
     * Sent by the NAS to the RADIUS server to confirm that the session has been successfully disconnected.
     */
    public static final byte DISCONNECT_ACK = 41;

    /**
     * Disconnect-NAK [RFC3576]
     * <p>
     * Sent by the NAS to indicate that the disconnect request could not be fulfilled.
     */
    public static final byte DISCONNECT_NAK = 42;

    /**
     * CoA-Request [RFC3576]
     * <p>
     * Change-of-Authorization request sent by the server to dynamically change session parameters.
     */
    public static final byte COA_REQUEST = 43;

    /**
     * CoA-ACK [RFC3576]
     * <p>
     * Sent by the NAS to confirm that the Change-of-Authorization request was successfully applied.
     */
    public static final byte COA_ACK = 44;

    /**
     * CoA-NAK [RFC3576]
     * <p>
     * Sent by the NAS to indicate that the Change-of-Authorization request could not be fulfilled.
     */
    public static final byte COA_NAK = 45;

    /**
     * IP-Address-Allocate [RFC2882]
     * <p>
     * Sent by a client to request the allocation of an IP address from a managed pool.
     */
    public static final byte IP_ADDRESS_ALLOCATE = 50;

    /**
     * IP-Address-Release [RFC2882]
     * <p>
     * Sent by a client to indicate that an allocated IP address is being released.
     */
    public static final byte IP_ADDRESS_RELEASE = 51;

    /**
     * Protocol-Error [RFC2882]
     * <p>
     * Sent by a client or server to indicate a protocol-level error in a received packet.
     */
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

    /**
     * Returns the name of the packet type for the given code.
     *
     * @param code packet type code
     * @return packet type name
     */
    @NonNull
    public static String getPacketTypeName(byte code) {
        return typeNames.getOrDefault(code, "Unknown (" + code + ")");
    }
}
