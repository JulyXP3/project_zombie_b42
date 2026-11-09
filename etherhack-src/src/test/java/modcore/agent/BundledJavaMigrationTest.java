package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class BundledJavaMigrationTest {
    private static final String INSTALLED = "@setlocal enableextensions disableDelayedExpansion\r\n@cd /d \"%~dp0\"\r\nSET _JAVA_OPTIONS=\r\n"
            + "SET PZ_CLASSPATH=./;projectzomboid.jar\r\n"
            + "\"%USERPROFILE%\\Zomboid\\modcore-agent\\attach-java\\bin\\java.exe\" -cp \"%USERPROFILE%\\Zomboid\\modcore-agent\\coordinator.jar\" modcore.attach.LaunchCoordinator --game-dir \"%~dp0.\" --user-home \"%USERPROFILE%\" -- -Xmx3072m -Djava.library.path=./win64/;./ -cp \"%PZ_CLASSPATH%\" zombie.gameStates.MainScreenState %*\r\n"
            + "exit /b %ERRORLEVEL%\r\nPAUSE\r\n";

    @Test
    void bundledDeploymentMigratesWithoutJavaAndCanBeInstalledAgainAndRemoved() throws Exception {
        var f = fixture();
        Path deploy = f.home().resolve("Zomboid/modcore-agent");
        byte[] original = Files.readAllBytes(deploy.resolve("ProjectZomboid64.bat.original"));
        var upgrade = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
        assertEquals(0, upgrade.exit(), upgrade.output());
        assertFalse(Files.exists(deploy.resolve("attach-java")), "owned old Java files and empty directories must be removed");
        Properties record = new Properties();
        try (var in = Files.newInputStream(deploy.resolve("installation.properties"))) { record.load(in); }
        assertEquals("3", record.getProperty("format"));
        assertTrue(record.stringPropertyNames().stream().noneMatch(name -> name.startsWith("file.attach-java/")));
        assertFalse(Files.readString(f.game().resolve("ProjectZomboid64.bat")).contains("attach-java"));
        var repeat = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
        assertEquals(0, repeat.exit(), repeat.output());
        var uninstall = ThreeFileDeploymentTest.run(f, "--uninstall-agent", "--yes");
        assertEquals(0, uninstall.exit(), uninstall.output());
        assertArrayEquals(original, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
        assertEquals("keep", Files.readString(f.home().resolve("Zomboid/modcore/config/user.json")));
    }

    @Test
    void changedOrUnregisteredOldJavaNoLongerBlocksTheUpgrade() throws Exception {
        for (boolean changed : List.of(false, true)) {
            var f = fixture();
            Path deploy = f.home().resolve("Zomboid/modcore-agent");
            Path file = deploy.resolve(changed ? "attach-java/bin/java.exe" : "attach-java/keep.txt");
            Files.writeString(file, "preserve me");
            var result = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
            assertEquals(0, result.exit(), result.output());
            if (changed) {
                assertFalse(Files.exists(deploy.resolve("attach-java/bin/java.exe")),
                        "the old Java files this installation owns are removed by the upgrade");
            } else {
                assertEquals("preserve me", Files.readString(file), "a file the record does not own is never deleted");
            }
            assertTrue(Files.readString(f.game().resolve("ProjectZomboid64.bat")).contains("LaunchCoordinator"));
        }
    }

    @Test
    void busyOldJavaDirectoryRollsBackTheUpgrade() throws Exception {
        var f = fixture();
        Path deploy = f.home().resolve("Zomboid/modcore-agent");
        byte[] originalRecord = Files.readAllBytes(deploy.resolve("installation.properties"));
        byte[] installedBatch = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        var before = new java.util.LinkedHashMap<Path, byte[]>();
        try (var files = Files.walk(deploy)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) before.put(file, Files.readAllBytes(file));
        }
        Process holder = new ProcessBuilder("cmd.exe", "/d", "/c", "pause")
                .directory(deploy.resolve("attach-java").toFile()).redirectErrorStream(true)
                .redirectOutput(f.root().resolve("holder.log").toFile()).start();
        try {
            assertTrue(holder.isAlive());
            var result = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
            assertTrue(holder.isAlive(), "the directory must stay in use throughout the upgrade");
            assertNotEquals(0, result.exit(), result.output());
            assertArrayEquals(originalRecord, Files.readAllBytes(deploy.resolve("installation.properties")));
            assertArrayEquals(installedBatch, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
            for (var file : before.entrySet()) assertArrayEquals(file.getValue(), Files.readAllBytes(file.getKey()), file.getKey().toString());
        } finally {
            holder.getOutputStream().close();
            if (!holder.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) holder.destroyForcibly();
            holder.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    @Test
    void bundledRecordStillUninstallsWithItsOriginalBatchRules() throws Exception {
        var f = fixture();
        var result = ThreeFileDeploymentTest.run(f, "--uninstall-agent", "--yes");
        assertEquals(0, result.exit(), result.output());
        assertEquals(LaunchConfigurationTest.BATCH, Files.readString(f.game().resolve("ProjectZomboid64.bat")));
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore-agent")));
    }

    private static ThreeFileDeploymentTest.Fixture fixture() throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        Path deploy = Files.createDirectories(f.home().resolve("Zomboid/modcore-agent"));
        byte[] original = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        byte[] installed = INSTALLED.getBytes(StandardCharsets.US_ASCII);
        Files.write(deploy.resolve("ProjectZomboid64.bat.original"), original);
        Files.write(f.game().resolve("ProjectZomboid64.bat"), installed);
        Properties record = new Properties();
        record.setProperty("format", "2");
        record.setProperty("game", f.game().toString());
        record.setProperty("home", f.home().toString());
        record.setProperty("game.sha256", PayloadArchive.digest(f.game().resolve("projectzomboid.jar")));
        record.setProperty("original.ProjectZomboid64.bat", PayloadArchive.digest(original));
        record.setProperty("installed.ProjectZomboid64.bat", PayloadArchive.digest(installed));
        for (String name : List.of("agent.jar", "runtime.jar", "coordinator.jar", "launch-gate.jar", "installer.jar",
                "attach-java/bin/java.exe", "attach-java/lib/modules", "attach-java/distribution.properties",
                "attach-java/legal/java.base/LICENSE")) {
            Path file = deploy.resolve(name);
            Files.createDirectories(file.getParent());
            Files.writeString(file, "owned old component: " + name);
            record.setProperty("file." + name, PayloadArchive.digest(file));
        }
        try (var out = Files.newOutputStream(deploy.resolve("installation.properties"))) { record.store(out, null); }
        return f;
    }
}
