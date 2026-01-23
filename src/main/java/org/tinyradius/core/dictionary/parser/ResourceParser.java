package org.tinyradius.core.dictionary.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.RadiusAttributeFactory;
import org.tinyradius.core.dictionary.MemoryDictionary;
import org.tinyradius.core.dictionary.Vendor;
import org.tinyradius.core.dictionary.WritableDictionary;
import org.tinyradius.core.dictionary.parser.resolver.ResourceResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.lang.Integer.parseInt;
import static org.tinyradius.core.attribute.AttributeTypes.VENDOR_SPECIFIC;

@Log4j2
@RequiredArgsConstructor
public class ResourceParser {

    private final WritableDictionary dictionary;
    private final ResourceResolver resourceResolver;
    private final FactoryProvider factoryProvider;

    // support for VALUE declared before ATTRIBUTE
    private final List<Consumer<WritableDictionary>> deferred = new LinkedList<>();
    private int currentVendor = -1;

    public ResourceParser(ResourceResolver resourceResolver) {
        this(new MemoryDictionary(), resourceResolver, RadiusAttributeFactory::fromDataType);
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
            case "BEGIN-TLV":
                log.warn("'BEGIN-TLV' not supported - ignoring");
                break;
            case "END-TLV":
                log.warn("'END-TLV' not supported - ignoring");
                break;
            case "PROTOCOL":
                log.warn("'PROTOCOL' not supported - ignoring");
                break;
            case "BEGIN-PROTOCOL":
                log.warn("'BEGIN-PROTOCOL' not supported - ignoring");
                break;
            case "END-PROTOCOL":
                log.warn("'END-PROTOCOL' not supported - ignoring");
                break;
            case "MEMBER": // for 'struct' compound type
                log.warn("'MEMBER' not supported - ignoring");
                break;
            case "STRUCT": 
                log.warn("'STRUCT' not supported - ignoring");
                break;
            default:
                throw new IOException("Could not decode tokens on line " + lineNum + ": " + Arrays.toString(tokens));
        }
    }

    private void parseBeginVendor(String[] tok, int lineNum) throws IOException {
        if (tok.length < 2 || tok.length > 3)
            throw new IOException("BEGIN-VENDOR parse error on line " + lineNum + ", " + Arrays.toString(tok));

        currentVendor = dictionary.getVendor(tok[1])
                .map(Vendor::id)
                .orElse(-1);
    }

    private void parseEndVendor(String[] tok, int lineNum) throws IOException {
        if (tok.length != 2)
            throw new IOException("End-Vendor parse error on line " + lineNum + ", " + Arrays.toString(tok));

        final int vendorId = dictionary.getVendor(tok[1])
                .map(Vendor::id)
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

        final int vendorId = offset == 1 ? parseInt(tok[1]) : currentVendor;
        final String name = tok[1 + offset];
        int type;
        try{
            type = validateType(Integer.decode(tok[2 + offset]), vendorId);
        } catch (NumberFormatException e){
            log.warn(String.format("Attribute type is not an integer and not supported - vendor: %d, attributeName: %s, type: %s", vendorId, name, tok[2 + offset]));
            return;
        }
        final String dataType = tok[3 + offset];
        final RadiusAttributeFactory<?> factory =
                factoryProvider.fromDataType(vendorId == -1 && type == VENDOR_SPECIFIC ? "vsa" : dataType);
        final String[] flags = tok.length == 4 + offset ?
                new String[0] :
                tok[4 + offset].split(",");

        dictionary.addAttributeTemplate(
                new AttributeTemplate(vendorId, type, name, dataType, factory, encryptFlag(flags), tagFlag(flags)));
    }

    /**
     * Parses a VALUE line containing an enumeration value.
     *
     * @return deferred Dictionary write, so it can be processed before ATTRIBUTE
     */
    private Consumer<WritableDictionary> parseValue(String[] tok, int lineNum) throws IOException {
        if (tok.length != 4)
            throw new IOException("VALUE parse error on line " + lineNum + ": " + Arrays.toString(tok));

        var attributeName = tok[1];
        var enumName = tok[2];
        var valStr = tok[3];

        // If the attributeName is not found, log and ignore instead of throwing RuntimeException
        return d -> d.getAttributeTemplate(attributeName)
            .ifPresentOrElse(at -> at.addEnumerationValue(Integer.decode(valStr), enumName), 
                () -> log.warn(String.format("Unknown attribute type while parsing VALUE: %s, line: %d", attributeName,lineNum)));
    }

    /**
     * Parses a line containing a vendor declaration.
     */
    private void parseVendor(String[] tok, int lineNum) throws IOException {
        if (tok.length < 3 || tok.length > 4)
            throw new IOException("VENDOR parse error on line " + lineNum + ": " + Arrays.toString(tok));

        final int[] format = tok.length == 4 ?
                formatFlag(tok[3]) : new int[]{1, 1};

        try {
            // Legacy TinyRadius format: VENDOR number vendor-name [format]
            var id = parseInt(tok[1]);
            var name = tok[2];

            dictionary.addVendor(new Vendor(id, name, format[0], format[1]));
        } catch (NumberFormatException e) {
            // FreeRadius format: VENDOR vendor-name number [format]
            try {
                var name = tok[1];
                var id = parseInt(tok[2]);

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

        var includeFile = tok[1];
        var nextResource = resourceResolver.resolve(currentResource, includeFile);

        if (!nextResource.isEmpty())
            parseDictionary(nextResource);
        else
            throw new IOException("Included file '" + includeFile + "' was not found, line " + lineNum + ", " + currentResource);
    }

    private int validateType(int type, int vendorId) {
        final int max = dictionary.getVendor(vendorId)
                .map(Vendor::typeSize)
                .map(t -> (int) Math.pow(2, 8d * t) - 1)
                .orElse(255);

        if (type < 0 || type > max)
            throw new IllegalArgumentException("Attribute type code out of bounds: " + type + ", max " + max);
        return type;
    }

    private int[] formatFlag(String flag) {
        if (flag.startsWith("format=")) {
            final String[] values = flag.substring(7).split(",");
            if (values.length == 2)
                try {
                    return new int[]{parseInt(values[0]), parseInt(values[1])};
                } catch (Exception ignored) {
                    // use default [1,1]
                }
        }
        log.warn("Ignoring vendor flag - invalid format: {}", flag);
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
        return Set.of(flags).contains("has_tag");
    }

    public interface FactoryProvider {
        RadiusAttributeFactory<? extends RadiusAttribute> fromDataType(String dataType);
    }

}
