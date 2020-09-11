package org.tinyradius.core.dictionary.parser.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileResourceResolver implements ResourceResolver {

    public static final ResourceResolver INSTANCE = new FileResourceResolver();

    private FileResourceResolver() {
    }

    @Override
    public String resolve(String currentResource, String nextResource) {
        final Path path = Paths.get(currentResource).getParent().resolve(nextResource);
        return Files.exists(path) ?
                path.toString() : "";
    }

    @Override
    public InputStream openStream(String resource) throws IOException {
        final Path path = Paths.get(resource);
        if (Files.exists(path))
            return Files.newInputStream(path);

        throw new IOException("Could not open stream, file not found: " + resource);
    }
}
