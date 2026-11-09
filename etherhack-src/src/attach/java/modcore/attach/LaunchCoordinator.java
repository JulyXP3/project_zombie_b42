package modcore.attach;

import com.sun.tools.attach.VirtualMachine;
import modcore.launch.SessionFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/** Creates the target itself; never searches for or adopts an existing game process. */
public final class LaunchCoordinator {
    /** 会话目录最多保留几份（含本次启动）：多余的按修改时间从旧到新删除。 */
    static final int SESSIONS_TO_KEEP = 3;

    private LaunchCoordinator() {}

    public static void main(String[] args) {
        try { run(args); }
        catch (Exception e) {
            System.err.println("[ModCore Coordinator] " + e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        JdkRequirement.requireCurrent();
        Path home = Path.of(System.getProperty("user.home"));
        boolean verify = false;
        for (int i = 0; i < args.length && !args[i].equals("--"); i++) {
            if (args[i].equals("--verify")) verify = true;
            if (args[i].equals("--user-home") && i + 1 < args.length) home = Path.of(args[++i]);
        }
        if (verify) { execute(args); return; }
        Path lockPath = home.resolve("Zomboid/modcore-deployment.lock");
        Files.createDirectories(lockPath.getParent());
        try (var channel = java.nio.channels.FileChannel.open(lockPath, java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.READ, java.nio.file.StandardOpenOption.WRITE)) {
            var lock = channel.tryLock(0, Long.MAX_VALUE, true);
            if (lock == null) throw new IOException("Deployment is locked by an installer");
            try (lock) { execute(args); }
        }
    }

    private static void execute(String[] args) throws Exception {
        Path game = null;
        Path home = Path.of(System.getProperty("user.home"));
        Path deployment = null;
        Path java = null;
        boolean verify = false;
        int index = 0;
        for (; index < args.length; index++) {
            String option = args[index];
            if (option.equals("--")) { index++; break; }
            if (option.equals("--verify")) { verify = true; continue; }
            if (++index == args.length) throw new IOException("Missing coordinator argument");
            switch (option) {
                case "--game-dir" -> game = Path.of(args[index]).toRealPath();
                case "--user-home" -> home = Path.of(args[index]).toRealPath();
                case "--deployment" -> deployment = Path.of(args[index]).toRealPath();
                case "--java" -> java = Path.of(args[index]).toRealPath();
                default -> throw new IOException("Unknown coordinator option: " + option);
            }
        }
        if (game == null) throw new IOException("Game directory is required");
        if (deployment == null) deployment = home.resolve("Zomboid/modcore-agent");
        if (!verify) verifyOwnership(deployment, game, home);
        if (java == null) java = game.resolve("jre64/bin/java.exe");
        List<String> vm = new ArrayList<>();
        List<String> gameArgs = new ArrayList<>();
        String classpath = game.resolve("projectzomboid.jar").toString();
        if (!verify) {
            boolean found = false;
            for (; index < args.length; index++) {
                if (args[index].equals("-cp")) {
                    if (index + 2 >= args.length || !args[index + 2].equals("zombie.gameStates.MainScreenState")) throw new IOException("Original game main is missing");
                    classpath = args[index + 1];
                    index += 3;
                    found = true;
                    break;
                }
                vm.add(args[index]);
            }
            if (!found) throw new IOException("Original game classpath is missing");
            for (; index < args.length; index++) gameArgs.add(args[index]);
        }
        boolean verifyOnly = verify || "1".equals(System.getenv("MODCORE_VERIFY_ONLY"));
        if (verifyOnly) vm.add("-Dmodcore.verifyOnly=true");
        if (!verify) vm.add("-Dmodcore.lock=" + home.resolve("Zomboid/modcore-deployment.lock"));
        vm.add("-Dmodcore.gameDir=" + game);
        vm.add("-Dmodcore.userHome=" + home);
        Path sessions = home.resolve("Zomboid/modcore-sessions");
        try (OwnedLaunch launch = start(java, deployment.resolve("launch-gate.jar"), deployment.resolve("agent.jar"),
                deployment.resolve("runtime.jar"), game.resolve("projectzomboid.jar"), classpath, sessions,
                Duration.ofSeconds(30), vm, gameArgs)) {
            launch.awaitWaiting();
            launch.attach();
            launch.release();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
            while (launch.process().isAlive()) {
                Properties state = SessionFiles.read(launch.directory().resolve("state.properties"), SessionFiles.STATE);
                String phase = state.getProperty("phase");
                if (phase.equals("LUA_READY") || phase.equals("UI_OPENED") || phase.equals("WAITING_USER")) {
                    System.out.println("[ModCore Coordinator] " + phase + " session=" + launch.directory());
                    return;
                }
                if (phase.equals("FAILED")) throw new IOException("Runtime initialization failed: " + state.getProperty("detail"));
                if (System.nanoTime() >= deadline) throw new IOException("Initialization still unconfirmed; game left running, see " + launch.directory());
                Thread.sleep(50);
            }
            String log = Files.readString(launch.directory().resolve("game.log"));
            System.out.print(log);
            if (launch.process().exitValue() != 0) throw new IOException("Owned game exited with " + launch.process().exitValue());
            if (!verifyOnly) throw new IOException("Game exited before initialization was confirmed; readiness is unconfirmed");
            Properties state = SessionFiles.read(launch.directory().resolve("state.properties"), SessionFiles.STATE);
            if (!state.getProperty("phase").equals("EXITED") || !log.contains("VERIFY_ONLY")) {
                throw new IOException("Non-initializing verification did not complete");
            }
        }
    }

    /**
     * 每次启动一份会话目录，不自动回收会越攒越多。这里只保留最新 {@link #SESSIONS_TO_KEEP} 份
     * （本次启动算一份），更旧的按修改时间删除；**正在运行的对局绝不删**——它的启动门还在往
     * state.properties 写阶段、协调器还在读，删掉会让那次启动直接失败。
     */
    static void pruneSessions(Path sessions, Path current) {
        List<Path> candidates = new ArrayList<>();
        try (var entries = Files.list(sessions)) {
            for (Path entry : entries.toList()) {
                if (!Files.isDirectory(entry) || entry.equals(current)) continue;
                if (!entry.getFileName().toString().startsWith("session-")) continue;
                if (sessionInUse(entry)) continue;
                candidates.add(entry);
            }
        } catch (IOException ignored) {
            return;   // 目录读不到就跳过清理，不影响本次启动
        }
        candidates.sort(Comparator.comparingLong(LaunchCoordinator::lastModified).reversed());
        int removed = 0;
        for (int i = SESSIONS_TO_KEEP - 1; i < candidates.size(); i++) {
            if (deleteTree(candidates.get(i))) removed++;
        }
        if (removed > 0) {
            System.out.println("[ModCore Coordinator] Removed " + removed
                    + " old launch session folder(s); keeping the newest " + SESSIONS_TO_KEEP + ".");
        }
    }

    /** 目录里的 pid 仍活着且进程启动时间一致，才算"对局在跑"。 */
    static boolean sessionInUse(Path session) {
        Path state = session.resolve("state.properties");
        if (!Files.isRegularFile(state)) return false;
        Properties properties = new Properties();
        try (var in = Files.newInputStream(state)) {
            properties.load(in);
        } catch (IOException e) {
            return false;
        }
        try {
            long pid = Long.parseLong(properties.getProperty("pid", ""));
            long started = Long.parseLong(properties.getProperty("started", ""));
            return ProcessHandle.of(pid)
                    .filter(ProcessHandle::isAlive)
                    .map(handle -> handle.info().startInstant()
                            .map(instant -> instant.toEpochMilli() == started).orElse(false))
                    .orElse(false);
        } catch (RuntimeException e) {
            return false;   // pid/started 缺失或格式不对：按"没在用"处理，删不掉也会自然保留
        }
    }

    private static boolean deleteTree(Path directory) {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            return false;   // 被占用（例如同类会话仍在写）就留着，下次启动再清
        }
    }

    private static long lastModified(Path directory) {
        try {
            return Files.getLastModifiedTime(directory).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static void verifyOwnership(Path deployment, Path game, Path home) throws IOException {
        Path record = SessionFiles.regular(deployment.resolve("installation.properties"));
        if (Files.size(record) > 65536) throw new IOException("Installation ownership record is too large");
        Properties properties = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("Duplicate ownership field");
                return super.put(key, value);
            }
        };
        try (var in = Files.newInputStream(record)) { properties.load(in); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid ownership record", e); }
        if (!"3".equals(properties.getProperty("format")) || !game.toString().equals(properties.getProperty("game"))
                || !home.toString().equals(properties.getProperty("home"))
                || !SessionFiles.digest(game.resolve("projectzomboid.jar")).equals(properties.getProperty("game.sha256"))) {
            throw new IOException("Installation ownership or game version mismatch; reinstall for this version");
        }
        List<String> launchFiles = List.of("ProjectZomboid64.bat", "ProjectZomboid64ShowConsole.bat", "ProjectZomboidOpenGLDebug64.bat");
        List<String> runtimeFiles = List.of("runtime.jar", "agent.jar", "coordinator.jar", "launch-gate.jar", "installer.jar");
        java.util.Set<String> owned = new java.util.HashSet<>(List.of("installation.properties"));
        for (String key : properties.stringPropertyNames()) {
            if (List.of("format", "game", "home", "game.sha256").contains(key)) continue;
            if (key.startsWith("file.")) {
                String name = key.substring(5);
                if (!runtimeFiles.contains(name)) {
                    throw new IOException("Invalid component ownership path: " + name);
                }
                if (!SessionFiles.digest(deployment.resolve(name)).equals(properties.getProperty(key))) {
                    throw new IOException("Managed component ownership mismatch: " + name);
                }
                owned.add(name);
            } else if (launchFiles.stream().noneMatch(name -> key.equals("original." + name) || key.equals("installed." + name))) {
                throw new IOException("Unexpected ownership record field: " + key);
            }
        }
        for (String name : runtimeFiles) {
            if (!properties.containsKey("file." + name)) throw new IOException("Managed component ownership missing: " + name);
        }
        for (String name : launchFiles) {
            String original = properties.getProperty("original." + name);
            String installed = properties.getProperty("installed." + name);
            if (original == null && installed == null && !name.equals("ProjectZomboid64.bat")
                    && !Files.exists(game.resolve(name))) continue;
            if (original == null || installed == null
                    || !SessionFiles.digest(deployment.resolve(name + ".original")).equals(original)
                    || !SessionFiles.digest(game.resolve(name)).equals(installed)) {
                throw new IOException("Launch-file ownership mismatch: " + name);
            }
            owned.add(name + ".original");
        }
        try (var paths = Files.walk(deployment)) {
            for (Path file : paths.toList()) {
                if (file.equals(deployment)) continue;
                String name = deployment.relativize(file).toString().replace('\\', '/');
                if (Files.isSymbolicLink(file) || !file.toRealPath().equals(file.toAbsolutePath().normalize())) {
                    throw new IOException("Redirected ownership path: " + name);
                }
                if (Files.isDirectory(file)) {
                    if (owned.stream().noneMatch(entry -> entry.startsWith(name + "/"))) {
                        throw new IOException("Unknown directory in owned deployment: " + name);
                    }
                } else if (!owned.contains(name)) {
                    throw new IOException("Unknown file in owned deployment: " + name);
                }
            }
        }
    }

    public static OwnedLaunch start(Path java, Path gate, Path agent, Path runtime, Path game,
                                    String classpath, Path sessions, Duration timeout,
                                    List<String> vmArguments, List<String> gameArguments) throws IOException {
        java = SessionFiles.regular(java);
        gate = SessionFiles.regular(gate);
        agent = SessionFiles.regular(agent);
        runtime = SessionFiles.regular(runtime);
        game = SessionFiles.regular(game);
        long millis = timeout.toMillis();
        if (millis <= 0 || millis > 300000) throw new IOException("Attach timeout must be between 1 and 300000 milliseconds");
        Files.createDirectories(sessions);
        Path directory = Files.createTempDirectory(sessions.toRealPath(), "session-");
        pruneSessions(directory.getParent(), directory);
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String token = HexFormat.of().formatHex(random);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("version", "1");
        fields.put("session", token);
        fields.put("timeout", Long.toString(millis));
        for (var artifact : Map.of("game", game, "agent", agent, "runtime", runtime, "gate", gate).entrySet()) {
            fields.put(artifact.getKey(), artifact.getValue().toString());
            fields.put(artifact.getKey() + ".sha256", SessionFiles.digest(artifact.getValue()));
        }
        SessionFiles.write(directory, "request.properties", fields, false);
        List<String> command = new ArrayList<>(List.of(java.toString()));
        command.addAll(vmArguments);
        command.addAll(List.of("-XX:+EnableDynamicAgentLoading", "--add-modules", "ALL-DEFAULT",
                "-Dmodcore.session=" + directory, "-Duser.home=" + directory,
                "-Djava.io.tmpdir=" + directory, "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "--module-path", gate.toString(), "-cp", classpath, "-m", "modcore.launch/modcore.launch.LaunchGate"));
        command.addAll(gameArguments);
        Path work = directory;
        Path userHome = directory;
        for (String arg : vmArguments) {
            if (arg.startsWith("-Dmodcore.gameDir=")) work = Path.of(arg.substring("-Dmodcore.gameDir=".length()));
            if (arg.startsWith("-Dmodcore.userHome=")) userHome = Path.of(arg.substring("-Dmodcore.userHome=".length()));
        }
        command.set(command.indexOf("-Duser.home=" + directory), "-Duser.home=" + userHome);
        ProcessBuilder builder = new ProcessBuilder(command).directory(work.toFile())
                .redirectInput(ProcessBuilder.Redirect.from(directory.resolve("stdin.txt").toFile()))
                .redirectErrorStream(true).redirectOutput(directory.resolve("game.log").toFile());
        Files.createFile(directory.resolve("stdin.txt"));
        for (String name : List.of("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH")) {
            builder.environment().remove(name);
        }
        builder.environment().put("USERPROFILE", userHome.toString());
        if (userHome.equals(directory)) {
            builder.environment().put("LOCALAPPDATA", directory.toString());
            builder.environment().put("APPDATA", directory.toString());
        }
        builder.environment().put("TEMP", directory.toString());
        builder.environment().put("TMP", directory.toString());
        Process process = builder.start();
        return new OwnedLaunch(process, directory, token, agent, work, millis);
    }

    public static final class OwnedLaunch implements AutoCloseable {
        private final Process process;
        private final Path directory;
        private final String token;
        private final Path agent;
        private final Path work;
        private final long started;
        private final String requestDigest;
        private final long deadline;
        private boolean attached;
        private boolean released;

        private OwnedLaunch(Process process, Path directory, String token, Path agent, Path work, long timeout) throws IOException {
            this.process = process;
            this.directory = directory;
            this.token = token;
            this.agent = agent;
            this.work = work;
            this.started = process.info().startInstant().orElseThrow(() -> new IOException("Target start time unavailable")).toEpochMilli();
            this.requestDigest = SessionFiles.digest(directory.resolve("request.properties"));
            this.deadline = System.nanoTime() + timeout * 1_000_000;
        }

        public Process process() { return process; }
        public Path directory() { return directory; }

        public void awaitWaiting() throws Exception {
            while (true) {
                requireAlive();
                if (Files.exists(directory.resolve("state.properties"))) {
                    requireWaiting();
                    return;
                }
                if (System.nanoTime() >= deadline) throw new IOException("Startup handshake timed out");
                Thread.sleep(20);
            }
        }

        public void attach() throws Exception {
            if (attached) {
                closeUnreleased();
                throw new IOException("Duplicate attach attempt for owned session");
            }
            requireWaiting();
            SessionFiles.verifyArtifacts(SessionFiles.read(directory.resolve("request.properties"), SessionFiles.REQUEST));
            String agentPath = attachPath(agent);
            String requestPath = attachPath(directory.resolve("request.properties"));
            VirtualMachine vm = VirtualMachine.attach(Long.toString(process.pid()));
            try { vm.loadAgent(agentPath, requestPath); }
            finally { vm.detach(); }
            requireWaiting();
            attached = true;
        }

        private String attachPath(Path file) throws IOException {
            String path = file.toString();
            if (path.chars().allMatch(c -> c < 128)) return path;
            if (work.getRoot().equals(file.getRoot())) {
                String relative = work.relativize(file).toString();
                if (relative.chars().allMatch(c -> c < 128)) return relative;
            }
            throw new IOException("Attach path cannot be represented safely relative to the game directory: " + file);
        }

        public void release() throws IOException {
            if (!attached || released) throw new IOException("Session is not awaiting release");
            requireWaiting();
            SessionFiles.write(directory, "release.properties", Map.of("version", "1", "session", token,
                    "pid", Long.toString(process.pid()), "started", Long.toString(started), "request", requestDigest), false);
            released = true;
        }

        private void requireWaiting() throws IOException {
            requireAlive();
            Properties state = SessionFiles.read(directory.resolve("state.properties"), SessionFiles.STATE);
            if (!token.equals(state.getProperty("session")) || process.pid() != SessionFiles.positive(state, "pid")
                    || started != SessionFiles.positive(state, "started") || !requestDigest.equals(state.getProperty("request"))
                    || !SessionFiles.digest(directory.resolve("request.properties")).equals(requestDigest)) {
                throw new IOException("Owned process/session identity mismatch");
            }
            if (!state.getProperty("phase").equals("WAITING_ATTACH")) throw new IOException("Target is not waiting: " + state.getProperty("phase"));
        }

        private void requireAlive() throws IOException {
            if (!process.isAlive() || process.info().startInstant().map(time -> time.toEpochMilli() != started).orElse(true)) {
                throw new IOException("Owned target exited or its identity changed");
            }
        }

        private void closeUnreleased() throws InterruptedException {
            if (!released && process.isAlive()) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
            }
        }

        @Override public void close() throws InterruptedException { closeUnreleased(); }
    }
}
