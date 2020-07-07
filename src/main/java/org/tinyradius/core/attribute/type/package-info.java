/**
 * Classes for Radius attributes and Radius attribute types.
 * <p>
 * RadiusAttribute is the interface for accessing attribute data.
 * <p>
 * OctetsAttribute is the base implementation representing general
 * byte array data. Other type-specific subclasses represent, manage,
 * and validate the storage of specific types.
 * <p>
 * EncodedAttribute wraps around OctetsAttribute or subclasses to support
 * encoding/encryption.
 * <p>
 * Vendor-specific attributes are supported by the class
 * VendorSpecificAttribute.
 */
package org.tinyradius.core.attribute.type;