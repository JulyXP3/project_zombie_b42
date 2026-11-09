package modcore.agent;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PayloadArchiveTest {
    @Test
    void authenticatedResourcesAreReadWithoutExtractingFiles() throws Exception {
        Path root = sandbox();
        Map<String, byte[]> entries = entries();
        entries.put("modcore/translations/CH.txt", "UI_Test=测试\n".getBytes(StandardCharsets.UTF_8));
        Path bin = root.resolve("modcore.bin");
        Files.write(bin, encrypt(entries));

        Object archive = read(bin);
        try (InputStream in = (InputStream) archive.getClass().getMethod("open", String.class)
                .invoke(archive, "modcore/translations/CH.txt")) {
            assertNotNull(in);
            assertEquals("UI_Test=测试\n", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(java.util.List.of("modcore/translations/CH.txt"), archive.getClass()
                .getMethod("resourceNames", String.class).invoke(archive, "modcore/translations/"));
        try (var files = Files.walk(root)) {
            assertEquals(java.util.List.of(bin), files.filter(Files::isRegularFile).toList());
        }
    }

    @Test
    void corruptionAndTruncationFailBeforeResourcesAreAvailable() throws Exception {
        Path root = sandbox();
        byte[] envelope = encrypt(entries());
        envelope[envelope.length - 1] ^= 1;
        Path corrupt = root.resolve("corrupt.bin");
        Files.write(corrupt, envelope);
        assertInstanceOf(java.io.IOException.class, failure(corrupt));
        Path shortFile = root.resolve("short.bin");
        Files.write(shortFile, Arrays.copyOf(envelope, 43));
        assertInstanceOf(java.io.IOException.class, failure(shortFile));
    }

    @Test
    void payloadCannotSupplyGameOrAgentClassesOrTraversalPaths() throws Exception {
        for (String path : java.util.List.of("zombie/GameWindow.class", "modcore/agent/AgentMain.class",
                "modcore/build/AgentLoadout.class", "modcore/../outside.txt", "modcore\\outside.txt", "/modcore/outside.txt")) {
            Map<String, byte[]> entries = entries();
            entries.put(path, new byte[]{1, 2, 3});
            Path bin = sandbox().resolve("modcore.bin");
            Files.write(bin, encrypt(entries));
            assertInstanceOf(java.io.IOException.class, failure(bin), path);
        }
    }

    @Test
    void legacyPayloadWithoutPatchMetadataIsRejectedExplicitly() throws Exception {
        Path bin = sandbox().resolve("legacy.bin");
        Files.write(bin, encrypt(Map.of("modcore/test.txt", new byte[]{1})));
        Throwable error = failure(bin);
        assertInstanceOf(java.io.IOException.class, error);
        assertTrue(error.getMessage().contains("metadata"), error.toString());
    }

    @Test
    void patchBytesRequireMatchingOriginalDigestMetadata() throws Exception {
        Map<String, byte[]> entries = entries();
        entries.put("META-INF/modcore-patches/fixture/game/Main.class", new byte[]{1});
        Path bin = sandbox().resolve("modcore.bin");
        Files.write(bin, encrypt(entries));
        assertInstanceOf(java.io.IOException.class, failure(bin));
    }

    @Test
    void realApplicationRequiresCompleteIndexedResourcesBeforeClassLoading() throws Exception {
        Map<String, byte[]> entries = entries();
        entries.put(PayloadArchive.METADATA, ("format.version=1\n"
                + "game.sha256=" + "0".repeat(64) + "\n"
                + "main.class=zombie.gameStates.MainScreenState\n").getBytes(StandardCharsets.UTF_8));
        Path bin = sandbox().resolve("missing-resources.bin");
        Files.write(bin, encrypt(entries));
        Throwable missing = failure(bin);
        assertInstanceOf(java.io.IOException.class, missing);
        assertTrue(missing.getMessage().contains("resource"), missing.toString());

        entries.put("modcore/modcore.properties", "version='fixture'\n".getBytes(StandardCharsets.UTF_8));
        entries.put("modcore/lua-prefix.properties", "lua.prefix=ABCD\n".getBytes(StandardCharsets.UTF_8));
        entries.put("modcore/symbols.txt", "EtherMain\n".getBytes(StandardCharsets.UTF_8));
        entries.put("modcore/resources.index", "modcore/lua/missing.lua\n".getBytes(StandardCharsets.UTF_8));
        entries.put("modcore/core/RuntimeBootstrap.class", new byte[]{1});
        Files.write(bin, encrypt(entries));
        Throwable absentEntry = failure(bin);
        assertInstanceOf(java.io.IOException.class, absentEntry);
        assertTrue(absentEntry.getMessage().contains("resource"), absentEntry.toString());
    }

    static Path sandbox() throws Exception {
        Path root = Path.of(System.getProperty("agent.testRoot"));
        Files.createDirectories(root);
        return Files.createTempDirectory(root, "archive-");
    }

    static Map<String, byte[]> entries() {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/modcore-agent.properties", ("format.version=1\n"
                + "game.sha256=" + "0".repeat(64) + "\n"
                + "main.class=fixture.game.Main\n").getBytes(StandardCharsets.UTF_8));
        return entries;
    }

    static byte[] encrypt(Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        byte[] key = new byte[32];
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(key);
        new java.security.SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        ByteArrayOutputStream envelope = new ByteArrayOutputStream();
        envelope.write(key);
        envelope.write(iv);
        envelope.write(cipher.doFinal(bytes.toByteArray()));
        return envelope.toByteArray();
    }

    private static Object read(Path bin) throws Exception {
        Class<?> type = assertDoesNotThrow(() -> Class.forName("modcore.agent.PayloadArchive"),
                "the production payload reader must exist");
        return type.getMethod("read", Path.class).invoke(null, bin);
    }

    private static Throwable failure(Path bin) throws Exception {
        InvocationTargetException error = assertThrows(InvocationTargetException.class, () -> read(bin));
        return error.getCause();
    }
}
