package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

class ThreeFileDeploymentTest {
    @Test
    void installerContainsAgentAndOfflineRuntime() throws Exception {
        try (JarFile installer = new JarFile(System.getProperty("agent.installerJar"))) {
            assertNotNull(installer.getJarEntry("modcore/payload.jar"));
            assertNotNull(installer.getJarEntry("modcore/agent.jar"), "three-file delivery must include its Agent");
        }
    }

    @Test
    void installLeavesTheExecutableConfigurationAloneAndUninstallRestoresExactBatchBytes() throws Exception {
        Fixture f = fixture();
        byte[] json = Files.readAllBytes(f.game.resolve("ProjectZomboid64.json"));
        byte[] batch = Files.readAllBytes(f.game.resolve("ProjectZomboid64.bat"));
        String original = PayloadArchive.digest(f.game.resolve("projectzomboid.jar"));
        Result install = run(f, "--install-agent", "--yes");
        assertEquals(0, install.exit, install.output);
        assertFalse(install.output.contains("Unavailable features"), install.output);
        assertTrue(install.output.contains("ProjectZomboid64.bat"), install.output);
        Path deploy = f.home.resolve("Zomboid/modcore-agent");
        assertTrue(Files.isRegularFile(deploy.resolve("agent.jar")));
        assertTrue(Files.isRegularFile(deploy.resolve("installation.properties")));
        assertTrue(Files.isRegularFile(f.home.resolve("Zomboid/modcore-agent/runtime.jar")));
        assertFalse(Files.exists(f.game.resolve("zombie")));
        assertArrayEquals(json, Files.readAllBytes(f.game.resolve("ProjectZomboid64.json")),
                "the exe configuration must stay byte-identical so the exe keeps starting the vanilla game");
        assertTrue(Files.readString(f.game.resolve("ProjectZomboid64.bat")).contains("modcore.attach.LaunchCoordinator"));
        Result repeat = run(f, "--install-agent", "--yes");
        assertEquals(0, repeat.exit, repeat.output);
        assertEquals(original, PayloadArchive.digest(f.game.resolve("projectzomboid.jar")));
        Result uninstall = run(f, "--uninstall-agent", "--yes");
        assertEquals(0, uninstall.exit, uninstall.output);
        assertArrayEquals(json, Files.readAllBytes(f.game.resolve("ProjectZomboid64.json")));
        assertArrayEquals(batch, Files.readAllBytes(f.game.resolve("ProjectZomboid64.bat")));
        assertFalse(Files.exists(f.home.resolve("Zomboid/modcore-agent/runtime.jar")));
        assertFalse(Files.exists(deploy.resolve("agent.jar")));
        assertEquals("keep", Files.readString(f.home.resolve("Zomboid/modcore/config/user.json")));
        assertEquals("save", Files.readString(f.home.resolve("Zomboid/Saves/sentinel.txt")));
        assertEquals("unrelated", Files.readString(f.game.resolve("unrelated.txt")));
    }

