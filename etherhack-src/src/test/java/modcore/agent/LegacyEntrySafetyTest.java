package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LegacyEntrySafetyTest {
    @Test
    void legacyBatchFilesNeverTouchExistingGameFiles() throws Exception {
        Path sourceRoot = Path.of(System.getProperty("agent.launchScript")).getParent();
        for (String name : List.of("install.bat", "uninstall.bat")) {
            Path root = PayloadArchiveTest.sandbox();
            Path script = root.resolve(name);
            Files.copy(sourceRoot.resolve(name), script);
            Path marker = root.resolve("zombie/GameWindow.class");
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "existing\n");
            Files.writeString(root.resolve("projectzomboid.jar"), "original-marker\n");
            Path log = root.resolve("batch.log");
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/d", "/c", name)
                    .directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
            builder.environment().put("USERPROFILE", root.resolve("home").toString());
            Process process = builder.start();
            process.getOutputStream().close();
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                fail("Legacy batch command timed out");
            }
            assertTrue(Files.exists(marker), name + " deleted the old override");
            assertEquals("existing\n", Files.readString(marker));
            assertEquals("original-marker\n", Files.readString(root.resolve("projectzomboid.jar")));
            // 没有安装包时 install.bat 走卸载流程；沙盘里连卸载脚本都没有，等于无事可做，不算失败。
            assertEquals(0, process.exitValue(), Files.readString(log));
        }
    }

    @Test
    void legacyCommandsCannotDeleteOrReplaceExistingOverrideFiles() throws Exception {
        for (String argument : List.of("--install", "--uninstall")) {
            Path root = PayloadArchiveTest.sandbox();
            Path marker = root.resolve("zombie/GameWindow.class");
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "existing-file-must-be-reviewed\n");
            Path log = root.resolve("legacy.log");
            Process process = new ProcessBuilder(List.of(
                    Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                    "-Duser.home=" + root.resolve("home"), "-Djava.io.tmpdir=" + root,
                    "-jar", System.getProperty("agent.installerJar"), argument))
                    .directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                fail("Legacy command timed out");
            }
            assertTrue(Files.exists(marker), argument + " deleted an existing file");
            assertEquals("existing-file-must-be-reviewed\n", Files.readString(marker));
            assertNotEquals(0, process.exitValue(), argument + " must reject the obsolete mode");
            assertTrue(Files.readString(log).contains("disabled"));
        }
    }
}
