package org.tinyradius.dictionary.parse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

class ClasspathResourceResolver implements ResourceResolver {

    @Override
    public String resolve(String currentResource, String nextResource) {
        final String path = Paths.get(currentResource).getParent().resolve(nextResource).toString();
        return this.getClass().getClassLoader().getResource(path) != null ?
                path : "";
    }

    @Override
    public InputStream openStream(String resource) throws IOException {
        final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(resource);
        if (stream != null)
            return stream;

        throw new IOException("Could not open stream, classpath resource not found: " + resource);
    }
}
