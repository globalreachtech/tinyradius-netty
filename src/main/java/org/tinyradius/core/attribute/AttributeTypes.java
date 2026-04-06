package org.tinyradius.core.attribute;

import lombok.experimental.UtilityClass;

/**
 * Based on official IANA assignments for <a href="https://www.iana.org/assignments/radius-types/radius-types.xhtml#radius-types-2">Radius Attribute Types</a>
 */
@SuppressWarnings("unused")
@UtilityClass
public class AttributeTypes {

    /**
     * User-Name [RFC2865]
     * <p>
     * Indicates the name of the user to be authenticated.
     */
    public static final byte USER_NAME = 1;

    /**
     * User-Password [RFC2865]
     * <p>
     * Contains the user's password, encrypted using the RADIUS shared secret.
     */
    public static final byte USER_PASSWORD = 2;

    /**
     * CHAP-Password [RFC2865]
     * <p>
     * Contains the response to a CHAP challenge.
     */
    public static final byte CHAP_PASSWORD = 3;

    /**
     * NAS-IP-Address [RFC2865]
     * <p>
     * The IPv4 address of the NAS requesting authentication.
     */
    public static final byte NAS_IP_ADDRESS = 4;

    /**
     * NAS-Port [RFC2865]
     * <p>
     * The physical port number of the NAS which is authenticating the user.
     */
    public static final byte NAS_PORT = 5;

    /**
     * Service-Type [RFC2865]
     * <p>
     * Indicates the type of service the user has requested (e.g., Login, Framed).
     */
    public static final byte SERVICE_TYPE = 6;

    /**
     * Framed-Protocol [RFC2865]
     * <p>
     * The framing protocol to be used for framed access (e.g., PPP, SLIP).
     */
    public static final byte FRAMED_PROTOCOL = 7;

    /**
     * Framed-IP-Address [RFC2865]
     * <p>
     * The IPv4 address to be configured for the user.
     */
    public static final byte FRAMED_IP_ADDRESS = 8;

    /**
     * Framed-IP-Netmask [RFC2865]
     * <p>
     * The IPv4 netmask to be configured for the user.
     */
    public static final byte FRAMED_IP_NETMASK = 9;

    /**
     * Framed-Routing [RFC2865]
     * <p>
     * Indicates the routing method for the user (e.g., None, Send routing packets).
     */
    public static final byte FRAMED_ROUTING = 10;

    /**
     * Filter-Id [RFC2865]
     * <p>
     * The name of the filter list to be applied for the user.
     */
    public static final byte FILTER_ID = 11;

    /**
     * Framed-MTU [RFC2865]
     * <p>
     * The Maximum Transmission Unit to be configured for the user.
     */
    public static final byte FRAMED_MTU = 12;

    /**
     * Framed-Compression [RFC2865]
     * <p>
     * The compression protocol to be used for the framed link (e.g., V.42bis, VJC).
     */
    public static final byte FRAMED_COMPRESSION = 13;

    /**
     * Login-IP-Host [RFC2865]
     * <p>
     * The IPv4 address of the host with which the user is to be connected.
     */
    public static final byte LOGIN_IP_HOST = 14;

    /**
     * Login-Service [RFC2865]
     * <p>
     * The service to be used to connect the user to the login host (e.g., Telnet, Rlogin).
     */
    public static final byte LOGIN_SERVICE = 15;

    /**
     * Login-TCP-Port [RFC2865]
     * <p>
     * The TCP port with which the user is to be connected.
     */
    public static final byte LOGIN_TCP_PORT = 16;

    /**
     * Reply-Message [RFC2865]
     * <p>
     * A text message to be displayed to the user.
     */
    public static final byte REPLY_MESSAGE = 18;

    /**
     * Callback-Number [RFC2865]
     * <p>
     * The number to be dialed for callback.
     */
    public static final byte CALLBACK_NUMBER = 19;

    /**
     * Callback-Id [RFC2865]
     * <p>
     * The name of the callback place to be used.
     */
    public static final byte CALLBACK_ID = 20;

    /**
     * Framed-Route [RFC2865]
     * <p>
     * Routing information to be configured for the user.
     */
    public static final byte FRAMED_ROUTE = 22;

    /**
     * Framed-IPX-Network [RFC2865]
     * <p>
     * The IPX network number to be configured for the user.
     */
    public static final byte FRAMED_IPX_NETWORK = 23;

    /**
     * State [RFC2865]
     * <p>
     * Used in Access-Challenge and Access-Request to match sessions.
     */
    public static final byte STATE = 24;

    /**
     * Class [RFC2865]
     * <p>
     * Arbitrary data sent by the server to be included in subsequent accounting records.
     */
    public static final byte CLASS = 25;

    /**
     * Vendor-Specific [RFC2865]
     * <p>
     * Used to carry vendor-specific information.
     */
    public static final byte VENDOR_SPECIFIC = 26;

