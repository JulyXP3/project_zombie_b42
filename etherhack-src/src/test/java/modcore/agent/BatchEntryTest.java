package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BatchEntryTest {
    @Test
    void buildUsesTheConfiguredExternalJdkAndPropagatesFailingGradleExitFromAnyWorkingDirectory() throws Exception {
        BuildEntryHarness.Fixture fixture = BuildEntryHarness.fixture("batch entry", 17);
        Path jdk = BuildEntryHarness.realJdk();
        Map<String, String> before = BuildEntryHarness.snapshot(fixture.game());

        BuildEntryHarness.Result result = BuildEntryHarness.run(fixture, "build.bat", Map.of(
                BuildEntryHarness.JAVA_OVERRIDE, jdk.toString(),
                BuildEntryHarness.GAME_OVERRIDE, fixture.game().toString()));

        assertEquals(17, result.exit(), result.output());
        Map<String, String> call = BuildEntryHarness.recording(fixture.source());
        assertEquals(jdk.toString(), call.get("JAVA_HOME"), result.output());
        assertNotEquals(fixture.game().resolve("jre64").toString(), call.get("JAVA_HOME"),
                "the game runtime must not be the build JDK");
        assertTrue(call.get("GRADLE_ARGS").contains("test") && call.get("GRADLE_ARGS").contains("assemble"),
                call.toString());
        assertFalse(call.get("GRADLE_ARGS").contains("-x"), call.toString());
        assertFalse(call.get("GRADLE_ARGS").contains("-PskipDeploymentTests"),
                "the default build must run every test: " + call);
        assertTrue(BuildEntryHarness.managedFixture(call).startsWith(fixture.root().resolve("temp")),
                "the full build prepares its own native fixture: " + call);
        assertEquals(before, BuildEntryHarness.snapshot(fixture.game()), "the game folder stays read-only");
    }

    @Test
    void buildFailsClearlyWhenTheGameRuntimeOrSourceIsUnavailable() throws Exception {
        BuildEntryHarness.Fixture fixture = BuildEntryHarness.fixture("batch missing");
        Files.delete(fixture.game().resolve("jre64/lib/modules"));

        BuildEntryHarness.Result result = BuildEntryHarness.run(fixture, "build.bat",
                BuildEntryHarness.environmentWithJavaHomeFallback(fixture));
        assertNotEquals(0, result.exit(), result.output());
        BuildEntryHarness.assertGradleNotInvoked(fixture.source());
        BuildEntryHarness.assertMessage(result, "a missing game jre64 must be named", "jre64");

        Files.copy(fixture.source().resolve("build.bat"), fixture.game().resolve("build.bat"));
        BuildEntryHarness.Fixture gameOnly = new BuildEntryHarness.Fixture(
                fixture.root(), fixture.game(), fixture.game());
        BuildEntryHarness.Result missingSource = BuildEntryHarness.run(gameOnly, "build.bat", Map.of());
        assertNotEquals(0, missingSource.exit(), missingSource.output());
        BuildEntryHarness.assertMessage(missingSource, "a game-only folder must explain the source checkout", "source");
    }

    @Test
    void threeFileBatchDeliveryUsesExternalJdkAndRestoresStartup() throws Exception {
        String configured = System.getenv("MODCORE_NATIVE_FIXTURE");
        org.junit.jupiter.api.Assumptions.assumeTrue(configured != null, "native runtime fixture was not provided");
        ThreeFileDeploymentTest.Fixture f = ThreeFileDeploymentTest.fixture();
        Path nativeRoot = Path.of(configured).toRealPath();
        assertTrue(nativeRoot.startsWith(Path.of(System.getProperty("agent.testRoot")).getParent().toRealPath()));
        copyTree(nativeRoot.resolve("jre64"), f.game().resolve("jre64"));
        for (String name : List.of("install.bat", "uninstall.bat")) Files.copy(sourceRoot().resolve(name), f.game().resolve(name));
        byte[] json = Files.readAllBytes(f.game().resolve("ProjectZomboid64.json"));
        byte[] bat = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        Result install = batch(f.root(), f.game(), "call install.bat", f.home());
        assertEquals(0, install.exit, install.output);
        assertTrue(Files.isRegularFile(f.home().resolve("Zomboid/modcore-agent/runtime.jar")));
        Result verify = batch(f.root(), f.game(), "call ProjectZomboid64.bat -nosteam", f.home(), true);
        assertEquals(0, verify.exit, verify.output);
        assertTrue(verify.output.contains("runtime classes linked"), verify.output);
        assertFalse(Files.exists(f.game().resolve("modcore-test.jar")),
                "a successful install deletes the installer jar next to the scripts");
        Result uninstall = batch(f.root(), f.game(), "call uninstall.bat", f.home());
        assertEquals(0, uninstall.exit, uninstall.output);
        assertArrayEquals(json, Files.readAllBytes(f.game().resolve("ProjectZomboid64.json")));
        assertArrayEquals(bat, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore-agent/installer.jar")),
                "the stored installer is removed together with the rest of the deployment");
        try (var entries = Files.list(f.home().resolve("Zomboid"))) {
            assertTrue(entries.noneMatch(path -> path.getFileName().toString().startsWith("modcore-install-")),
                    "uninstall leaves no modcore-install-* folder behind");
        }
    }

    @Test
    void runningTheInstallScriptAgainWithoutAJarUninstalls() throws Exception {
        String configured = System.getenv("MODCORE_NATIVE_FIXTURE");
        org.junit.jupiter.api.Assumptions.assumeTrue(configured != null, "native runtime fixture was not provided");
        ThreeFileDeploymentTest.Fixture f = ThreeFileDeploymentTest.fixture();
        Path nativeRoot = Path.of(configured).toRealPath();
        copyTree(nativeRoot.resolve("jre64"), f.game().resolve("jre64"));
        for (String name : List.of("install.bat", "uninstall.bat")) Files.copy(sourceRoot().resolve(name), f.game().resolve(name));
        byte[] bat = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        assertEquals(0, batch(f.root(), f.game(), "call install.bat", f.home()).exit,
                "first run installs");
        assertFalse(Files.exists(f.game().resolve("modcore-test.jar")));
        // 再装一次（安装包已被删掉）：这次应当走卸载流程。
        assertEquals(0, batch(f.root(), f.game(), "call install.bat", f.home()).exit,
                "a run without an installer jar uninstalls");
        assertArrayEquals(bat, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore-agent")));
        try (var entries = Files.list(f.home().resolve("Zomboid"))) {
            assertTrue(entries.noneMatch(path -> path.getFileName().toString().startsWith("modcore-install-")),
                    "no staging folder survives the uninstall flow");
        }
    }

    @Test
    void repeatedInstallsKeepOnlyOneStagingFolder() throws Exception {
        String configured = System.getenv("MODCORE_NATIVE_FIXTURE");
        org.junit.jupiter.api.Assumptions.assumeTrue(configured != null, "native runtime fixture was not provided");
        ThreeFileDeploymentTest.Fixture f = ThreeFileDeploymentTest.fixture();
        Path nativeRoot = Path.of(configured).toRealPath();
        copyTree(nativeRoot.resolve("jre64"), f.game().resolve("jre64"));
        for (String name : List.of("install.bat", "uninstall.bat")) Files.copy(sourceRoot().resolve(name), f.game().resolve(name));
        Path jar = f.game().resolve("modcore-test.jar");
        byte[] installer = Files.readAllBytes(jar);
        for (int round = 0; round < 3; round++) {
            Files.write(jar, installer);
            assertEquals(0, batch(f.root(), f.game(), "call install.bat", f.home()).exit, "install round " + round);
        }
        try (var entries = Files.list(f.home().resolve("Zomboid"))) {
            assertEquals(1, entries.filter(path -> path.getFileName().toString().startsWith("modcore-install-")).count(),
                    "only the newest modcore-install-* folder is kept");
        }
        assertEquals(0, batch(f.root(), f.game(), "call uninstall.bat", f.home()).exit, "uninstall");
    }

    @Test
    void batchUsesTheHighestInstallerVersionWhenSeveralExist() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path game = Files.createDirectories(root.resolve("game"));
        Files.writeString(game.resolve("projectzomboid.jar"), "original");
        Files.writeString(game.resolve("modcore-3.2.6-B42.jar"), "older");
        Files.writeString(game.resolve("modcore-3.2.7-B42.jar"), "newer");
        Files.copy(sourceRoot().resolve("install.bat"), game.resolve("install.bat"));
        Result result = batch(root, game, "call install.bat --yes", null);
        assertTrue(result.output.contains("modcore-3.2.7-B42.jar"), result.output);
        assertFalse(result.output.contains("Using installer: " + game.resolve("modcore-3.2.6-B42.jar")), result.output);
        assertEquals("original", Files.readString(game.resolve("projectzomboid.jar")));
    }

    @Test
    void uninstallAcceptsOneLocalInstallerWhenNoStoredCopyExists() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path game = Files.createDirectories(root.resolve("game"));
        Path home = Files.createDirectories(root.resolve("home"));
        Files.writeString(game.resolve("projectzomboid.jar"), "original");
        Files.createDirectories(game.resolve("jre64/bin"));
        // Only the existence check matters here: the run must get past installer selection.
        Files.write(game.resolve("jre64/bin/java.exe"), new byte[]{0});
        Files.writeString(game.resolve("modcore-test.jar"), "installer marker");
        Files.copy(sourceRoot().resolve("uninstall.bat"), game.resolve("uninstall.bat"));
        Result result = batch(root, game, "call uninstall.bat", home);
        assertTrue(result.output.contains("Using uninstaller: " + game.resolve("modcore-test.jar")), result.output);
        assertTrue(result.output.contains("Check 3 failed"), result.output);
        assertEquals("original", Files.readString(game.resolve("projectzomboid.jar")));
    }

    @Test
    void uninstallUsesTheHighestInstallerVersionWhenSeveralExist() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path game = Files.createDirectories(root.resolve("game"));
        Path home = Files.createDirectories(root.resolve("home"));
        Files.writeString(game.resolve("projectzomboid.jar"), "original");
        Files.createDirectories(game.resolve("jre64/bin"));
        Files.write(game.resolve("jre64/bin/java.exe"), new byte[]{0});
        Files.writeString(game.resolve("modcore-3.2.6-B42.jar"), "older");
        Files.writeString(game.resolve("modcore-3.2.7-B42.jar"), "newer");
        Files.copy(sourceRoot().resolve("uninstall.bat"), game.resolve("uninstall.bat"));
        Result result = batch(root, game, "call uninstall.bat", home);
        assertTrue(result.output.contains("Using uninstaller: " + game.resolve("modcore-3.2.7-B42.jar")), result.output);
        assertEquals("original", Files.readString(game.resolve("projectzomboid.jar")));
    }

    @Test
    void fastModeSkipsDeploymentTestsAndMarksTheJarAsUnverified() throws Exception {
        BuildEntryHarness.Fixture fixture = BuildEntryHarness.fixture("batch fast");

        BuildEntryHarness.Result result = BuildEntryHarness.run(fixture, "build.bat",
                BuildEntryHarness.validEnvironment(fixture), "--fast");

        assertEquals(0, result.exit(), result.output());
        Map<String, String> call = BuildEntryHarness.recording(fixture.source());
        assertTrue(call.get("GRADLE_ARGS").contains("-PskipDeploymentTests"), call.toString());
        assertTrue(call.get("GRADLE_ARGS").contains("test") && call.get("GRADLE_ARGS").contains("assemble"),
                call.toString());
        assertTrue(BuildEntryHarness.managedFixture(call).startsWith(fixture.root().resolve("temp")),
                "even the fast full entry needs the safe native fixture for the remaining tests: " + call);
        assertTrue(result.output().contains("not a verified delivery package"), result.output());
        assertFalse(result.output().contains("Build and tests passed."),
                "the fast path must not claim a verified build: " + result.output());
    }

    private static Path sourceRoot() { return Path.of(System.getProperty("agent.launchScript")).getParent(); }

    static void copyTree(Path source, Path target) throws Exception {
        try (var entries = Files.walk(source)) {
            for (Path path : entries.toList()) {
                Path destination = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) Files.createDirectories(destination);
                else if (!Files.exists(destination)) Files.copy(path, destination);
            }
        }
    }

    private static Result batch(Path root, Path work, String command, Path home) throws Exception {
        return batch(root, work, command, home, false);
    }

    private static Result batch(Path root, Path work, String command, Path home, boolean verify) throws Exception {
        Path log = Files.createTempFile(root, "batch-", ".log");
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/d", "/c", command).directory(work.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().remove("JAVA_HOME");
        builder.environment().remove("JDK_HOME");
        builder.environment().remove("JAVA_TOOL_OPTIONS");
        builder.environment().remove("JDK_JAVA_OPTIONS");
        builder.environment().remove("_JAVA_OPTIONS");
        builder.environment().put("PATH", System.getenv("SystemRoot") + "\\System32");
        // build.bat stays open for a double-click user; automation must not wait for a key.
        builder.environment().put("MODCORE_NO_PAUSE", "1");
        if (home != null) {
            builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
            builder.environment().put("USERPROFILE", home.toString());
            builder.environment().put("LOCALAPPDATA", root.resolve("appdata").toString());
            builder.environment().put("TEMP", root.toString());
            builder.environment().put("TMP", root.toString());
        }
        if (verify) builder.environment().put("MODCORE_VERIFY_ONLY", "1");
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(150, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
            fail("Batch entry timed out: " + command);
        }
        return new Result(process.exitValue(), new String(Files.readAllBytes(log),
                java.nio.charset.Charset.forName(System.getProperty("native.encoding", "UTF-8"))));
    }

    record Result(int exit, String output) {}
}
