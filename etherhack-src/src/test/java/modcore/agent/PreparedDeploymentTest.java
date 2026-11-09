package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PreparedDeploymentTest {
    @Test
    void realPreviewPayloadLoadsAllPreparedTargetsWithoutStartingGame() throws Exception {
        var f = ThreeFileDeploymentTest.fixture();
        var install = ThreeFileDeploymentTest.run(f, "--install-agent", "--yes");
        assertEquals(0, install.exit(), install.output());
        Path deployment = f.home().resolve("Zomboid/modcore-agent");
        PayloadArchive archive = PayloadArchive.readRuntime(deployment.resolve("runtime.jar"));
        assertEquals(PayloadArchive.digest(f.game().resolve("projectzomboid.jar")), archive.gameDigest());
        assertFalse(archive.patchNames().isEmpty());
        assertNotNull(archive.open("modcore/core/RuntimeBootstrap.class"));
        assertNull(archive.open("modcore/GamePatcher.class"));
        Path script = Path.of(System.getProperty("agent.launchScript"));
        Path scriptLog = f.root().resolve("script.log");
        Process launch = new ProcessBuilder(List.of("pwsh", "-NoLogo", "-NoProfile", "-File", script.toString(),
                "-GameDirectory", f.game().toString(), "-DeploymentDirectory", deployment.toString(),
                "-JavaExecutable", Path.of(System.getProperty("java.home"), "bin/java.exe").toString(),
                "-UserHome", f.home().toString(), "-VerifyOnly"))
                .directory(f.root().toFile()).redirectErrorStream(true).redirectOutput(scriptLog.toFile()).start();
        assertTrue(launch.waitFor(90, TimeUnit.SECONDS));
        String output = Files.readString(scriptLog);
        assertEquals(0, launch.exitValue(), output);
        assertTrue(output.contains("Verified " + archive.patchNames().size()), output);
        assertTrue(output.contains("runtime classes linked") && output.contains("VERIFY_ONLY"), output);
        assertFalse(output.contains("GAME_STARTING"), output);
        assertFalse(Files.exists(f.game().resolve("zombie")));
        assertFalse(Files.exists(f.game().resolve("modcore")));
        var uninstall = ThreeFileDeploymentTest.run(f, "--uninstall-agent", "--yes");
        assertEquals(0, uninstall.exit(), uninstall.output());
    }
}
