package modcore;

import modcore.build.LaunchConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2026-11-07: 启动 bat 的改写放宽。
 *
 * 目标：允许带引号的 PZ_CLASSPATH / -cp 写法、允许 setlocal 带 enableDelayedExpansion、
 * 已经是本项目安装形态的 bat 幂等返回；但其它 agent 的定制仍然拒绝。
 */
class LaunchConfigurationTest {

    private static final String VANILLA = """
            @setlocal enableextensions
            @cd /d "%~dp0"
            SET _JAVA_OPTIONS=

            SET PZ_CLASSPATH=./;projectzomboid.jar
            ".\\jre64\\bin\\java.exe" -Dzomboid.steam=1 -cp %PZ_CLASSPATH% zombie.gameStates.MainScreenState %1 %2

            IF %ERRORLEVEL% NEQ 0 (
            ".\\jre64\\bin\\java.exe" -Dzomboid.steam=1 -cp %PZ_CLASSPATH% zombie.gameStates.MainScreenState %1 %2

            )

            PAUSE
            """;

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String patch(String value) throws IOException {
        return new String(LaunchConfiguration.editBatch(bytes(value)), StandardCharsets.ISO_8859_1);
    }

    @Test
    void vanillaTwoLaunchLinesArePatched() throws Exception {
        String patched = patch(VANILLA);
        int launches = patched.split("modcore\\.attach\\.LaunchCoordinator", -1).length - 1;
        assertTrue(launches == 1, "controlled launch must not retain automatic retry, got " + launches);
        assertFalse(patched.contains("-javaagent:"));
        assertTrue(patched.contains("SET PZ_CLASSPATH=./;projectzomboid.jar"));
        assertTrue(patched.contains("@setlocal enableextensions disableDelayedExpansion"));
        assertTrue(patched.contains("%*"));
    }

    /**
     * 42.22.2 实际随包发布的启动 bat：CRLF、重试分支行尾 {@code IF %ERRORLEVEL% NEQ 0 ( } 带一个空格、
     * 分支内带空行。旧正则要求 {@code (} 紧接换行，真实文件匹配不上 → 重试分支没被删掉 → 安装报
     * “Expected one controlled game launch after removing the original retry branch”。
     */
    private static String shippedTwoLaunchBat(String jvmArguments) {
        return "@setlocal enableextensions\r\n"
                + "@cd /d \"%~dp0\"\r\n"
                + "SET _JAVA_OPTIONS=\r\n"
                + "\r\n"
                + "SET PZ_CLASSPATH=./;projectzomboid.jar\r\n"
                + "\".\\jre64\\bin\\java.exe\" " + jvmArguments
                + " -cp %PZ_CLASSPATH% zombie.gameStates.MainScreenState %1 %2\r\n"
                + "\r\n"
                + "IF %ERRORLEVEL% NEQ 0 ( \r\n"
                + "\".\\jre64\\bin\\java.exe\" " + jvmArguments
                + " -cp %PZ_CLASSPATH% zombie.gameStates.MainScreenState %1 %2\r\n"
                + "\r\n"
                + ")\r\n"
                + "\r\n"
                + "PAUSE\r\n";
    }

    @Test
    void retryBranchWithTrailingBlankIsRemovedLike4222ShipsIt() throws Exception {
        String patched = patch(shippedTwoLaunchBat("-XX:+UseShenandoahGC -Xms10240m -Xmx10240m"));
        assertEquals(1, patched.split("modcore\\.attach\\.LaunchCoordinator", -1).length - 1,
                "the shipped automatic retry must be removed, not left as a second launch");
        assertFalse(patched.contains("IF %ERRORLEVEL% NEQ 0"));
        assertTrue(patched.contains("-Xms10240m"), "original JVM arguments must survive");
        assertTrue(patched.contains("%*"));
        assertArrayEquals(bytes(patched), LaunchConfiguration.editBatch(bytes(patched)));
    }

    @Test
    void singleLaunchDebugBatWithoutRetryBranchIsAccepted() throws Exception {
        String debug = "@setlocal enableextensions\r\n"
                + "@cd /d \"%~dp0\"\r\n"
                + "SET _JAVA_OPTIONS=\r\n"
                + "SET PZ_CLASSPATH=./;projectzomboid.jar\r\n"
                + "\".\\jre64\\bin\\java.exe\" -Dorg.lwjgl.util.NoChecks=false"
                + " -cp %PZ_CLASSPATH% zombie.gameStates.MainScreenState -debuglog=Shader\r\n"
                + "PAUSE\r\n";
        String patched = patch(debug);
        assertEquals(1, patched.split("modcore\\.attach\\.LaunchCoordinator", -1).length - 1);
        assertTrue(patched.contains("-debuglog=Shader"), "extra game arguments must survive");
        assertEquals(1, patched.split(":MODCORE_NO_JDK", -1).length - 1, "exactly one JDK fallback label");
    }

    @Test
    void alreadyPatchedBatchIsIdempotent() throws Exception {
        byte[] patched = LaunchConfiguration.editBatch(bytes(VANILLA));
        assertArrayEquals(patched, LaunchConfiguration.editBatch(patched));
    }

    @Test
    void quotedClasspathAndLfLineEndingsAreAccepted() throws Exception {
        String quoted = """
                @setlocal enableextensions
                SET "PZ_CLASSPATH=./;projectzomboid.jar"
                ".\\jre64\\bin\\java.exe" -Dzomboid.steam=1 -cp "%PZ_CLASSPATH%" zombie.gameStates.MainScreenState %1 %2
                """;
        String patched = patch(quoted);
        assertTrue(patched.contains("modcore.attach.LaunchCoordinator"));
        assertTrue(patched.contains("SET \"PZ_CLASSPATH=./;projectzomboid.jar\""));
    }

    @Test
    void enabledDelayedExpansionIsNormalizedToDisabled() throws Exception {
        String delayed = """
                @setlocal enableextensions enableDelayedExpansion
                SET PZ_CLASSPATH=./;projectzomboid.jar
                ".\\jre64\\bin\\java.exe" -Dzomboid.steam=1 -cp %PZ_CLASSPATH% zombie.gameStates.MainScreenState %1 %2
                """;
        String patched = patch(delayed);
        assertTrue(patched.contains("@setlocal enableextensions disableDelayedExpansion"));
        assertFalse(patched.contains(" enableDelayedExpansion"));
    }

    @Test
    void foreignAgentCustomizationIsPreservedNotRejected() throws Exception {
        String foreign = """
                @setlocal enableextensions
                SET PZ_CLASSPATH=./;projectzomboid.jar
                ".\\jre64\\bin\\java.exe" -javaagent:other-agent.jar -cp %PZ_CLASSPATH% zombie.gameStates.MainScreenState %1 %2
                """;
        String patched = patch(foreign);
        assertTrue(patched.contains("modcore.attach.LaunchCoordinator"), "installation must still happen");
        assertTrue(patched.contains("-javaagent:other-agent.jar"), "another tool's options are kept as-is");
    }
}
