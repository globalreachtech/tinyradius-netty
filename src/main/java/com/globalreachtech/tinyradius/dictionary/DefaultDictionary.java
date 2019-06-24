package com.globalreachtech.tinyradius.dictionary;

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

    private DefaultDictionary() {
    }

    private static final String DICTIONARY_RESOURCE = "default_dictionary";
    public static final DefaultDictionary INSTANCE = new DefaultDictionary();

    /*
      Creates the singleton INSTANCE of this object
      and parses the classpath resource.
     */
    static {
        try {
            InputStream source = DefaultDictionary.class.getClassLoader().getResourceAsStream(DICTIONARY_RESOURCE);
            if (source != null)
                DictionaryParser.parseDictionary(source, INSTANCE);
        } catch (IOException e) {
            throw new RuntimeException("default dictionary unavailable", e);
        }
    }

}
