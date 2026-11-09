package modcore.agent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LaunchConfigurationTest {
    static final String BATCH = "@setlocal enableextensions\r\n@cd /d \"%~dp0\"\r\nSET _JAVA_OPTIONS=\r\n"
            + "SET PZ_CLASSPATH=./;projectzomboid.jar\r\n"
            + "\".\\jre64\\bin\\java.exe\" -Xmx3072m -Djava.library.path=./win64/;./ -cp %PZ_CLASSPATH% zombie.gameStates.MainScreenState %1 %2\r\n"
            + "IF %ERRORLEVEL% NEQ 0 (\r\n"
            + "\".\\jre64\\bin\\java.exe\" -Xmx3072m -cp %PZ_CLASSPATH% zombie.gameStates.MainScreenState %1 %2\r\n)\r\nPAUSE\r\n";

    @Test
    void controlledLaunchKeepsJvmArgumentsAndRemovesAutomaticRetry() throws Exception {
        String command = new String(batch(BATCH.getBytes(StandardCharsets.US_ASCII)), StandardCharsets.US_ASCII);
        assertEquals(1, command.split("modcore.attach.LaunchCoordinator", -1).length - 1);
        assertFalse(command.contains("-javaagent:"));
        assertEquals(1, command.split("%\\*", -1).length - 1, "game arguments must still be forwarded");
        assertTrue(command.contains("-Djava.library.path=./win64/;./"), "original JVM arguments must stay");
        assertTrue(command.contains("-Xmx3072m"));
        assertFalse(command.contains("IF %ERRORLEVEL% NEQ 0"), "failed attach must not start a second game");
        assertTrue(command.contains("SET PZ_CLASSPATH=./;projectzomboid.jar"));
        assertTrue(command.startsWith("@setlocal enableextensions disableDelayedExpansion"));
        assertTrue(command.contains("zombie.gameStates.MainScreenState"), "the coordinator validates the original main token");
    }

    @Test
    void existingAgentAndClasspathSettingsArePreservedInsteadOfRejected() throws Exception {
        String agent = new String(batch(BATCH.replace("-Xmx3072m", "-javaagent:other.jar")
                .getBytes(StandardCharsets.US_ASCII)), StandardCharsets.US_ASCII);
        assertEquals(1, agent.split("modcore\\.attach\\.LaunchCoordinator", -1).length - 1);
        assertTrue(agent.contains("-javaagent:other.jar"), "another tool's agent is kept as-is");
        for (String input : new String[]{
                BATCH.replace("-Xmx3072m", "-Djava.system.class.loader=Other"),
                BATCH.replace("SET PZ_CLASSPATH=./;projectzomboid.jar", "SET PZ_CLASSPATH=./;projectzomboid.jar;other.jar")
        }) {
            assertEquals(1, new String(batch(input.getBytes(StandardCharsets.US_ASCII)), StandardCharsets.US_ASCII)
                    .split("modcore\\.attach\\.LaunchCoordinator", -1).length - 1, input);
        }
        // 没有游戏启动行时仍然拒绝：没有可改写的目标。
        assertThrows(java.io.IOException.class, () -> batch("@echo custom startup\r\n".getBytes(StandardCharsets.US_ASCII)));
        assertThrows(java.io.IOException.class, () -> batch(BATCH.replace("zombie.gameStates.MainScreenState", "other.Main")
                .getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void enabledDelayedExpansionIsNormalizedInsteadOfRejected() throws Exception {
        // 2026-11-07 放宽: 允许 bat 已带 enableDelayedExpansion, 统一压成 disable。
        String command = new String(batch(BATCH.replace("setlocal enableextensions",
                "setlocal enableextensions EnableDelayedExpansion").getBytes(StandardCharsets.US_ASCII)),
                StandardCharsets.US_ASCII);
        assertTrue(command.startsWith("@setlocal enableextensions disableDelayedExpansion"));
        assertFalse(command.contains(" enableDelayedExpansion"));
    }

    static byte[] batch(byte[] input) throws Exception {
        Class<?> editor = assertDoesNotThrow(() -> Class.forName("modcore.build.LaunchConfiguration"));
        try {
            return (byte[]) editor.getMethod("editBatch", byte[].class).invoke(null, (Object) input);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception cause) throw cause;
            throw failure;
        }
    }

    @Test
    void oversizedInstalledBatchStillFailsTheInputLimit() {
        byte[] input = new byte[1024 * 1024 + 1];
        java.util.Arrays.fill(input, (byte) ' ');
        byte[] marker = "modcore.attach.LaunchCoordinator".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(marker, 0, input, 0, marker.length);
        assertThrows(java.io.IOException.class, () -> batch(input));
    }

    @Test
    void theExecutableConfigurationIsNeverRewritten() throws Exception {
        Class<?> editor = assertDoesNotThrow(() -> Class.forName("modcore.build.LaunchConfiguration"));
        assertThrows(NoSuchMethodException.class, () -> editor.getMethod("editJson", byte[].class, Path.class, Path.class),
                "with a batch-only entry point the exe configuration must stay untouched");
    }
}
