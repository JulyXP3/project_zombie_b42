package modcore.agent;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class InstallationMigrationTest {
    @Test
    void legacyRecordUsesLegacyBatchRulesThenMigratesToOwnedComponents() throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        Path deploy = Files.createDirectories(f.home().resolve("Zomboid/modcore-agent"));
        byte[] original = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        byte[] installed = modcore.build.LaunchConfiguration.editLegacyBatch(original);
        byte[] oldAgent = "old owned agent".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] oldPayload = "old owned encrypted payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(f.game().resolve("ProjectZomboid64.bat"), installed);
        Files.write(deploy.resolve("ProjectZomboid64.bat.original"), original);
        Files.write(deploy.resolve("agent.jar"), oldAgent);
        Files.write(f.home().resolve("Zomboid/modcore.bin"), oldPayload);
        Properties p = new Properties();
        p.setProperty("format", "1");
        p.setProperty("game", f.game().toString());
        p.setProperty("home", f.home().toString());
        p.setProperty("game.sha256", PayloadArchive.digest(f.game().resolve("projectzomboid.jar")));
        p.setProperty("agent.sha256", PayloadArchive.digest(oldAgent));
        p.setProperty("payload.sha256", PayloadArchive.digest(oldPayload));
        p.setProperty("original.ProjectZomboid64.bat", PayloadArchive.digest(original));
        p.setProperty("installed.ProjectZomboid64.bat", PayloadArchive.digest(installed));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.store(out, null);
        Files.write(deploy.resolve("installation.properties"), out.toByteArray());
        var install = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
        assertEquals(0, install.exit(), install.output());
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore.bin")));
        assertTrue(Files.isRegularFile(deploy.resolve("runtime.jar")));
        assertArrayEquals(original, Files.readAllBytes(deploy.resolve("ProjectZomboid64.bat.original")));
        var uninstall = ThreeFileDeploymentTest.run(f, "--uninstall-agent", "--yes");
        assertEquals(0, uninstall.exit(), uninstall.output());
        assertArrayEquals(original, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
    }

    @Test
    void oldRecordLeavesAnUnhashedInstallerInPlace() throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        Path deploy = Files.createDirectories(f.home().resolve("Zomboid/modcore-agent"));
        byte[] original = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        byte[] installed = modcore.build.LaunchConfiguration.editLegacyBatch(original);
        byte[] bytes = new byte[]{1, 2, 3};
        Files.write(f.game().resolve("ProjectZomboid64.bat"), installed);
        Files.write(deploy.resolve("ProjectZomboid64.bat.original"), original);
        Files.write(deploy.resolve("agent.jar"), bytes);
        Files.write(f.home().resolve("Zomboid/modcore.bin"), bytes);
        Files.writeString(deploy.resolve("installer.jar"), "unowned preserve me");
        Properties record = new Properties();
        record.setProperty("format", "1"); record.setProperty("game", f.game().toString()); record.setProperty("home", f.home().toString());
        record.setProperty("game.sha256", PayloadArchive.digest(f.game().resolve("projectzomboid.jar")));
        record.setProperty("agent.sha256", PayloadArchive.digest(bytes)); record.setProperty("payload.sha256", PayloadArchive.digest(bytes));
        record.setProperty("original.ProjectZomboid64.bat", PayloadArchive.digest(original));
        record.setProperty("installed.ProjectZomboid64.bat", PayloadArchive.digest(installed));
        try (var out = Files.newOutputStream(deploy.resolve("installation.properties"))) { record.store(out, null); }
        var result = ThreeFileDeploymentTest.run(f, "--uninstall-agent", "--yes");
        assertEquals(0, result.exit(), result.output());
        assertEquals("unowned preserve me", Files.readString(deploy.resolve("installer.jar")),
                "a file the record does not own is never deleted");
        assertArrayEquals(original, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore.bin")), "owned legacy files are removed");
    }

    @Test
    void heldDeploymentLockPreventsInstallation() throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        Path file = f.home().resolve("Zomboid/modcore-deployment.lock");
        byte[] original = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        try (var channel = java.nio.channels.FileChannel.open(file, java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.READ, java.nio.file.StandardOpenOption.WRITE);
             var lock = channel.lock(0, Long.MAX_VALUE, true)) {
            var result = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
            assertNotEquals(0, result.exit(), result.output());
            assertTrue(result.output().contains("locked"), result.output());
            assertArrayEquals(original, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
        }
    }

    @Test
    void existingSessionStateNoLongerBlocksDeployment() throws Exception {
        for (boolean live : new boolean[]{false, true}) {
            var f = ThreeFileDeploymentTest.fixture();
            Path session = Files.createDirectories(f.home().resolve("Zomboid/modcore-sessions/session-test"));
            String state = live ? "pid=" + ProcessHandle.current().pid() + "\nstarted="
                    + ProcessHandle.current().info().startInstant().orElseThrow().toEpochMilli() + "\n" : "pid=broken\n";
            Files.writeString(session.resolve("state.properties"), state);
            var install = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
            assertEquals(0, install.exit(), install.output());
            assertTrue(Files.isRegularFile(f.home().resolve("Zomboid/modcore-agent/installation.properties")));
        }
    }
}
