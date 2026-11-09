package modcore.build;

import modcore.agent.PayloadArchive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Explicit, reversible deployment. Game archives, executable settings and saves are never replaced. */
public final class AgentInstaller {
    private AgentInstaller() {}

    public static void run(String[] args) throws Exception {
        boolean uninstall = args[0].equals("--uninstall-agent");
        boolean yes = false;
        Path game = Path.of("").toAbsolutePath();
        Path home = Path.of(System.getProperty("user.home"));
        for (int i = 1; i < args.length; i++) {
            String option = args[i];
            if (option.equals("--yes")) { yes = true; continue; }
            if (!option.equals("--game-dir") && !option.equals("--user-home")) throw new IOException("Unknown install option: " + option);
            if (++i == args.length) throw new IOException("Missing value for " + option);
            if (option.equals("--game-dir")) game = Path.of(args[i]);
            else home = Path.of(args[i]);
        }
        game = game.toRealPath();
        home = home.toRealPath();
        if (!Files.isRegularFile(game.resolve("projectzomboid.jar"))) throw new IOException("projectzomboid.jar is missing");
        try (DeploymentLock ignored = DeploymentLock.acquire(home)) {
            deploy(game, home, uninstall, yes);
        }
    }

    private static void deploy(Path game, Path home, boolean uninstall, boolean yes) throws Exception {
        InstallationRecord record = new InstallationRecord(game, home);
        boolean existing = record.exists();
        if (existing) {
            record.load();
            existing = !record.foreign;
        }
        if (uninstall && !existing) {
            // 放宽：没装过就当作已经卸干净，正常返回，不当成错误。
            warnLaunchFilesStillModded(game);
            removeStagingDirectories(home.resolve("Zomboid"));
            System.out.println("Nothing to uninstall: no managed installation was found for this game folder.");
            return;
        }
        if (!uninstall) preflight(record, existing);
        System.out.println((uninstall ? "Uninstall" : "Install") + " ModCore for: " + game);
        System.out.println("Game JAR/EXE/JSON, saves, user configuration and logs are not removed or replaced.");
        if (!yes) {
            var console = System.console();
            if (console == null || !"YES".equals(console.readLine("Close the game. Type YES to continue: "))) {
                throw new IOException("Confirmation was not granted; no installation changes were made");
            }
        }
        if (uninstall) uninstall(record);
        else install(record, existing);
    }

    private static void preflight(InstallationRecord record, boolean existing) throws IOException {
        for (String name : InstallationRecord.LAUNCH_FILES) {
            byte[] original = existing ? record.original.get(name)
                    : DeploymentTransaction.read(record.game.resolve(name), 1024 * 1024);
            if (original == null) {
                if (name.equals("ProjectZomboid64.bat")) throw new IOException("Required launch file is missing: " + name);
                continue;
            }
            record.original.put(name, original);
            record.installed.put(name, LaunchConfiguration.editBatch(original));
        }
    }

