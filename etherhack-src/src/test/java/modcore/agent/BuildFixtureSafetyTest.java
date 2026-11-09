package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static modcore.agent.BuildEntryHarness.*;
import static org.junit.jupiter.api.Assertions.*;

class BuildFixtureSafetyTest {
    @Test
    void helperCannotOverwriteAnOutputOutsideItsManagedStore() throws Exception {
        Fixture f = fixture("pointer escape");
        Path sentinel = Files.writeString(f.root().resolve("outside.txt"), "unregistered sentinel");
        Result result = invokePreparation(f, sentinel);
        assertNotEquals(0, result.exit(), result.output());
        assertEquals("unregistered sentinel", Files.readString(sentinel));
        assertNoManagedFixture(f);
    }

    @Test
    void unregisteredSharedPointerIsNotAdoptedOrOverwritten() throws Exception {
        Fixture f = fixture("unknown pointer");
        Path store = Files.createDirectories(f.root().resolve("temp/native-runtime-build"));
        Path sentinel = Files.writeString(store.resolve("current"), "unregistered sentinel");
        Result result = run(f, "build.bat", validEnvironment(f));
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
        assertEquals("unregistered sentinel", Files.readString(sentinel));
    }

    @Test
    void manifestAndCopiedFileCannotDriftTogether() throws Exception {
        Fixture f = fixture("manifest drift");
        Path managed = firstRun(f);
        Path release = managed.resolve("jre64/release");
        String oldDigest = digest(release);
        Files.writeString(release, "JAVA_VERSION=\"99.0.0\"\nOS_ARCH=\"x86_64\"\n");
        Path manifest = managed.resolve("manifest.json");
        String text = Files.readString(manifest).replace(oldDigest, digest(release));
        text = text.replaceFirst("(?s)(\"path\"\\s*:\\s*\"jre64/release\"[^}]*?\"length\"\\s*:\\s*)\\d+",
                "$1" + Files.size(release));
        Files.writeString(manifest, text);
        Map<String, String> before = snapshot(managed);
        Result result = run(f, "build.bat", validEnvironment(f));
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
        assertEquals(before, snapshot(managed), "a changed record must not authorize replacement");
    }

    @Test
    void duplicateManifestEntriesAreRejected() throws Exception {
        Fixture f = fixture("duplicate manifest");
        Path managed = firstRun(f);
        Path manifest = managed.resolve("manifest.json");
        String text = Files.readString(manifest);
        int array = text.indexOf("[", text.indexOf("\"files\""));
        int start = text.indexOf("{", array);
        int end = text.indexOf("}", start) + 1;
        String first = text.substring(start, end);
        Files.writeString(manifest, text.substring(0, start) + first + ",\n" + text.substring(start));
        Result result = run(f, "build.bat", validEnvironment(f));
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
    }

    @Test
    void manifestCannotClaimAParentPathDuringRepair() throws Exception {
        Fixture f = fixture("manifest parent");
        Path managed = firstRun(f);
        Path manifest = managed.resolve("manifest.json");
        Files.writeString(manifest, Files.readString(manifest)
                .replace("jre64/bin/java.exe", "jre64/../outside.txt"));
        Files.delete(managed.resolve("jre64/bin/java.exe"));
        Map<String, String> before = snapshot(managed);
        Result result = run(f, "build.bat", validEnvironment(f));
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
        assertEquals(before, snapshot(managed));
    }

    @Test
    void unknownFilesInTheManagedStoreArePreservedAndRejected() throws Exception {
        Fixture f = fixture("store unknown");
        Path store = Files.createDirectories(f.root().resolve("temp/native-runtime-build"));
        Path sentinel = Files.writeString(store.resolve("notes.txt"), "not owned by the build");
        Result result = run(f, "build.bat", validEnvironment(f));
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
        assertEquals("not owned by the build", Files.readString(sentinel));
    }

    @Test
    void gameRootCannotContainTheBuildOutput() throws Exception {
        Fixture f = fixture("game overlap");
        writeSmallJre64(f.root());
        Files.copy(f.game().resolve("projectzomboid.jar"), f.root().resolve("projectzomboid.jar"));
        Map<String, String> before = snapshot(f.root());
        Result result = run(f, "build.bat", Map.of(JAVA_OVERRIDE, realJdk().toString(), GAME_OVERRIDE, f.root().toString()));
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
        assertNoManagedFixture(f);
        assertEquals(before.get("projectzomboid.jar"), digest(f.root().resolve("projectzomboid.jar")));
    }

    @Test
    void junctionInTheManagedRuntimeDoesNotTouchItsTarget() throws Exception {
        Fixture f = fixture("managed junction");
        Path managed = firstRun(f);
        Path outside = Files.createDirectories(f.root().resolve("outside"));
        Path sentinel = Files.writeString(outside.resolve("sentinel.txt"), "leave outside alone");
        createJunction(managed.resolve("jre64/linked"), outside);
        Result result = run(f, "build.bat", validEnvironment(f));
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
        assertEquals("leave outside alone", Files.readString(sentinel));
    }

    @Test
    void junctionInTheTempAncestorIsRejectedBeforeCreatingAnyOutput() throws Exception {
        Fixture f = fixture("temp junction");
        Path outside = Files.createDirectories(f.root().resolve("outside"));
        Files.writeString(outside.resolve("sentinel.txt"), "untouched");
        createJunction(f.root().resolve("temp"), outside);
        Map<String, String> before = snapshot(outside);
        Result result = run(f, "build.bat", validEnvironment(f));
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
        assertEquals(before, snapshot(outside));
    }

