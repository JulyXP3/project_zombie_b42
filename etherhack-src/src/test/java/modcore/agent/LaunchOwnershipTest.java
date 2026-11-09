package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LaunchOwnershipTest {
    @Test
    void unknownDeploymentFileAndChangedBatchAreRejectedBeforeTargetCreation() throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        var install = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
        assertEquals(0, install.exit(), install.output());
        Path deployment = f.home().resolve("Zomboid/modcore-agent");
        Path unknown = deployment.resolve("unknown.jar");
        Files.writeString(unknown, "must be preserved");
        assertRejectedBeforeSession(f, deployment);
        assertEquals("must be preserved", Files.readString(unknown));
        Files.delete(unknown);
        Path batch = f.game().resolve("ProjectZomboid64.bat");
        Files.writeString(batch, Files.readString(batch).replace("3072", "4096"));
        assertRejectedBeforeSession(f, deployment);
    }

    private static void assertRejectedBeforeSession(ThreeFileDeploymentTest.Fixture f, Path deployment) throws Exception {
        Path log = Files.createTempFile(f.root(), "ownership-", ".log");
        ProcessBuilder builder = new ProcessBuilder(List.of(Path.of(System.getProperty("java.home"), "bin/java.exe").toString(),
                "-cp", deployment.resolve("coordinator.jar").toString(), "modcore.attach.LaunchCoordinator",
                "--game-dir", f.game().toString(), "--user-home", f.home().toString(),
                "--java", Path.of(System.getProperty("java.home"), "bin/java.exe").toString(), "--",
                "-cp", "./;projectzomboid.jar", "zombie.gameStates.MainScreenState"))
                .directory(f.game().toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("MODCORE_VERIFY_ONLY", "1");
        Process process = builder.start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS));
        String output = Files.readString(log);
        assertNotEquals(0, process.exitValue(), output);
        assertFalse(Files.exists(f.home().resolve("Zomboid/modcore-sessions")), output);
    }

    @Test
    void changedManagedRuntimeIsRejectedBeforeStartingTarget() throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        var install = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
        assertEquals(0, install.exit(), install.output());
        Path deployment = f.home().resolve("Zomboid/modcore-agent");
        Files.writeString(deployment.resolve("runtime.jar"), "changed");
        Path log = f.root().resolve("coordinator.log");
        ProcessBuilder builder = new ProcessBuilder(List.of(Path.of(System.getProperty("java.home"), "bin/java.exe").toString(),
                "-cp", deployment.resolve("coordinator.jar").toString(), "modcore.attach.LaunchCoordinator",
                "--game-dir", f.game().toString(), "--user-home", f.home().toString(),
                "--java", Path.of(System.getProperty("java.home"), "bin/java.exe").toString(), "--",
                "-cp", "./;projectzomboid.jar", "zombie.gameStates.MainScreenState"))
                .directory(f.game().toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("MODCORE_VERIFY_ONLY", "1");
        Process process = builder.start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS));
        String output = Files.readString(log);
        assertNotEquals(0, process.exitValue(), output);
        Path sessions = f.home().resolve("Zomboid/modcore-sessions");
        assertFalse(Files.exists(sessions), "ownership must be verified before a target process or session is created");
        assertTrue(output.contains("ownership"), output);
    }
}
