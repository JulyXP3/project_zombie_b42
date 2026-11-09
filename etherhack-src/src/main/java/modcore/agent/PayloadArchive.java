package modcore.agent;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Reads the authenticated deployment envelope; never extracts an archive entry. */
public final class PayloadArchive {
    public static final String METADATA = "META-INF/modcore-agent.properties";
    public static final String PATCH_ROOT = "META-INF/modcore-patches/";
    private static final int MAX_ARCHIVE_BYTES = 64 * 1024 * 1024;
    private static final int MAX_ENTRY_BYTES = 16 * 1024 * 1024;
    private static final int MAX_ENTRIES = 10000;

    private final Path source;
    private final Properties metadata;
    private final Map<String, byte[]> resources;
    private final Map<String, byte[]> patches;

    private PayloadArchive(Path source, Properties metadata, Map<String, byte[]> resources,
                           Map<String, byte[]> patches) {
        this.source = source;
        this.metadata = metadata;
        this.resources = Collections.unmodifiableMap(resources);
        this.patches = Collections.unmodifiableMap(patches);
    }

    public static PayloadArchive readRuntime(Path file) throws IOException {
        byte[] plain;
        try (InputStream in = Files.newInputStream(file)) { plain = readBounded(in, MAX_ARCHIVE_BYTES); }
        return parse(file, plain, "2");
    }

