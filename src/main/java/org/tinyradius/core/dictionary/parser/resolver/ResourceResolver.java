package org.tinyradius.core.dictionary.parser.resolver;

import org.jspecify.annotations.NonNull;

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
    @NonNull
    String resolve(@NonNull String currentResource, @NonNull String nextResource);

    /**
     * Opens an input stream for reading the given dictionary resource.
     *
     * @param resource the resource to open
     * @return an input stream for reading
     * @throws IOException if the resource cannot be opened
     */
    @NonNull
    InputStream openStream(@NonNull String resource) throws IOException;
}