    private static void install(InstallationRecord record, boolean existing) throws Exception {
        Path cache = record.home.resolve("Zomboid");
        DeploymentTransaction.checkPath(cache);
        Files.createDirectories(cache);
        // 每次安装前先清掉上一次留下的临时目录：Zomboid 下最多只保留本次这一个 modcore-install-*。
        removeStagingDirectories(cache);
        Path staging = Files.createTempDirectory(cache, "modcore-install-");
        Path source = staging.resolve("payload.jar");
        Path runtime = staging.resolve("runtime.jar");
        Map<String, byte[]> components = new LinkedHashMap<>();
        for (String name : List.of("agent.jar", "launch-gate.jar", "coordinator.jar")) {
            byte[] bytes = resource("modcore/" + name, 8 * 1024 * 1024);
            Files.write(staging.resolve(name), bytes);
            components.put(name, bytes);
        }
        Files.write(source, resource("modcore/payload.jar", 64 * 1024 * 1024));
        Path self = Path.of(AgentInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        components.put("installer.jar", Files.readAllBytes(self));
        Path work = Files.createDirectory(staging.resolve("work"));
        Path isolatedHome = Files.createDirectory(staging.resolve("home"));
        Path java = Path.of(System.getProperty("java.home"), "bin/java.exe");
        command(java, work, isolatedHome, staging.resolve("prepare.log"), List.of("-jar", self.toString(), "--prepare-runtime",
                record.game.resolve("projectzomboid.jar").toString(), source.toString(), runtime.toString()));
        command(java, work, isolatedHome, staging.resolve("verify.log"),
                List.of("-cp", staging.resolve("coordinator.jar").toString(), "modcore.attach.LaunchCoordinator",
                        "--verify", "--java", java.toString(), "--deployment", staging.toString(),
                        "--game-dir", record.game.toString(), "--user-home", isolatedHome.toString()));
        components.put("runtime.jar", Files.readAllBytes(runtime));
        PayloadArchive archive = PayloadArchive.readRuntime(runtime);
        Map<Path, byte[]> before = new LinkedHashMap<>();
        Map<Path, byte[]> after = new LinkedHashMap<>();
        for (var component : components.entrySet()) {
            Path target = record.directory.resolve(component.getKey());
            change(before, after, target, existing ? record.current.get(target) : null, component.getValue());
        }
        if (existing) {
            for (var file : record.current.entrySet()) {
                if ((file.getKey().startsWith(record.directory) && !file.getKey().equals(record.manifest())
                        && record.original.keySet().stream().noneMatch(name -> file.getKey().equals(record.backup(name))))
                        || (record.legacy && file.getKey().equals(record.payload()))) {
                    if (!after.containsKey(file.getKey())) change(before, after, file.getKey(), file.getValue(), null);
                }
            }
        }
        for (String name : record.original.keySet()) change(before, after, record.backup(name),
                existing ? record.current.get(record.backup(name)) : null, record.original.get(name));
        change(before, after, record.manifest(), existing ? record.current.get(record.manifest()) : null, record.encode(components));
        for (String name : record.original.keySet()) change(before, after, record.game.resolve(name),
                existing ? record.current.get(record.game.resolve(name)) : record.original.get(name), record.installed.get(name));
        DeploymentTransaction.apply(before, after);
        try {
            if (record.bundledJava) removeEmptyOwnedJavaDirectories(record);
        } catch (IOException failure) {
            Map<Path, byte[]> rollbackBefore = new LinkedHashMap<>();
            Map<Path, byte[]> rollbackAfter = new LinkedHashMap<>();
            for (Path file : new ArrayList<>(before.keySet()).reversed()) {
                change(rollbackBefore, rollbackAfter, file, after.get(file), before.get(file));
            }
            try { DeploymentTransaction.apply(rollbackBefore, rollbackAfter); }
            catch (IOException rollback) { failure.addSuppressed(new IOException("Upgrade rollback requires manual review", rollback)); }
            throw failure;
        }
        System.out.println("Installation completed. Start with ProjectZomboid64.bat to play with ModCore.");
        System.out.println("ProjectZomboid64.exe keeps starting the unmodified game.");
        if (!archive.disabledPatches().isEmpty()) System.out.println("WARNING: Unavailable features in this payload: " + archive.disabledPatches());
        System.out.println("Preparation and verification logs: " + staging);
        // These are inspected, explicitly created temporary artifacts; keep logs and session evidence.
        for (String name : components.keySet()) {
            Path file = staging.resolve(name);
            if (!name.equals("installer.jar") && Files.isRegularFile(file)) Files.delete(file);
        }
        Files.delete(source);
    }

    private static void uninstall(InstallationRecord record) throws IOException {
        Map<Path, byte[]> before = new LinkedHashMap<>();
        Map<Path, byte[]> after = new LinkedHashMap<>();
        for (String name : record.original.keySet()) change(before, after, record.game.resolve(name), record.current.get(record.game.resolve(name)), record.original.get(name));
        for (var file : record.current.entrySet()) {
            if (!file.getKey().startsWith(record.game)) change(before, after, file.getKey(), file.getValue(), null);
        }
        DeploymentTransaction.apply(before, after);
        removeStagingDirectories(record.home.resolve("Zomboid"));
        removeSessionDirectories(record.home.resolve("Zomboid/modcore-sessions"));
        for (String name : InstallationRecord.LAUNCH_FILES) {
            if (record.original.containsKey(name)) continue;
            warnIfStillModded(record.game.resolve(name));
        }
        if (Files.isDirectory(record.directory)) {
            try (var paths = Files.walk(record.directory)) {
                for (Path directory : paths.filter(Files::isDirectory).sorted(java.util.Comparator.reverseOrder()).toList()) {
                    try (var entries = Files.list(directory)) { if (entries.findAny().isEmpty()) Files.delete(directory); }
                }
            }
        }
        System.out.println("Uninstall completed. Original launch files restored; user configurations, logs and saves preserved.");
    }

    /**
     * 卸载时回收每次启动留下的会话目录（原本不清，卸载后仍留一堆 game.log）。
     * 只删以 session- 开头的目录；正在运行的对局（pid 活着且启动时间一致）保持不动。
     * 与 LaunchCoordinator 里的同名逻辑是刻意重复的：两者分属不同构建单元，
     * 安装器 JAR 不打包协调器类，不能互相引用。
     */
    static void removeSessionDirectories(Path sessionsRoot) {
        if (!Files.isDirectory(sessionsRoot)) return;
        int removed = 0;
        int kept = 0;
        try (var entries = Files.list(sessionsRoot)) {
            for (Path entry : entries.toList()) {
                if (!Files.isDirectory(entry) || !entry.getFileName().toString().startsWith("session-")) continue;
                if (sessionInUse(entry) || !deleteTree(entry)) {
                    kept++;
                    continue;
                }
                removed++;
            }
        } catch (IOException ignored) {
            // 提示性清理，失败不影响卸载结果
        }
        System.out.println("[INFO] Removed " + removed + " launch session folder(s) from "
                + sessionsRoot + (kept > 0 ? "; kept " + kept + " still in use or locked" : "") + ".");
    }

    private static boolean sessionInUse(Path session) {
        Path state = session.resolve("state.properties");
        if (!Files.isRegularFile(state)) return false;
        java.util.Properties properties = new java.util.Properties();
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
            return false;
        }
    }

