package com.globalreachtech.tinyradius.packet;

import com.globalreachtech.tinyradius.attribute.RadiusAttribute;
import com.globalreachtech.tinyradius.attribute.VendorSpecificAttribute;
import com.globalreachtech.tinyradius.dictionary.AttributeType;
import com.globalreachtech.tinyradius.dictionary.DefaultDictionary;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.util.RadiusException;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * This class represents a Radius packet. Subclasses provide convenience methods
 * for special packet types.
 */
public class RadiusPacket implements Cloneable {

    /**
     * Packet type codes.
     */
    public static final int ACCESS_REQUEST = 1;
    public static final int ACCESS_ACCEPT = 2;
    public static final int ACCESS_REJECT = 3;
    public static final int ACCOUNTING_REQUEST = 4;
    public static final int ACCOUNTING_RESPONSE = 5;
    public static final int ACCOUNTING_STATUS = 6;
    public static final int PASSWORD_REQUEST = 7;
    public static final int PASSWORD_ACCEPT = 8;
    public static final int PASSWORD_REJECT = 9;
    public static final int ACCOUNTING_MESSAGE = 10;
    public static final int ACCESS_CHALLENGE = 11;
    public static final int STATUS_SERVER = 12;
    public static final int STATUS_CLIENT = 13;
    public static final int DISCONNECT_REQUEST = 40;    // RFC 2882
    public static final int DISCONNECT_ACK = 41;
    public static final int DISCONNECT_NAK = 42;
    public static final int COA_REQUEST = 43;
    public static final int COA_ACK = 44;
    public static final int COA_NAK = 45;
    public static final int STATUS_REQUEST = 46;
    public static final int STATUS_ACCEPT = 47;
    public static final int STATUS_REJECT = 48;
    public static final int RESERVED = 255;

    /**
     * Maximum packet length.
     */
    public static final int MAX_PACKET_LENGTH = 4096;

    /**
     * Packet header length.
     */
    public static final int RADIUS_HEADER_LENGTH = 20;

    /**
     * Builds a Radius packet without attributes. Retrieves
     * the next packet identifier.
     *
     * @param type packet type
     */
    public RadiusPacket(final int type) {
        this(type, getNextPacketIdentifier());
    }

    /**
     * Builds a Radius packet with the given type and identifier
     * and without attributes.
     *
     * @param type       packet type
     * @param identifier packet identifier
     */
    public RadiusPacket(final int type, final int identifier) {
        this(type, identifier, new ArrayList<>());
    }

    /**
     * Builds a Radius packet with the given type, identifier and
     * attributes.
     *
     * @param type       packet type
     * @param identifier packet identifier
     * @param attributes list of RadiusAttribute objects
     */
    public RadiusPacket(final int type, final int identifier, final List<RadiusAttribute> attributes) {
        setPacketType(type);
        setPacketIdentifier(identifier);
        setAttributes(attributes);
    }

    /**
     * Builds an empty Radius packet.
     */
    public RadiusPacket() {
    }

    /**
     * Returns the packet identifier for this Radius packet.
     *
     * @return packet identifier
     */
    public int getPacketIdentifier() {
        return packetIdentifier;
    }

    /**
     * Sets the packet identifier for this Radius packet.
     *
     * @param identifier packet identifier, 0-255
     */
    public void setPacketIdentifier(int identifier) {
        if (identifier < 0 || identifier > 255)
            throw new IllegalArgumentException("packet identifier out of bounds");
        this.packetIdentifier = identifier;
    }

    /**
     * Returns the type of this Radius packet.
     *
     * @return packet type
     */
    public int getPacketType() {
        return packetType;
    }

    /**
     * Returns the type name of this Radius packet.
     *
     * @return name
     */
    public String getPacketTypeName() {
        switch (getPacketType()) {
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
            // RFC 2882
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
                return "Unknown (" + getPacketType() + ")";
        }
    }

