package modcore.launch;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Versioned, bounded records shared by the gate and its owning coordinator. */
public final class SessionFiles {
    public static final Set<String> REQUEST = Set.of("version", "session", "game", "game.sha256", "agent",
            "agent.sha256", "runtime", "runtime.sha256", "gate", "gate.sha256", "timeout");
    public static final Set<String> STATE = Set.of("version", "session", "pid", "started", "request", "phase", "code", "detail");
    public static final Set<String> RELEASE = Set.of("version", "session", "pid", "started", "request");
    private static final int LIMIT = 32768;

    private SessionFiles() {}

    public static Path regular(Path file) throws IOException {
        Path absolute = file.toAbsolutePath().normalize();
        for (Path part = absolute; part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part) || !part.toRealPath().equals(part.toRealPath(LinkOption.NOFOLLOW_LINKS))) {
                throw new IOException("Path redirection is not allowed: " + part);
            }
        }
        if (!Files.isRegularFile(absolute, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Not a regular file: " + file);
        return absolute.toRealPath();
    }

    public static Properties read(Path file, Set<String> allowed) throws IOException {
        regular(file);
        if (Files.size(file) > LIMIT) throw new IOException("Session record exceeds size limit");
        byte[] bytes;
        try (var in = Files.newInputStream(file)) { bytes = in.readNBytes(LIMIT + 1); }
        if (bytes.length > LIMIT) throw new IOException("Session record exceeds size limit");
        Properties props = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("Duplicate session field: " + key);
                return super.put(key, value);
            }
        };
        String text = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        try { props.load(new StringReader(text)); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid session record", e); }
        if (!props.stringPropertyNames().equals(allowed)) throw new IOException("Unexpected or missing session fields");
        if (!props.getProperty("version").equals("1")) throw new IOException("Unsupported session version");
        if (!props.getProperty("session").matches("[0-9a-f]{64}")) throw new IOException("Invalid session identifier");
        return props;
    }

    public static void write(Path directory, String name, Map<String, String> fields, boolean replace) throws IOException {
        if (!Set.of("request.properties", "state.properties", "release.properties").contains(name)) {
            throw new IOException("Unexpected session filename");
        }
        Properties props = new Properties();
        props.putAll(fields);
        StringWriter writer = new StringWriter();
        props.store(writer, null);
        byte[] bytes = writer.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > LIMIT) throw new IOException("Session record exceeds size limit");
        Path target = directory.resolve(name);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            regular(target);
            if (!replace) throw new IOException("Session record already exists: " + name);
        }
        Path staging = Files.createTempFile(directory, "record-", ".tmp");
        try {
            Files.write(staging, bytes, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(staging); }
    }

    public static String digest(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(regular(file))) {
                byte[] buffer = new byte[16384];
                int count;
                while ((count = in.read(buffer)) >= 0) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public static void verifyArtifacts(Properties request) throws IOException {
        for (String name : Set.of("game", "agent", "runtime", "gate")) {
            String expected = request.getProperty(name + ".sha256");
            if (expected == null || !expected.matches("[0-9a-f]{64}")
                    || !digest(Path.of(request.getProperty(name))).equals(expected)) {
                throw new IOException("Artifact digest mismatch: " + name);
            }
        }
    }

    public static long positive(Properties record, String key) throws IOException {
        try {
            long value = Long.parseLong(record.getProperty(key));
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (RuntimeException e) { throw new IOException("Invalid positive session field: " + key, e); }
    }
}
