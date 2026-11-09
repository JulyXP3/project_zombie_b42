package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class AgentPackagingTest {
    @Test
    void agentContainsOnlyStartupInfrastructureAndNoTestsOrAsm() throws Exception {
        Path file = Path.of(System.getProperty("agent.jarFile"));
        assertTrue(Files.isRegularFile(file), "the separate agent artifact must be built");
        try (JarFile jar = new JarFile(file.toFile())) {
            assertEquals("modcore.agent.AgentMain", jar.getManifest().getMainAttributes().getValue("Agent-Class"));
            assertNull(jar.getManifest().getMainAttributes().getValue("Premain-Class"));
            assertNull(jar.getManifest().getMainAttributes().getValue("Main-Class"));
            assertNull(jar.getEntry("modcore/agent/Launcher.class"));
            assertNull(jar.getEntry("modcore/agent/PayloadClassLoader.class"));
            assertNotNull(jar.getEntry("modcore/agent/AgentMain.class"));
            for (var entry : jar.stream().toList()) {
                if (!entry.getName().endsWith(".class")) continue;
                assertTrue(entry.getName().startsWith("modcore/agent/"), entry.getName());
                assertFalse(entry.getName().contains("Test"), entry.getName());
            }
        }
    }

    @Test
    void runtimeArchiveExcludesInstallerAgentAsmAndLegacyBootstrap() throws Exception {
        try (JarFile jar = new JarFile(System.getProperty("agent.payloadJar"))) {
            assertNotNull(jar.getEntry("modcore/core/RuntimeBootstrap.class"));
            assertNotNull(jar.getEntry("modcore/core/CoreMain.class"));
            assertNotNull(jar.getEntry("modcore/resources.index"));
            assertNotNull(jar.getEntry("modcore/symbols.txt"));
            assertNull(jar.getEntry("modcore/GamePatcher.class"));
            assertNull(jar.getEntry("modcore/Main.class"));
            assertNull(jar.getEntry("modcore/utils/Patch.class"));
            for (var entry : jar.stream().toList()) {
                String name = entry.getName();
                assertFalse(name.startsWith("zombie/") || name.startsWith("org/objectweb/asm/")
                        || name.startsWith("modcore/agent/") || name.startsWith("modcore/build/")
                        || name.endsWith("coreboot.class"), name);
            }
        }
    }
}