    /**
     * Session-Timeout [RFC2865]
     * <p>
     * The maximum number of seconds of service to be provided.
     */
    public static final byte SESSION_TIMEOUT = 27;

    /**
     * Idle-Timeout [RFC2865]
     * <p>
     * The maximum number of consecutive seconds of idle time before termination.
     */
    public static final byte IDLE_TIMEOUT = 28;

    /**
     * Termination-Action [RFC2865]
     * <p>
     * Indicates what action should be taken when the session times out (e.g., Default, RADIUS-Request).
     */
    public static final byte TERMINATION_ACTION = 29;

    /**
     * Called-Station-Id [RFC2865]
     * <p>
     * The phone number or MAC address called by the user.
     */
    public static final byte CALLED_STATION_ID = 30;

    /**
     * Calling-Station-Id [RFC2865]
     * <p>
     * The phone number or MAC address from which the user called.
     */
    public static final byte CALLING_STATION_ID = 31;

    /**
     * NAS-Identifier [RFC2865]
     * <p>
     * A string identifying the NAS requesting authentication.
     */
    public static final byte NAS_IDENTIFIER = 32;

    /**
     * Proxy-State [RFC2865]
     * <p>
     * Used by proxy servers to track requests.
     */
    public static final byte PROXY_STATE = 33;

    /**
     * Login-LAT-Service [RFC2865]
     * <p>
     * The LAT service to be used for the user.
     */
    public static final byte LOGIN_LAT_SERVICE = 34;

    /**
     * Login-LAT-Node [RFC2865]
     * <p>
     * The LAT node to be used for the user.
     */
    public static final byte LOGIN_LAT_NODE = 35;

    /**
     * Login-LAT-Group [RFC2865]
     * <p>
     * The LAT group string to be used for the user.
     */
    public static final byte LOGIN_LAT_GROUP = 36;

    /**
     * Framed-AppleTalk-Link [RFC2865]
     * <p>
     * The AppleTalk network number for the link to the user.
     */
    public static final byte FRAMED_APPLE_TALK_LINK = 37;

    /**
     * Framed-AppleTalk-Network [RFC2865]
     * <p>
     * The AppleTalk network number for the user's network.
     */
    public static final byte FRAMED_APPLE_TALK_NETWORK = 38;

    /**
     * Framed-AppleTalk-Zone [RFC2865]
     * <p>
     * The AppleTalk zone name for the user.
     */
    public static final byte FRAMED_APPLE_TALK_ZONE = 39;

    /**
     * Acct-Status-Type [RFC2866]
     * <p>
     * Indicates whether the Accounting-Request marks the start, stop, or interim update of a session.
     */
    public static final byte ACCT_STATUS_TYPE = 40;

    /**
     * Acct-Delay-Time [RFC2866]
     * <p>
     * Indicates how many seconds the NAS has been trying to send the Accounting-Request.
     */
    public static final byte ACCT_DELAY_TIME = 41;

    /**
     * Acct-Input-Octets [RFC2866]
     * <p>
     * The total number of octets received from the user during the session.
     */
    public static final byte ACCT_INPUT_OCTETS = 42;

    /**
     * Acct-Output-Octets [RFC2866]
     * <p>
     * The total number of octets sent to the user during the session.
     */
    public static final byte ACCT_OUTPUT_OCTETS = 43;

    /**
     * Acct-Session-Id [RFC2866]
     * <p>
     * A unique identifier for the user's session.
     */
    public static final byte ACCT_SESSION_ID = 44;

    /**
     * Acct-Authentic [RFC2866]
     * <p>
     * Indicates how the user was authenticated (e.g., RADIUS, Local, Remote).
     */
    public static final byte ACCT_AUTHENTIC = 45;

    /**
     * Acct-Session-Time [RFC2866]
     * <p>
     * The total duration of the session in seconds.
     */
    public static final byte ACCT_SESSION_TIME = 46;

    /**
     * Acct-Input-Packets [RFC2866]
     * <p>
     * The total number of packets received from the user during the session.
     */
    public static final byte ACCT_INPUT_PACKETS = 47;

    /**
     * Acct-Output-Packets [RFC2866]
     * <p>
     * The total number of packets sent to the user during the session.
     */
    public static final byte ACCT_OUTPUT_PACKETS = 48;

    /**
     * Acct-Terminate-Cause [RFC2866]
     * <p>
     * Indicates why the session was terminated (e.g., User Request, Lost Carrier).
     */
    public static final byte ACCT_TERMINATE_CAUSE = 49;

    /**
     * Acct-Multi-Session-Id [RFC2866]
     * <p>
     * Used to link together multiple related accounting sessions.
     */
    public static final byte ACCT_MULTI_SESSION_ID = 50;