    public static PayloadArchive read(Path file) throws IOException {
        if (Files.size(file) > MAX_ARCHIVE_BYTES) {
            throw new IOException("Payload envelope exceeds the size limit");
        }
        byte[] envelope;
        try (InputStream in = Files.newInputStream(file)) {
            envelope = readBounded(in, MAX_ARCHIVE_BYTES);
        }
        if (envelope.length < 60) {
            throw new IOException("Truncated payload envelope");
        }
        byte[] key = Arrays.copyOfRange(envelope, 0, 32);
        byte[] plain;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, envelope, 32, 12));
            plain = cipher.doFinal(envelope, 44, envelope.length - 44);
        } catch (GeneralSecurityException e) {
            throw new IOException("Payload authentication failed", e);
        } finally {
            Arrays.fill(key, (byte) 0);
        }

        return parse(file, plain, "1");
    }

    private static PayloadArchive parse(Path file, byte[] plain, String format) throws IOException {
        try {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            long total = 0;
            int count = 0;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(plain))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (++count > MAX_ENTRIES) throw new IOException("Too many payload entries");
                    String name = entry.getName();
                    validatePath(name, entry.isDirectory());
                    if (entry.isDirectory()) continue;
                    if (entries.containsKey(name)) throw new IOException("Duplicate payload entry: " + name);
                    byte[] bytes = readBounded(zip, MAX_ENTRY_BYTES);
                    total += bytes.length;
                    if (total > MAX_ARCHIVE_BYTES) throw new IOException("Expanded payload exceeds the size limit");
                    entries.put(name, bytes);
                }
            }
            byte[] config = entries.remove(METADATA);
            if (config == null) throw new IOException("Agent metadata missing; prepare this payload again");
            Properties metadata = new Properties() {
                @Override public synchronized Object put(Object key, Object value) {
                    if (containsKey(key)) throw new IllegalArgumentException("Duplicate payload metadata: " + key);
                    return super.put(key, value);
                }
            };
            try { metadata.load(new ByteArrayInputStream(config)); }
            catch (IllegalArgumentException e) { throw new IOException("Invalid payload metadata", e); }
            if (!format.equals(metadata.getProperty("format.version"))) {
                throw new IOException("Unsupported agent metadata format");
            }
            if (format.equals("2")) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    if (!digest(entry.getValue()).equals(metadata.getProperty("entry." + entry.getKey()))) {
                        throw new IOException("Runtime entry digest mismatch or unlisted entry: " + entry.getKey());
                    }
                }
                for (String key : metadata.stringPropertyNames()) {
                    if (key.startsWith("entry.") && !entries.containsKey(key.substring(6))) {
                        throw new IOException("Indexed runtime entry missing: " + key.substring(6));
                    }
                    if (!key.startsWith("entry.") && !key.startsWith("original.")
                            && !List.of("format.version", "game.sha256", "main.class", "disabled.patches").contains(key)) {
                        throw new IOException("Unknown runtime metadata: " + key);
                    }
                }
            }
            if (!isDigest(metadata.getProperty("game.sha256"))) {
                throw new IOException("Invalid game digest in agent metadata");
            }
            String main = metadata.getProperty("main.class", "");
            if (!isClassName(main.replace('.', '/')) || main.startsWith("modcore.")) {
                throw new IOException("Invalid application main class in metadata");
            }

            Map<String, byte[]> resources = new LinkedHashMap<>();
            Map<String, byte[]> patches = new LinkedHashMap<>();
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                String name = entry.getKey();
                if (name.equals("META-INF/MANIFEST.MF")) continue;
                if (name.startsWith(PATCH_ROOT) && name.endsWith(".class")) {
                    String target = name.substring(PATCH_ROOT.length(), name.length() - 6);
                    if (!isClassName(target) || isReserved(target)
                            || !isDigest(metadata.getProperty("original." + target))) {
                        throw new IOException("Invalid patch metadata: " + target);
                    }
                    patches.put(target, entry.getValue());
                } else if (name.startsWith("modcore/") && !name.startsWith("modcore/agent/")
                        && !name.startsWith("modcore/build/") && !name.startsWith("modcore/launch/")
                        && !name.startsWith("modcore/attach/")) {
                    resources.put(name, entry.getValue());
                } else {
                    throw new IOException("Disallowed payload entry: " + name);
                }
            }
            for (String keyName : metadata.stringPropertyNames()) {
                if (keyName.startsWith("original.") && !patches.containsKey(keyName.substring(9))) {
                    throw new IOException("Patch bytes missing for " + keyName.substring(9));
                }
            }
            if (main.equals("zombie.gameStates.MainScreenState")) validateRuntimeResources(resources);
            return new PayloadArchive(file.toAbsolutePath().normalize(), metadata, resources, patches);
        } finally {
            Arrays.fill(plain, (byte) 0);
        }
    }

    private static void validateRuntimeResources(Map<String, byte[]> resources) throws IOException {
        for (String name : List.of("modcore/modcore.properties", "modcore/symbols.txt",
                "modcore/lua-prefix.properties", "modcore/resources.index", "modcore/core/RuntimeBootstrap.class")) {
            byte[] bytes = resources.get(name);
            if (bytes == null || bytes.length == 0) throw new IOException("Required runtime resource missing: " + name);
        }
        Properties prefix = new Properties();
        prefix.load(new ByteArrayInputStream(resources.get("modcore/lua-prefix.properties")));
        if (!prefix.getProperty("lua.prefix", "").matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IOException("Invalid runtime prefix resource");
        }
        String index = new String(resources.get("modcore/resources.index"), java.nio.charset.StandardCharsets.UTF_8);
        for (String name : index.lines().filter(line -> !line.isBlank()).toList()) {
            if (!resources.containsKey(name)) throw new IOException("Indexed runtime resource missing: " + name);
        }
    }

    public InputStream open(String name) {
        byte[] bytes = resources.get(name);
        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }

    public List<String> resourceNames(String prefix) {
        return resources.keySet().stream().filter(name -> name.startsWith(prefix)).sorted().toList();
    }

    public String gameDigest() {
        return metadata.getProperty("game.sha256");
    }

    public String mainClass() {
        return metadata.getProperty("main.class");
    }

    public String disabledPatches() {
        return metadata.getProperty("disabled.patches", "");
    }

    Path source() {
        return source;
    }

    List<String> patchNames() {
        return patches.keySet().stream().sorted().toList();
    }

    String originalDigest(String name) {
        return metadata.getProperty("original." + name);
    }

    byte[] patchBytes(String name) {
        byte[] bytes = patches.get(name);
        return bytes == null ? null : bytes.clone();
    }

    byte[] classBytes(String name) {
        byte[] bytes = resources.get(name.replace('.', '/') + ".class");
        return bytes == null ? null : bytes.clone();
    }

    public static String digest(Path path) throws IOException {
        MessageDigest digest = sha256();
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String digest(byte[] bytes) {
        return HexFormat.of().formatHex(sha256().digest(bytes));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static byte[] readBounded(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = in.read(buffer)) != -1) {
            if (count > limit - out.size()) throw new IOException("Payload entry exceeds the size limit");
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }

    private static void validatePath(String name, boolean directory) throws IOException {
        String path = directory && name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
        if (path.isEmpty() || path.startsWith("/") || path.contains("\\") || path.contains(":")) {
            throw new IOException("Invalid payload path: " + name);
        }
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw new IOException("Invalid payload path: " + name);
            }
        }
    }

    private static boolean isDigest(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static boolean isClassName(String value) {
        return value.matches("[A-Za-z_$][A-Za-z0-9_$]*(/[A-Za-z_$][A-Za-z0-9_$]*)*");
    }

    private static boolean isReserved(String name) {
        return name.startsWith("java/") || name.startsWith("javax/") || name.startsWith("jdk/")
                || name.startsWith("sun/") || name.startsWith("modcore/");
    }
}
