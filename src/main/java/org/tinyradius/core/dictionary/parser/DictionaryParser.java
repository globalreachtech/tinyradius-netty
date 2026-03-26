package org.tinyradius.core.dictionary.parser;

import org.tinyradius.core.dictionary.WritableDictionary;
import org.tinyradius.core.dictionary.parser.resolver.ClasspathResourceResolver;
import org.tinyradius.core.dictionary.parser.resolver.FileResourceResolver;
import org.tinyradius.core.dictionary.parser.resolver.ResourceResolver;

import java.io.IOException;

/**
 * Parses a dictionary in Radiator format and fills a WritableDictionary.
 * @param resourceResolver The resource resolver to use to read dictionary files
 */
public record DictionaryParser(ResourceResolver resourceResolver) {

    /**
     * Creates a new DictionaryParser that resolves files via the classpath.
     *
     * @return a new DictionaryParser that resolves files via the classpath
     */
    public static DictionaryParser newClasspathParser() {
        return new DictionaryParser(ClasspathResourceResolver.INSTANCE);
    }

    /**
     * Creates a new DictionaryParser that resolves files via the local filesystem.
     * @return a new DictionaryParser that resolves files via the local filesystem
     */
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
    public WritableDictionary parseDictionary(String resource) throws IOException {
        final ResourceParser resourceParser = new ResourceParser(resourceResolver);
        return resourceParser.parseDictionary(resource);
    }
}