    /**
     * Acct-Link-Count [RFC2866]
     * <p>
     * The number of links in a multi-link session.
     */
    public static final byte ACCT_LINK_COUNT = 51;

    /**
     * Acct-Input-Gigawords [RFC2869]
     * <p>
     * Indicates how many times the Acct-Input-Octets counter has rolled over 2^32.
     */
    public static final byte ACCT_INPUT_GIGAWORDS = 52;

    /**
     * Acct-Output-Gigawords [RFC2869]
     * <p>
     * Indicates how many times the Acct-Output-Octets counter has rolled over 2^32.
     */
    public static final byte ACCT_OUTPUT_GIGAWORDS = 53;

    /**
     * Event-Timestamp [RFC2869]
     * <p>
     * The time at which the event occurred (seconds since Jan 1, 1970).
     */
    public static final byte EVENT_TIMESTAMP = 55;

    /**
     * Egress-VLANID [RFC4675]
     * <p>
     * The VLAN ID to be used for egress packets.
     */
    public static final byte EGRESS_VLANID = 56;

    /**
     * Ingress-Filters [RFC4675]
     * <p>
     * Indicates whether ingress filters should be enabled or disabled for the user.
     */
    public static final byte INGRESS_FILTERS = 57;

    /**
     * Egress-VLAN-Name [RFC4675]
     * <p>
     * The name of the egress VLAN.
     */
    public static final byte EGRESS_VLAN_NAME = 58;

    /**
     * User-Priority-Table [RFC4675]
     * <p>
     * A table defining the mapping of user priority to traffic class.
     */
    public static final byte USER_PRIORITY_TABLE = 59;

    /**
     * CHAP-Challenge [RFC2865]
     * <p>
     * The challenge sent by the NAS to the user in CHAP authentication.
     */
    public static final byte CHAP_CHALLENGE = 60;

    /**
     * NAS-Port-Type [RFC2865]
     * <p>
     * The type of physical port used by the NAS (e.g., Async, ISDN, Ethernet).
     */
    public static final byte NAS_PORT_TYPE = 61;

    /**
     * Port-Limit [RFC2865]
     * <p>
     * The maximum number of ports that the user may use concurrently.
     */
    public static final byte PORT_LIMIT = 62;

    /**
     * Login-LAT-Port [RFC2865]
     * <p>
     * The LAT port to be used for the user.
     */
    public static final byte LOGIN_LAT_PORT = 63;

    /**
     * Tunnel-Type [RFC2868]
     * <p>
     * The tunneling protocol to be used (e.g., PPTP, L2TP).
     */
    public static final byte TUNNEL_TYPE = 64;

    /**
     * Tunnel-Medium-Type [RFC2868]
     * <p>
     * The transport medium to be used for the tunnel (e.g., IPv4, IPv6).
     */
    public static final byte TUNNEL_MEDIUM_TYPE = 65;

    /**
     * Tunnel-Client-Endpoint [RFC2868]
     * <p>
     * The address of the client end of the tunnel.
     */
    public static final byte TUNNEL_CLIENT_ENDPOINT = 66;

    /**
     * Tunnel-Server-Endpoint [RFC2868]
     * <p>
     * The address of the server end of the tunnel.
     */
    public static final byte TUNNEL_SERVER_ENDPOINT = 67;

    /**
     * Acct-Tunnel-Connection [RFC2867]
     * <p>
     * A unique identifier for a tunnel connection in accounting.
     */
    public static final byte ACCT_TUNNEL_CONNECTION = 68;

    /**
     * Tunnel-Password [RFC2868]
     * <p>
     * The password used for tunnel authentication, encrypted.
     */
    public static final byte TUNNEL_PASSWORD = 69;

    /**
     * ARAP-Password [RFC2869]
     * <p>
     * The user's ARAP password.
     */
    public static final byte ARAP_PASSWORD = 70;

    /**
     * ARAP-Features [RFC2869]
     * <p>
     * The features supported by the ARAP protocol.
     */
    public static final byte ARAP_FEATURES = 71;

    /**
     * ARAP-Zone-Access [RFC2869]
     * <p>
     * The zone access permissions for ARAP.
     */
    public static final byte ARAP_ZONE_ACCESS = 72;

    /**
     * ARAP-Security [RFC2869]
     * <p>
     * The security level for ARAP.
     */
    public static final byte ARAP_SECURITY = 73;

    /**
     * ARAP-Security-Data [RFC2869]
     * <p>
     * The security data for ARAP.
     */
    public static final byte ARAP_SECURITY_DATA = 74;

    /**
     * Password-Retry [RFC2869]
     * <p>
     * The number of password retries allowed.
     */
    public static final byte PASSWORD_RETRY = 75;

    /**
     * Prompt [RFC2869]
     * <p>
     * Indicates whether a prompt should be displayed to the user.
     */
    public static final byte PROMPT = 76;

