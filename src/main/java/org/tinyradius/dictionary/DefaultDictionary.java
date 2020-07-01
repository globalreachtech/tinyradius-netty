package org.tinyradius.dictionary;

import org.tinyradius.dictionary.parse.DictionaryParser;

import java.io.IOException;

/**
 * The default dictionary is a singleton object containing
 * a dictionary in the memory that is filled on application
 * startup using the default dictionary file from the
 * classpath resource
 * <code>org.tinyradius.dictionary.default_dictionary</code>.
 */
public class DefaultDictionary {

    private static final String DEFAULT_SOURCE = "org/tinyradius/dictionary/default_dictionary";

    public static final WritableDictionary INSTANCE = create();

    private DefaultDictionary() {
    }

    private static WritableDictionary create() {
        final DictionaryParser dictionaryParser = DictionaryParser.newClasspathParser();

        try {
            return dictionaryParser.parseDictionary(DEFAULT_SOURCE);
        } catch (IOException e) {
            throw new IllegalStateException("Default dictionary unavailable", e);
        }
    }
}
