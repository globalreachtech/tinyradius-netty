package org.tinyradius.core.dictionary.parser.resolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResolverTest {

    @TempDir
    Path tempDir;

    @Test
    public void testClasspathResourceResolver() throws IOException {
        ResourceResolver resolver = ClasspathResourceResolver.INSTANCE;

        // Test success
        try (InputStream is = resolver.openStream("org/tinyradius/core/dictionary/default_dictionary")) {
            assertNotNull(is);
        }

        // Test failure
        assertThrows(IOException.class, () -> resolver.openStream("non_existent_resource"));
    }

    @Test
    public void testFileResourceResolver() throws IOException {
        ResourceResolver resolver = FileResourceResolver.INSTANCE;

        // Test failure (non-existent file)
        assertThrows(IOException.class, () -> resolver.openStream(tempDir.resolve("non_existent_file").toString()));

        // Test success (existent file)
        Path file = tempDir.resolve("test_file");
        java.nio.file.Files.writeString(file, "test content");
        try (InputStream is = resolver.openStream(file.toString())) {
            assertNotNull(is);
        }
    }
}
