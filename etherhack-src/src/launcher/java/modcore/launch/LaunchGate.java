package modcore.launch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Holds the original main thread until a verified first-definition agent is ready. */
public final class LaunchGate {
    private static final Object LOCK = new Object();
    private static Path directory;
    private static Properties request;
    private static String requestDigest;
    private static long pid;
    private static long started;
    private static String phase;
    private static String failure;
    private static Runnable preflight;
    private static String mainClass;
    private static boolean installed;
    private static java.nio.channels.FileChannel deploymentChannel;
    private static java.nio.channels.FileLock deploymentLock;

    private LaunchGate() {}

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Throwable error) {
            Throwable cause = error instanceof InvocationTargetException invocation ? invocation.getCause() : error;
            fail("STARTUP_FAILED", cause.toString());
            cause.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        if (!LaunchGate.class.getModule().isNamed()) throw new IOException("Launch gate must run as a named module");
        Path file = SessionFiles.regular(Path.of(System.getProperty("modcore.session"), "request.properties"));
        String lockPath = System.getProperty("modcore.lock");
        if (lockPath != null) {
            Path lock = SessionFiles.regular(Path.of(lockPath));
            deploymentChannel = java.nio.channels.FileChannel.open(lock, java.nio.file.StandardOpenOption.READ);
            deploymentLock = deploymentChannel.tryLock(0, Long.MAX_VALUE, true);
            if (deploymentLock == null) throw new IOException("Deployment is locked by an installer");
        }
        synchronized (LOCK) {
            directory = file.getParent();
            request = SessionFiles.read(file, SessionFiles.REQUEST);
            requestDigest = SessionFiles.digest(file);
            System.setProperty("modcore.sessionId", request.getProperty("session"));
            System.setProperty("modcore.buildId", request.getProperty("runtime.sha256"));
            pid = ProcessHandle.current().pid();
            started = ProcessHandle.current().info().startInstant().orElseThrow().toEpochMilli();
            SessionFiles.verifyArtifacts(request);
            Path source = Path.of(LaunchGate.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            if (!source.equals(Path.of(request.getProperty("gate")).toRealPath())) throw new IOException("Launch gate source differs");
            publish("WAITING_ATTACH", "", "");
        }
        long timeout = SessionFiles.positive(request, "timeout");
        if (timeout > 300000) throw new IOException("Attach timeout exceeds limit");
        long deadline = System.nanoTime() + timeout * 1_000_000;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (LOCK) {
                if (!"FAILED".equals(phase)) {
                    try { publish("EXITED", "", ""); }
                    catch (IOException e) { System.err.println("[ModCore Gate] Could not record exit: " + e); }
                }
            }
        }, "modcore-session-exit"));
        while (true) {
            synchronized (LOCK) {
                if (failure != null) throw new IOException(failure);
                if (Files.exists(directory.resolve("release.properties"))) {
                    if (!installed) throw new IOException("Release before agent installation");
                    Properties release = SessionFiles.read(directory.resolve("release.properties"), SessionFiles.RELEASE);
                    checkIdentity(release.getProperty("session"), SessionFiles.positive(release, "pid"),
                            SessionFiles.positive(release, "started"), release.getProperty("request"));
                    break;
                }
                if (System.nanoTime() >= deadline) {
                    fail("ATTACH_TIMEOUT", "The owned session was not released before the attach deadline");
                    throw new IOException(failure);
                }
                LOCK.wait(25);
            }
        }
        synchronized (LOCK) {
            if (failure != null) throw new IOException(failure);
            SessionFiles.verifyArtifacts(request);
            preflight.run();
            if (failure != null) throw new IOException(failure);
            publish("ARMED", "", "");
            if (Boolean.getBoolean("modcore.verifyOnly")) {
                System.out.println("[ModCore Gate] VERIFY_ONLY: official main was not invoked");
                return;
            }
            publish("GAME_STARTING", "", "");
        }
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        if (Thread.currentThread().getContextClassLoader() != loader) throw new IOException("Unexpected context loader");
        Class<?> main = Class.forName(mainClass, false, loader);
        if (main.getClassLoader() != loader) throw new IOException("Main class did not use the system loader");
        main.getMethod("main", String[].class).invoke(null, (Object) args);
    }

    public static void install(String session, long processId, long startTime, String digest,
                               String applicationMain, Runnable verification) throws IOException {
        synchronized (LOCK) {
            if (installed || !"WAITING_ATTACH".equals(phase) || failure != null) {
                fail("DUPLICATE_ATTACH", "Agent installation is not allowed in the current phase");
                throw new IOException("Agent already installed or session is not waiting");
            }
            try {
                checkIdentity(session, processId, startTime, digest);
                if (verification == null || !applicationMain.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+")) {
                    throw new IOException("Invalid agent callback");
                }
                installed = true;
                mainClass = applicationMain;
                preflight = verification;
                LOCK.notifyAll();
            } catch (IOException e) {
                fail("AGENT_IDENTITY", e.toString());
                throw e;
            }
        }
    }

    public static void runtimeState(String state, long generation, String detail) throws IOException {
        synchronized (LOCK) {
            if (!java.util.Set.of("JAVA_READY", "LUA_READY", "UI_OPENED", "WAITING_USER", "FAILED").contains(state)
                    || generation < 0 || !installed) throw new IOException("Invalid runtime state event");
            publish(state, state.equals("FAILED") ? "RUNTIME_FAILED" : "", "generation=" + generation + " " + detail);
        }
    }

    public static void fail(String code, String detail) {
        synchronized (LOCK) {
            if (failure != null) return;
            failure = code + ": " + detail;
            System.err.println("[ModCore Gate] " + failure);
            if (directory != null && request != null && requestDigest != null) {
                try { publish("FAILED", code, detail); }
                catch (IOException e) { System.err.println("[ModCore Gate] Could not record failure: " + e); }
            }
            LOCK.notifyAll();
        }
    }

    private static void checkIdentity(String session, long processId, long startTime, String digest) throws IOException {
        if (request == null || !request.getProperty("session").equals(session) || pid != processId
                || started != startTime || !requestDigest.equals(digest)
                || !SessionFiles.digest(directory.resolve("request.properties")).equals(requestDigest)) {
            throw new IOException("Owned session identity mismatch");
        }
    }

    private static void publish(String state, String code, String detail) throws IOException {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("version", "1");
        fields.put("session", request.getProperty("session"));
        fields.put("pid", Long.toString(pid));
        fields.put("started", Long.toString(started));
        fields.put("request", requestDigest);
        fields.put("phase", state);
        fields.put("code", code);
        fields.put("detail", detail.length() > 4096 ? detail.substring(0, 4096) : detail);
        SessionFiles.write(directory, "state.properties", fields, true);
        phase = state;
        System.out.println("[ModCore Gate] " + state);
    }
}
