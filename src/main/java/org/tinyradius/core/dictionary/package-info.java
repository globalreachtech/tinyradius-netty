/**
 * Interfaces and classes that manage a dictionary of RADIUS attribute types.
 * <p>
 * This package provides a centralized way to load and manage RADIUS attribute
 * definitions. It aims to support the standard FreeRadius and Radiator dictionary formats.
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Loading dictionaries from the classpath or filesystem.</li>
 *   <li>Recursive dictionary loading to handle multiple vendor-specific files.</li>
 *   <li>Tolerance for unsupported attribute types with warnings.</li>
 *   <li>Enumeration support for attribute values.</li>
 * </ul>
 */
package org.tinyradius.core.dictionary;
