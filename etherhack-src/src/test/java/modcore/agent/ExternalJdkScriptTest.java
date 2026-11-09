package modcore.agent;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ExternalJdkScriptTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void powerShellLaunchFindsExternalJdkWithoutBundledRuntime(boolean useHome) throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        var install = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
        assertEquals(0, install.exit(), install.output());
        Path log = f.root().resolve("launch-script.log");
        Path script = Path.of(System.getProperty("agent.launchScript"));
        ProcessBuilder builder = new ProcessBuilder("pwsh", "-NoLogo", "-NoProfile", "-File", script.toString(),
                "-GameDirectory", f.game().toString(), "-DeploymentDirectory", f.home().resolve("Zomboid/modcore-agent").toString(),
                "-JavaExecutable", Path.of(System.getProperty("java.home"), "bin/java.exe").toString(),
                "-UserHome", f.home().toString(), "-VerifyOnly").directory(f.root().toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        if (useHome) {
            builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
            builder.environment().put("PATH", System.getenv("SystemRoot") + "\\System32");
        } else {
            builder.environment().remove("JAVA_HOME");
            builder.environment().put("PATH", Path.of(System.getProperty("java.home"), "bin") + ";" + System.getenv("SystemRoot") + "\\System32");
        }
        for (String key : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) builder.environment().remove(key);
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(90, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
            fail("PowerShell launch timed out");
        }
        String output = Files.readString(log);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("VERIFY_ONLY"), output);
        assertTrue(output.contains("runtime classes linked"), output);
        assertFalse(output.contains("GAME_STARTING"), output);
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore-agent/attach-java")));
    }
}
