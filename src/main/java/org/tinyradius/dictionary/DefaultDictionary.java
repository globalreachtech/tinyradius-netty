package org.tinyradius.dictionary;

import java.io.IOException;
import java.io.InputStream;

/**
 * The default dictionary is a singleton object containing
 * a dictionary in the memory that is filled on application
 * startup using the default dictionary file from the
 * classpath resource
 * <code>org.tinyradius.dictionary.default_dictionary</code>.
 */
public class DefaultDictionary
extends MemoryDictionary{

	/**
	 * Returns the singleton instance of this object.
	 * @return DefaultDictionary instance
	 */
	public static Dictionary getDefaultDictionary() {
		return instance;
	}
	
	/**
	 * Make constructor private so that a DefaultDictionary
	 * cannot be constructed by other classes. 
	 */
	private DefaultDictionary() {
	}
	
	private static final String DICTIONARY_RESOURCE = "org/tinyradius/dictionary/default_dictionary";
	private static DefaultDictionary instance = new DefaultDictionary();
	
	/**
	 * Creates the singleton instance of this object
	 * and parses the classpath ressource.
	 */
	static {
		try {
    		InputStream source = DefaultDictionary.class.getClassLoader().getResourceAsStream(DICTIONARY_RESOURCE);
			if (source != null)
				DictionaryParser.parseDictionary(source, instance);
		} catch (IOException e) {
			throw new RuntimeException("default dictionary unavailable", e);
		}
	}
	
}
