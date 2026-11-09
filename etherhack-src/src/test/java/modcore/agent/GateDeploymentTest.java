package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Properties;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class GateDeploymentTest {
    @Test
    void threeFileInstallerContainsGateAndCoordinatorWithoutBundledJava() throws Exception {
        try (var jar = new JarFile(System.getProperty("agent.installerJar"))) {
            for (String name : new String[]{"modcore/launch-gate.jar", "modcore/coordinator.jar"}) {
                assertNotNull(jar.getJarEntry(name), name);
            }
            assertNull(jar.getJarEntry("modcore/attach-java.zip"), "the user supplies JDK 25");
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().endsWith("/java.exe")));
        }
    }

    @Test
    void newInstallationUsesOrdinaryRuntimeAndPreservesOriginalClasspath() throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        var result = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
        assertEquals(0, result.exit(), result.output());
        var directory = f.home().resolve("Zomboid/modcore-agent");
        assertTrue(Files.isRegularFile(directory.resolve("runtime.jar")));
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore.bin")));
        assertFalse(PayloadArchive.readRuntime(directory.resolve("runtime.jar")).patchNames().isEmpty());
        Properties record = new Properties();
        try (var in = Files.newInputStream(directory.resolve("installation.properties"))) { record.load(in); }
        assertFalse(Files.exists(directory.resolve("attach-java")), "new deployments must not contain a Java runtime");
        assertEquals("3", record.getProperty("format"));
        assertTrue(record.stringPropertyNames().stream().noneMatch(name -> name.startsWith("file.attach-java/")));
        assertNotNull(record.getProperty("file.installer.jar"));
        String batch = Files.readString(f.game().resolve("ProjectZomboid64.bat"));
        assertTrue(batch.contains("PZ_CLASSPATH=./;projectzomboid.jar"), batch);
        assertTrue(batch.contains("modcore.attach.LaunchCoordinator"), batch);
        assertFalse(batch.contains("-javaagent:"), batch);
        Files.writeString(directory.resolve("coordinator.jar"), "changed");
        var uninstall = ThreeFileDeploymentTest.run(f, "--uninstall-agent", "--yes");
        assertEquals(0, uninstall.exit(), uninstall.output());
        assertFalse(Files.readString(f.game().resolve("ProjectZomboid64.bat")).contains("LaunchCoordinator"),
                "uninstall restores the original launch file even if a component was changed");
    }
}
