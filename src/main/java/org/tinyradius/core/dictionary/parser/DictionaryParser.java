package org.tinyradius.core.dictionary.parser;

import org.jspecify.annotations.NonNull;
import org.tinyradius.core.dictionary.WritableDictionary;
import org.tinyradius.core.dictionary.parser.resolver.ClasspathResourceResolver;
import org.tinyradius.core.dictionary.parser.resolver.FileResourceResolver;
import org.tinyradius.core.dictionary.parser.resolver.ResourceResolver;

import java.io.IOException;

/**
 * Parses a dictionary in Radiator format and fills a WritableDictionary.
 *
 * @param resourceResolver The resource resolver to use to read dictionary files
 */
public record DictionaryParser(@NonNull ResourceResolver resourceResolver) {

    /**
     * Creates a new DictionaryParser that resolves files via the classpath.
     *
     * @return a new DictionaryParser that resolves files via the classpath
     */
    @NonNull
    public static DictionaryParser newClasspathParser() {
        return new DictionaryParser(ClasspathResourceResolver.INSTANCE);
    }

    /**
     * Creates a new DictionaryParser that resolves files via the local filesystem.
     *
     * @return a new DictionaryParser that resolves files via the local filesystem
     */
    @NonNull
    public static DictionaryParser newFileParser() {
        return new DictionaryParser(FileResourceResolver.INSTANCE);
    }

    /**
     * Returns a new dictionary filled with the contents
     * from the given input stream.
     *
     * @param resource location of resource, resolved depending on {@link ResourceResolver}
     * @return dictionary object
     * @throws IOException parse error reading from input
     */
    @NonNull
    public WritableDictionary parseDictionary(@NonNull String resource) throws IOException {
        var resourceParser = new ResourceParser(resourceResolver);
        return resourceParser.parseDictionary(resource);
    }
}
