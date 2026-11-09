package modcore.agent;

import modcore.attach.LaunchCoordinator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SignedInstrumentationProcessTest {
    private static LaunchGateFixture fixture;

    @BeforeAll
    static void buildFixture() throws Exception { fixture = LaunchGateFixture.create(); }

    @Test
    void signedClassCallsPayloadHelperAndKeepsSignerWithoutExtraction() throws Exception {
        for (Path java : fixture.runtimes()) {
            try (var launch = launch(java, fixture.productionRuntime(false), fixture.game().toString())) {
                launch.awaitWaiting();
                launch.attach();
                launch.release();
                assertTrue(launch.process().waitFor(30, TimeUnit.SECONDS));
                String output = Files.readString(launch.directory().resolve("game.log"));
                assertEquals(0, launch.process().exitValue(), output);
                assertTrue(output.contains("SIGNED-GATE-PASS"), output);
                assertEquals(fixture.gameDigest(), PayloadArchive.digest(fixture.game()));
                assertFalse(Files.exists(launch.directory().resolve("modcore")), "runtime classes must not be extracted");
                assertFalse(Files.exists(launch.directory().resolve("fixture")), "game classes must not be extracted");
            }
        }
    }

    @Test
    void mismatchedPatchInputStopsBeforeApplicationMain() throws Exception {
        try (var launch = launch(fixture.runtimes().getFirst(), fixture.productionRuntime(true), fixture.game().toString())) {
            launch.awaitWaiting();
            launch.attach();
            launch.release();
            assertTrue(launch.process().waitFor(30, TimeUnit.SECONDS));
            String output = Files.readString(launch.directory().resolve("game.log"));
            assertNotEquals(0, launch.process().exitValue(), output);
            assertTrue(output.contains("digest"), output);
            assertFalse(Files.exists(launch.directory().resolve("main-calls")));
        }
    }

    @Test
    void gateRequiresAgentAndRejectsRuntimeAlreadyVisibleOnGameClasspath() throws Exception {
        Path runtime = fixture.productionRuntime(false);
        try (var launch = launch(fixture.runtimes().getFirst(), runtime, fixture.game().toString())) {
            launch.awaitWaiting();
            assertThrows(Exception.class, launch::release);
            assertFalse(Files.exists(launch.directory().resolve("main-calls")));
        }
        try (var launch = launch(fixture.runtimes().getFirst(), runtime, fixture.game() + java.io.File.pathSeparator + runtime)) {
            launch.awaitWaiting();
            assertThrows(Exception.class, launch::attach);
            assertTrue(launch.process().waitFor(15, TimeUnit.SECONDS));
            assertNotEquals(0, launch.process().exitValue());
            assertFalse(Files.exists(launch.directory().resolve("main-calls")));
            assertTrue(Files.readString(launch.directory().resolve("game.log")).contains("already visible"));
        }
    }

    private static LaunchCoordinator.OwnedLaunch launch(Path java, Path runtime, String classpath) throws Exception {
        return LaunchCoordinator.start(java, Path.of(System.getProperty("gate.jarFile")), Path.of(System.getProperty("agent.jarFile")),
                runtime, fixture.game(), classpath, fixture.root(), Duration.ofSeconds(30),
                List.of("-Xverify:all", "-Dfixture.agentClass=modcore.agent.AgentMain"), List.of("first argument", "中文参数"));
    }
}