    private static boolean deleteTree(Path directory) {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Zomboid 下我们自己的安装准备目录：安装前清旧、卸载时清空，最多只留当前一份。 */
    private static void removeStagingDirectories(Path zombieRoot) {
        if (!Files.isDirectory(zombieRoot)) return;
        try (var entries = Files.list(zombieRoot)) {
            for (Path entry : entries.toList()) {
                if (!entry.getFileName().toString().startsWith("modcore-install-")) continue;
                try (var paths = Files.walk(entry)) {
                    for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                } catch (IOException failure) {
                    System.out.println("[WARN] Could not remove an old temporary folder: " + entry);
                }
            }
        } catch (IOException ignored) {
            // 提示性清理，失败不影响安装或卸载。
        }
    }

    private static void warnLaunchFilesStillModded(Path game) {
        for (String name : InstallationRecord.LAUNCH_FILES) warnIfStillModded(game.resolve(name));
    }

    private static void warnIfStillModded(Path file) {
        try {
            if (!Files.isRegularFile(file)) return;
            String text = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
            if (text.contains("modcore.attach.LaunchCoordinator")) {
                System.out.println("[WARN] " + file.getFileName()
                        + " still starts ModCore, but no original backup was found; restore it manually.");
            }
        } catch (IOException ignored) {
            // 提示性检查：读不到就不提示，不影响卸载。
        }
    }

    private static void removeEmptyOwnedJavaDirectories(InstallationRecord record) throws IOException {
        Path root = record.directory.resolve("attach-java");
        var directories = new java.util.HashSet<Path>();
        for (String name : record.components.keySet()) {
            if (!name.startsWith("attach-java/")) continue;
            for (Path directory = record.directory.resolve(name).getParent(); directory.startsWith(root); directory = directory.getParent()) {
                directories.add(directory);
            }
        }
        for (Path directory : directories.stream().sorted(java.util.Comparator.reverseOrder()).toList()) {
            DeploymentTransaction.checkPath(directory);
            if (!Files.isDirectory(directory)) continue;
            try (var entries = Files.list(directory)) {
                // 放宽：目录里还有不认识的文件就整个留着，不因为旧 Java 清理不完全而中断升级。
                if (entries.findAny().isPresent()) continue;
            }
            Files.delete(directory);
        }
    }

    private static byte[] resource(String name, int limit) throws IOException {
        try (var input = AgentInstaller.class.getClassLoader().getResourceAsStream(name)) {
            if (input == null) throw new IOException("Installer component missing: " + name);
            byte[] bytes = input.readNBytes(limit + 1);
            if (bytes.length > limit) throw new IOException("Installer component exceeds limit: " + name);
            return bytes;
        }
    }

    private static void command(Path java, Path work, Path home, Path log, List<String> command) throws Exception {
        List<String> args = new ArrayList<>(List.of(java.toString(), "-Duser.home=" + home, "-Djava.io.tmpdir=" + work,
                "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8"));
        args.addAll(command);
        ProcessBuilder builder = new ProcessBuilder(args).directory(work.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("LOCALAPPDATA", home.resolve("appdata").toString());
        builder.environment().put("USERPROFILE", home.toString());
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
            throw new IOException("Preparation or verification timed out; see " + log);
        }
        String output = Files.readString(log, StandardCharsets.UTF_8);
        System.out.print(output);
        if (process.exitValue() != 0) throw new IOException("Preparation or verification failed with exit " + process.exitValue());
    }

    private static void change(Map<Path, byte[]> before, Map<Path, byte[]> after, Path file, byte[] original, byte[] replacement) {
        before.put(file, original);
        after.put(file, replacement);
    }
}
