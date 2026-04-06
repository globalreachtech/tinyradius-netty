package org.tinyradius.core.dictionary.parser.resolver;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

public class ClasspathResourceResolver implements ResourceResolver {

    public static final @NonNull ResourceResolver INSTANCE = new ClasspathResourceResolver();

    private ClasspathResourceResolver() {
    }

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

    @Override
    @NonNull
    public InputStream openStream(@NonNull String resource) throws IOException {
        var stream = this.getClass().getClassLoader().getResourceAsStream(resource);
        if (stream != null)
            return stream;

        throw new IOException("Could not open stream, classpath resource not found: " + resource);
    }
}