    /**
     * Sets the type of this Radius packet.
     *
     * @param type packet type, 0-255
     */
    public void setPacketType(int type) {
        if (type < 1 || type > 255)
            throw new IllegalArgumentException("packet type out of bounds");
        this.packetType = type;
    }

    /**
     * Sets the list of attributes for this Radius packet.
     *
     * @param attributes list of RadiusAttribute objects
     */
    public void setAttributes(List<RadiusAttribute> attributes) {
        this.attributes = requireNonNull(attributes, "attributes list is null");
    }

    /**
     * Adds a Radius attribute to this packet. Can also be used
     * to add Vendor-Specific sub-attributes. If a attribute with
     * a vendor code != -1 is passed in, a VendorSpecificAttribute
     * is created for the sub-attribute.
     *
     * @param attribute RadiusAttribute object
     */
    public void addAttribute(RadiusAttribute attribute) {
        requireNonNull(attributes, "attribute is null");

        attribute.setDictionary(getDictionary());
        if (attribute.getVendorId() == -1)
            this.attributes.add(attribute);
        else {
            VendorSpecificAttribute vsa = new VendorSpecificAttribute(attribute.getVendorId());
            vsa.addSubAttribute(attribute);
            this.attributes.add(vsa);
        }
    }

    /**
     * Adds a Radius attribute to this packet.
     * Uses AttributeTypes to lookup the type code and converts
     * the value.
     * Can also be used to add sub-attributes.
     *
     * @param typeName name of the attribute, for example "NAS-Ip-Address"
     * @param value    value of the attribute, for example "127.0.0.1"
     * @throws IllegalArgumentException if type name is unknown
     */
    public void addAttribute(String typeName, String value) {
        if (typeName == null || typeName.isEmpty())
            throw new IllegalArgumentException("type name is empty");
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("value is empty");

        AttributeType type = dictionary.getAttributeTypeByName(typeName);
        if (type == null)
            throw new IllegalArgumentException("unknown attribute type '" + typeName + "'");

        RadiusAttribute attribute = RadiusAttribute.createRadiusAttribute(getDictionary(), type.getVendorId(), type.getTypeCode());
        attribute.setAttributeValue(value);
        addAttribute(attribute);
    }

    /**
     * Removes the specified attribute from this packet.
     *
     * @param attribute RadiusAttribute to remove
     */
    public void removeAttribute(RadiusAttribute attribute) {
        if (attribute.getVendorId() == -1) {
            if (!this.attributes.remove(attribute))
                throw new IllegalArgumentException("no such attribute");
        } else {
            // remove Vendor-Specific sub-attribute
            List<RadiusAttribute> vsas = getVendorAttributes(attribute.getVendorId());
            for (RadiusAttribute o : vsas) {
                VendorSpecificAttribute vsa = (VendorSpecificAttribute) o;
                List<RadiusAttribute> sas = vsa.getSubAttributes();
                if (sas.contains(attribute)) {
                    vsa.removeSubAttribute(attribute);
                    if (sas.size() == 1)
                        // removed the last sub-attribute
                        // --> remove the whole Vendor-Specific attribute
                        removeAttribute(vsa);
                }
            }
        }
    }

    /**
     * Removes all attributes from this packet which have got
     * the specified type.
     *
     * @param type attribute type to remove
     */
    public void removeAttributes(int type) {
        if (type < 1 || type > 255)
            throw new IllegalArgumentException("attribute type out of bounds");

        Iterator i = attributes.iterator();
        while (i.hasNext()) {
            RadiusAttribute attribute = (RadiusAttribute) i.next();
            if (attribute.getAttributeType() == type)
                i.remove();
        }
    }

    /**
     * Removes the last occurence of the attribute of the given
     * type from the packet.
     *
     * @param type attribute type code
     */
    public void removeLastAttribute(int type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        if (attrs == null || attrs.size() == 0)
            return;

        RadiusAttribute lastAttribute = attrs.get(attrs.size() - 1);
        removeAttribute(lastAttribute);
    }

