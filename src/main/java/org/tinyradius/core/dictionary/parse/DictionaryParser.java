package org.tinyradius.core.dictionary.parse;

import org.tinyradius.core.dictionary.WritableDictionary;

import java.io.IOException;

/**
 * Parses a dictionary in Radiator format and fills a WritableDictionary.
 */
public class DictionaryParser {

    private final ResourceResolver resourceResolver;

    private DictionaryParser(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public static DictionaryParser newClasspathParser() {
        return new DictionaryParser(new ClasspathResourceResolver());
    }

    public static DictionaryParser newFileParser() {
        return new DictionaryParser(new FileResourceResolver());
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