    /**
     * Connect-Info [RFC2869]
     * <p>
     * Text information about the connection.
     */
    public static final byte CONNECT_INFO = 77;

    /**
     * Configuration-Token [RFC2869]
     * <p>
     * A token used to retrieve configuration information.
     */
    public static final byte CONFIGURATION_TOKEN = 78;

    /**
     * EAP-Message [RFC2869]
     * <p>
     * Carries EAP (Extensible Authentication Protocol) packets.
     */
    public static final byte EAP_MESSAGE = 79;

    /**
     * Message-Authenticator [RFC2869]
     * <p>
     * A HMAC-MD5 hash used to protect EAP messages from tampering.
     */
    public static final byte MESSAGE_AUTHENTICATOR = 80;

    /**
     * Tunnel-Private-Group-ID [RFC2868]
     * <p>
     * An identifier for a private group within a tunnel (often used for VLAN ID).
     */
    public static final byte TUNNEL_PRIVATE_GROUP_ID = 81;

    /**
     * Tunnel-Assignment-ID [RFC2868]
     * <p>
     * An identifier for a specific tunnel assignment.
     */
    public static final byte TUNNEL_ASSIGNMENT_ID = 82;

    /**
     * Tunnel-Preference [RFC2868]
     * <p>
     * The preference level for a tunnel.
     */
    public static final byte TUNNEL_PREFERENCE = 83;

    /**
     * ARAP-Challenge-Response [RFC2869]
     * <p>
     * The response to an ARAP challenge.
     */
    public static final byte ARAP_CHALLENGE_RESPONSE = 84;

    /**
     * Acct-Interim-Interval [RFC2869]
     * <p>
     * The interval in seconds between interim accounting updates.
     */
    public static final byte ACCT_INTERIM_INTERVAL = 85;

    /**
     * Acct-Tunnel-Packets-Lost [RFC2867]
     * <p>
     * The number of packets lost in a tunnel.
     */
    public static final byte ACCT_TUNNEL_PACKETS_LOST = 86;

    /**
     * NAS-Port-Id [RFC2869]
     * <p>
     * A string identifying the physical port of the NAS.
     */
    public static final byte NAS_PORT_ID = 87;

    /**
     * Framed-Pool [RFC2869]
     * <p>
     * The name of the address pool to be used for IP address allocation.
     */
    public static final byte FRAMED_POOL = 88;

    /**
     * CUI [RFC4372]
     * <p>
     * Chargeable User Identity, used for billing across different networks.
     */
    public static final byte CUI = 89;

    /**
     * Tunnel-Client-Auth-ID [RFC2868]
     * <p>
     * The authentication identifier for the tunnel client.
     */
    public static final byte TUNNEL_CLIENT_AUTH_ID = 90;

    /**
     * Tunnel-Server-Auth-ID [RFC2868]
     * <p>
     * The authentication identifier for the tunnel server.
     */
    public static final byte TUNNEL_SERVER_AUTH_ID = 91;

    /**
     * NAS-Filter-Rule [RFC4849]
     * <p>
     * A filter rule to be applied at the NAS.
     */
    public static final byte NAS_FILTER_RULE = 92;

    /**
     * Originating-Line-Info [RFC7155]
     * <p>
     * Information about the originating line of the call.
     */
    public static final byte ORIGINATING_LINE_INFO = 94;

    /**
     * NAS-IPv6-Address [RFC3162]
     * <p>
     * The IPv6 address of the NAS.
     */
    public static final byte NAS_IPV6_ADDRESS = 95;

    /**
     * Framed-Interface-Id [RFC3162]
     * <p>
     * The IPv6 interface identifier to be used for the user.
     */
    public static final byte FRAMED_INTERFACE_ID = 96;

    /**
     * Framed-IPv6-Prefix [RFC3162]
     * <p>
     * The IPv6 prefix to be configured for the user.
     */
    public static final byte FRAMED_IPV6_PREFIX = 97;

    /**
     * Login-IPv6-Host [RFC3162]
     * <p>
     * The IPv6 address of the login host.
     */
    public static final byte LOGIN_IPV6_HOST = 98;

    /**
     * Framed-IPv6-Route [RFC3162]
     * <p>
     * IPv6 routing information to be configured for the user.
     */
    public static final byte FRAMED_IPV6_ROUTE = 99;

    /**
     * Framed-IPv6-Pool [RFC3162]
     * <p>
     * The name of the IPv6 address pool to be used.
     */
    public static final byte FRAMED_IPv6_POOL = 100;

    /**
     * Error-Cause Attribute [RFC3576]
     * <p>
     * Provides a detailed reason for a Disconnect-NAK or CoA-NAK.
     */
    public static final byte ERROR_CAUSE_ATTRIBUTE = 101;

    /**
     * EAP-Key-Name [RFC4072][RFC7268]
     * <p>
     * A name for the keying material derived from an EAP authentication.
     */
    public static final byte EAP_KEY_NAME = 102;

