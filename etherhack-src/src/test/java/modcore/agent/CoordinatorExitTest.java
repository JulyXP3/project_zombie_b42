package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CoordinatorExitTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void coordinatorRequiresConfirmedReadinessAndLeavesGameRunningAtUserWait(boolean waitForUser) throws Exception {
        var fixture = LaunchGateFixture.create();
        Path deployment = Files.createDirectories(fixture.root().resolve("deployment"));
        Path home = Files.createDirectories(fixture.root().resolve("home"));
        Path game = Files.createDirectories(fixture.root().resolve("game"));
        Files.copy(fixture.game(), game.resolve("projectzomboid.jar"));
        for (var item : java.util.Map.of("agent.jar", Path.of(System.getProperty("agent.jarFile")),
                "launch-gate.jar", Path.of(System.getProperty("gate.jarFile")), "runtime.jar", fixture.productionRuntime(false),
                "coordinator.jar", Path.of(System.getProperty("gate.jarFile")).resolveSibling("coordinator.jar")).entrySet()) {
            Files.copy(item.getValue(), deployment.resolve(item.getKey()));
        }
        Properties record = new Properties();
        record.setProperty("format", "3");
        record.setProperty("game", game.toString());
        record.setProperty("home", home.toString());
        record.setProperty("game.sha256", PayloadArchive.digest(game.resolve("projectzomboid.jar")));
        for (String name : List.of("agent.jar", "launch-gate.jar", "runtime.jar", "coordinator.jar")) {
            record.setProperty("file." + name, PayloadArchive.digest(deployment.resolve(name)));
        }
        Files.writeString(deployment.resolve("installer.jar"), "unused fixture component");
        record.setProperty("file.installer.jar", PayloadArchive.digest(deployment.resolve("installer.jar")));
        byte[] original = LaunchConfigurationTest.BATCH.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] installed = modcore.build.LaunchConfiguration.editBatch(original);
        Files.write(deployment.resolve("ProjectZomboid64.bat.original"), original);
        Files.write(game.resolve("ProjectZomboid64.bat"), installed);
        record.setProperty("original.ProjectZomboid64.bat", PayloadArchive.digest(original));
        record.setProperty("installed.ProjectZomboid64.bat", PayloadArchive.digest(installed));
        try (var out = Files.newOutputStream(deployment.resolve("installation.properties"))) { record.store(out, null); }
        Path log = fixture.root().resolve("coordinator.log");
        Process coordinator = new ProcessBuilder(List.of(Path.of(System.getProperty("java.home"), "bin/java.exe").toString(),
                "-cp", deployment.resolve("coordinator.jar").toString(), "modcore.attach.LaunchCoordinator", "--game-dir", game.toString(),
                "--user-home", home.toString(), "--deployment", deployment.toString(), "--java", fixture.runtimes().getFirst().toString(),
                "--", "-Dfixture.agentClass=modcore.agent.AgentMain", "-Dfixture.waitForUser=" + waitForUser, "-cp", game.resolve("projectzomboid.jar").toString(),
                "zombie.gameStates.MainScreenState", "first argument", "中文参数"))
                .directory(fixture.root().toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        assertTrue(coordinator.waitFor(30, TimeUnit.SECONDS));
        if (!waitForUser) {
            assertNotEquals(0, coordinator.exitValue(), "zero game exit is not proof of initialization: " + Files.readString(log));
            assertTrue(Files.readString(log).contains("unconfirmed"));
            return;
        }
        assertEquals(0, coordinator.exitValue(), new String(Files.readAllBytes(log)));
        Path session;
        try (var paths = Files.list(home.resolve("Zomboid/modcore-sessions"))) { session = paths.findFirst().orElseThrow(); }
        Properties state = new Properties();
        try (var reader = Files.newBufferedReader(session.resolve("state.properties"))) { state.load(reader); }
        ProcessHandle target = ProcessHandle.of(Long.parseLong(state.getProperty("pid"))).orElseThrow();
        try {
            assertTrue(target.isAlive());
            assertEquals("WAITING_USER", state.getProperty("phase"));
            try (var channel = java.nio.channels.FileChannel.open(home.resolve("Zomboid/modcore-deployment.lock"),
                    java.nio.file.StandardOpenOption.READ, java.nio.file.StandardOpenOption.WRITE)) {
                var exclusive = channel.tryLock();
                try { assertNull(exclusive, "the game must retain the shared deployment lock after its coordinator exits"); }
                finally { if (exclusive != null) exclusive.release(); }
            }
            Files.writeString(session.resolve("user-done"), "done");
            target.onExit().get(15, TimeUnit.SECONDS);
            assertTrue(Files.readString(session.resolve("game.log")).contains("USER-WAIT-FINISHED"));
        } finally { if (target.isAlive()) target.destroyForcibly(); }
    }
}
