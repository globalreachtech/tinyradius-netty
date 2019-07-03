package org.tinyradius.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Integer.parseInt;

/**
 * Parses a dictionary in "Radiator format" and fills a WritableDictionary.
 */
public class DictionaryParser {

    private static final Logger logger = LoggerFactory.getLogger(DictionaryParser.class);

    private final ResourceResolver resourceResolver;

    public DictionaryParser(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    /**
     * Returns a new dictionary filled with the contents
     * from the given input stream.
     *
     * @param resource location of resource, resolved depending on {@link ResourceResolver}
     * @return dictionary object
     * @throws IOException parse error reading from input
     */
    public Dictionary parseDictionary(String resource) throws IOException {
        WritableDictionary d = new MemoryDictionary();
        parseDictionary(d, resource);
        return d;
    }

    /**
     * Parses the dictionary from the specified InputStream.
     *
     * @param dictionary dictionary data is written to
     * @param resource   location of resource, resolved depending on {@link ResourceResolver}
     * @throws IOException parse error reading from input
     */
    void parseDictionary(WritableDictionary dictionary, String resource) throws IOException {
        try (InputStream inputStream = resourceResolver.openStream(resource);
             BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            int lineNum = -1;
            while ((line = in.readLine()) != null) {

                lineNum++;
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty())
                    continue;

                final String[] tokens = line.split("\\s+");
                if (tokens.length == 0)
                    continue;
                try {
                    switch (tokens[0].toUpperCase()) {
                        case "ATTRIBUTE":
                            parseAttributeLine(dictionary, tokens, lineNum);
                            break;
                        case "VALUE":
                            parseValueLine(dictionary, tokens, lineNum);
                            break;
                        case "$INCLUDE":
                            includeDictionaryFile(dictionary, tokens, lineNum, resource);
                            break;
                        case "VENDORATTR":
                            parseVendorAttributeLine(dictionary, tokens, lineNum);
                            break;
                        case "VENDOR":
                            parseVendorLine(dictionary, tokens, lineNum);
                            break;
                        default:
                            logger.warn("unknown line type: {} line: {}", tokens[0], lineNum);
                    }
                } catch (Exception e) {
                    logger.warn("error processing line: {}", tokens, e);
                }
            }
        }
    }

    /**
     * Parse a line that declares an attribute.
     */
    private void parseAttributeLine(WritableDictionary dictionary, String[] tok, int lineNum) {
        if (tok.length != 4)
            logger.warn("attribute parse error on line {}, {}", lineNum, tok);

        // read name, code, type
        String name = tok[1];
        int code = parseInt(tok[2]);
        String typeStr = tok[3];

        // create and cache object
        dictionary.addAttributeType(new AttributeType(code, name, typeStr));
    }

    /**
     * Parses a VALUE line containing an enumeration value.
     */
    private void parseValueLine(WritableDictionary dictionary, String[] tok, int lineNum) {
        if (tok.length != 4)
            logger.warn("value parse error on line {}: {}", lineNum, tok);

        String typeName = tok[1];
        String enumName = tok[2];
        String valStr = tok[3];

        AttributeType at = dictionary.getAttributeTypeByName(typeName);
        if (at == null)
            logger.warn("unknown attribute type: {}, line: {}", typeName, lineNum);
        else
            at.addEnumerationValue(parseInt(valStr), enumName);
    }

    /**
     * Parses a line that declares a Vendor-Specific attribute.
     */
    private void parseVendorAttributeLine(WritableDictionary dictionary, String[] tok, int lineNum) {
        if (tok.length != 5)
            logger.warn("vendor attribute parse error on line {}: {}", lineNum, tok);

        String vendor = tok[1];
        String name = tok[2];
        int code = parseInt(tok[3]);
        String typeStr = tok[4];

        dictionary.addAttributeType(new AttributeType(parseInt(vendor), code, name, typeStr));
    }

    /**
     * Parses a line containing a vendor declaration.
     */
    private void parseVendorLine(WritableDictionary dictionary, String[] tok, int lineNum) {
        if (tok.length != 3)
            logger.warn("vendor parse error on line {}: {}", lineNum, tok);

        int vendorId = parseInt(tok[1]);
        String vendorName = tok[2];

        dictionary.addVendor(vendorId, vendorName);
    }

    /**
     * Includes a dictionary file.
     */
    private void includeDictionaryFile(WritableDictionary dictionary, String[] tok, int lineNum, String currentResource) throws IOException {
        if (tok.length != 2)
            logger.warn("dictionary parse error on line {}: {}", lineNum, tok);
        String includeFile = tok[1];

        final String nextResource = resourceResolver.resolve(currentResource, includeFile);

        if (!nextResource.isEmpty())
            parseDictionary(dictionary, nextResource);
        else
            logger.warn("included file '{}' not found, line {}, {}", includeFile, lineNum, currentResource);
    }


    public interface ResourceResolver {
        String resolve(String currentResource, String nextResource);

        InputStream openStream(String resource);
    }

    public static class FileResourceResolver implements ResourceResolver {

        @Override
        public String resolve(String currentResource, String nextResource) {
            final Path path = Paths.get(currentResource).getParent().resolve(nextResource);
            return Files.exists(path) ?
                    path.toString() : "";
        }

        @Override
        public InputStream openStream(String resource) {
            try {
                final Path path = Paths.get(resource);
                if (Files.exists(path))
                    return Files.newInputStream(path);

                logger.warn("could not open stream, file not found: {}", resource);
            } catch (IOException e) {
                logger.warn("io exception opening file: {}", resource, e);
            }
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    public static class ClasspathResourceResolver implements ResourceResolver {
        @Override
        public String resolve(String currentResource, String nextResource) {
            final String path = Paths.get(currentResource).getParent().resolve(nextResource).toString();
            return this.getClass().getClassLoader().getResource(path) != null ?
                    path : "";
        }

        @Override
        public InputStream openStream(String resource) {
            final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(resource);
            if (stream != null)
                return stream;

            logger.warn("could not open stream, classpath resource not found: {}", resource);
            return new ByteArrayInputStream(new byte[0]);
        }
    }
}

