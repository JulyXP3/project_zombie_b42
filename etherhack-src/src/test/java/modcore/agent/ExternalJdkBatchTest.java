package modcore.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ExternalJdkBatchTest {
    @Test
    void noExternalJdkRefusesInstallationEvenWithGameJavaPresent() throws Exception {
        var f = fixture();
        refuseInstall(f, null, systemPath());
    }

    @Test
    void invalidJavaHomeDoesNotFallBackToAWorkingPath() throws Exception {
        var f = fixture();
        refuseInstall(f, f.root().resolve("missing JDK").toString(), jdkPath());
    }

    @Test
    void gameJavaCannotStandInForAnExternalFullJdk() throws Exception {
        var f = fixture();
        refuseInstall(f, f.game().resolve("jre64").toString(), jdkPath());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void javaHomeOrPathSupportsInstallLaunchAndRemoval(boolean useHome) throws Exception {
        var f = fixture();
        String javaHome = useHome ? System.getProperty("java.home") : null;
        String path = useHome ? systemPath() : jdkPath();
        byte[] original = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        var install = batch(f, "install.bat", javaHome, path);
        assertEquals(0, install.exit(), install.output());
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore-agent/attach-java")));
        var launch = batch(f, "ProjectZomboid64.bat -nosteam", javaHome, path);
        assertEquals(0, launch.exit(), launch.output());
        assertTrue(launch.output().contains("VERIFY_ONLY"), launch.output());
        assertTrue(launch.output().contains("runtime classes linked"), launch.output());
        var remove = batch(f, "uninstall.bat", javaHome, path);
        assertEquals(0, remove.exit(), remove.output());
        assertArrayEquals(original, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore-agent")));
    }

    @Test
    void missingJdkAfterInstallationCannotStartGameOrChangeDeployment() throws Exception {
        var f = fixture();
        var install = batch(f, "install.bat", System.getProperty("java.home"), systemPath());
        assertEquals(0, install.exit(), install.output());
        Path deployment = f.home().resolve("Zomboid/modcore-agent");
        byte[] record = Files.readAllBytes(deployment.resolve("installation.properties"));
        byte[] installed = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        for (String entry : List.of("ProjectZomboid64.bat", "uninstall.bat")) {
            var result = batch(f, entry, null, systemPath());
            assertNotEquals(0, result.exit(), result.output());
            assertTrue(result.output().contains("JDK 25"), result.output());
            assertArrayEquals(record, Files.readAllBytes(deployment.resolve("installation.properties")));
            assertArrayEquals(installed, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
        }
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore-sessions")), "no game process may be created");
    }

    @Test
    void javaHomeWithUnicodeSpacesAndShellCharactersIsNotReparsed() throws Exception {
        var f = fixture();
        Path jdk = f.root().resolve("JDK 25 测试 ! & (local)");
        BatchEntryTest.copyTree(Path.of(System.getProperty("java.home")), jdk);
        String original = PayloadArchive.digest(jdk.resolve("bin/java.exe"));
        var result = batch(f, "install.bat", jdk.toString(), systemPath());
        assertEquals(0, result.exit(), result.output());
        result = batch(f, "ProjectZomboid64.bat", jdk.toString(), systemPath());
        assertEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("VERIFY_ONLY"), result.output());
        result = batch(f, "uninstall.bat", jdk.toString(), systemPath());
        assertEquals(0, result.exit(), result.output());
        assertEquals(original, PayloadArchive.digest(jdk.resolve("bin/java.exe")), "external JDK is not owned or deleted");
    }

    private static void refuseInstall(ThreeFileDeploymentTest.Fixture f, String home, String path) throws Exception {
        byte[] original = Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat"));
        var result = batch(f, "install.bat", home, path);
        assertNotEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("JDK 25"), result.output());
        assertArrayEquals(original, Files.readAllBytes(f.game().resolve("ProjectZomboid64.bat")));
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore-agent")));
    }

    private static ThreeFileDeploymentTest.Fixture fixture() throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        String configured = System.getenv("MODCORE_NATIVE_FIXTURE");
        assertNotNull(configured, "complete verification requires a native fixture");
        BatchEntryTest.copyTree(Path.of(configured).resolve("jre64"), f.game().resolve("jre64"));
        Path source = Path.of(System.getProperty("agent.launchScript")).getParent();
        for (String script : List.of("install.bat", "uninstall.bat")) Files.copy(source.resolve(script), f.game().resolve(script));
        return f;
    }

    private static ThreeFileDeploymentTest.Result batch(ThreeFileDeploymentTest.Fixture f, String entry, String javaHome, String path) throws Exception {
        Path log = Files.createTempFile(f.root(), "external-jdk-", ".log");
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/d", "/c", "call " + entry).directory(f.game().toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        for (String key : List.of("JAVA_HOME", "JDK_HOME", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) builder.environment().remove(key);
        if (javaHome != null) builder.environment().put("JAVA_HOME", javaHome);
        builder.environment().put("PATH", path);
        builder.environment().put("USERPROFILE", f.home().toString());
        builder.environment().put("LOCALAPPDATA", f.root().resolve("appdata").toString());
        builder.environment().put("TEMP", f.root().toString());
        builder.environment().put("TMP", f.root().toString());
        builder.environment().put("MODCORE_VERIFY_ONLY", "1");
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(150, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
            fail("Batch entry timed out: " + entry);
        }
        return new ThreeFileDeploymentTest.Result(process.exitValue(), Files.readString(log,
                Charset.forName(System.getProperty("native.encoding", "UTF-8"))));
    }

    private static String systemPath() { return System.getenv("SystemRoot") + "\\System32"; }
    private static String jdkPath() { return Path.of(System.getProperty("java.home"), "bin") + ";" + systemPath(); }
}
