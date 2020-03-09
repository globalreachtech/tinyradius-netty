package org.tinyradius.dictionary;

import org.tinyradius.attribute.util.AttributeType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static java.lang.Integer.parseInt;

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
    public void parseDictionary(WritableDictionary dictionary, String resource) throws IOException {
        try (InputStream inputStream = resourceResolver.openStream(resource);
             BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            int lineNum = -1;
            while ((line = in.readLine()) != null) {

                lineNum++;
                parseLine(dictionary, line, lineNum, resource);
            }
        }
    }

    private void parseLine(WritableDictionary dictionary, String rawLine, int lineNum, String resource) throws IOException {
        final String line = rawLine.trim();
        if (line.startsWith("#") || line.isEmpty())
            return;

        final String[] tokens = line.split("\\s+");

        if (tokens.length != 0)
            parseTokens(dictionary, tokens, lineNum, resource);
    }

    private void parseTokens(WritableDictionary dictionary, String[] tokens, int lineNum, String resource) throws IOException {
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
                throw new IOException("Unknown line type: " + tokens[0] + " line: " + lineNum);
        }
    }

    /**
     * Parse a line that declares an attribute.
     */
    private void parseAttributeLine(WritableDictionary dictionary, String[] tok, int lineNum) throws IOException {
        if (tok.length != 4) {
            throw new IOException("Attribute parse error on line " + lineNum + ", " + Arrays.toString(tok));
        }

        // read name, type code, type string
        String name = tok[1];
        int type = parseInt(tok[2]);
        String typeStr = tok[3];

        // create and cache object
        dictionary.addAttributeType(new AttributeType(-1, type, name, typeStr));
    }

    /**
     * Parses a VALUE line containing an enumeration value.
     */
    private void parseValueLine(WritableDictionary dictionary, String[] tok, int lineNum) throws IOException {
        if (tok.length != 4) {
            throw new IOException("Value parse error on line " + lineNum + ": " + Arrays.toString(tok));
        }

        String typeName = tok[1];
        String enumName = tok[2];
        String valStr = tok[3];

        AttributeType at = dictionary.getAttributeTypeByName(typeName);
        if (at == null)
            throw new IOException("Unknown attribute type: " + typeName + ", line: " + lineNum);
        else
            at.addEnumerationValue(parseInt(valStr), enumName);
    }

    /**
     * Parses a line that declares a Vendor-Specific attribute.
     */
    private void parseVendorAttributeLine(WritableDictionary dictionary, String[] tok, int lineNum) throws IOException {
        if (tok.length != 5) {
            throw new IOException("Vendor Attribute parse error on line " + lineNum + ": " + Arrays.toString(tok));
        }

        int vendor = parseInt(tok[1]);
        String name = tok[2];
        int code = parseInt(tok[3]);
        String typeStr = tok[4];

        dictionary.addAttributeType(new AttributeType(vendor, code, name, typeStr));
    }

    /**
     * Parses a line containing a vendor declaration.
     */
    private void parseVendorLine(WritableDictionary dictionary, String[] tok, int lineNum) throws IOException {
        if (tok.length != 3) {
            throw new IOException("Vendor parse error on line " + lineNum + ": " + Arrays.toString(tok));
        }

        int vendorId = parseInt(tok[1]);
        String vendorName = tok[2];

        dictionary.addVendor(vendorId, vendorName);
    }

    /**
     * Includes a dictionary file.
     */
    private void includeDictionaryFile(WritableDictionary dictionary, String[] tok, int lineNum, String currentResource) throws IOException {
        if (tok.length != 2) {
            throw new IOException("Dictionary include parse error on line " + lineNum + ": " + Arrays.toString(tok));
        }
        String includeFile = tok[1];

        final String nextResource = resourceResolver.resolve(currentResource, includeFile);

        if (!nextResource.isEmpty())
            parseDictionary(dictionary, nextResource);
        else
            throw new IOException("Included file '" + includeFile + "' was not found, line " + lineNum + ", " + currentResource);
    }

    public interface ResourceResolver {
        String resolve(String currentResource, String nextResource);

        InputStream openStream(String resource) throws IOException;
    }

    private static class FileResourceResolver implements ResourceResolver {

        @Override
        public String resolve(String currentResource, String nextResource) {
            final Path path = Paths.get(currentResource).getParent().resolve(nextResource);
            return Files.exists(path) ?
                    path.toString() : "";
        }

        @Override
        public InputStream openStream(String resource) throws IOException {
            final Path path = Paths.get(resource);
            if (Files.exists(path))
                return Files.newInputStream(path);

            throw new IOException("could not open stream, file not found: " + resource);
        }
    }

    private static class ClasspathResourceResolver implements ResourceResolver {
        @Override
        public String resolve(String currentResource, String nextResource) {
            final String path = Paths.get(currentResource).getParent().resolve(nextResource).toString();
            return this.getClass().getClassLoader().getResource(path) != null ?
                    path : "";
        }

        @Override
        public InputStream openStream(String resource) throws IOException {
            final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(resource);
            if (stream != null)
                return stream;

            throw new IOException("could not open stream, classpath resource not found: " + resource);
        }
    }
}

