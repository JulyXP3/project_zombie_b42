package modcore.agent;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Shared fixture for the build entry points. It creates a throw-away checkout
 * under the project temp tree, a game directory with a complete-looking
 * {@code jre64} and a fake Gradle wrapper that records the environment the
 * entry point exported. All commands run through a real {@code cmd.exe}, so the
 * batch quoting rules are under test rather than simulated.
 *
 * <p>Only tests use this class; production code never references it.</p>
 */
final class BuildEntryHarness {
    static final String JAVA_OVERRIDE = "MODCORE_BUILD_JAVA_HOME";
    static final String GAME_OVERRIDE = "MODCORE_BUILD_GAME_DIR";
    static final String NATIVE_FIXTURE = "MODCORE_NATIVE_FIXTURE";
    static final String CALL_FILE = "gradle-call.txt";
    static final String CWD_FILE = "gradle-call-cwd.txt";
    static final String COMPILE_DEPENDENCY = "compile dependency marker";
    private static final long TIMEOUT_SECONDS = 180;

    private BuildEntryHarness() {
    }

    record Fixture(Path root, Path source, Path game) {
    }

    record Result(int exit, String output) {
    }

    /**
     * Creates a sandbox with a checkout (real build.bat/build-fast.bat copied
     * from the production tree), a compile dependency, and a game directory
     * whose jre64 has the same four anchors the real game runtime has.
     */
    static Fixture fixture(String label) throws Exception {
        return fixture(label, 0);
    }

