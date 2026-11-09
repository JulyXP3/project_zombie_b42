package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static modcore.agent.BuildEntryHarness.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Behaviour of the two build entry points:
 *
 * <ul>
 *   <li>the two top-of-file paths (BUILD_JAVA_HOME / BUILD_GAME_DIR) are what a
 *       user edits; automation may override them with MODCORE_BUILD_JAVA_HOME /
 *       MODCORE_BUILD_GAME_DIR,</li>
 *   <li>compilation uses the external full JDK 25 only, never the game's
 *       jre64 as a fallback,</li>
 *   <li>missing sources, a missing compile dependency, a wrong JDK, a missing
 *       game jre64 and a mismatched projectzomboid.jar are rejected before
 *       Gradle starts,</li>
 *   <li>invalid input never edits the game directory.</li>
 * </ul>
 */
class BuildPathConfigTest {
    @Test
    void topConfigurationIsTheUserEditableEntryForTheFullBuild() throws Exception {
        Fixture fixture = fixture("top config");
        Path jdk = realJdk();
        editTopConfig(fixture.source().resolve("build.bat"), jdk.toString(), fixture.game().toString());

        Result result = run(fixture, "build.bat", Map.of());

        assertEquals(0, result.exit(), result.output());
        Map<String, String> call = recording(fixture.source());
        assertEquals(jdk.toString(), call.get("JAVA_HOME"), "the filled-in JDK must be used: " + result.output());
        assertEquals(fixture.source(), Path.of(call.get("CWD")), "the entry point must run from its own folder");
        Path managed = managedFixture(call);
        assertTrue(managed.startsWith(fixture.root().resolve("temp")),
                "the native fixture must live under the project temp folder: " + managed);
        assertTrue(Files.isRegularFile(managed.resolve("jre64/release")), managed.toString());
        assertTrue(call.get("GRADLE_ARGS").contains("test"), call.toString());
        assertTrue(call.get("GRADLE_ARGS").contains("assemble"), call.toString());
        assertFalse(result.output().contains("Press any key"), "automation must not be paused: " + result.output());
    }

