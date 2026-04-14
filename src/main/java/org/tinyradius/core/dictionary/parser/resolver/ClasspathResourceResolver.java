package org.tinyradius.core.dictionary.parser.resolver;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

/**
 * Resolves dictionary resources from the classpath.
 * <p>
 * This resolver locates dictionary files as classpath resources,
 * typically packaged within the application JAR or available
 * on the classpath. Use this resolver to load built-in
 * dictionary files.
 */
public class ClasspathResourceResolver implements ResourceResolver {

    /**
     * Singleton instance of ClasspathResourceResolver.
     */
    public static final @NonNull ResourceResolver INSTANCE = new ClasspathResourceResolver();

    private ClasspathResourceResolver() {
    }

    /**
     * Resolves the next resource path relative to the current resource path on the classpath.
     *
     * @param currentResource current resource path on the classpath
     * @param nextResource    next resource path to resolve
     * @return the resolved classpath resource path or an empty string if not found
     */
    @Override
    @NonNull
    public String resolve(@NonNull String currentResource, @NonNull String nextResource) {

        var parent = Paths.get(currentResource).getParent();
        var path = parent != null
                ? parent.resolve(nextResource).toString()
                : Paths.get(nextResource).toString();

        return this.getClass().getClassLoader().getResource(path) != null ?
                path : "";
    }

    /**
     * Opens an InputStream for the specified classpath resource.
     *
     * @param resource classpath resource path
     * @return InputStream for the resource
     * @throws IOException if the resource is not found or cannot be opened
     */
    @Override
    @NonNull
    public InputStream openStream(@NonNull String resource) throws IOException {
        var stream = this.getClass().getClassLoader().getResourceAsStream(resource);
        if (stream != null)
            return stream;

        throw new IOException("Could not open stream, classpath resource not found: " + resource);
    }
}
