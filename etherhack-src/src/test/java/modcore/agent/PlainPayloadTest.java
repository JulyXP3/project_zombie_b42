package modcore.agent;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PlainPayloadTest {
    @Test
    void ordinaryRuntimeJarReadsIndexedBytesWithoutDecryptionOrExtraction() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Map<String, byte[]> entries = entries();
        Path runtime = write(root, entries);
        PayloadArchive archive = assertDoesNotThrow(() -> readRuntime(runtime),
                "the new runtime format must load as an ordinary JAR");
        try (var in = archive.open("modcore/data.txt")) {
            assertEquals("plain resource", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (var files = Files.list(root)) { assertEquals(1, files.count()); }
    }

    @Test
    void ordinaryRuntimeRejectsTamperingAndUnlistedEntries() throws Exception {
        for (boolean extra : new boolean[]{false, true}) {
            Map<String, byte[]> entries = entries();
            entries.put(extra ? "modcore/unlisted.txt" : "modcore/data.txt", new byte[]{1});
            Path runtime = write(PayloadArchiveTest.sandbox(), entries);
            assertThrows(IOException.class, () -> readRuntime(runtime));
        }
    }

    @Test
    void ordinaryRuntimeCannotSupplyAgentGateCoordinatorOrGameClasses() throws Exception {
        for (String name : new String[]{"modcore/agent/AgentMain.class", "modcore/launch/LaunchGate.class",
                "modcore/attach/LaunchCoordinator.class", "zombie/GameWindow.class", "modcore/../escape"}) {
            Map<String, byte[]> entries = entries();
            entries.put(name, new byte[]{1});
            index(entries);
            Path runtime = write(PayloadArchiveTest.sandbox(), entries);
            assertThrows(IOException.class, () -> readRuntime(runtime), name);
        }
    }

    private static PayloadArchive readRuntime(Path runtime) throws Exception {
        var method = assertDoesNotThrow(() -> PayloadArchive.class.getMethod("readRuntime", Path.class),
                "the ordinary runtime reader must exist");
        try { return (PayloadArchive) method.invoke(null, runtime); }
        catch (java.lang.reflect.InvocationTargetException e) { throw (Exception) e.getCause(); }
    }

    static Map<String, byte[]> entries() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("modcore/data.txt", "plain resource".getBytes(StandardCharsets.UTF_8));
        index(entries);
        return entries;
    }

    static void index(Map<String, byte[]> entries) throws Exception {
        Properties props = new Properties();
        props.setProperty("format.version", "2");
        props.setProperty("game.sha256", "0".repeat(64));
        props.setProperty("main.class", "fixture.game.Main");
        for (var entry : entries.entrySet()) {
            if (!entry.getKey().equals(PayloadArchive.METADATA)) props.setProperty("entry." + entry.getKey(), PayloadArchive.digest(entry.getValue()));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        entries.put(PayloadArchive.METADATA, out.toByteArray());
    }

    static Path write(Path root, Map<String, byte[]> entries) throws Exception {
        Path runtime = root.resolve("runtime.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(runtime))) {
            for (var entry : entries.entrySet()) {
                out.putNextEntry(new JarEntry(entry.getKey()));
                out.write(entry.getValue());
                out.closeEntry();
            }
        }
        return runtime;
    }
}
