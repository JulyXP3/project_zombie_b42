package modcore.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Classpath streams are supplied by the application loader; no filesystem extraction fallback. */
public final class RuntimeResources {
    private RuntimeResources() {}

    public static InputStream open(String path) {
        if (path == null || !path.startsWith("modcore/") || path.contains("\\")
                || path.contains("/../") || path.contains("/./")) {
            return null;
        }
        return RuntimeResources.class.getClassLoader().getResourceAsStream(path);
    }

    public static InputStream require(String path) throws IOException {
        InputStream in = open(path);
        if (in == null) throw new IOException("Required runtime resource missing: " + path);
        return in;
    }

    public static List<String> list(String prefix) throws IOException {
        try (InputStream in = require("modcore/resources.index");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().filter(name -> name.startsWith(prefix)).sorted().distinct().toList();
        }
    }
}