    static Fixture fixture(String label, int gradleExit) throws Exception {
        Path testRoot = Path.of(System.getProperty("agent.testRoot"));
        Files.createDirectories(testRoot);
        Path root = Files.createTempDirectory(testRoot, "build-" + label + " ");
        Path source = Files.createDirectories(root.resolve("source " + label));
        Path game = Files.createDirectories(root.resolve("game " + label));
        Path production = Path.of(System.getProperty("agent.launchScript")).getParent();
        Files.copy(production.resolve("build.bat"), source.resolve("build.bat"));
        Files.copy(production.resolve("build-fast.bat"), source.resolve("build-fast.bat"));
        // A formal preparation helper belongs next to the entry points; copy the
        // root PowerShell scripts so the fixture is a faithful mini checkout.
        try (var entries = Files.list(production)) {
            for (Path script : entries.filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                    .endsWith(".ps1")).toList()) {
                Files.copy(script, source.resolve(script.getFileName()));
            }
        }
        Files.writeString(source.resolve("build.gradle.kts"), "// fixture\n", StandardCharsets.UTF_8);
        Files.createDirectories(source.resolve("lib"));
        Files.writeString(source.resolve("lib/projectzomboid.jar"), COMPILE_DEPENDENCY, StandardCharsets.UTF_8);
        Files.writeString(game.resolve("projectzomboid.jar"), COMPILE_DEPENDENCY, StandardCharsets.UTF_8);
        writeSmallJre64(game);
        // Sentinels: an invalid configuration must never edit the game directory.
        Files.writeString(game.resolve("ProjectZomboid64.bat"), "@echo off\r\nrem game launch sentinel\r\n",
                StandardCharsets.US_ASCII);
        Files.writeString(game.resolve("sentinel.txt"), "do not touch", StandardCharsets.UTF_8);
        Files.writeString(source.resolve("gradlew.bat"), fakeGradle(gradleExit), StandardCharsets.US_ASCII);
        return new Fixture(root, source, game);
    }

    /**
     * A game runtime that is complete enough for the copy/validate logic, but
     * small enough to copy in every case. It intentionally has no javac: the
     * game ships a runtime, not a JDK.
     */
    static void writeSmallJre64(Path game) throws IOException {
        Path jre = game.resolve("jre64");
        write(jre.resolve("bin/java.exe"), "runtime java executable");
        write(jre.resolve("bin/keytool.exe"), "runtime keytool executable");
        write(jre.resolve("bin/server/jvm.dll"), "runtime vm library");
        write(jre.resolve("lib/modules"), "runtime module image");
        write(jre.resolve("lib/jvm.cfg"), "runtime vm configuration");
        write(jre.resolve("conf/net.properties"), "# runtime configuration");
        write(jre.resolve("legal/java.base/LICENSE"), "runtime license");
        write(jre.resolve("DISCLAIMER"), "runtime disclaimer");
        write(jre.resolve("release"), "IMPLEMENTOR=\"Azul Systems, Inc.\"\n"
                + "JAVA_RUNTIME_VERSION=\"25.0.5+8-LTS\"\n"
                + "JAVA_VERSION=\"25.0.5\"\n"
                + "OS_ARCH=\"x86_64\"\n");
    }

    private static void write(Path path, String text) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    private static String fakeGradle(int exit) {
        return "@echo off\r\n"
                + "setlocal EnableExtensions DisableDelayedExpansion\r\n"
                + ">\"%~dp0" + CALL_FILE + "\" set JAVA_HOME\r\n"
                + ">>\"%~dp0" + CALL_FILE + "\" set " + NATIVE_FIXTURE + "\r\n"
                // "cd" without arguments prints the path literally; unlike echo
                // it does not re-parse & ( ) ! inside the folder name.
                + ">\"%~dp0" + CWD_FILE + "\" cd\r\n"
                + ">>\"%~dp0" + CALL_FILE + "\" echo GRADLE_ARGS=%*\r\n"
                + "exit /b " + exit + "\r\n";
    }

    /**
     * Replaces the value of a top-of-file configuration line, which is the
     * documented user workflow ("fill in the two paths and run"). The copy is
     * the one under test; the production file stays untouched.
     */
    static void editTopConfig(Path script, String javaHome, String gameDir) throws IOException {
        String text = Files.readString(script, StandardCharsets.UTF_8);
        text = replaceSet(text, "BUILD_JAVA_HOME", javaHome);
        text = replaceSet(text, "BUILD_GAME_DIR", gameDir);
        Files.writeString(script, text, StandardCharsets.UTF_8);
    }

    private static String replaceSet(String text, String name, String value) {
        Pattern pattern = Pattern.compile("(?m)^([^\\r\\n]*?set \"" + Pattern.quote(name) + "=)[^\"\\r\\n]*(.*)$");
        Matcher matcher = pattern.matcher(text);
        assertTrue(matcher.find(), "the build entry must configure " + name + " near the top: " + text);
        // Only the user-editable top line is replaced; later automation override
        // lines reference the same variable and must keep their own value.
        StringBuilder replaced = new StringBuilder();
        matcher.appendReplacement(replaced, "$1" + Matcher.quoteReplacement(value) + "$2");
        matcher.appendTail(replaced);
        return replaced.toString();
    }

    /**
     * Runs an entry point from a directory unrelated to the checkout, so a
     * script that depends on the caller's working directory fails here.
     */
    static Result run(Fixture fixture, String scriptName, Map<String, String> environment, String... arguments)
            throws Exception {
        Path script = fixture.source().resolve(scriptName);
        Files.deleteIfExists(fixture.source().resolve(CALL_FILE));
        Files.deleteIfExists(fixture.source().resolve(CWD_FILE));
        Path log = Files.createTempFile(fixture.root(), "batch-", ".log");
        StringBuilder command = new StringBuilder("call \"").append(script).append('"');
        for (String argument : arguments) {
            command.append(" \"").append(argument).append('"');
        }
        ProcessBuilder builder = new ProcessBuilder(comspec(), "/d", "/c", command.toString())
                .directory(fixture.root().toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        Map<String, String> child = builder.environment();
        for (String key : List.of("JAVA_HOME", "JDK_HOME", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS",
                "CLASSPATH", "JAVA_OPTS", "GRADLE_OPTS", "PZ_GAME_DIR", "BUILD_JAVA_HOME", "BUILD_GAME_DIR",
                JAVA_OVERRIDE, GAME_OVERRIDE, NATIVE_FIXTURE)) {
            child.remove(key);
        }
        Path home = Files.createDirectories(fixture.root().resolve("home"));
        Path appdata = Files.createDirectories(fixture.root().resolve("appdata"));
        child.put("PATH", pathWithPowerShell());
        child.put("TEMP", fixture.root().toString());
        child.put("TMP", fixture.root().toString());
        child.put("USERPROFILE", home.toString());
        child.put("LOCALAPPDATA", appdata.toString());
        child.put("MODCORE_NO_PAUSE", "1");
        child.putAll(environment);
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
            fail("Build entry timed out: " + command);
        }
        return new Result(process.exitValue(), Files.readString(log, nativeCharset()));
    }

    static Map<String, String> validEnvironment(Fixture fixture) throws IOException {
        return Map.of(JAVA_OVERRIDE, realJdk().toString(), GAME_OVERRIDE, fixture.game().toString());
    }

    /**
     * A valid JAVA_HOME/PATH fallback. A script that silently falls back from an
     * invalid explicit override reaches Gradle and fails the test, while the
     * required behaviour is to stop before Gradle and report the explicit value.
     */
    static Map<String, String> environmentWithJavaHomeFallback(Fixture fixture) throws IOException {
        Map<String, String> environment = new LinkedHashMap<>(validEnvironment(fixture));
        Path jdk = realJdk();
        environment.put("JAVA_HOME", jdk.toString());
        environment.put("PATH", jdk.resolve("bin") + ";" + pathWithPowerShell());
        return environment;
    }

    /** Parses what the fake Gradle wrapper observed. */
    static Map<String, String> recording(Path source) throws IOException {
        Path call = source.resolve(CALL_FILE);
        assertTrue(Files.isRegularFile(call), "the build entry did not invoke Gradle");
        Map<String, String> values = parseCallFile(call);
        Path cwd = source.resolve(CWD_FILE);
        if (Files.isRegularFile(cwd)) {
            String directory = readExistingPath(cwd);
            if (directory != null && !directory.isEmpty()) values.put("CWD", directory);
        }
        return values;
    }

    /**
     * Command output uses the console code page, which can be the active UTF-8
     * code page or the ANSI/OEM code page depending on the host. A path that
     * exists on disk identifies the correct decoding, so Unicode fixture paths
     * are not misread.
     */
    private static Map<String, String> parseCallFile(Path call) throws IOException {
        byte[] bytes = Files.readAllBytes(call);
        Map<String, String> fallback = null;
        for (Charset charset : List.of(nativeCharset(), StandardCharsets.UTF_8)) {
            Map<String, String> values = parseRecordedLines(new String(bytes, charset));
            if (fallback == null) fallback = values;
            String fixture = values.get(NATIVE_FIXTURE);
            if (fixture != null && Files.isDirectory(Path.of(fixture))) return values;
        }
        return fallback;
    }

    private static Map<String, String> parseRecordedLines(String text) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : text.split("\\R")) {
            int separator = line.indexOf('=');
            if (separator <= 0) continue;
            String key = line.substring(0, separator);
            if (key.matches("[A-Za-z_][A-Za-z0-9_]*")) values.put(key, line.substring(separator + 1));
        }
        return values;
    }

    private static String readExistingPath(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String fallback = null;
        for (Charset charset : List.of(nativeCharset(), StandardCharsets.UTF_8)) {
            String text = new String(bytes, charset).strip();
            if (text.isEmpty()) continue;
            if (fallback == null) fallback = text;
            try {
                if (Files.exists(Path.of(text))) return text;
            } catch (java.nio.file.InvalidPathException ignored) {
                // Try the next decoding.
            }
        }
        return fallback;
    }

    static Path managedFixture(Map<String, String> call) {
        String value = call.get(NATIVE_FIXTURE);
        assertNotNull(value, "the full build must export the auto-prepared native fixture: " + call);
        assertFalse(value.isBlank(), "the exported native fixture path is empty");
        return Path.of(value);
    }

    static void assertGradleNotInvoked(Path source) {
        assertFalse(Files.exists(source.resolve(CALL_FILE)),
                "an invalid configuration must be rejected before Gradle starts");
    }

    static void assertNoManagedFixture(Fixture fixture) throws IOException {
        Path temp = fixture.root().resolve("temp");
        if (!Files.exists(temp)) return;
        try (var walk = Files.walk(temp)) {
            List<Path> runtimes = walk.filter(path -> path.getFileName() != null
                    && path.getFileName().toString().equalsIgnoreCase("jre64")).toList();
            assertTrue(runtimes.isEmpty(), "the fast entry must not prepare a native fixture: " + runtimes);
        }
    }

    static Map<String, String> snapshot(Path directory) throws IOException {
        Map<String, String> files = new TreeMap<>();
        if (!Files.exists(directory)) return files;
        try (var walk = Files.walk(directory)) {
            for (Path path : walk.filter(Files::isRegularFile).toList()) {
                files.put(directory.relativize(path).toString().replace('\\', '/'), digest(path));
            }
        }
        return files;
    }

    static String digest(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(path));
            StringBuilder text = new StringBuilder();
            for (byte value : digest.digest()) text.append(String.format("%02x", value));
            return text.toString();
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }

    static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
        }
    }

    /**
     * Real Windows x64 full JDK 25. Tests never copy it: it is only ever passed
     * as a read-only path argument, so the development JDK is not duplicated.
     */
    static Path realJdk() throws IOException {
        Path home = Path.of(System.getProperty("java.home"));
        for (String tool : List.of("java", "javac", "keytool", "jarsigner")) {
            assertTrue(Files.isRegularFile(home.resolve("bin").resolve(tool + ".exe")),
                    "the test JVM is not a full JDK: " + home);
        }
        String release = Files.readString(home.resolve("release"), StandardCharsets.ISO_8859_1);
        assertTrue(release.contains("JAVA_VERSION=\"25"),
                "the build entry contract requires JDK 25 but the test JVM is: " + home + " (" + release + ")");
        assertTrue(release.contains("OS_ARCH=\"x86_64\""),
                "the build entry contract requires a Windows x64 JDK: " + home + " (" + release + ")");
        return home;
    }

    /**
     * A real JDK that is not version 25, used to prove the version check runs
     * real tools instead of trusting marker files. No fake java.exe is used.
     */
    static Path olderJdk() {
        List<Path> candidates = new ArrayList<>();
        String explicit = System.getenv("MODCORE_TEST_OLDER_JDK");
        if (explicit != null && !explicit.isBlank()) candidates.add(Path.of(explicit));
        for (String drive : List.of("E:\\", "D:\\", "C:\\")) {
            for (int version = 17; version <= 24; version++) candidates.add(Path.of(drive + "JDK" + version));
        }
        for (String root : List.of("C:\\Program Files\\Java", "C:\\Program Files\\Eclipse Adoptium",
                "C:\\Program Files\\Microsoft", "C:\\Program Files\\Zulu")) {
            Path directory = Path.of(root);
            if (!Files.isDirectory(directory)) continue;
            try (var entries = Files.list(directory)) {
                candidates.addAll(entries.filter(Files::isDirectory).toList());
            } catch (IOException ignored) {
                // A missing vendor directory simply contributes no candidates.
            }
        }
        for (Path candidate : candidates) {
            if (isOlderJdk(candidate)) return candidate;
        }
        throw new AssertionError("A real JDK 17-24 is required to prove the version check; "
                + "none was found (set MODCORE_TEST_OLDER_JDK to one)");
    }

    private static boolean isOlderJdk(Path home) {
        for (String tool : List.of("java", "javac", "keytool", "jarsigner")) {
            if (!Files.isRegularFile(home.resolve("bin").resolve(tool + ".exe"))) return false;
        }
        Path release = home.resolve("release");
        if (!Files.isRegularFile(release)) return false;
        try {
            String text = Files.readString(release, StandardCharsets.ISO_8859_1);
            Matcher matcher = Pattern.compile("JAVA_VERSION=\"(\\d+)").matcher(text);
            if (!matcher.find()) return false;
            int major = Integer.parseInt(matcher.group(1));
            return major >= 17 && major <= 24;
        } catch (IOException error) {
            return false;
        }
    }

    /**
     * Creates a real directory junction so the fixture preparation can be
     * tested against a reparse point. Junctions are created with mklink /J and
     * need no administrator rights.
     */
    static void createJunction(Path link, Path target) throws Exception {
        Path log = Files.createTempFile(link.getParent(), "junction-", ".log");
        ProcessBuilder builder = new ProcessBuilder(comspec(), "/d", "/c",
                "mklink /J \"" + link + "\" \"" + target + "\"")
                .directory(link.getParent().toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().put("PATH", systemPath());
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            fail("mklink /J timed out: " + link);
        }
        String output = Files.readString(log, nativeCharset());
        assertEquals(0, process.exitValue(), "could not create a directory junction: " + output);
    }

    static void markModified(Path path, FileTime time) throws IOException {
        Files.setLastModifiedTime(path, time);
    }

    static String lower(Result result) {
        return result.output().toLowerCase(java.util.Locale.ROOT);
    }

    static void assertMessage(Result result, String description, String... alternatives) {
        String output = lower(result);
        for (String alternative : alternatives) {
            if (output.contains(alternative.toLowerCase(java.util.Locale.ROOT))) return;
        }
        fail(description + ": " + result.output());
    }

    static String pathWithPowerShell() {
        String systemRoot = System.getenv("SystemRoot");
        return powerShellDirectory() + ";" + systemPath() + ";" + systemRoot;
    }

    static Path powerShellDirectory() {
        // Java cannot see the Microsoft Store pwsh.exe app-execution alias via
        // Files.exists, so ask where.exe for the real PATH lookup result.
        Path where = Path.of(System.getenv("SystemRoot"), "System32", "where.exe");
        if (Files.isRegularFile(where)) {
            try {
                Path log = Files.createTempFile("pwsh-where-", ".log");
                Process process = new ProcessBuilder(where.toString(), "pwsh")
                        .redirectErrorStream(true).redirectOutput(log.toFile()).start();
                process.getOutputStream().close();
                if (process.waitFor(15, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    for (String line : Files.readAllLines(log, nativeCharset())) {
                        if (line.isBlank()) continue;
                        Path parent = Path.of(line.trim()).getParent();
                        if (parent != null && Files.isDirectory(parent)) return parent;
                    }
                }
            } catch (Exception ignored) {
                // Fall through to the fixed install locations below.
            }
        }
        Path installed = Path.of("C:\\Program Files\\PowerShell\\7");
        if (Files.isRegularFile(installed.resolve("pwsh.exe"))) return installed;
        throw new AssertionError("PowerShell 7 (pwsh.exe) must be on PATH; Windows PowerShell 5.1 is not a fallback");
    }

    private static String systemPath() {
        return System.getenv("SystemRoot") + "\\System32";
    }

    private static String comspec() {
        Path command = Path.of(System.getenv("SystemRoot"), "System32", "cmd.exe");
        assertTrue(Files.isRegularFile(command), "cmd.exe is missing: " + command);
        return command.toString();
    }

    private static Charset nativeCharset() {
        return Charset.forName(System.getProperty("native.encoding", "UTF-8"));
    }
}