    /**
     * Removes all sub-attributes of the given vendor and
     * type.
     *
     * @param vendorId vendor ID
     * @param typeCode attribute type code
     */
    public void removeAttributes(int vendorId, int typeCode) {
        if (vendorId == -1) {
            removeAttributes(typeCode);
            return;
        }

        List<RadiusAttribute> vsas = getVendorAttributes(vendorId);
        for (RadiusAttribute o : vsas) {
            VendorSpecificAttribute vsa = (VendorSpecificAttribute) o;

            List<RadiusAttribute> sas = vsa.getSubAttributes();
            for (Iterator j = sas.iterator(); j.hasNext(); ) {
                RadiusAttribute attr = (RadiusAttribute) j.next();
                if (attr.getAttributeType() == typeCode &&
                        attr.getVendorId() == vendorId)
                    j.remove();
            }
            if (sas.size() == 0)
                // removed the last sub-attribute
                // --> remove the whole Vendor-Specific attribute
                removeAttribute(vsa);
        }
    }

    /**
     * Returns all attributes of this packet of the given type.
     * Returns an empty list if there are no such attributes.
     *
     * @param attributeType type of attributes to get
     * @return list of RadiusAttribute objects, does not return null
     */
    public List<RadiusAttribute> getAttributes(int attributeType) {
        if (attributeType < 1 || attributeType > 255)
            throw new IllegalArgumentException("attribute type out of bounds");

        LinkedList<RadiusAttribute> result = new LinkedList<>();
        for (RadiusAttribute attribute : attributes) {
            if (attributeType == attribute.getAttributeType())
                result.add(attribute);
        }
        return result;
    }

    /**
     * Returns all attributes of this packet that have got the
     * given type and belong to the given vendor ID.
     * Returns an empty list if there are no such attributes.
     *
     * @param vendorId      vendor ID
     * @param attributeType attribute type code
     * @return list of RadiusAttribute objects, never null
     */
    public List<RadiusAttribute> getAttributes(int vendorId, int attributeType) {
        if (vendorId == -1)
            return getAttributes(attributeType);

        LinkedList<RadiusAttribute> result = new LinkedList<>();
        List<RadiusAttribute> vsas = getVendorAttributes(vendorId);
        for (RadiusAttribute o : vsas) {
            VendorSpecificAttribute vsa = (VendorSpecificAttribute) o;
            List<RadiusAttribute> sas = vsa.getSubAttributes();
            for (RadiusAttribute sa : sas) {
                if (sa.getAttributeType() == attributeType &&
                        sa.getVendorId() == vendorId)
                    result.add(sa);
            }
        }

        return result;
    }

    /**
     * Returns a list of all attributes belonging to this Radius
     * packet.
     *
     * @return List of RadiusAttribute objects
     */
    public List getAttributes() {
        return attributes;
    }

    /**
     * Returns a Radius attribute of the given type which may only occur once
     * in the Radius packet.
     *
     * @param type attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     * @throws RuntimeException if there are multiple occurences of the
     *                          requested attribute type
     */
    public RadiusAttribute getAttribute(int type) {
        List<RadiusAttribute> attrs = getAttributes(type);
        if (attrs.size() > 1)
            throw new RuntimeException("multiple attributes of requested type " + type);
        else if (attrs.size() == 0)
            return null;
        else
            return attrs.get(0);
    }

    /**
     * Returns a Radius attribute of the given type and vendor ID
     * which may only occur once in the Radius packet.
     *
     * @param vendorId vendor ID
     * @param type     attribute type
     * @return RadiusAttribute object or null if there is no such attribute
     * @throws RuntimeException if there are multiple occurences of the
     *                          requested attribute type
     */
    public RadiusAttribute getAttribute(int vendorId, int type) {
        if (vendorId == -1)
            return getAttribute(type);

        List attrs = getAttributes(vendorId, type);
        if (attrs.size() > 1)
            throw new RuntimeException("multiple attributes of requested type " + type);
        else if (attrs.size() == 0)
            return null;
        else
            return (RadiusAttribute) attrs.get(0);
    }