    /**
     * Digest-Response [RFC5090]
     * <p>
     * The response to a HTTP/SIP digest challenge.
     */
    public static final byte DIGEST_RESPONSE = 103;

    /**
     * Digest-Realm [RFC5090]
     * <p>
     * The realm used in digest authentication.
     */
    public static final byte DIGEST_REALM = 104;

    /**
     * Digest-Nonce [RFC5090]
     * <p>
     * The nonce used in digest authentication.
     */
    public static final byte DIGEST_NONCE = 105;

    /**
     * Digest-Response-Auth [RFC5090]
     * <p>
     * Authentication data for digest response.
     */
    public static final byte DIGEST_RESPONSE_AUTH = 106;

    /**
     * Digest-Nextnonce [RFC5090]
     * <p>
     * The next nonce to be used in digest authentication.
     */
    public static final byte DIGEST_NEXTNONCE = 107;

    /**
     * Digest-Method [RFC5090]
     * <p>
     * The HTTP method used in digest authentication.
     */
    public static final byte DIGEST_METHOD = 108;

    /**
     * Digest-URI [RFC5090]
     * <p>
     * The URI being requested in digest authentication.
     */
    public static final byte DIGEST_URI = 109;

    /**
     * Digest-Qop [RFC5090]
     * <p>
     * The Quality of Protection parameter in digest authentication.
     */
    public static final byte DIGEST_QOP = 110;

    /**
     * Digest-Algorithm [RFC5090]
     * <p>
     * The algorithm used in digest authentication.
     */
    public static final byte DIGEST_ALGORITHM = 111;

    /**
     * Digest-Entity-Body-Hash [RFC5090]
     * <p>
     * The hash of the entity body in digest authentication.
     */
    public static final byte DIGEST_ENTITY_BODY_HASH = 112;

    /**
     * Digest-CNonce [RFC5090]
     * <p>
     * The client nonce used in digest authentication.
     */
    public static final byte DIGEST_CNONCE = 113;

    /**
     * Digest-Nonce-Count [RFC5090]
     * <p>
     * The hexadecimal count of nonces sent by the client.
     */
    public static final byte DIGEST_NONCE_COUNT = 114;

    /**
     * Digest-Username [RFC5090]
     * <p>
     * The username used in digest authentication.
     */
    public static final byte DIGEST_USERNAME = 115;

    /**
     * Digest-Opaque [RFC5090]
     * <p>
     * Opaque data used in digest authentication.
     */
    public static final byte DIGEST_OPAQUE = 116;

    /**
     * Digest-Auth-Param [RFC5090]
     * <p>
     * Authentication parameters for digest authentication.
     */
    public static final byte DIGEST_AUTH_PARAM = 117;

    /**
     * Digest-AKA-Auts [RFC5090]
     * <p>
     * Authentication synchronization parameter for AKA.
     */
    public static final byte DIGEST_AKA_AUTS = 118;

    /**
     * Digest-Domain [RFC5090]
     * <p>
     * The domain list used in digest authentication.
     */
    public static final byte DIGEST_DOMAIN = 119;

    /**
     * Digest-Stale [RFC5090]
     * <p>
     * Indicates whether the nonce is stale.
     */
    public static final byte DIGEST_STALE = 120;

    /**
     * Digest-HA1 [RFC5090]
     * <p>
     * The precalculated HA1 value for digest authentication.
     */
    public static final byte DIGEST_HA_1 = 121;

    /**
     * SIP-AOR [RFC5090]
     * <p>
     * SIP Address of Record.
     */
    public static final byte SIP_AOR = 122;

    /**
     * Delegated-IPv6-Prefix [RFC4818]
     * <p>
     * An IPv6 prefix delegated to the user.
     */
    public static final byte DELEGATED_IPV6_PREFIX = 123;

    /**
     * MIP6-Feature-Vector [RFC5447]
     * <p>
     * Capability vector for Mobile IPv6.
     */
    public static final byte MIP6_FEATURE_VECTOR = 124;

    /**
     * MIP6-Home-Link-Prefix [RFC5447]
     * <p>
     * The prefix of the home link for Mobile IPv6.
     */
    public static final byte MIP6_HOME_LINK_PREFIX = 125;

    /**
     * Operator-Name [RFC5580]
     * <p>
     * The name of the network operator.
     */
    public static final byte OPERATOR_NAME = 126;

    /**
     * Location-Information [RFC5580]
     * <p>
     * Geographic or civic location information.
     */
    public static final byte LOCATION_INFORMATION = 127;

    /**
     * Location-Data [RFC5580]
     * <p>
     * Binary location data.
     */
    public static final byte LOCATION_DATA = (byte) 128;