    @Test
    void occupiedCopiedRuntimeIsNotReplacedDuringRepair() throws Exception {
        Fixture f = fixture("copied runtime locked");
        Path managed = firstRun(f);
        Files.delete(managed.resolve("jre64/lib/modules"));
        Path release = managed.resolve("jre64/release");
        String before = digest(release);
        Result result;
        try (FileChannel channel = FileChannel.open(release, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            result = run(f, "build.bat", validEnvironment(f));
        }
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
        assertEquals(before, digest(release));
        assertFalse(Files.exists(managed.resolve("jre64/lib/modules")));
    }

    @Test
    void anotherFullBuildCannotChangeTheFixtureWhileGradleIsRunning() throws Exception {
        Fixture f = fixture("concurrent build");
        Path ready = f.source().resolve("gradle-ready");
        Path release = f.source().resolve("gradle-release");
        Files.writeString(f.source().resolve("hold.ps1"), "param([string]$Ready,[string]$Release)\n"
                + "[IO.File]::WriteAllText($Ready,'ready')\n"
                + "$deadline=[DateTime]::UtcNow.AddSeconds(60)\n"
                + "while(-not [IO.File]::Exists($Release)){if([DateTime]::UtcNow -gt $deadline){exit 19};Start-Sleep -Milliseconds 100}\n");
        Files.writeString(f.source().resolve("gradlew.bat"), "@echo off\r\n"
                + "if exist \"%~dp0gradle-ready\" exit /b 0\r\n"
                + "pwsh -NoProfile -File \"%~dp0hold.ps1\" -Ready \"%~dp0gradle-ready\" -Release \"%~dp0gradle-release\"\r\n"
                + "exit /b %ERRORLEVEL%\r\n");
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/d", "/c", "call \"" + f.source().resolve("build.bat") + "\"")
                .directory(f.root().toFile()).redirectErrorStream(true)
                .redirectOutput(f.root().resolve("first-build.log").toFile());
        for (String key : List.of("JAVA_HOME", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS",
                "MODCORE_NATIVE_FIXTURE", "PZ_GAME_DIR", JAVA_OVERRIDE, GAME_OVERRIDE)) builder.environment().remove(key);
        builder.environment().putAll(validEnvironment(f));
        builder.environment().put("PATH", pathWithPowerShell());
        builder.environment().put("MODCORE_NO_PAUSE", "1");
        Process first = builder.start();
        first.getOutputStream().close();
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (!Files.exists(ready) && first.isAlive() && System.nanoTime() < deadline) Thread.sleep(100);
            assertTrue(Files.exists(ready), "the first build must reach the waiting Gradle wrapper");
            Result second = run(f, "build.bat", validEnvironment(f));
            assertNotEquals(0, second.exit(), "a concurrent build must be refused: " + second.output());
        } finally {
            Files.writeString(release, "release");
            if (!first.waitFor(30, TimeUnit.SECONDS)) first.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
        }
        assertEquals(0, first.exitValue(), "the first build must retain its own fixture and finish");
    }

    @Test
    void fastBuildChecksTheConfiguredGameJarWithoutNeedingItsRuntime() throws Exception {
        Fixture f = fixture("fast no runtime");
        deleteTree(f.game().resolve("jre64"));
        Result result = run(f, "build-fast.bat", validEnvironment(f));
        assertEquals(0, result.exit(), result.output());
        assertNoManagedFixture(f);
        Files.writeString(f.game().resolve("projectzomboid.jar"), "a different target version");
        Result mismatch = run(f, "build-fast.bat", validEnvironment(f));
        assertNotEquals(0, mismatch.exit(), mismatch.output());
        assertGradleNotInvoked(f.source());
        assertNoManagedFixture(f);
    }

    @Test
    void fastBuildRefusesAnInvalidExplicitGamePath() throws Exception {
        Fixture f = fixture("fast bad game");
        Result result = run(f, "build-fast.bat", Map.of(JAVA_OVERRIDE, realJdk().toString(),
                GAME_OVERRIDE, f.root().resolve("missing-game").toString()));
        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(f.source());
        assertNoManagedFixture(f);
    }

    private static Path firstRun(Fixture f) throws Exception {
        Result result = run(f, "build.bat", validEnvironment(f));
        assertEquals(0, result.exit(), result.output());
        return managedFixture(recording(f.source()));
    }

    private static Result invokePreparation(Fixture f, Path output) throws Exception {
        Path log = f.root().resolve("prepare-output.log");
        ProcessBuilder builder = new ProcessBuilder("pwsh.exe", "-NoProfile", "-File",
                f.source().resolve("prepare-native-fixture.ps1").toString(), "-GameDir", f.game().toString(),
                "-CompileDependency", f.source().resolve("lib/projectzomboid.jar").toString(), "-PointerFile", output.toString())
                .directory(f.root().toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("PATH", pathWithPowerShell());
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            fail("preparation helper timed out");
        }
        return new Result(process.exitValue(), Files.readString(log,
                java.nio.charset.Charset.forName(System.getProperty("native.encoding", "UTF-8"))));
    }
}