    /**
     * Returns a single Radius attribute of the given type name.
     * Also returns sub-attributes.
     *
     * @param type attribute type name
     * @return RadiusAttribute object or null if there is no such attribute
     * @throws RuntimeException if the attribute occurs multiple times
     */
    public RadiusAttribute getAttribute(String type) {
        if (type == null || type.isEmpty())
            throw new IllegalArgumentException("type name is empty");

        AttributeType t = dictionary.getAttributeTypeByName(type);
        if (t == null)
            throw new IllegalArgumentException("unknown attribute type name '" + type + "'");

        return getAttribute(t.getVendorId(), t.getTypeCode());
    }

    /**
     * Returns the value of the Radius attribute of the given type or
     * null if there is no such attribute.
     * Also returns sub-attributes.
     *
     * @param type attribute type name
     * @return value of the attribute as a string or null if there
     * is no such attribute
     * @throws IllegalArgumentException if the type name is unknown
     * @throws RuntimeException         attribute occurs multiple times
     */
    public String getAttributeValue(String type) {
        RadiusAttribute attr = getAttribute(type);
        if (attr == null)
            return null;
        else
            return attr.getAttributeValue();
    }

    /**
     * Returns the Vendor-Specific attribute(s) for the given vendor ID.
     *
     * @param vendorId vendor ID of the attribute(s)
     * @return List with VendorSpecificAttribute objects, never null
     */
    public List<RadiusAttribute> getVendorAttributes(int vendorId) {
        LinkedList<RadiusAttribute> result = new LinkedList<>();
        for (RadiusAttribute a : attributes) {
            if (a instanceof VendorSpecificAttribute) {
                VendorSpecificAttribute vsa = (VendorSpecificAttribute) a;
                if (vsa.getChildVendorId() == vendorId)
                    result.add(vsa);
            }
        }
        return result;
    }

    /**
     * Returns the Vendor-Specific attribute for the given vendor ID.
     * If there is more than one Vendor-Specific
     * attribute with the given vendor ID, the first attribute found is
     * returned. If there is no such attribute, null is returned.
     *
     * @param vendorId vendor ID of the attribute
     * @return the attribute or null if there is no such attribute
     * @see #getVendorAttributes(int)
     * @deprecated use getVendorAttributes(int)
     */
    public VendorSpecificAttribute getVendorAttribute(int vendorId) {
        for (RadiusAttribute o : getAttributes(VendorSpecificAttribute.VENDOR_SPECIFIC)) {
            VendorSpecificAttribute vsa = (VendorSpecificAttribute) o;
            if (vsa.getChildVendorId() == vendorId)
                return vsa;
        }
        return null;
    }

    /**
     * Encodes this Radius packet and sends it to the specified output
     * stream.
     *
     * @param out          output stream to use
     * @param sharedSecret shared secret to be used to encode this packet
     * @throws IOException communication error
     */
    public void encodeRequestPacket(OutputStream out, String sharedSecret) throws IOException, RadiusException {
        encodePacket(out, sharedSecret, null);
    }

    /**
     * Encodes this Radius clientResponse packet and sends it to the specified output
     * stream.
     *
     * @param out          output stream to use
     * @param sharedSecret shared secret to be used to encode this packet
     * @param request      Radius clientRequest packet
     * @throws IOException communication error
     */
    public void encodeResponsePacket(OutputStream out, String sharedSecret, RadiusPacket request) throws IOException, RadiusException {
        encodePacket(out, sharedSecret, requireNonNull(request, "clientRequest cannot be null"));
    }

