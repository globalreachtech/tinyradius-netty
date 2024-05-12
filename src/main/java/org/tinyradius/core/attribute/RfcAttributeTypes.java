package org.tinyradius.core.attribute;

import lombok.experimental.UtilityClass;

/**
 * Based on official IANA assignments for <a href="https://www.iana.org/assignments/radius-types/radius-types.xhtml#radius-types-2">Radius Attribute Types</a>
 */
@UtilityClass
public class RfcAttributeTypes {

    public static final byte USER_NAME = 1;
    public static final byte USER_PASSWORD = 2;
    public static final byte CHAP_PASSWORD = 3;
    public static final byte NAS_IP_ADDRESS = 4;
    public static final byte NAS_PORT = 5;
    public static final byte SERVICE_TYPE = 6;
    public static final byte FRAMED_PROTOCOL = 7;
    public static final byte FRAMED_IP_ADDRESS = 8;
    public static final byte FRAMED_IP_NETMASK = 9;
    public static final byte FRAMED_ROUTING = 10;
    public static final byte FILTER_ID = 11;
    public static final byte FRAMED_MTU = 12;
    public static final byte FRAMED_COMPRESSION = 13;
    public static final byte LOGIN_IP_HOST = 14;
    public static final byte LOGIN_SERVICE = 15;
    public static final byte LOGIN_TCP_PORT = 16;
    public static final byte REPLY_MESSAGE = 18;
    public static final byte CALLBACK_NUMBER = 19;
    public static final byte CALLBACK_ID = 20;
    public static final byte FRAMED_ROUTE = 22;
    public static final byte FRAMED_IPX_NETWORK = 23;
    public static final byte STATE = 24;
    public static final byte CLASS = 25;
    public static final byte VENDOR_SPECIFIC = 26;
    public static final byte SESSION_TIMEOUT = 27;
    public static final byte IDLE_TIMEOUT = 28;
    public static final byte TERMINATION_ACTION = 29;
    public static final byte CALLED_STATION_ID = 30;
    public static final byte CALLING_STATION_ID = 31;
    public static final byte NAS_IDENTIFIER = 32;
    public static final byte PROXY_STATE = 33;
    public static final byte LOGIN_LAT_SERVICE = 34;
    public static final byte LOGIN_LAT_NODE = 35;
    public static final byte LOGIN_LAT_GROUP = 36;
    public static final byte FRAMED_APPLE_TALK_LINK = 37;
    public static final byte FRAMED_APPLE_TALK_NETWORK = 38;
    public static final byte FRAMED_APPLE_TALK_ZONE = 39;
    public static final byte ACCT_STATUS_TYPE = 40;
    public static final byte ACCT_DELAY_TIME = 41;
    public static final byte ACCT_INPUT_OCTETS = 42;
    public static final byte ACCT_OUTPUT_OCTETS = 43;
    public static final byte ACCT_SESSION_ID = 44;
    public static final byte ACCT_AUTHENTIC = 45;
    public static final byte ACCT_SESSION_TIME = 46;
    public static final byte ACCT_INPUT_PACKETS = 47;
    public static final byte ACCT_OUTPUT_PACKETS = 48;
    public static final byte ACCT_TERMINATE_CAUSE = 49;
    public static final byte ACCT_MULTI_SESSION_ID = 50;
    public static final byte ACCT_LINK_COUNT = 51;
    public static final byte ACCT_INPUT_GIGAWORDS = 52;
    public static final byte ACCT_OUTPUT_GIGAWORDS = 53;
    public static final byte EVENT_TIMESTAMP = 55;
    public static final byte EGRESS_VLANID = 56;
    public static final byte INGRESS_FILTERS = 57;
    public static final byte EGRESS_VLAN_NAME = 58;
    public static final byte USER_PRIORITY_TABLE = 59;
    public static final byte CHAP_CHALLENGE = 60;
    public static final byte NAS_PORT_TYPE = 61;
    public static final byte PORT_LIMIT = 62;
    public static final byte LOGIN_LAT_PORT = 63;
    public static final byte TUNNEL_TYPE = 64;
    public static final byte TUNNEL_MEDIUM_TYPE = 65;
    public static final byte TUNNEL_CLIENT_ENDPOINT = 66;
    public static final byte TUNNEL_SERVER_ENDPOINT = 67;
    public static final byte ACCT_TUNNEL_CONNECTION = 68;
    public static final byte TUNNEL_PASSWORD = 69;
    public static final byte ARAP_PASSWORD = 70;
    public static final byte ARAP_FEATURES = 71;
    public static final byte ARAP_ZONE_ACCESS = 72;
    public static final byte ARAP_SECURITY = 73;
    public static final byte ARAP_SECURITY_DATA = 74;
    public static final byte PASSWORD_RETRY = 75;
    public static final byte PROMPT = 76;
    public static final byte CONNECT_INFO = 77;
    public static final byte CONFIGURATION_TOKEN = 78;
    public static final byte EAP_MESSAGE = 79;
    public static final byte MESSAGE_AUTHENTICATOR = 80;
    public static final byte TUNNEL_PRIVATE_GROUP_ID = 81;
    public static final byte TUNNEL_ASSIGNMENT_ID = 82;
    public static final byte TUNNEL_PREFERENCE = 83;
    public static final byte ARAP_CHALLENGE_RESPONSE = 84;
    public static final byte ACCT_INTERIM_INTERVAL = 85;
    public static final byte ACCT_TUNNEL_PACKETS_LOST = 86;
    public static final byte NAS_PORT_ID = 87;
    public static final byte FRAMED_POOL = 88;
    public static final byte CUI = 89;
    public static final byte TUNNEL_CLIENT_AUTH_ID = 90;
    public static final byte TUNNEL_SERVER_AUTH_ID = 91;
    public static final byte NAS_FILTER_RULE = 92;
    public static final byte ORIGINATING_LINE_INFO = 94;
    public static final byte NAS_IPV6_ADDRESS = 95;
    public static final byte FRAMED_INTERFACE_ID = 96;
    public static final byte FRAMED_IPV6_PREFIX = 97;
    public static final byte LOGIN_IPV6_HOST = 98;
    public static final byte FRAMED_IPV6_ROUTE = 99;
    public static final byte FRAMED_IPV6_POOL = 100;
    public static final byte ERROR_CAUSE_ATTRIBUTE = 101;
    public static final byte EAP_KEY_NAME = 102;
    public static final byte DIGEST_RESPONSE = 103;
    public static final byte DIGEST_REALM = 104;
    public static final byte DIGEST_NONCE = 105;
    public static final byte DIGEST_RESPONSE_AUTH = 106;
    public static final byte DIGEST_NEXTNONCE = 107;
    public static final byte DIGEST_METHOD = 108;
    public static final byte DIGEST_URI = 109;
    public static final byte DIGEST_QOP = 110;
    public static final byte DIGEST_ALGORITHM = 111;
    public static final byte DIGEST_ENTITY_BODY_HASH = 112;
    public static final byte DIGEST_CNONCE = 113;
    public static final byte DIGEST_NONCE_COUNT = 114;
    public static final byte DIGEST_USERNAME = 115;
    public static final byte DIGEST_OPAQUE = 116;
    public static final byte DIGEST_AUTH_PARAM = 117;
    public static final byte DIGEST_AKA_AUTS = 118;
    public static final byte DIGEST_DOMAIN = 119;
    public static final byte DIGEST_STALE = 120;
    public static final byte DIGEST_HA_1 = 121;
    public static final byte SIP_AOR = 122;
    public static final byte DELEGATED_IPV6_PREFIX = 123;
    public static final byte MIP6_FEATURE_VECTOR = 124;
    public static final byte MIP6_HOME_LINK_PREFIX = 125;
    public static final byte OPERATOR_NAME = 126;
    public static final byte LOCATION_INFORMATION = 127;
    public static final byte LOCATION_DATA = (byte) 128;
    public static final byte BASIC_LOCATION_POLICY_RULES = (byte) 129;
    public static final byte EXTENDED_LOCATION_POLICY_RULES = (byte) 130;
    public static final byte LOCATION_CAPABLE = (byte) 131;
    public static final byte REQUESTED_LOCATION_INFO = (byte) 132;
    public static final byte FRAMED_MANAGEMENT_PROTOCOL = (byte) 133;
    public static final byte MANAGEMENT_TRANSPORT_PROTECTION = (byte) 134;
    public static final byte MANAGEMENT_POLICY_ID = (byte) 135;
    public static final byte MANAGEMENT_PRIVILEGE_LEVEL = (byte) 136;
    public static final byte PKM_SS_CERT = (byte) 137;
    public static final byte PKM_CA_CERT = (byte) 138;
    public static final byte PKM_CONFIG_SETTINGS = (byte) 139;
    public static final byte PKM_CRYPTOSUITE_LIST = (byte) 140;
    public static final byte PKM_SAID = (byte) 141;
    public static final byte PKM_SA_DESCRIPTOR = (byte) 142;
    public static final byte PKM_AUTH_KEY = (byte) 143;
    public static final byte DS_LITE_TUNNEL_NAME = (byte) 144;
    public static final byte MOBILE_NODE_IDENTIFIER = (byte) 145;
    public static final byte SERVICE_SELECTION = (byte) 146;
    public static final byte PMIP6_HOME_LMA_IPV6_ADDRESS = (byte) 147;
    public static final byte PMIP6_VISITED_LMA_IPV6_ADDRESS = (byte) 148;
    public static final byte PMIP6_HOME_LMA_IPV4_ADDRESS = (byte) 149;
    public static final byte PMIP6_VISITED_LMA_IPV4_ADDRESS = (byte) 150;
    public static final byte PMIP6_HOME_HN_PREFIX = (byte) 151;
    public static final byte PMIP6_VISITED_HN_PREFIX = (byte) 152;
    public static final byte PMIP6_HOME_INTERFACE_ID = (byte) 153;
    public static final byte PMIP6_VISITED_INTERFACE_ID = (byte) 154;
    public static final byte PMIP6_HOME_IPV4_HOA = (byte) 155;
    public static final byte PMIP6_VISITED_IPV4_HOA = (byte) 156;
    public static final byte PMIP6_HOME_DHCP4_SERVER_ADDRESS = (byte) 157;
    public static final byte PMIP6_VISITED_DHCP4_SERVER_ADDRESS = (byte) 158;
    public static final byte PMIP6_HOME_DHCP6_SERVER_ADDRESS = (byte) 159;
    public static final byte PMIP6_VISITED_DHCP6_SERVER_ADDRESS = (byte) 160;
    public static final byte PMIP6_HOME_IPV4_GATEWAY = (byte) 161;
    public static final byte PMIP6_VISITED_IPV4_GATEWAY = (byte) 162;
    public static final byte EAP_LOWER_LAYER = (byte) 163;
    public static final byte GSS_ACCEPTOR_SERVICE_NAME = (byte) 164;
    public static final byte GSS_ACCEPTOR_HOST_NAME = (byte) 165;
    public static final byte GSS_ACCEPTOR_SERVICE_SPECIFICS = (byte) 166;
    public static final byte GSS_ACCEPTOR_REALM_NAME = (byte) 167;
    public static final byte FRAMED_IPV6_ADDRESS = (byte) 168;
    public static final byte DNS_SERVER_IPV6_ADDRESS = (byte) 169;
    public static final byte ROUTE_IPV6_INFORMATION = (byte) 170;
    public static final byte DELEGATED_IPV6_PREFIX_POOL = (byte) 171;
    public static final byte STATEFUL_IPV6_ADDRESS_POOL = (byte) 172;
    public static final byte IPV6_6RD_CONFIGURATION = (byte) 173;
    public static final byte ALLOWED_CALLED_STATION_ID = (byte) 174;
    public static final byte EAP_PEER_ID = (byte) 175;
    public static final byte EAP_SERVER_ID = (byte) 176;
    public static final byte MOBILITY_DOMAIN_ID = (byte) 177;
    public static final byte PREAUTH_TIMEOUT = (byte) 178;
    public static final byte NETWORK_ID_NAME = (byte) 179;
    public static final byte EAPOL_ANNOUNCEMENT = (byte) 180;
    public static final byte WLAN_HESSID = (byte) 181;
    public static final byte WLAN_VENUE_INFO = (byte) 182;
    public static final byte WLAN_VENUE_LANGUAGE = (byte) 183;
    public static final byte WLAN_VENUE_NAME = (byte) 184;
    public static final byte WLAN_REASON_CODE = (byte) 185;
    public static final byte WLAN_PAIRWISE_CIPHER = (byte) 186;
    public static final byte WLAN_GROUP_CIPHER = (byte) 187;
    public static final byte WLAN_AKM_SUITE = (byte) 188;
    public static final byte WLAN_GROUP_MGMT_CIPHER = (byte) 189;
    public static final byte WLAN_RF_BAND = (byte) 190;
    public static final byte EXTENDED_ATTRIBUTE_1 = (byte) 241;
    public static final byte EXTENDED_ATTRIBUTE_2 = (byte) 242;
    public static final byte EXTENDED_ATTRIBUTE_3 = (byte) 243;
    public static final byte EXTENDED_ATTRIBUTE_4 = (byte) 244;
    public static final byte EXTENDED_ATTRIBUTE_5 = (byte) 245;
    public static final byte EXTENDED_ATTRIBUTE_6 = (byte) 246;

}
