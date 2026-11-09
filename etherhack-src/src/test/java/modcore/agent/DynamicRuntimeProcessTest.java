package modcore.agent;

import modcore.attach.LaunchCoordinator;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DynamicRuntimeProcessTest {
    @Test
    void productionAgentTransformsSignedFixtureAndRejectsInputDriftBeforeMain() throws Exception {
        LaunchGateFixture fixture = LaunchGateFixture.create();
        for (boolean wrong : new boolean[]{false, true}) {
            for (Path java : fixture.runtimes()) {
                try (var launch = LaunchCoordinator.start(java, Path.of(System.getProperty("gate.jarFile")),
                        Path.of(System.getProperty("agent.jarFile")), fixture.productionRuntime(wrong), fixture.game(),
                        fixture.game().toString(), fixture.root(), Duration.ofSeconds(30),
                        List.of("-Xverify:all", "-Dfixture.agentClass=modcore.agent.AgentMain"), List.of("first argument", "中文参数"))) {
                    launch.awaitWaiting();
                    launch.attach();
                    launch.release();
                    assertTrue(launch.process().waitFor(30, TimeUnit.SECONDS));
                    String log = Files.readString(launch.directory().resolve("game.log"));
                    if (wrong) {
                        assertNotEquals(0, launch.process().exitValue(), log);
                        assertFalse(Files.exists(launch.directory().resolve("main-calls")), log);
                        assertTrue(log.contains("digest") && log.contains("fixture/game/Target"), log);
                    } else {
                        assertEquals(0, launch.process().exitValue(), log);
                        assertTrue(log.contains("SIGNED-GATE-PASS"), log);
                    }
                }
            }
        }
    }

    @Test
    void realPreparedPatchSetLinksOnGameJreWithoutInitializingGame() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path runtime = root.resolve("runtime.jar");
        Class<?> loadout = Class.forName("modcore.build.AgentLoadout");
        var prepare = assertDoesNotThrow(() -> loadout.getMethod("prepareRuntime", Path.class, Path.class, Path.class),
                "ordinary runtime preparation must be available");
        Path game = Path.of(System.getProperty("headshot.gameJar"));
        prepare.invoke(null, game, Path.of(System.getProperty("agent.payloadJar")), runtime);
        PayloadArchive archive = PayloadArchive.readRuntime(runtime);
        assertFalse(archive.patchNames().isEmpty());
        String fixture = System.getenv("MODCORE_NATIVE_FIXTURE");
        assertNotNull(fixture, "full game JRE fixture is required");
        Path java = Path.of(fixture, "jre64/bin/java.exe");
        try (var launch = LaunchCoordinator.start(java, Path.of(System.getProperty("gate.jarFile")),
                Path.of(System.getProperty("agent.jarFile")), runtime, game, game.toString(), root,
                Duration.ofSeconds(30), List.of("-Xverify:all", "-Dmodcore.verifyOnly=true"), List.of())) {
            launch.awaitWaiting();
            launch.attach();
            launch.release();
            if (!launch.process().waitFor(45, TimeUnit.SECONDS)) {
                launch.process().destroyForcibly();
                fail("Non-initializing verification timed out");
            }
            String log = Files.readString(launch.directory().resolve("game.log"));
            assertEquals(0, launch.process().exitValue(), log);
            assertTrue(log.contains("Verified " + archive.patchNames().size() + " prepared class transformations"), log);
            assertTrue(log.contains("VERIFY_ONLY"), log);
            assertFalse(log.contains("GAME_STARTING"), log);
            assertFalse(log.contains("JAVA_READY"), log);
            assertFalse(Files.exists(launch.directory().resolve("Zomboid")), "preflight must not initialize the game");
        }
    }
}