    /**
     * Basic-Location-Policy-Rules [RFC5580]
     * <p>
     * Policy rules for location information sharing.
     */
    public static final byte BASIC_LOCATION_POLICY_RULES = (byte) 129;

    /**
     * Extended-Location-Policy-Rules [RFC5580]
     * <p>
     * Extended policy rules for location information.
     */
    public static final byte EXTENDED_LOCATION_POLICY_RULES = (byte) 130;

    /**
     * Location-Capable [RFC5580]
     * <p>
     * Indicates location information capabilities.
     */
    public static final byte LOCATION_CAPABLE = (byte) 131;

    /**
     * Requested-Location-Info [RFC5580]
     * <p>
     * Indicates the type of location information requested.
     */
    public static final byte REQUESTED_LOCATION_INFO = (byte) 132;

    /**
     * Framed-Management-Protocol [RFC5607]
     * <p>
     * The management protocol to be used (e.g., SNMP, NETCONF).
     */
    public static final byte FRAMED_MANAGEMENT_PROTOCOL = (byte) 133;

    /**
     * Management-Transport-Protection [RFC5607]
     * <p>
     * The transport protection to be used for management (e.g., SSH, TLS).
     */
    public static final byte MANAGEMENT_TRANSPORT_PROTECTION = (byte) 134;

    /**
     * Management-Policy-Id [RFC5607]
     * <p>
     * The management policy identifier.
     */
    public static final byte MANAGEMENT_POLICY_ID = (byte) 135;

    /**
     * Management-Privilege-Level [RFC5607]
     * <p>
     * The privilege level for management access.
     */
    public static final byte MANAGEMENT_PRIVILEGE_LEVEL = (byte) 136;

    /**
     * PKM-SS-Cert [RFC5904]
     * <p>
     * SS certificate for PKM (Privacy Key Management).
     */
    public static final byte PKM_SS_CERT = (byte) 137;

    /**
     * PKM-CA-Cert [RFC5904]
     * <p>
     * CA certificate for PKM.
     */
    public static final byte PKM_CA_CERT = (byte) 138;

    /**
     * PKM-Config-Settings [RFC5904]
     * <p>
     * Configuration settings for PKM.
     */
    public static final byte PKM_CONFIG_SETTINGS = (byte) 139;

    /**
     * PKM-Cryptosuite-List [RFC5904]
     * <p>
     * List of supported cryptosuites for PKM.
     */
    public static final byte PKM_CRYPTOSUITE_LIST = (byte) 140;

    /**
     * PKM-SAID [RFC5904]
     * <p>
     * Security Association Identifier for PKM.
     */
    public static final byte PKM_SAID = (byte) 141;

    /**
     * PKM-SA-Descriptor [RFC5904]
     * <p>
     * Security Association Descriptor for PKM.
     */
    public static final byte PKM_SA_DESCRIPTOR = (byte) 142;

    /**
     * PKM-Auth-Key [RFC5904]
     * <p>
     * Authentication key for PKM.
     */
    public static final byte PKM_AUTH_KEY = (byte) 143;

    /**
     * DS-Lite-Tunnel-Name [RFC6519]
     * <p>
     * The name of the Dual-Stack Lite tunnel.
     */
    public static final byte DS_LITE_TUNNEL_NAME = (byte) 144;

    /**
     * Mobile-Node-Identifier [RFC6572]
     * <p>
     * Identifier for a mobile node in PMIP6.
     */
    public static final byte MOBILE_NODE_IDENTIFIER = (byte) 145;

    /**
     * Service-Selection [RFC6572]
     * <p>
     * Selection of a specific service or network.
     */
    public static final byte SERVICE_SELECTION = (byte) 146;

    /**
     * PMIP6-Home-LMA-IPv6-Address [RFC6572]
     * <p>
     * The IPv6 address of the Home LMA (Local Mobility Anchor).
     */
    public static final byte PMIP6_HOME_LMA_IPV6_ADDRESS = (byte) 147;

    /**
     * PMIP6-Visited-LMA-IPv6-Address [RFC6572]
     * <p>
     * The IPv6 address of the Visited LMA.
     */
    public static final byte PMIP6_VISITED_LMA_IPV6_ADDRESS = (byte) 148;

    /**
     * PMIP6-Home-LMA-IPv4-Address [RFC6572]
     * <p>
     * The IPv4 address of the Home LMA.
     */
    public static final byte PMIP6_HOME_LMA_IPV4_ADDRESS = (byte) 149;

    /**
     * PMIP6-Visited-LMA-IPv4-Address [RFC6572]
     * <p>
     * The IPv4 address of the Visited LMA.
     */
    public static final byte PMIP6_VISITED_LMA_IPV4_ADDRESS = (byte) 150;

    /**
     * PMIP6-Home-HN-Prefix [RFC6572]
     * <p>
     * The Home Network prefix for PMIP6.
     */
    public static final byte PMIP6_HOME_HN_PREFIX = (byte) 151;

