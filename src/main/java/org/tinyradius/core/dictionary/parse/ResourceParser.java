package org.tinyradius.core.dictionary.parse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.MemoryDictionary;
import org.tinyradius.core.dictionary.Vendor;
import org.tinyradius.core.dictionary.WritableDictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class ResourceParser {

    private static final Logger logger = LogManager.getLogger();

    private final WritableDictionary dictionary;
    private final ResourceResolver resourceResolver;

    private final List<Consumer<WritableDictionary>> deferred = new LinkedList<>();
    private int currentVendor = -1;

    public ResourceParser(ResourceResolver resourceResolver) {
        this(new MemoryDictionary(), resourceResolver);
    }

    public ResourceParser(WritableDictionary dictionary, ResourceResolver resourceResolver) {
        this.dictionary = dictionary;
        this.resourceResolver = resourceResolver;
    }

    /**
     * Parses the dictionary from the specified InputStream.
     *
     * @param resource location of resource, resolved depending on {@link ResourceResolver}
     * @return dictionary with contents loaded from specified resource
     * @throws IOException parse error reading from input
     */
    public WritableDictionary parseDictionary(String resource) throws IOException {
        try (InputStream inputStream = resourceResolver.openStream(resource);
             BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            int lineNum = -1;
            while ((line = in.readLine()) != null) {

                lineNum++;
                parseLine(line, lineNum, resource);
            }

            currentVendor = -1;

            try {
                for (Consumer<WritableDictionary> d : deferred) {
                    d.accept(dictionary);
                }
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException)
                    throw (IOException) e.getCause();
                throw e;
            }
        }
        return dictionary;
    }

    private void parseLine(String rawLine, int lineNum, String resource) throws IOException {
        final int commentIndex = rawLine.indexOf('#');
        final String line = commentIndex == -1 ?
                rawLine.trim() :
                rawLine.substring(0, commentIndex).trim();

        if (line.isEmpty())
            return;

        final String[] tokens = line.split("\\s+");

        if (tokens.length != 0)
            parseTokens(tokens, lineNum, resource);
    }

    private void parseTokens(String[] tokens, int lineNum, String resource) throws IOException {
        // todo BEGIN-TLV
        switch (tokens[0].toUpperCase()) {
            case "END-VENDOR":
                parseEndVendor(tokens, lineNum);
                break;
            case "BEGIN-VENDOR":
                parseBeginVendor(tokens, lineNum);
                break;
            case "ATTRIBUTE":
            case "VENDORATTR":
                parseAttribute(tokens, lineNum);
                break;
            case "VALUE":
                deferred.add(parseValue(tokens, lineNum));
                break;
            case "$INCLUDE":
                includeDictionaryFile(tokens, lineNum, resource);
                break;
            case "VENDOR":
                parseVendor(tokens, lineNum);
                break;
            default:
                throw new IOException("Could not decode tokens on line " + lineNum + ": " + Arrays.toString(tokens));
        }
    }

    private void parseBeginVendor(String[] tok, int lineNum) throws IOException {
        if (tok.length != 2)
            throw new IOException("BEGIN-VENDOR parse error on line " + lineNum + ", " + Arrays.toString(tok));

        currentVendor = dictionary.getVendor(tok[1])
                .map(Vendor::getId)
                .orElse(-1);
    }

    private void parseEndVendor(String[] tok, int lineNum) throws IOException {
        if (tok.length != 2)
            throw new IOException("End-Vendor parse error on line " + lineNum + ", " + Arrays.toString(tok));

        final int vendorId = dictionary.getVendor(tok[1])
                .map(Vendor::getId)
                .orElse(-1);

        if (currentVendor != vendorId)
            throw new IOException("END-VENDOR parse error on line " + lineNum + ", " + Arrays.toString(tok) +
                    " (no corresponding Begin-Vendor found)");

        currentVendor = -1;
    }

    /**
     * Parse a line that declares an attribute.
     */
    private void parseAttribute(String[] tok, int lineNum) throws IOException {
        // ATTRIBUTE    Ascend-Send-Secret  214	string
        // VENDORATTR   529     Ascend-Send-Secret  214 string

        // VENDORATTR have extra vendorId field in tok[1]
        final int offset = tok[0].equals("VENDORATTR") ? 1 : 0;

        if (tok.length < 4 + offset || tok.length > 5 + offset)
            throw new IOException(tok[0] + " parse error on line " + lineNum + ", " + Arrays.toString(tok));

        final int vendorId = offset == 1 ? Integer.parseInt(tok[1]) : currentVendor;
        final String name = tok[1 + offset];
        final byte type = convertType(Integer.parseInt(tok[2 + offset]), vendorId);
        final String typeStr = tok[3 + offset];

        // no flags
        if (tok.length == 4 + offset) {
            dictionary.addAttributeTemplate(
                    new AttributeTemplate(vendorId, type, name, typeStr, (byte) 0, false));
            return;
        }

        final String[] flags = tok[4 + offset].split(",");
        dictionary.addAttributeTemplate(
                new AttributeTemplate(vendorId, type, name, typeStr, encryptFlag(flags), tagFlag(flags)));
    }

    /**
     * Parses a VALUE line containing an enumeration value.
     *
     * @return deferred Dictionary write, so it can be processed before ATTRIBUTE
     */
    private Consumer<WritableDictionary> parseValue(String[] tok, int lineNum) throws IOException {
        if (tok.length != 4) {
            throw new IOException("VALUE parse error on line " + lineNum + ": " + Arrays.toString(tok));
        }

        final String attributeName = tok[1];
        final String enumName = tok[2];
        final String valStr = tok[3];

        return d -> d.getAttributeTemplate(attributeName)
                .orElseThrow(() -> new RuntimeException(new IOException("Unknown attribute type: " + attributeName + ", line: " + lineNum)))
                .addEnumerationValue(Integer.decode(valStr), enumName);
    }

    /**
     * Parses a line containing a vendor declaration.
     */
    private void parseVendor(String[] tok, int lineNum) throws IOException {
        if (tok.length < 3 || tok.length > 4) {
            throw new IOException("VENDOR parse error on line " + lineNum + ": " + Arrays.toString(tok));
        }

        final int[] format = tok.length == 4 ?
                formatFlag(tok[3]) : new int[]{1, 1};

        try {
            // Legacy TinyRadius format: VENDOR number vendor-name [format]
            final int id = Integer.parseInt(tok[1]);
            final String name = tok[2];

            dictionary.addVendor(new Vendor(id, name, format[0], format[1]));
        } catch (NumberFormatException e) {
            // FreeRadius format: VENDOR vendor-name number [format]
            try {
                final String name = tok[1];
                final int id = Integer.parseInt(tok[2]);

                dictionary.addVendor(new Vendor(id, name, format[0], format[1]));
            } catch (NumberFormatException e1) {
                throw new IOException("Vendor parse error on line " + lineNum + ": " + Arrays.toString(tok));
            }
        }
    }

    /**
     * Includes a dictionary file.
     */
    private void includeDictionaryFile(String[] tok, int lineNum, String currentResource) throws IOException {
        if (tok.length != 2)
            throw new IOException("Dictionary include parse error on line " + lineNum + ": " + Arrays.toString(tok));

        final String includeFile = tok[1];
        final String nextResource = resourceResolver.resolve(currentResource, includeFile);

        if (!nextResource.isEmpty())
            new ResourceParser(dictionary, resourceResolver).parseDictionary(nextResource);
        else
            throw new IOException("Included file '" + includeFile + "' was not found, line " + lineNum + ", " + currentResource);
    }

    private byte convertType(int type, int vendorId) {
        final int max = dictionary.getVendor(vendorId)
                .map(Vendor::getTypeSize)
                .map(t -> 2 ^ (8 * t) - 1)
                .orElse(255);
        // todo

        if (type < 0 || type > 255)
            throw new IllegalArgumentException("Attribute type code out of bounds: " + type);
        return (byte) type;
    }

    private int[] formatFlag(String flag) {
        if (flag.startsWith("format=")) {
            final String[] values = flag.substring(7).split(",");
            if (values.length == 2)
                try {
                    return new int[]{Integer.parseInt(values[0]), Integer.parseInt(values[1])};
                } catch (Exception ignored) {
                }
        }
        logger.warn("Ignoring vendor flag - invalid format: {}", flag);
        return new int[]{1, 1};
    }

    private byte encryptFlag(String[] flags) {
        for (final String flag : flags) {
            if (flag.length() == 9 && flag.startsWith("encrypt="))
                return Byte.parseByte(flag.substring(8, 9));
        }
        return 0;
    }

    private boolean tagFlag(String[] flags) {
        for (final String flag : flags) {
            if (flag.equals("has_tag"))
                return true;
        }
        return false;
    }

}
