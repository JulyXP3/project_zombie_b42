package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class JdkRequirementTest {
    @Test
    void completeExternalJdk25IsAccepted() throws Exception {
        var result = check(Path.of(System.getProperty("java.home")));
        assertEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("JDK 25"), result.output());
    }

    @Test
    void missingAttachModuleIsRejected() throws Exception {
        var result = check(Path.of(System.getProperty("java.home")), "--limit-modules", "java.base,jdk.compiler");
        assertNotEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("jdk.attach"), result.output());
    }

    @Test
    void gameRuntimeIsNotAcceptedAsTheExternalJdk() throws Exception {
        String configured = System.getenv("MODCORE_NATIVE_FIXTURE");
        assertNotNull(configured, "complete verification requires the native runtime fixture");
        var result = check(Path.of(configured).resolve("jre64"));
        assertNotEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("JDK 25"), result.output());
    }

    @Test
    void unsupportedArchitectureIsRejected() throws Exception {
        var result = check(Path.of(System.getProperty("java.home")), "-Dos.arch=x86");
        assertNotEquals(0, result.exit(), result.output());
        assertTrue(result.output().contains("x64"), result.output());
    }

    @Test
    void unsupportedJavaVersionsAreRejected() throws Exception {
        Class<?> checker = assertDoesNotThrow(() -> Class.forName("modcore.attach.JdkRequirement"));
        var lookup = java.lang.invoke.MethodHandles.privateLookupIn(checker, java.lang.invoke.MethodHandles.lookup());
        var check = lookup.findStatic(checker, "checkVersion", java.lang.invoke.MethodType.methodType(void.class, int.class));
        assertDoesNotThrow(() -> check.invoke(25));
        for (int version : List.of(8, 17, 21, 24, 26)) {
            var exception = assertThrows(java.io.IOException.class, () -> check.invoke(version));
            assertTrue(exception.getMessage().contains("JDK 25"), exception.getMessage());
        }
    }

    private static ThreeFileDeploymentTest.Result check(Path home, String... options) throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path log = root.resolve("jdk-check.log");
        List<String> command = new ArrayList<>(List.of(home.resolve("bin/java.exe").toString()));
        command.addAll(List.of(options));
        command.addAll(List.of("-Duser.home=" + root, "-Djava.io.tmpdir=" + root, "-cp",
                System.getProperty("agent.installerJar"), "modcore.attach.JdkRequirement"));
        ProcessBuilder builder = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        for (String key : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")) builder.environment().remove(key);
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            fail("JDK check timed out");
        }
        return new ThreeFileDeploymentTest.Result(process.exitValue(), Files.readString(log,
                Charset.forName(System.getProperty("native.encoding", "UTF-8"))));
    }
}