    /**
     * PMIP6-Visited-HN-Prefix [RFC6572]
     * <p>
     * The Visited Network prefix for PMIP6.
     */
    public static final byte PMIP6_VISITED_HN_PREFIX = (byte) 152;

    /**
     * PMIP6-Home-Interface-ID [RFC6572]
     * <p>
     * The interface identifier for the user's home network.
     */
    public static final byte PMIP6_HOME_INTERFACE_ID = (byte) 153;

    /**
     * PMIP6-Visited-Interface-ID [RFC6572]
     * <p>
     * The interface identifier for the user's visited network.
     */
    public static final byte PMIP6_VISITED_INTERFACE_ID = (byte) 154;

    /**
     * PMIP6-Home-IPv4-HoA [RFC6572]
     * <p>
     * The IPv4 Home Address for the user.
     */
    public static final byte PMIP6_HOME_IPV4_HOA = (byte) 155;

    /**
     * PMIP6-Visited-IPv4-HoA [RFC6572]
     * <p>
     * The IPv4 Home Address in the visited network.
     */
    public static final byte PMIP6_VISITED_IPV4_HOA = (byte) 156;

    /**
     * PMIP6-Home-DHCP4-Server-Address [RFC6572]
     * <p>
     * The IPv4 address of the home DHCP server.
     */
    public static final byte PMIP6_HOME_DHCP4_SERVER_ADDRESS = (byte) 157;

    /**
     * PMIP6-Visited-DHCP4-Server-Address [RFC6572]
     * <p>
     * The IPv4 address of the visited DHCP server.
     */
    public static final byte PMIP6_VISITED_DHCP4_SERVER_ADDRESS = (byte) 158;

    /**
     * PMIP6-Home-DHCP6-Server-Address [RFC6572]
     * <p>
     * The IPv6 address of the home DHCP server.
     */
    public static final byte PMIP6_HOME_DHCP6_SERVER_ADDRESS = (byte) 159;

    /**
     * PMIP6-Visited-DHCP6-Server-Address [RFC6572]
     * <p>
     * The IPv6 address of the visited DHCP server.
     */
    public static final byte PMIP6_VISITED_DHCP6_SERVER_ADDRESS = (byte) 160;

    /**
     * PMIP6-Home-IPv4-Gateway [RFC6572]
     * <p>
     * The IPv4 address of the home gateway.
     */
    public static final byte PMIP6_HOME_IPV4_GATEWAY = (byte) 161;

    /**
     * PMIP6-Visited-IPv4-Gateway [RFC6572]
     * <p>
     * The IPv4 address of the visited gateway.
     */
    public static final byte PMIP6_VISITED_IPV4_GATEWAY = (byte) 162;

    /**
     * EAP-Lower-Layer [RFC6677]
     * <p>
     * Specifies the lower layer protocol over which EAP is running.
     */
    public static final byte EAP_LOWER_LAYER = (byte) 163;

    /**
     * GSS-Acceptor-Service-Name [RFC7055]
     * <p>
     * The GSS-API service name for the acceptor.
     */
    public static final byte GSS_ACCEPTOR_SERVICE_NAME = (byte) 164;

    /**
     * GSS-Acceptor-Host-Name [RFC7055]
     * <p>
     * The GSS-API host name for the acceptor.
     */
    public static final byte GSS_ACCEPTOR_HOST_NAME = (byte) 165;

    /**
     * GSS-Acceptor-Service-Specifics [RFC7055]
     * <p>
     * Service-specific data for GSS-API.
     */
    public static final byte GSS_ACCEPTOR_SERVICE_SPECIFICS = (byte) 166;

    /**
     * GSS-Acceptor-Realm-Name [RFC7055]
     * <p>
     * The GSS-API realm name for the acceptor.
     */
    public static final byte GSS_ACCEPTOR_REALM_NAME = (byte) 167;

    /**
     * Framed-IPv6-Address [RFC6911]
     * <p>
     * The IPv6 address to be configured for the user.
     */
    public static final byte FRAMED_IPV6_ADDRESS = (byte) 168;

    /**
     * DNS-Server-IPv6-Address [RFC6911]
     * <p>
     * The IPv6 address of a DNS server.
     */
    public static final byte DNS_SERVER_IPV6_ADDRESS = (byte) 169;

    /**
     * Route-IPv6-Information [RFC6911]
     * <p>
     * Specific IPv6 route information for the user.
     */
    public static final byte ROUTE_IPV6_INFORMATION = (byte) 170;

    /**
     * Delegated-IPv6-Prefix-Pool [RFC6911]
     * <p>
     * The name of the pool from which IPv6 prefixes are delegated.
     */
    public static final byte DELEGATED_IPV6_PREFIX_POOL = (byte) 171;