    /**
     * Reads a Radius clientRequest packet from the given input stream and
     * creates an appropiate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param sharedSecret shared secret to be used to decode this packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeRequestPacket(InputStream in, String sharedSecret)
            throws IOException, RadiusException {
        return decodePacket(DefaultDictionary.getDefaultDictionary(), in, sharedSecret, null);
    }

    /**
     * Reads a Radius clientResponse packet from the given input stream and
     * creates an appropiate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Checks the packet authenticator.
     *
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius clientRequest packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeResponsePacket(InputStream in, String sharedSecret, RadiusPacket request) throws IOException, RadiusException {
        return decodePacket(DefaultDictionary.getDefaultDictionary(), in, sharedSecret,
                requireNonNull(request, "clientRequest may not be null"));
    }

    /**
     * Reads a Radius clientRequest packet from the given input stream and
     * creates an appropiate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param dictionary   dictionary to use for attributes
     * @param in           InputStream to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeRequestPacket(Dictionary dictionary, InputStream in, String sharedSecret)
            throws IOException, RadiusException {
        return decodePacket(dictionary, in, sharedSecret, null);
    }

    /**
     * Reads a Radius clientResponse packet from the given input stream and
     * creates an appropiate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Checks the packet authenticator.
     *
     * @param dictionary   dictionary to use for attributes
     * @param in           InputStream to read packet from
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius clientRequest packet
     * @return new RadiusPacket object
     * @throws IOException     IO error
     * @throws RadiusException malformed packet
     */
    public static RadiusPacket decodeResponsePacket(Dictionary dictionary, InputStream in, String sharedSecret, RadiusPacket request)
            throws IOException, RadiusException {
        return decodePacket(dictionary, in, sharedSecret,
                requireNonNull(request, "clientRequest may not be null"));
    }

    /**
     * Retrieves the next packet identifier to use and increments the static
     * storage.
     *
     * @return the next packet identifier to use
     */
    public static synchronized int getNextPacketIdentifier() {
        nextPacketId++;
        if (nextPacketId > 255)
            nextPacketId = 0;
        return nextPacketId;
    }

    /**
     * Creates a RadiusPacket object. Depending on the passed type, the
     * appropiate successor is chosen. Sets the type, but does not touch
     * the packet identifier.
     *
     * @param type packet type
     * @return RadiusPacket object
     */
    public static RadiusPacket createRadiusPacket(final int type, Dictionary dictionary) {
        requireNonNull(dictionary, "dictionary cannot be null");

        RadiusPacket rp;
        switch (type) {
            case ACCESS_REQUEST:
                rp = new AccessRequest();
                break;
            case COA_REQUEST:
                rp = new CoaRequest();
                break;
            case DISCONNECT_REQUEST:
                rp = new DisconnectRequest();
                break;
            case ACCOUNTING_REQUEST:
                rp = new AccountingRequest();
                break;

            case ACCESS_ACCEPT:
            case ACCESS_REJECT:
            case ACCOUNTING_RESPONSE:
            default:
                rp = new RadiusPacket();
        }

        rp.setDictionary(dictionary);
        rp.setPacketType(type);
        return rp;
    }

    /**
     * Creates a RadiusPacket object. Depending on the passed type, the
     * appropiate successor is chosen. Sets the type, but does not touch
     * the packet identifier.
     *
     * @param type packet type
     * @return RadiusPacket object
     */
    public static RadiusPacket createRadiusPacket(final int type) {
        return createRadiusPacket(type, DefaultDictionary.getDefaultDictionary());
    }

    /**
     * §§
     * String representation of this packet, for debugging purposes.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(getPacketTypeName());
        s.append(", ID ");
        s.append(packetIdentifier);
        for (RadiusAttribute attr : attributes) {
            s.append("\n");
            s.append(attr.toString());
        }
        return s.toString();
    }

    /**
     * Returns the authenticator for this Radius packet.
     * For a Radius packet read from a stream, this will return the
     * authenticator sent by the server. For a new Radius packet to be sent,
     * this will return the authenticator created by the method
     * createAuthenticator() and will return null if no authenticator
     * has been created yet.
     *
     * @return authenticator, 16 bytes
     */
    public byte[] getAuthenticator() {
        return authenticator;
    }

