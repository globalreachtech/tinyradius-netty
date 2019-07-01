package org.tinyradius.dictionary;

import org.tinyradius.dictionary.DictionaryParser.ClasspathResourceResolver;

import java.io.IOException;
import java.io.InputStream;

/**
 * The default dictionary is a singleton object containing
 * a dictionary in the memory that is filled on application
 * startup using the default dictionary file from the
 * classpath resource
 * <code>org.tinyradius.dictionary.default_dictionary</code>.
 */
public class DefaultDictionary extends MemoryDictionary {

    private static final String DEFAULT_SOURCE = "org/tinyradius/dictionary/default_dictionary";
    private static final String CUSTOM_SOURCE = "tinyradius_dictionary";

    public static final DefaultDictionary INSTANCE = new DefaultDictionary();

    private DefaultDictionary() {
        final DictionaryParser dictionaryParser = new DictionaryParser(new ClasspathResourceResolver());

        try (InputStream source = this.getClass().getClassLoader().getResourceAsStream(CUSTOM_SOURCE)) {
            dictionaryParser.parseDictionary(
                    this, source != null ? CUSTOM_SOURCE : DEFAULT_SOURCE);
        } catch (IOException e) {
            throw new RuntimeException("default dictionary unavailable", e);
        }
    }

}
