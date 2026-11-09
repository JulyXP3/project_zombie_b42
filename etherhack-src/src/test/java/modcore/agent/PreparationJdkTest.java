package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PreparationJdkTest {
    @Test
    void commandLinePreparationRejectsRuntimeOnlyJavaBeforeWritingPayload() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path output = root.resolve("runtime.jar");
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin/java.exe").toString(),
                "--limit-modules", "java.base", "-Duser.home=" + root, "-Djava.io.tmpdir=" + root,
                "-jar", System.getProperty("agent.installerJar"), "--prepare-runtime",
                System.getProperty("headshot.gameJar"), System.getProperty("agent.payloadJar"), output.toString()));
        String text = run(root, command);
        assertFalse(Files.exists(output), text);
        assertTrue(text.contains("JDK 25"), text);
    }

    @Test
    void preparationScriptChecksTheSelectedJavaBeforeCreatingOutput() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path output = root.resolve("must-not-exist");
        Path script = Path.of(System.getProperty("agent.launchScript")).resolveSibling("prepare-agent.ps1");
        String configured = System.getenv("MODCORE_NATIVE_FIXTURE");
        assertNotNull(configured);
        var command = new ArrayList<>(List.of("pwsh", "-NoLogo", "-NoProfile", "-File", script.toString(),
                "-GameJar", System.getProperty("headshot.gameJar"), "-RuntimeJar", System.getProperty("agent.payloadJar"),
                "-InstallerJar", System.getProperty("agent.installerJar"), "-OutputDirectory", output.toString(),
                "-JavaExecutable", Path.of(configured, "jre64/bin/java.exe").toString()));
        String text = run(root, command);
        assertFalse(Files.exists(output), text);
        assertTrue(text.contains("JDK 25"), text);
    }

    private static String run(Path root, List<String> command) throws Exception {
        Path log = root.resolve("preparation.log");
        ProcessBuilder builder = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        for (String key : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) builder.environment().remove(key);
        builder.environment().put("LOCALAPPDATA", root.resolve("appdata").toString());
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            fail("Preparation check timed out");
        }
        String output = Files.readString(log, java.nio.charset.Charset.forName(System.getProperty("native.encoding", "UTF-8")));
        assertNotEquals(0, process.exitValue(), output);
        return output;
    }
}
