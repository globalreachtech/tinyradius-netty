/**
 * Supporting classes to create and manage attributes.
 * <p>
 * This package provides classes and interfaces for managing RADIUS attributes,
 * including sub-attributes and vendor-specific attributes.
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link org.tinyradius.core.attribute.AttributeTemplate}: Stores metadata
 *   from the dictionary (name, code, type, tag, encryption) and acts as a factory
 *   blueprint for {@link org.tinyradius.core.attribute.type.RadiusAttribute} instances.</li>
 *   <li>{@link org.tinyradius.core.attribute.AttributeHolder}: Interface for objects
 *   that can hold a collection of attributes, such as packets or sub-attribute holders.</li>
 *   <li>{@link org.tinyradius.core.attribute.AttributeTypes}: Constants for common
 *   attribute type codes.</li>
 * </ul>
 */
package org.tinyradius.core.attribute;