    /**
     * Sets the authenticator to be used for this Radius packet.
     * This method should seldomly be used.
     * Authenticators are created and managed by this class internally.
     *
     * @param authenticator authenticator
     */
    public void setAuthenticator(byte[] authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Returns the dictionary this Radius packet uses.
     *
     * @return Dictionary instance
     */
    public Dictionary getDictionary() {
        return dictionary;
    }

    /**
     * Sets a custom dictionary to use. If no dictionary is set,
     * the default dictionary is used.
     * Also copies the dictionary to the attributes.
     *
     * @param dictionary Dictionary class to use
     * @see DefaultDictionary
     */
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
        for (RadiusAttribute attr : attributes) {
            attr.setDictionary(dictionary);
        }
    }

    /**
     * Encodes this Radius packet and sends it to the specified output
     * stream.
     *
     * @param out          output stream to use
     * @param sharedSecret shared secret to be used to encode this packet
     * @param request      Radius clientRequest packet if this packet to be encoded
     *                     is a clientResponse packet, null if this packet is a clientRequest packet
     * @throws IOException      communication error
     * @throws RuntimeException if required packet data has not been set
     */
    protected void encodePacket(OutputStream out, String sharedSecret, RadiusPacket request) throws IOException, RadiusException {
        // check shared secret
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new RuntimeException("no shared secret has been set");

        // check clientRequest authenticator
        if (request != null && request.getAuthenticator() == null)
            throw new RuntimeException("clientRequest authenticator not set");

        // clientRequest packet authenticator
        if (request == null) {
            // first create authenticator if needed, then encode attributes
            // (User-Password attribute needs the authenticator)
            if (authenticator == null)
                authenticator = createRequestAuthenticator(sharedSecret);
            encodeRequestAttributes(sharedSecret);
        }

        byte[] attributes = getAttributeBytes();
        int packetLength = RADIUS_HEADER_LENGTH + attributes.length;
        if (packetLength > MAX_PACKET_LENGTH)
            throw new RuntimeException("packet too long");

        // clientResponse packet authenticator
        if (request != null) {
            // after encoding attributes, create authenticator
            authenticator = createResponseAuthenticator(sharedSecret, packetLength, attributes, request.getAuthenticator());
        } else {
            // update authenticator after encoding attributes
            authenticator = updateRequestAuthenticator(sharedSecret, packetLength, attributes);
        }

        DataOutputStream dos = new DataOutputStream(out);
        dos.writeByte(getPacketType());
        dos.writeByte(getPacketIdentifier());
        dos.writeShort(packetLength);
        dos.write(getAuthenticator());
        dos.write(attributes);
        dos.flush();
    }

    /**
     * This method exists for subclasses to be overridden in order to
     * encode packet attributes like the User-Password attribute.
     * The method may use getAuthenticator() to get the clientRequest
     * authenticator.
     *
     * @param sharedSecret
     */
    protected void encodeRequestAttributes(String sharedSecret) throws RadiusException {
    }

    /**
     * Creates a clientRequest authenticator for this packet. This clientRequest authenticator
     * is constructed as described in RFC 2865.
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @return clientRequest authenticator, 16 bytes
     */
    protected byte[] createRequestAuthenticator(String sharedSecret) {
        byte[] secretBytes = sharedSecret.getBytes(UTF_8);
        byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);

