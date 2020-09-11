package org.tinyradius;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestUtils {

    public static String getStackTrace(Throwable t) {
        final StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