    @Test
    void unownedPayloadNoLongerBlocksInstallAndIsLeftAlone() throws Exception {
        Fixture f = fixture();
        Path marker = f.home.resolve("Zomboid/modcore.bin");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "must remain");
        byte[] json = Files.readAllBytes(f.game.resolve("ProjectZomboid64.json"));
        Result result = run(f, "--install-agent", "--yes");
        assertEquals(0, result.exit, result.output);
        assertEquals("must remain", Files.readString(marker), "an unowned file is never deleted");
        assertArrayEquals(json, Files.readAllBytes(f.game.resolve("ProjectZomboid64.json")));
        assertTrue(Files.readString(f.game.resolve("ProjectZomboid64.bat"))
                .contains("modcore.attach.LaunchCoordinator"), "the launch file is still installed");
    }

    @Test
    void existingZombieOverrideDirectoryIsNeverTouched() throws Exception {
        Fixture f = fixture();
        Path marker = f.game.resolve("zombie/GameWindow.class");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "must remain");
        byte[] json = Files.readAllBytes(f.game.resolve("ProjectZomboid64.json"));
        Result result = run(f, "--install-agent", "--yes");
        assertEquals("must remain", Files.readString(marker), "an existing override class is never deleted");
        assertArrayEquals(json, Files.readAllBytes(f.game.resolve("ProjectZomboid64.json")));
        assertTrue(result.output.contains("Install ModCore for:"),
                "the old override no longer blocks the install step: " + result.output);
    }

    @Test
    void uninstallRestoresTheOriginalStartupEvenIfItWasEditedAfterwards() throws Exception {
        Fixture f = fixture();
        byte[] original = Files.readAllBytes(f.game.resolve("ProjectZomboid64.bat"));
        Result install = run(f, "--install-agent", "--yes");
        assertEquals(0, install.exit, install.output);
        Path batch = f.game.resolve("ProjectZomboid64.bat");
        Files.writeString(batch, Files.readString(batch).replace("3072", "4096"));
        Result result = run(f, "--uninstall-agent", "--yes");
        assertEquals(0, result.exit, result.output);
        assertArrayEquals(original, Files.readAllBytes(batch), "the stored original backup is restored");
        assertFalse(Files.exists(f.home.resolve("Zomboid/modcore-agent/runtime.jar")));
    }

    @Test
    void nonAsciiPathsStillWorkBecauseOnlyTheBatchFileIsEdited() throws Exception {
        Fixture f = fixture("three files 测试");
        Result install = run(f, "--install-agent", "--yes");
        assertEquals(0, install.exit, install.output);
        assertTrue(Files.readString(f.game.resolve("ProjectZomboid64.bat")).contains("modcore.attach.LaunchCoordinator"));
        Result uninstall = run(f, "--uninstall-agent", "--yes");
        assertEquals(0, uninstall.exit, uninstall.output);
        assertFalse(Files.exists(f.home.resolve("Zomboid/modcore-agent/runtime.jar")));
    }

    @Test
    void absentConfirmationMakesNoChanges() throws Exception {
        Fixture f = fixture();
        byte[] batch = Files.readAllBytes(f.game.resolve("ProjectZomboid64.bat"));
        Result result = run(f, "--install-agent");
        assertNotEquals(0, result.exit, result.output);
        assertArrayEquals(batch, Files.readAllBytes(f.game.resolve("ProjectZomboid64.bat")));
        assertFalse(Files.exists(f.home.resolve("Zomboid/modcore-agent/runtime.jar")));
    }

    static Fixture fixture() throws Exception {
        return fixture("three file fixture");
    }

    static Fixture fixture(String folder) throws Exception {
        Path root = Files.createDirectories(PayloadArchiveTest.sandbox().resolve(folder));
        Path game = Files.createDirectories(root.resolve("game"));
        Path home = Files.createDirectories(root.resolve("home"));
        Files.copy(Path.of(System.getProperty("headshot.gameJar")), game.resolve("projectzomboid.jar"));
        Files.copy(Path.of(System.getProperty("agent.installerJar")), game.resolve("modcore-test.jar"));
        Files.writeString(game.resolve("ProjectZomboid64.json"), """
                {
                  "mainClass": "zombie/gameStates/MainScreenState",
                  "classpath": [".", "projectzomboid.jar"],
                  "vmArgs": ["-Xmx3072m", "--enable-native-access=ALL-UNNAMED"]
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(game.resolve("ProjectZomboid64.bat"), LaunchConfigurationTest.BATCH, StandardCharsets.US_ASCII);
        Files.writeString(game.resolve("unrelated.txt"), "unrelated");
        Files.createDirectories(home.resolve("Zomboid/modcore/config"));
        Files.writeString(home.resolve("Zomboid/modcore/config/user.json"), "keep");
        Files.createDirectories(home.resolve("Zomboid/Saves"));
        Files.writeString(home.resolve("Zomboid/Saves/sentinel.txt"), "save");
        return new Fixture(root, game, home);
    }

    static Result run(Fixture f, String... options) throws Exception {
        Path log = Files.createTempFile(f.root, "install-", ".log");
        List<String> args = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin/java.exe").toString(),
                "-Duser.home=" + f.home, "-Djava.io.tmpdir=" + f.root, "-jar", f.game.resolve("modcore-test.jar").toString()));
        args.addAll(List.of(options));
        args.addAll(List.of("--game-dir", f.game.toString(), "--user-home", f.home.toString()));
        ProcessBuilder builder = new ProcessBuilder(args).directory(f.game.toFile()).redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().put("LOCALAPPDATA", f.root.resolve("appdata").toString());
        builder.environment().put("USERPROFILE", f.home.toString());
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(90, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
            fail("Deployment timed out: " + new String(Files.readAllBytes(log),
                    java.nio.charset.Charset.forName(System.getProperty("native.encoding", "UTF-8"))));
        }
        return new Result(process.exitValue(), new String(Files.readAllBytes(log),
                java.nio.charset.Charset.forName(System.getProperty("native.encoding", "UTF-8"))));
    }

    record Fixture(Path root, Path game, Path home) {}
    record Result(int exit, String output) {}
}
