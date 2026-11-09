package modcore.agent;

import modcore.core.LuaTranslator;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceLoadingTest {
    @Test
    void translationsLoadWithoutAnUnpackedTranslationDirectory() {
        assertFalse(Files.exists(Path.of("modcore/translations")));
        LuaTranslator translator = new LuaTranslator();
        translator.loadTranslations();
        assertEquals("Info", translator.getTranslate("UI_Nav_Info"));
        assertEquals("missing-test-key", translator.getTranslate("missing-test-key"));
        assertFalse(Files.exists(Path.of("modcore/translations")));
    }

    @Test
    void resourceIndexEnumeratesTranslationsAndImageBytesWithoutAFilePath() throws Exception {
        Class<?> resources = assertDoesNotThrow(() -> Class.forName("modcore.utils.RuntimeResources"));
        @SuppressWarnings("unchecked") List<String> names = (List<String>) resources.getMethod("list", String.class)
                .invoke(null, "modcore/translations/");
        assertEquals(List.of("modcore/translations/CN.txt", "modcore/translations/EN.txt", "modcore/translations/RU.txt"), names);
        try (InputStream in = (InputStream) resources.getMethod("open", String.class)
                .invoke(null, "modcore/media/ui/info.png")) {
            assertNotNull(in);
            byte[] header = in.readNBytes(8);
            assertArrayEquals(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}, header);
        }
        assertFalse(Files.exists(Path.of("modcore/media")));
    }

    @Test
    void luaMissingResourceDoesNotOpenAnUnrelatedDeploymentJar() throws Exception {
        Path file = Path.of("modcore-fixture-missing-resource.jar");
        try (var zip = new java.util.jar.JarOutputStream(Files.newOutputStream(file))) {
            zip.putNextEntry(new java.util.jar.JarEntry("modcore/missing-test-resource.txt"));
            zip.write("unrelated".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        var lookup = java.lang.invoke.MethodHandles.privateLookupIn(modcore.core.LuaLoader.class,
                java.lang.invoke.MethodHandles.lookup());
        var open = lookup.findStatic(modcore.core.LuaLoader.class, "openResource",
                java.lang.invoke.MethodType.methodType(InputStream.class, String.class));
        try {
            Object result;
            try { result = open.invoke("modcore/missing-test-resource.txt"); }
            catch (Throwable failure) { throw new AssertionError(failure); }
            if (result instanceof InputStream in) in.close();
            assertNull(result, "missing resources must not be taken from a scanned CWD JAR");
        } finally {
            try {
                var handle = lookup.findStaticVarHandle(modcore.core.LuaLoader.class, "sourceJar", java.util.jar.JarFile.class);
                var cached = (java.util.jar.JarFile) handle.get();
                if (cached != null) cached.close();
                handle.set(null);
            } catch (NoSuchFieldException ignored) {
            }
            Files.delete(file);
        }
    }

    @Test
    void bootstrapPreflightReadsRequiredResourcesWithoutCreatingDirectories() throws Exception {
        Class<?> bootstrap = assertDoesNotThrow(() -> Class.forName("modcore.core.RuntimeBootstrap"));
        assertDoesNotThrow(() -> bootstrap.getMethod("boot").invoke(null));
        assertFalse(Files.exists(Path.of("modcore/lua")));
        assertFalse(Files.exists(Path.of("modcore/symbols.txt")));
    }
}
