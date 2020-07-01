package org.tinyradius.dictionary.parse;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceResolver {

    String resolve(String currentResource, String nextResource);

    InputStream openStream(String resource) throws IOException;
}
