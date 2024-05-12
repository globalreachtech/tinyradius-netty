package org.tinyradius.core.dictionary.parser;

import lombok.RequiredArgsConstructor;
import org.tinyradius.core.dictionary.WritableDictionary;
import org.tinyradius.core.dictionary.parser.resolver.ClasspathResourceResolver;
import org.tinyradius.core.dictionary.parser.resolver.FileResourceResolver;
import org.tinyradius.core.dictionary.parser.resolver.ResourceResolver;

import java.io.IOException;

/**
 * Parses a dictionary in Radiator format and fills a WritableDictionary.
 */
@RequiredArgsConstructor
public class DictionaryParser {

    private final ResourceResolver resourceResolver;

    public static DictionaryParser newClasspathParser() {
        return new DictionaryParser(ClasspathResourceResolver.INSTANCE);
    }

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
