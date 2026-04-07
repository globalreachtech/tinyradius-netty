package org.tinyradius.core.dictionary.parser.resolver;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileResourceResolver implements ResourceResolver {

    public static final ResourceResolver INSTANCE = new FileResourceResolver();

    private FileResourceResolver() {
    }

    @Override
    @NonNull
    public String resolve(@NonNull String currentResource, @NonNull String nextResource) {
        var path = Paths.get(currentResource).getParent().resolve(nextResource);
        return Files.exists(path) ?
                path.toString() : "";
    }

    @Override
    @NonNull
    public InputStream openStream(@NonNull String resource) throws IOException {
        var path = Paths.get(resource);
        if (Files.exists(path))
            return Files.newInputStream(path);

        throw new IOException("Could not open stream, file not found: " + resource);
    }
}
