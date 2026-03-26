package org.tinyradius.core.dictionary.parser.resolver;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for resolving and opening dictionary resources.
 */
public interface ResourceResolver {

    /**
     * Resolves a dictionary resource relative to the current one.
     *
     * @param currentResource the current resource being processed
     * @param nextResource    the resource requested (e.g. via an $INCLUDE directive)
     * @return the resolved resource path
     */
    String resolve(String currentResource, String nextResource);

    /**
     * Opens an input stream for reading the given dictionary resource.
     * @param resource the resource to open
     * @return an input stream for reading
     * @throws IOException if the resource cannot be opened
     */
    InputStream openStream(String resource) throws IOException;
}