    @Test
    void theTopJavaHomeIsTheValueActuallyValidated() throws Exception {
        Fixture fixture = fixture("top bad jdk");
        editTopConfig(fixture.source().resolve("build.bat"),
                fixture.root().resolve("missing top jdk").toString(), fixture.game().toString());

        Result result = run(fixture, "build.bat", Map.of());

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "a broken top-of-file JDK must stop the build, not be ignored", "JDK");
    }

    @Test
    void explicitAutomationOverridesWinOverTheTopConfiguration() throws Exception {
        Fixture fixture = fixture("override config");
        editTopConfig(fixture.source().resolve("build.bat"),
                fixture.root().resolve("missing default jdk").toString(),
                fixture.root().resolve("missing default game").toString());

        Result result = run(fixture, "build.bat", validEnvironment(fixture));

        assertEquals(0, result.exit(), result.output());
        assertEquals(realJdk().toString(), recording(fixture.source()).get("JAVA_HOME"), result.output());
    }

    @Test
    void invalidExplicitJdkIsRejectedInsteadOfFallingBackToTopJavaHomeOrPath() throws Exception {
        Fixture fixture = fixture("bad jdk");
        Path jdk = realJdk();
        editTopConfig(fixture.source().resolve("build.bat"), jdk.toString(), fixture.game().toString());

        Result missing = run(fixture, "build.bat", Map.of(
                JAVA_OVERRIDE, fixture.root().resolve("missing jdk").toString(),
                "JAVA_HOME", jdk.toString(),
                "PATH", jdk.resolve("bin") + ";" + pathWithPowerShell()));
        assertNotEquals(0, missing.exit(), missing.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(missing, "a missing explicitly configured JDK must name the requirement", "JDK");

        Path wrongMetadata = Files.createDirectories(fixture.root().resolve("wrong-version metadata"));
        Files.createDirectories(wrongMetadata.resolve("bin"));
        for (String tool : new String[]{"java", "javac", "keytool", "jarsigner"}) {
            Files.writeString(wrongMetadata.resolve("bin/" + tool + ".exe"), "not an executable: negative metadata fixture");
        }
        Files.writeString(wrongMetadata.resolve("release"), "JAVA_VERSION=\"17.0.1\"\nOS_ARCH=\"x86_64\"\n");
        Result wrongVersion = run(fixture, "build.bat", Map.of(
                JAVA_OVERRIDE, wrongMetadata.toString(),
                "JAVA_HOME", jdk.toString(),
                "PATH", jdk.resolve("bin") + ";" + pathWithPowerShell()));
        assertNotEquals(0, wrongVersion.exit(), wrongVersion.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(wrongVersion, "incompatible release metadata must fail before running any tool", "version", "25");
    }

    @Test
    void incompleteJdkIsRejectedBeforeGradle() throws Exception {
        Fixture fixture = fixture("incomplete jdk");
        Path jdk = realJdk();
        Path partial = Files.createDirectories(fixture.root().resolve("partial jdk"));
        Files.createDirectories(partial.resolve("bin"));
        Files.copy(jdk.resolve("bin/java.exe"), partial.resolve("bin/java.exe"));
        Files.copy(jdk.resolve("release"), partial.resolve("release"));
        Map<String, String> environment = new LinkedHashMap<>(environmentWithJavaHomeFallback(fixture));
        environment.put(JAVA_OVERRIDE, partial.toString());

        Result result = run(fixture, "build.bat", environment);

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "a JDK without javac/keytool/jarsigner must be rejected", "JDK");
    }

    @Test
    void incompleteGameJre64IsRejectedBeforeGradle() throws Exception {
        Fixture fixture = fixture("bad jre64");
        Files.delete(fixture.game().resolve("jre64/lib/modules"));
        Map<String, String> before = snapshot(fixture.game());

        Result result = run(fixture, "build.bat", environmentWithJavaHomeFallback(fixture));

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "a game folder without a complete jre64 must be rejected", "jre64");
        assertEquals(before, snapshot(fixture.game()), "an invalid configuration must not edit the game folder");
    }

    @Test
    void mismatchedCompileDependencyIsReportedWithoutOverwritingEitherJar() throws Exception {
        Fixture fixture = fixture("jar mismatch");
        Path library = fixture.source().resolve("lib/projectzomboid.jar");
        Path gameJar = fixture.game().resolve("projectzomboid.jar");
        Files.writeString(gameJar, "different game build");
        String libraryDigest = digest(library);
        String gameDigest = digest(gameJar);

        Result result = run(fixture, "build.bat", environmentWithJavaHomeFallback(fixture));

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "a compile dependency that does not match the game must be named",
                "projectzomboid.jar");
        assertEquals(libraryDigest, digest(library), "the compile dependency must not be overwritten");
        assertEquals(gameDigest, digest(gameJar), "the game JAR must not be overwritten");
    }

    @Test
    void pathsWithSpacesChineseAndShellCharactersSurviveTheEntryPoint() throws Exception {
        Fixture fixture = fixture("测试 ! & (build)");
        Map<String, String> before = snapshot(fixture.game());

        Result result = run(fixture, "build.bat", validEnvironment(fixture));

        assertEquals(0, result.exit(), result.output());
        Map<String, String> call = recording(fixture.source());
        assertEquals(realJdk().toString(), call.get("JAVA_HOME"), result.output());
        assertEquals(fixture.source(), Path.of(call.get("CWD")), result.output());
        Path managed = managedFixture(call);
        assertTrue(managed.startsWith(fixture.root().resolve("temp")), managed.toString());
        assertTrue(managed.toString().contains("&") && managed.toString().contains("!"),
                "the fixture path itself must keep the special characters: " + managed);
        assertEquals(before, snapshot(fixture.game()), "the game folder stays read-only");
    }

    @Test
    void invalidInputsLeaveTheGameFolderUntouchedAndDoNotRunGradle() throws Exception {
        Fixture fixture = fixture("sentinel");
        Path jdk = realJdk();
        editTopConfig(fixture.source().resolve("build.bat"), jdk.toString(), fixture.game().toString());
        Map<String, String> before = snapshot(fixture.game());

        Map<String, String> environment = new LinkedHashMap<>(environmentWithJavaHomeFallback(fixture));
        environment.put(GAME_OVERRIDE, fixture.root().resolve("missing game").toString());
        Result missingGame = run(fixture, "build.bat", environment);

        assertNotEquals(0, missingGame.exit(), missingGame.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(missingGame, "a missing game folder must be named", "game", "jre64");
        assertEquals(before, snapshot(fixture.game()), "the game folder stays byte identical");
    }

    @Test
    void fullBuildRefusesAMissingCompileDependencyBeforeGradle() throws Exception {
        Fixture fixture = fixture("missing dependency");
        Files.delete(fixture.source().resolve("lib/projectzomboid.jar"));

        Result result = run(fixture, "build.bat", validEnvironment(fixture));

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "a missing compile dependency must be named", "projectzomboid.jar");
    }

    @Test
    void fastBuildTopConfigurationIsUsedWithoutPreparingAnyNativeFixture() throws Exception {
        Fixture fixture = fixture("fast top");
        Path jdk = realJdk();
        editTopConfig(fixture.source().resolve("build-fast.bat"), jdk.toString(), fixture.game().toString());

        Result result = run(fixture, "build-fast.bat", Map.of());

        assertEquals(0, result.exit(), result.output());
        Map<String, String> call = recording(fixture.source());
        assertEquals(jdk.toString(), call.get("JAVA_HOME"), result.output());
        assertFalse(call.containsKey(NATIVE_FIXTURE), "the fast entry must not export a native fixture: " + call);
        assertTrue(call.get("GRADLE_ARGS").contains("clean jar"), call.toString());
        assertTrue(call.get("GRADLE_ARGS").contains("-x test"), call.toString());
        assertFalse(call.get("GRADLE_ARGS").contains("assemble"), call.toString());
        assertNoManagedFixture(fixture);
        assertTrue(result.output().contains("not a verified delivery package"), result.output());
    }

    @Test
    void fastBuildTopJavaHomeIsTheValueActuallyValidated() throws Exception {
        Fixture fixture = fixture("fast bad top jdk");
        editTopConfig(fixture.source().resolve("build-fast.bat"),
                fixture.root().resolve("missing top jdk").toString(), fixture.game().toString());

        Result result = run(fixture, "build-fast.bat", Map.of());

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "a broken top-of-file JDK must stop the fast entry, not be ignored", "JDK");
    }
}