        MessageDigest md5 = getMd5Digest();
        md5.reset();
        md5.update(secretBytes);
        md5.update(randomBytes);
        return md5.digest();
    }

    /**
     * AccountingRequest overrides this
     * method to create a clientRequest authenticator as specified by RFC 2866.
     *
     * @param sharedSecret shared secret
     * @param packetLength length of the final Radius packet
     * @param attributes   attribute data
     * @return new clientRequest authenticator
     */
    protected byte[] updateRequestAuthenticator(String sharedSecret, int packetLength, byte[] attributes) {
        return authenticator;
    }

    /**
     * Creates an authenticator for a Radius clientResponse packet.
     *
     * @param sharedSecret         shared secret
     * @param packetLength         length of clientResponse packet
     * @param attributes           encoded attributes of clientResponse packet
     * @param requestAuthenticator clientRequest packet authenticator
     * @return new 16 byte clientResponse authenticator
     */
    protected byte[] createResponseAuthenticator(String sharedSecret, int packetLength, byte[] attributes, byte[] requestAuthenticator) {
        MessageDigest md5 = getMd5Digest();
        md5.reset();
        md5.update((byte) getPacketType());
        md5.update((byte) getPacketIdentifier());
        md5.update((byte) (packetLength >> 8));
        md5.update((byte) (packetLength & 0x0ff));
        md5.update(requestAuthenticator, 0, requestAuthenticator.length);
        md5.update(attributes, 0, attributes.length);
        md5.update(sharedSecret.getBytes(UTF_8));
        return md5.digest();
    }

    /**
     * Reads a Radius packet from the given input stream and
     * creates an appropiate RadiusPacket descendant object.
     * Reads in all attributes and returns the object.
     * Decodes the encrypted fields and attributes of the packet.
     *
     * @param dictionary   dictionary to use for attributes
     * @param sharedSecret shared secret to be used to decode this packet
     * @param request      Radius clientRequest packet if this is a clientResponse packet to be
     *                     decoded, null if this is a clientRequest packet to be decoded
     * @return new RadiusPacket object
     * @throws IOException     if an IO error occurred
     * @throws RadiusException if the Radius packet is malformed
     */
    protected static RadiusPacket decodePacket(Dictionary dictionary, InputStream in, String sharedSecret, RadiusPacket request)
            throws IOException, RadiusException {
        // check shared secret
        if (sharedSecret == null || sharedSecret.isEmpty())
            throw new RuntimeException("no shared secret has been set");

        // check clientRequest authenticator
        if (request != null && request.getAuthenticator() == null)
            throw new RuntimeException("clientRequest authenticator not set");

        // read and check header
        int type = in.read() & 0x0ff;
        int identifier = in.read() & 0x0ff;
        int length = (in.read() & 0x0ff) << 8 | (in.read() & 0x0ff);

        if (request != null && request.getPacketIdentifier() != identifier)
            throw new RadiusException("bad packet: invalid packet identifier (clientRequest: " + request.getPacketIdentifier() + ", clientResponse: " + identifier);
        if (length < RADIUS_HEADER_LENGTH)
            throw new RadiusException("bad packet: packet too short (" + length + " bytes)");
        if (length > MAX_PACKET_LENGTH)
            throw new RadiusException("bad packet: packet too long (" + length + " bytes)");

        // read rest of packet
        byte[] authenticator = new byte[16];
        byte[] attributeData = new byte[length - RADIUS_HEADER_LENGTH];
        in.read(authenticator);
        in.read(attributeData);

        // check and count attributes
        int pos = 0;
        while (pos < attributeData.length) {
            if (pos + 1 >= attributeData.length)
                throw new RadiusException("bad packet: attribute length mismatch");
            int attributeLength = attributeData[pos + 1] & 0x0ff;
            if (attributeLength < 2)
                throw new RadiusException("bad packet: invalid attribute length");
            pos += attributeLength;
        }
        if (pos != attributeData.length)
            throw new RadiusException("bad packet: attribute length mismatch");

        // create RadiusPacket object; set properties
        RadiusPacket rp = createRadiusPacket(type, dictionary);
        rp.setPacketType(type);
        rp.setPacketIdentifier(identifier);
        rp.authenticator = authenticator;

        // load attributes
        pos = 0;
        while (pos < attributeData.length) {
            int attributeType = attributeData[pos] & 0x0ff;
            int attributeLength = attributeData[pos + 1] & 0x0ff;
            RadiusAttribute a = RadiusAttribute.createRadiusAttribute(dictionary, -1, attributeType);
            a.readAttribute(attributeData, pos, attributeLength);
            rp.addAttribute(a);
            pos += attributeLength;
        }

        // clientRequest packet?
        if (request == null) {
            // decode attributes
            rp.decodeRequestAttributes(sharedSecret);
            rp.checkRequestAuthenticator(sharedSecret, length, attributeData);
        } else {
            // clientResponse packet: check authenticator
            rp.checkResponseAuthenticator(sharedSecret, length, attributeData, request.getAuthenticator());
        }

        return rp;
    }

    /**
     * Checks the clientRequest authenticator against the supplied shared secret.
     * Overriden by AccountingRequest to handle special accounting clientRequest
     * authenticators. There is no way to check clientRequest authenticators for
     * authentication requests as they contain secret bytes.
     *
     * @param sharedSecret shared secret
     * @param packetLength total length of the packet
     * @param attributes   clientRequest attribute data
     */
    protected void checkRequestAuthenticator(String sharedSecret, int packetLength, byte[] attributes)
            throws RadiusException {
    }

    /**
     * Can be overriden to decode encoded clientRequest attributes such as
     * User-Password. This method may use getAuthenticator() to get the
     * clientRequest authenticator.
     *
     * @param sharedSecret
     */
    protected void decodeRequestAttributes(String sharedSecret)
            throws RadiusException {
    }

    /**
     * This method checks the authenticator of this Radius packet. This method
     * may be overriden to include special attributes in the authenticator check.
     *
     * @param sharedSecret         shared secret to be used to encrypt the authenticator
     * @param packetLength         length of the clientResponse packet
     * @param attributes           attribute data of the clientResponse packet
     * @param requestAuthenticator 16 bytes authenticator of the clientRequest packet belonging
     *                             to this clientResponse packet
     */
    protected void checkResponseAuthenticator(String sharedSecret, int packetLength, byte[] attributes, byte[] requestAuthenticator)
            throws RadiusException {
        byte[] authenticator = createResponseAuthenticator(sharedSecret, packetLength, attributes, requestAuthenticator);
        byte[] receivedAuth = getAuthenticator();
        for (int i = 0; i < 16; i++)
            if (authenticator[i] != receivedAuth[i])
                throw new RadiusException("clientResponse authenticator invalid");
    }

    /**
     * Returns a MD5 digest.
     *
     * @return MessageDigest object
     */
    protected MessageDigest getMd5Digest() {
        if (md5Digest == null)
            try {
                md5Digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsae) {
                throw new RuntimeException("md5 digest not available", nsae);
            }
        return md5Digest;
    }

    /**
     * Encodes the attributes of this Radius packet to a byte array.
     *
     * @return byte array with encoded attributes
     * @throws IOException error writing data
     */
    protected byte[] getAttributeBytes()
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(MAX_PACKET_LENGTH);
        for (RadiusAttribute a : attributes) {
            bos.write(a.writeAttribute());
        }
        bos.flush();
        return bos.toByteArray();
    }

    /**
     * @return
     * @throws CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Type of this Radius packet.
     */
    private int packetType = 0;

    /**
     * Identifier of this packet.
     */
    private int packetIdentifier = 0;

    /**
     * Attributes for this packet.
     */
    private List<RadiusAttribute> attributes = new ArrayList<>();

    /**
     * MD5 digest.
     */
    private MessageDigest md5Digest = null;

    /**
     * Authenticator for this Radius packet.
     */
    private byte[] authenticator = null;

    /**
     * Dictionary to look up attribute names.
     */
    private Dictionary dictionary = DefaultDictionary.getDefaultDictionary();

    /**
     * Next packet identifier.
     */
    private static int nextPacketId = 0;

    /**
     * Random number generator.
     */
    private static SecureRandom random = new SecureRandom();

}
