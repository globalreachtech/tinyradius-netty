package org.tinyradius.core.dictionary.parser.resolver;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Resolves dictionary resources from the filesystem.
 * <p>
 * This resolver locates dictionary files by resolving relative paths
 * against the filesystem. It is typically used with absolute paths
 * or paths relative to a base directory.
 */
public class FileResourceResolver implements ResourceResolver {

    /**
     * Singleton instance of FileResourceResolver.
     */
    public static final ResourceResolver INSTANCE = new FileResourceResolver();

    private FileResourceResolver() {
    }

    /**
     * Resolves the next resource path relative to the current resource path on the filesystem.
     *
     * @param currentResource current resource path
     * @param nextResource    next resource path to resolve
     * @return the resolved absolute path or an empty string if not found
     */
    @Override
    @NonNull
    public String resolve(@NonNull String currentResource, @NonNull String nextResource) {
        var path = Paths.get(currentResource).getParent().resolve(nextResource);
        return Files.exists(path) ?
                path.toString() : "";
    }

    /**
     * Opens an InputStream for the specified filesystem resource.
     *
     * @param resource filesystem path
     * @return InputStream for the resource
     * @throws IOException if the file is not found or cannot be opened
     */
    @Override
    @NonNull
    public InputStream openStream(@NonNull String resource) throws IOException {
        var path = Paths.get(resource);
        if (Files.exists(path))
            return Files.newInputStream(path);

        throw new IOException("Could not open stream, file not found: " + resource);
    }
}
