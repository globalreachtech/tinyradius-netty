package org.tinyradius.core.dictionary;

import org.jspecify.annotations.NonNull;
import org.tinyradius.core.dictionary.parser.DictionaryParser;

import java.io.IOException;

/**
 * The default dictionary is a singleton object containing
 * a dictionary in the memory that is filled on application
 * startup using the default dictionary file from the
 * classpath resource
 * <code>org.tinyradius.dictionary.default_dictionary</code>.
 */
public class DefaultDictionary {

    private static final String DEFAULT_SOURCE = "org/tinyradius/core/dictionary/default_dictionary";

    /**
     * The singleton instance of the default dictionary.
     */
    @NonNull
    public static final WritableDictionary INSTANCE = create();

    private DefaultDictionary() {
    }

    /**
     * Creates the default dictionary by parsing the classpath resource.
     *
     * @return the default dictionary
     */
    @NonNull
    private static WritableDictionary create() {
        var dictionaryParser = DictionaryParser.newClasspathParser();

        try {
            return dictionaryParser.parseDictionary(DEFAULT_SOURCE);
        } catch (IOException e) {
            throw new IllegalStateException("Default dictionary unavailable", e);
        }
    }
}
