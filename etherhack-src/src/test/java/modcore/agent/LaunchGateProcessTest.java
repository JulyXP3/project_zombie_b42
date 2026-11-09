package modcore.agent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LaunchGateProcessTest {
    private static LaunchGateFixture fixture;

    @BeforeAll
    static void fixture() throws Exception {
        fixture = LaunchGateFixture.create();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 5000, 30000})
    void delayedAttachNeverInitializesGameAndReleasesMainExactlyOnce(long delay) throws Exception {
        for (Path java : fixture.runtimes()) {
            try (Launch launch = start(java, 60000, fixture.agent(), List.of())) {
                launch.call("awaitWaiting");
                Thread.sleep(delay);
                assertTrue(launch.process().isAlive(), launch.output());
                assertEquals("WAITING_ATTACH", launch.state().getProperty("phase"));
                assertFalse(Files.exists(launch.directory().resolve("game-initialized")));
                assertFalse(Files.exists(launch.directory().resolve("main-calls")));
                launch.call("attach");
                assertFalse(Files.exists(launch.directory().resolve("game-initialized")), launch.output());
                launch.call("release");
                assertTrue(launch.process().waitFor(30, TimeUnit.SECONDS), launch.output());
                assertEquals(0, launch.process().exitValue(), launch.output());
                assertEquals("1", Files.readString(launch.directory().resolve("main-calls")));
                assertTrue(launch.output().contains("SIGNED-GATE-PASS"), launch.output());
                assertTrue(launch.output().contains("PRELOADED_TARGETS=0"), launch.output());
                assertEquals("EXITED", launch.state().getProperty("phase"));
                assertEquals(fixture.gameDigest(), PayloadArchive.digest(fixture.game()));
            }
        }
    }

    @Test
    void attachCannotOutliveTheOwnedLaunchDeadline() throws Exception {
        try (Launch launch = start(fixture.runtimes().getFirst(), 1200, fixture.agent(), List.of("-Dfixture.attachHoldMs=6000"))) {
            launch.call("awaitWaiting");
            long start = System.nanoTime();
            assertThrows(Exception.class, () -> launch.call("attach"));
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 4500,
                    "attach must be bounded even if the listener is stuck");
            assertFalse(Files.exists(launch.directory().resolve("main-calls")));
        }
    }

    @Test
    void missingAttachTimesOutWithoutRunningMain() throws Exception {
        try (Launch launch = start(fixture.runtimes().getFirst(), 700, fixture.agent(), List.of())) {
            launch.call("awaitWaiting");
            assertTrue(launch.process().waitFor(15, TimeUnit.SECONDS));
            assertNotEquals(0, launch.process().exitValue(), launch.output());
            assertEquals("FAILED", launch.state().getProperty("phase"));
            assertEquals("ATTACH_TIMEOUT", launch.state().getProperty("code"));
            assertFalse(Files.exists(launch.directory().resolve("main-calls")));
        }
    }

    @Test
    void duplicateAttachBeforeReleasePoisonsSession() throws Exception {
        try (Launch launch = start(fixture.runtimes().getFirst(), 15000, fixture.agent(), List.of())) {
            launch.call("awaitWaiting");
            launch.call("attach");
            assertThrows(Exception.class, () -> launch.call("attach"));
            assertThrows(Exception.class, () -> launch.call("release"));
            assertTrue(launch.process().waitFor(15, TimeUnit.SECONDS));
            assertNotEquals(0, launch.process().exitValue(), launch.output());
            assertFalse(Files.exists(launch.directory().resolve("main-calls")));
        }
    }

    @Test
    void changedSessionIdentityIsRefusedBeforeAttach() throws Exception {
        for (String key : List.of("pid", "started", "session", "request")) {
            try (Launch launch = start(fixture.runtimes().getFirst(), 15000, fixture.agent(), List.of())) {
                launch.call("awaitWaiting");
                Path stateFile = launch.directory().resolve("state.properties");
                Properties state = launch.state();
                state.setProperty(key, key.equals("pid") || key.equals("started") ? "1" : "0".repeat(64));
                try (var writer = Files.newBufferedWriter(stateFile)) { state.store(writer, null); }
                assertThrows(Exception.class, () -> launch.call("attach"), key);
                assertFalse(Files.exists(launch.directory().resolve("main-calls")));
            }
        }
    }

    @Test
    void deadOwnedProcessIsNotReplacedByAnotherAttachTarget() throws Exception {
        try (Launch launch = start(fixture.runtimes().getFirst(), 15000, fixture.agent(), List.of())) {
            launch.call("awaitWaiting");
            launch.process().destroyForcibly();
            assertTrue(launch.process().waitFor(10, TimeUnit.SECONDS));
            assertThrows(Exception.class, () -> launch.call("attach"));
            assertFalse(Files.exists(launch.directory().resolve("main-calls")));
        }
    }

    @Test
    void missingAgentAndMissingModuleDoNotStartGame() throws Exception {
        assertThrows(Exception.class, () -> start(fixture.runtimes().getFirst(), 15000,
                fixture.root().resolve("missing-agent.jar"), List.of()));
        try (Launch launch = start(fixture.runtimes().getFirst(), 15000, fixture.agent(),
                List.of("--limit-modules", "java.base"))) {
            assertThrows(Exception.class, () -> launch.call("awaitWaiting"));
            assertTrue(launch.process().waitFor(10, TimeUnit.SECONDS));
            assertNotEquals(0, launch.process().exitValue(), launch.output());
            assertFalse(Files.exists(launch.directory().resolve("main-calls")));
        }
    }

    @Test
    void alreadyLoadedTargetAndPreflightFailureNeverEnterMain() throws Exception {
        for (String fault : List.of("preloaded", "preflight", "duplicate")) {
            try (Launch launch = start(fixture.runtimes().getFirst(), 15000, fixture.agent(),
                    List.of("-Dfixture.fault=" + fault))) {
                launch.call("awaitWaiting");
                if (!fault.equals("preflight")) {
                    assertThrows(Exception.class, () -> launch.call("attach"));
                } else {
                    launch.call("attach");
                    launch.call("release");
                }
                assertTrue(launch.process().waitFor(15, TimeUnit.SECONDS));
                assertNotEquals(0, launch.process().exitValue(), launch.output());
                assertEquals("FAILED", launch.state().getProperty("phase"));
                assertFalse(Files.exists(launch.directory().resolve("main-calls")));
                assertFalse(Files.exists(launch.directory().resolve("game-initialized")));
            }
        }
    }

    private static Launch start(Path java, long timeout, Path agent, List<String> vmArgs) throws Exception {
        Class<?> coordinator = assertDoesNotThrow(() -> Class.forName("modcore.attach.LaunchCoordinator"),
                "the controlled launch coordinator must exist before the game can run");
        Object owned;
        try {
            owned = coordinator.getMethod("start", Path.class, Path.class, Path.class, Path.class,
                    Path.class, String.class, Path.class, Duration.class, List.class, List.class)
                    .invoke(null, java, Path.of(System.getProperty("gate.jarFile")), agent, fixture.runtime(),
                            fixture.game(), fixture.game().toString(), fixture.root(), Duration.ofMillis(timeout),
                            vmArgs, List.of("first argument", "中文参数"));
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
        return new Launch(owned);
    }

    private record Launch(Object owned) implements AutoCloseable {
        Object call(String name) throws Exception {
            try { return owned.getClass().getMethod(name).invoke(owned); }
            catch (InvocationTargetException e) {
                if (e.getCause() instanceof Exception exception) throw exception;
                throw e;
            }
        }
        Process process() throws Exception { return (Process) call("process"); }
        Path directory() throws Exception { return (Path) call("directory"); }
        String output() throws Exception {
            Path file = directory().resolve("game.log");
            return Files.exists(file) ? Files.readString(file) : "";
        }
        Properties state() throws Exception {
            Properties state = new Properties();
            try (var reader = Files.newBufferedReader(directory().resolve("state.properties"))) { state.load(reader); }
            return state;
        }
        @Override public void close() throws Exception { call("close"); }
    }
}