    /**
     * Stateful-IPv6-Address-Pool [RFC6911]
     * <p>
     * The name of the pool for stateful IPv6 address allocation.
     */
    public static final byte STATEFUL_IPV6_ADDRESS_POOL = (byte) 172;

    /**
     * IPv6-6rd-Configuration [RFC6930]
     * <p>
     * Configuration parameters for IPv6 Rapid Deployment (6rd).
     */
    public static final byte IPV6_6RD_CONFIGURATION = (byte) 173;

    /**
     * Allowed-Called-Station-Id [RFC7268]
     * <p>
     * Specifies the SSIDs or MAC addresses a user is permitted to connect to.
     */
    public static final byte ALLOWED_CALLED_STATION_ID = (byte) 174;

    /**
     * EAP-Peer-Id [RFC7268]
     * <p>
     * The identity of the EAP peer.
     */
    public static final byte EAP_PEER_ID = (byte) 175;

    /**
     * EAP-Server-Id [RFC7268]
     * <p>
     * The identity of the EAP server.
     */
    public static final byte EAP_SERVER_ID = (byte) 176;

    /**
     * Mobility-Domain-Id [RFC7268]
     * <p>
     * Identifier for the mobility domain in 802.11r.
     */
    public static final byte MOBILITY_DOMAIN_ID = (byte) 177;

    /**
     * Preauth-Timeout [RFC7268]
     * <p>
     * The timeout for pre-authentication.
     */
    public static final byte PREAUTH_TIMEOUT = (byte) 178;

    /**
     * Network-Id-Name [RFC7268]
     * <p>
     * The name of the network identifier.
     */
    public static final byte NETWORK_ID_NAME = (byte) 179;

    /**
     * EAPoL-Announcement [RFC7268]
     * <p>
     * Carries 802.1X EAPoL announcement information.
     */
    public static final byte EAPOL_ANNOUNCEMENT = (byte) 180;

    /**
     * WLAN-HESSID [RFC7268]
     * <p>
     * The Homogeneous Extended Service Set Identifier for Wi-Fi.
     */
    public static final byte WLAN_HESSID = (byte) 181;

    /**
     * WLAN-Venue-Info [RFC7268]
     * <p>
     * Information about the Wi-Fi venue.
     */
    public static final byte WLAN_VENUE_INFO = (byte) 182;

    /**
     * WLAN-Venue-Language [RFC7268]
     * <p>
     * The language used for WLAN venue information.
     */
    public static final byte WLAN_VENUE_LANGUAGE = (byte) 183;

    /**
     * WLAN-Venue-Name [RFC7268]
     * <p>
     * The name of the WLAN venue.
     */
    public static final byte WLAN_VENUE_NAME = (byte) 184;

    /**
     * WLAN-Reason-Code [RFC7268]
     * <p>
     * The reason code for WLAN events.
     */
    public static final byte WLAN_REASON_CODE = (byte) 185;

    /**
     * WLAN-Pairwise-Cipher [RFC7268]
     * <p>
     * The pairwise cipher suite to be used.
     */
    public static final byte WLAN_PAIRWISE_CIPHER = (byte) 186;

    /**
     * WLAN-Group-Cipher [RFC7268]
     * <p>
     * The group cipher suite to be used.
     */
    public static final byte WLAN_GROUP_CIPHER = (byte) 187;

    /**
     * WLAN-AKM-Suite [RFC7268]
     * <p>
     * The Authentication and Key Management suite.
     */
    public static final byte WLAN_AKM_SUITE = (byte) 188;

    /**
     * WLAN-Group-Mgmt-Cipher [RFC7268]
     * <p>
     * The group management cipher suite to be used.
     */
    public static final byte WLAN_GROUP_MGMT_CIPHER = (byte) 189;

    /**
     * WLAN-RF-Band [RFC7268]
     * <p>
     * The radio frequency band to be used.
     */
    public static final byte WLAN_RF_BAND = (byte) 190;

    /**
     * Extended-Attribute-1 [RFC6929]
     * <p>
     * Used for extending the attribute space beyond 255.
     */
    public static final byte EXTENDED_ATTRIBUTE_1 = (byte) 241;

    /**
     * Extended-Attribute-2 [RFC6929]
     */
    public static final byte EXTENDED_ATTRIBUTE_2 = (byte) 242;

    /**
     * Extended-Attribute-3 [RFC6929]
     */
    public static final byte EXTENDED_ATTRIBUTE_3 = (byte) 243;

    /**
     * Extended-Attribute-4 [RFC6929]
     */
    public static final byte EXTENDED_ATTRIBUTE_4 = (byte) 244;

    /**
     * Extended-Attribute-5 [RFC6929]
     */
    public static final byte EXTENDED_ATTRIBUTE_5 = (byte) 245;

    /**
     * Extended-Attribute-6 [RFC6929]
     */
    public static final byte EXTENDED_ATTRIBUTE_6 = (byte) 246;

}
