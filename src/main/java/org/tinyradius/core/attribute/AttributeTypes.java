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
     */
    public static final byte USER_NAME = 1;

    /**
     * User-Password [RFC2865]
     */
    public static final byte USER_PASSWORD = 2;

    /**
     * CHAP-Password [RFC2865]
     */
    public static final byte CHAP_PASSWORD = 3;

    /**
     * NAS-IP-Address [RFC2865]
     */
    public static final byte NAS_IP_ADDRESS = 4;

    /**
     * NAS-Port [RFC2865]
     */
    public static final byte NAS_PORT = 5;

    /**
     * Service-Type [RFC2865]
     */
    public static final byte SERVICE_TYPE = 6;

    /**
     * Framed-Protocol [RFC2865]
     */
    public static final byte FRAMED_PROTOCOL = 7;

    /**
     * Framed-IP-Address [RFC2865]
     */
    public static final byte FRAMED_IP_ADDRESS = 8;

    /**
     * Framed-IP-Netmask [RFC2865]
     */
    public static final byte FRAMED_IP_NETMASK = 9;

    /**
     * Framed-Routing [RFC2865]
     */
    public static final byte FRAMED_ROUTING = 10;

    /**
     * Filter-Id [RFC2865]
     */
    public static final byte FILTER_ID = 11;

    /**
     * Framed-MTU [RFC2865]
     */
    public static final byte FRAMED_MTU = 12;

    /**
     * Framed-Compression [RFC2865]
     */
    public static final byte FRAMED_COMPRESSION = 13;

    /**
     * Login-IP-Host [RFC2865]
     */
    public static final byte LOGIN_IP_HOST = 14;

    /**
     * Login-Service [RFC2865]
     */
    public static final byte LOGIN_SERVICE = 15;

    /**
     * Login-TCP-Port [RFC2865]
     */
    public static final byte LOGIN_TCP_PORT = 16;

    /**
     * Reply-Message [RFC2865]
     */
    public static final byte REPLY_MESSAGE = 18;

    /**
     * Callback-Number [RFC2865]
     */
    public static final byte CALLBACK_NUMBER = 19;

    /**
     * Callback-Id [RFC2865]
     */
    public static final byte CALLBACK_ID = 20;

    /**
     * Framed-Route [RFC2865]
     */
    public static final byte FRAMED_ROUTE = 22;

    /**
     * Framed-IPX-Network [RFC2865]
     */
    public static final byte FRAMED_IPX_NETWORK = 23;

    /**
     * State [RFC2865]
     */
    public static final byte STATE = 24;

    /**
     * Class [RFC2865]
     */
    public static final byte CLASS = 25;

    /**
     * Vendor-Specific [RFC2865]
     */
    public static final byte VENDOR_SPECIFIC = 26;

    /**
     * Session-Timeout [RFC2865]
     */
    public static final byte SESSION_TIMEOUT = 27;

    /**
     * Idle-Timeout [RFC2865]
     */
    public static final byte IDLE_TIMEOUT = 28;

    /**
     * Termination-Action [RFC2865]
     */
    public static final byte TERMINATION_ACTION = 29;

    /**
     * Called-Station-Id [RFC2865]
     */
    public static final byte CALLED_STATION_ID = 30;

    /**
     * Calling-Station-Id [RFC2865]
     */
    public static final byte CALLING_STATION_ID = 31;

    /**
     * NAS-Identifier [RFC2865]
     */
    public static final byte NAS_IDENTIFIER = 32;

    /**
     * Proxy-State [RFC2865]
     */
    public static final byte PROXY_STATE = 33;

    /**
     * Login-LAT-Service [RFC2865]
     */
    public static final byte LOGIN_LAT_SERVICE = 34;

    /**
     * Login-LAT-Node [RFC2865]
     */
    public static final byte LOGIN_LAT_NODE = 35;

    /**
     * Login-LAT-Group [RFC2865]
     */
    public static final byte LOGIN_LAT_GROUP = 36;

    /**
     * Framed-AppleTalk-Link [RFC2865]
     */
    public static final byte FRAMED_APPLE_TALK_LINK = 37;

    /**
     * Framed-AppleTalk-Network [RFC2865]
     */
    public static final byte FRAMED_APPLE_TALK_NETWORK = 38;

    /**
     * Framed-AppleTalk-Zone [RFC2865]
     */
    public static final byte FRAMED_APPLE_TALK_ZONE = 39;

    /**
     * Acct-Status-Type [RFC2866]
     */
    public static final byte ACCT_STATUS_TYPE = 40;

    /**
     * Acct-Delay-Time [RFC2866]
     */
    public static final byte ACCT_DELAY_TIME = 41;

    /**
     * Acct-Input-Octets [RFC2866]
     */
    public static final byte ACCT_INPUT_OCTETS = 42;

    /**
     * Acct-Output-Octets [RFC2866]
     */
    public static final byte ACCT_OUTPUT_OCTETS = 43;

    /**
     * Acct-Session-Id [RFC2866]
     */
    public static final byte ACCT_SESSION_ID = 44;

    /**
     * Acct-Authentic [RFC2866]
     */
    public static final byte ACCT_AUTHENTIC = 45;

    /**
     * Acct-Session-Time [RFC2866]
     */
    public static final byte ACCT_SESSION_TIME = 46;

    /**
     * Acct-Input-Packets [RFC2866]
     */
    public static final byte ACCT_INPUT_PACKETS = 47;

    /**
     * Acct-Output-Packets [RFC2866]
     */
    public static final byte ACCT_OUTPUT_PACKETS = 48;

    /**
     * Acct-Terminate-Cause [RFC2866]
     */
    public static final byte ACCT_TERMINATE_CAUSE = 49;

    /**
     * Acct-Multi-Session-Id [RFC2866]
     */
    public static final byte ACCT_MULTI_SESSION_ID = 50;

    /**
     * Acct-Link-Count [RFC2866]
     */
    public static final byte ACCT_LINK_COUNT = 51;

    /**
     * Acct-Input-Gigawords [RFC2869]
     */
    public static final byte ACCT_INPUT_GIGAWORDS = 52;

    /**
     * Acct-Output-Gigawords [RFC2869]
     */
    public static final byte ACCT_OUTPUT_GIGAWORDS = 53;

    /**
     * Event-Timestamp [RFC2869]
     */
    public static final byte EVENT_TIMESTAMP = 55;

    /**
     * Egress-VLANID [RFC4675]
     */
    public static final byte EGRESS_VLANID = 56;

    /**
     * Ingress-Filters [RFC4675]
     */
    public static final byte INGRESS_FILTERS = 57;

    /**
     * Egress-VLAN-Name [RFC4675]
     */
    public static final byte EGRESS_VLAN_NAME = 58;

    /**
     * User-Priority-Table [RFC4675]
     */
    public static final byte USER_PRIORITY_TABLE = 59;

    /**
     * CHAP-Challenge [RFC2865]
     */
    public static final byte CHAP_CHALLENGE = 60;

    /**
     * NAS-Port-Type [RFC2865]
     */
    public static final byte NAS_PORT_TYPE = 61;

    /**
     * Port-Limit [RFC2865]
     */
    public static final byte PORT_LIMIT = 62;

    /**
     * Login-LAT-Port [RFC2865]
     */
    public static final byte LOGIN_LAT_PORT = 63;

    /**
     * Tunnel-Type [RFC2868]
     */
    public static final byte TUNNEL_TYPE = 64;

    /**
     * Tunnel-Medium-Type [RFC2868]
     */
    public static final byte TUNNEL_MEDIUM_TYPE = 65;

    /**
     * Tunnel-Client-Endpoint [RFC2868]
     */
    public static final byte TUNNEL_CLIENT_ENDPOINT = 66;

    /**
     * Tunnel-Server-Endpoint [RFC2868]
     */
    public static final byte TUNNEL_SERVER_ENDPOINT = 67;

    /**
     * Acct-Tunnel-Connection [RFC2867]
     */
    public static final byte ACCT_TUNNEL_CONNECTION = 68;

    /**
     * Tunnel-Password [RFC2868]
     */
    public static final byte TUNNEL_PASSWORD = 69;

    /**
     * ARAP-Password [RFC2869]
     */
    public static final byte ARAP_PASSWORD = 70;

    /**
     * ARAP-Features [RFC2869]
     */
    public static final byte ARAP_FEATURES = 71;

    /**
     * ARAP-Zone-Access [RFC2869]
     */
    public static final byte ARAP_ZONE_ACCESS = 72;

    /**
     * ARAP-Security [RFC2869]
     */
    public static final byte ARAP_SECURITY = 73;

    /**
     * ARAP-Security-Data [RFC2869]
     */
    public static final byte ARAP_SECURITY_DATA = 74;

    /**
     * Password-Retry [RFC2869]
     */
    public static final byte PASSWORD_RETRY = 75;

    /**
     * Prompt [RFC2869]
     */
    public static final byte PROMPT = 76;

    /**
     * Connect-Info [RFC2869]
     */
    public static final byte CONNECT_INFO = 77;

    /**
     * Configuration-Token [RFC2869]
     */
    public static final byte CONFIGURATION_TOKEN = 78;

    /**
     * EAP-Message [RFC2869]
     */
    public static final byte EAP_MESSAGE = 79;

    /**
     * Message-Authenticator [RFC2869]
     */
    public static final byte MESSAGE_AUTHENTICATOR = 80;

    /**
     * Tunnel-Private-Group-ID [RFC2868]
     */
    public static final byte TUNNEL_PRIVATE_GROUP_ID = 81;

    /**
     * Tunnel-Assignment-ID [RFC2868]
     */
    public static final byte TUNNEL_ASSIGNMENT_ID = 82;

    /**
     * Tunnel-Preference [RFC2868]
     */
    public static final byte TUNNEL_PREFERENCE = 83;

    /**
     * ARAP-Challenge-Response [RFC2869]
     */
    public static final byte ARAP_CHALLENGE_RESPONSE = 84;

    /**
     * Acct-Interim-Interval [RFC2869]
     */
    public static final byte ACCT_INTERIM_INTERVAL = 85;

    /**
     * Acct-Tunnel-Packets-Lost [RFC2867]
     */
    public static final byte ACCT_TUNNEL_PACKETS_LOST = 86;

    /**
     * NAS-Port-Id [RFC2869]
     */
    public static final byte NAS_PORT_ID = 87;

    /**
     * Framed-Pool [RFC2869]
     */
    public static final byte FRAMED_POOL = 88;

    /**
     * CUI [RFC4372]
     */
    public static final byte CUI = 89;

    /**
     * Tunnel-Client-Auth-ID [RFC2868]
     */
    public static final byte TUNNEL_CLIENT_AUTH_ID = 90;

    /**
     * Tunnel-Server-Auth-ID [RFC2868]
     */
    public static final byte TUNNEL_SERVER_AUTH_ID = 91;

    /**
     * NAS-Filter-Rule [RFC4849]
     */
    public static final byte NAS_FILTER_RULE = 92;

    /**
     * Originating-Line-Info [RFC7155]
     */
    public static final byte ORIGINATING_LINE_INFO = 94;

    /**
     * NAS-IPv6-Address [RFC3162]
     */
    public static final byte NAS_IPV6_ADDRESS = 95;

    /**
     * Framed-Interface-Id [RFC3162]
     */
    public static final byte FRAMED_INTERFACE_ID = 96;

    /**
     * Framed-IPv6-Prefix [RFC3162]
     */
    public static final byte FRAMED_IPV6_PREFIX = 97;

    /**
     * Login-IPv6-Host [RFC3162]
     */
    public static final byte LOGIN_IPV6_HOST = 98;

    /**
     * Framed-IPv6-Route [RFC3162]
     */
    public static final byte FRAMED_IPV6_ROUTE = 99;

    /**
     * Framed-IPv6-Pool [RFC3162]
     */
    public static final byte FRAMED_IPV6_POOL = 100;

    /**
     * Error-Cause Attribute [RFC3576]
     */
    public static final byte ERROR_CAUSE_ATTRIBUTE = 101;

    /**
     * EAP-Key-Name [RFC4072][RFC7268]
     */
    public static final byte EAP_KEY_NAME = 102;

    /**
     * Digest-Response [RFC5090]
     */
    public static final byte DIGEST_RESPONSE = 103;

    /**
     * Digest-Realm [RFC5090]
     */
    public static final byte DIGEST_REALM = 104;

    /**
     * Digest-Nonce [RFC5090]
     */
    public static final byte DIGEST_NONCE = 105;

    /**
     * Digest-Response-Auth [RFC5090]
     */
    public static final byte DIGEST_RESPONSE_AUTH = 106;

    /**
     * Digest-Nextnonce [RFC5090]
     */
    public static final byte DIGEST_NEXTNONCE = 107;

    /**
     * Digest-Method [RFC5090]
     */
    public static final byte DIGEST_METHOD = 108;

    /**
     * Digest-URI [RFC5090]
     */
    public static final byte DIGEST_URI = 109;

    /**
     * Digest-Qop [RFC5090]
     */
    public static final byte DIGEST_QOP = 110;

    /**
     * Digest-Algorithm [RFC5090]
     */
    public static final byte DIGEST_ALGORITHM = 111;

    /**
     * Digest-Entity-Body-Hash [RFC5090]
     */
    public static final byte DIGEST_ENTITY_BODY_HASH = 112;

    /**
     * Digest-CNonce [RFC5090]
     */
    public static final byte DIGEST_CNONCE = 113;

    /**
     * Digest-Nonce-Count [RFC5090]
     */
    public static final byte DIGEST_NONCE_COUNT = 114;

    /**
     * Digest-Username [RFC5090]
     */
    public static final byte DIGEST_USERNAME = 115;

    /**
     * Digest-Opaque [RFC5090]
     */
    public static final byte DIGEST_OPAQUE = 116;

    /**
     * Digest-Auth-Param [RFC5090]
     */
    public static final byte DIGEST_AUTH_PARAM = 117;

    /**
     * Digest-AKA-Auts [RFC5090]
     */
    public static final byte DIGEST_AKA_AUTS = 118;

    /**
     * Digest-Domain [RFC5090]
     */
    public static final byte DIGEST_DOMAIN = 119;

    /**
     * Digest-Stale [RFC5090]
     */
    public static final byte DIGEST_STALE = 120;

    /**
     * Digest-HA1 [RFC5090]
     */
    public static final byte DIGEST_HA_1 = 121;

    /**
     * SIP-AOR [RFC5090]
     */
    public static final byte SIP_AOR = 122;

    /**
     * Delegated-IPv6-Prefix [RFC4818]
     */
    public static final byte DELEGATED_IPV6_PREFIX = 123;

    /**
     * MIP6-Feature-Vector [RFC5447]
     */
    public static final byte MIP6_FEATURE_VECTOR = 124;

    /**
     * MIP6-Home-Link-Prefix [RFC5447]
     */
    public static final byte MIP6_HOME_LINK_PREFIX = 125;

    /**
     * Operator-Name [RFC5580]
     */
    public static final byte OPERATOR_NAME = 126;

    /**
     * Location-Information [RFC5580]
     */
    public static final byte LOCATION_INFORMATION = 127;

    /**
     * Location-Data [RFC5580]
     */
    public static final byte LOCATION_DATA = (byte) 128;

    /**
     * Basic-Location-Policy-Rules [RFC5580]
     */
    public static final byte BASIC_LOCATION_POLICY_RULES = (byte) 129;

    /**
     * Extended-Location-Policy-Rules [RFC5580]
     */
    public static final byte EXTENDED_LOCATION_POLICY_RULES = (byte) 130;

    /**
     * Location-Capable [RFC5580]
     */
    public static final byte LOCATION_CAPABLE = (byte) 131;

    /**
     * Requested-Location-Info [RFC5580]
     */
    public static final byte REQUESTED_LOCATION_INFO = (byte) 132;

    /**
     * Framed-Management-Protocol [RFC5607]
     */
    public static final byte FRAMED_MANAGEMENT_PROTOCOL = (byte) 133;

    /**
     * Management-Transport-Protection [RFC5607]
     */
    public static final byte MANAGEMENT_TRANSPORT_PROTECTION = (byte) 134;

    /**
     * Management-Policy-Id [RFC5607]
     */
    public static final byte MANAGEMENT_POLICY_ID = (byte) 135;

    /**
     * Management-Privilege-Level [RFC5607]
     */
    public static final byte MANAGEMENT_PRIVILEGE_LEVEL = (byte) 136;

    /**
     * PKM-SS-Cert [RFC5904]
     */
    public static final byte PKM_SS_CERT = (byte) 137;

    /**
     * PKM-CA-Cert [RFC5904]
     */
    public static final byte PKM_CA_CERT = (byte) 138;

    /**
     * PKM-Config-Settings [RFC5904]
     */
    public static final byte PKM_CONFIG_SETTINGS = (byte) 139;

    /**
     * PKM-Cryptosuite-List [RFC5904]
     */
    public static final byte PKM_CRYPTOSUITE_LIST = (byte) 140;

    /**
     * PKM-SAID [RFC5904]
     */
    public static final byte PKM_SAID = (byte) 141;

    /**
     * PKM-SA-Descriptor [RFC5904]
     */
    public static final byte PKM_SA_DESCRIPTOR = (byte) 142;

    /**
     * PKM-Auth-Key [RFC5904]
     */
    public static final byte PKM_AUTH_KEY = (byte) 143;

    /**
     * DS-Lite-Tunnel-Name [RFC6519]
     */
    public static final byte DS_LITE_TUNNEL_NAME = (byte) 144;

    /**
     * Mobile-Node-Identifier [RFC6572]
     */
    public static final byte MOBILE_NODE_IDENTIFIER = (byte) 145;

    /**
     * Service-Selection [RFC6572]
     */
    public static final byte SERVICE_SELECTION = (byte) 146;

    /**
     * PMIP6-Home-LMA-IPv6-Address [RFC6572]
     */
    public static final byte PMIP6_HOME_LMA_IPV6_ADDRESS = (byte) 147;

    /**
     * PMIP6-Visited-LMA-IPv6-Address [RFC6572]
     */
    public static final byte PMIP6_VISITED_LMA_IPV6_ADDRESS = (byte) 148;

    /**
     * PMIP6-Home-LMA-IPv4-Address [RFC6572]
     */
    public static final byte PMIP6_HOME_LMA_IPV4_ADDRESS = (byte) 149;

    /**
     * PMIP6-Visited-LMA-IPv4-Address [RFC6572]
     */
    public static final byte PMIP6_VISITED_LMA_IPV4_ADDRESS = (byte) 150;

    /**
     * PMIP6-Home-HN-Prefix [RFC6572]
     */
    public static final byte PMIP6_HOME_HN_PREFIX = (byte) 151;

    /**
     * PMIP6-Visited-HN-Prefix [RFC6572]
     */
    public static final byte PMIP6_VISITED_HN_PREFIX = (byte) 152;

    /**
     * PMIP6-Home-Interface-ID [RFC6572]
     */
    public static final byte PMIP6_HOME_INTERFACE_ID = (byte) 153;

    /**
     * PMIP6-Visited-Interface-ID [RFC6572]
     */
    public static final byte PMIP6_VISITED_INTERFACE_ID = (byte) 154;

    /**
     * PMIP6-Home-IPv4-HoA [RFC6572]
     */
    public static final byte PMIP6_HOME_IPV4_HOA = (byte) 155;

    /**
     * PMIP6-Visited-IPv4-HoA [RFC6572]
     */
    public static final byte PMIP6_VISITED_IPV4_HOA = (byte) 156;

    /**
     * PMIP6-Home-DHCP4-Server-Address [RFC6572]
     */
    public static final byte PMIP6_HOME_DHCP4_SERVER_ADDRESS = (byte) 157;

    /**
     * PMIP6-Visited-DHCP4-Server-Address [RFC6572]
     */
    public static final byte PMIP6_VISITED_DHCP4_SERVER_ADDRESS = (byte) 158;

    /**
     * PMIP6-Home-DHCP6-Server-Address [RFC6572]
     */
    public static final byte PMIP6_HOME_DHCP6_SERVER_ADDRESS = (byte) 159;

    /**
     * PMIP6-Visited-DHCP6-Server-Address [RFC6572]
     */
    public static final byte PMIP6_VISITED_DHCP6_SERVER_ADDRESS = (byte) 160;

    /**
     * PMIP6-Home-IPv4-Gateway [RFC6572]
     */
    public static final byte PMIP6_HOME_IPV4_GATEWAY = (byte) 161;

    /**
     * PMIP6-Visited-IPv4-Gateway [RFC6572]
     */
    public static final byte PMIP6_VISITED_IPV4_GATEWAY = (byte) 162;

    /**
     * EAP-Lower-Layer [RFC6677]
     */
    public static final byte EAP_LOWER_LAYER = (byte) 163;

    /**
     * GSS-Acceptor-Service-Name [RFC7055]
     */
    public static final byte GSS_ACCEPTOR_SERVICE_NAME = (byte) 164;

    /**
     * GSS-Acceptor-Host-Name [RFC7055]
     */
    public static final byte GSS_ACCEPTOR_HOST_NAME = (byte) 165;

    /**
     * GSS-Acceptor-Service-Specifics [RFC7055]
     */
    public static final byte GSS_ACCEPTOR_SERVICE_SPECIFICS = (byte) 166;

    /**
     * GSS-Acceptor-Realm-Name [RFC7055]
     */
    public static final byte GSS_ACCEPTOR_REALM_NAME = (byte) 167;

    /**
     * Framed-IPv6-Address [RFC6911]
     */
    public static final byte FRAMED_IPV6_ADDRESS = (byte) 168;

    /**
     * DNS-Server-IPv6-Address [RFC6911]
     */
    public static final byte DNS_SERVER_IPV6_ADDRESS = (byte) 169;

    /**
     * Route-IPv6-Information [RFC6911]
     */
    public static final byte ROUTE_IPV6_INFORMATION = (byte) 170;

    /**
     * Delegated-IPv6-Prefix-Pool [RFC6911]
     */
    public static final byte DELEGATED_IPV6_PREFIX_POOL = (byte) 171;

    /**
     * Stateful-IPv6-Address-Pool [RFC6911]
     */
    public static final byte STATEFUL_IPV6_ADDRESS_POOL = (byte) 172;

    /**
     * IPv6-6rd-Configuration [RFC6930]
     */
    public static final byte IPV6_6RD_CONFIGURATION = (byte) 173;

    /**
     * Allowed-Called-Station-Id [RFC7268]
     */
    public static final byte ALLOWED_CALLED_STATION_ID = (byte) 174;

    /**
     * EAP-Peer-Id [RFC7268]
     */
    public static final byte EAP_PEER_ID = (byte) 175;

    /**
     * EAP-Server-Id [RFC7268]
     */
    public static final byte EAP_SERVER_ID = (byte) 176;

    /**
     * Mobility-Domain-Id [RFC7268]
     */
    public static final byte MOBILITY_DOMAIN_ID = (byte) 177;

    /**
     * Preauth-Timeout [RFC7268]
     */
    public static final byte PREAUTH_TIMEOUT = (byte) 178;

    /**
     * Network-Id-Name [RFC7268]
     */
    public static final byte NETWORK_ID_NAME = (byte) 179;

    /**
     * EAPoL-Announcement [RFC7268]
     */
    public static final byte EAPOL_ANNOUNCEMENT = (byte) 180;

    /**
     * WLAN-HESSID [RFC7268]
     */
    public static final byte WLAN_HESSID = (byte) 181;

    /**
     * WLAN-Venue-Info [RFC7268]
     */
    public static final byte WLAN_VENUE_INFO = (byte) 182;

    /**
     * WLAN-Venue-Language [RFC7268]
     */
    public static final byte WLAN_VENUE_LANGUAGE = (byte) 183;

    /**
     * WLAN-Venue-Name [RFC7268]
     */
    public static final byte WLAN_VENUE_NAME = (byte) 184;

    /**
     * WLAN-Reason-Code [RFC7268]
     */
    public static final byte WLAN_REASON_CODE = (byte) 185;

    /**
     * WLAN-Pairwise-Cipher [RFC7268]
     */
    public static final byte WLAN_PAIRWISE_CIPHER = (byte) 186;

    /**
     * WLAN-Group-Cipher [RFC7268]
     */
    public static final byte WLAN_GROUP_CIPHER = (byte) 187;

    /**
     * WLAN-AKM-Suite [RFC7268]
     */
    public static final byte WLAN_AKM_SUITE = (byte) 188;

    /**
     * WLAN-Group-Mgmt-Cipher [RFC7268]
     */
    public static final byte WLAN_GROUP_MGMT_CIPHER = (byte) 189;

    /**
     * WLAN-RF-Band [RFC7268]
     */
    public static final byte WLAN_RF_BAND = (byte) 190;

    /**
     * Extended-Attribute-1 [RFC6929]
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
