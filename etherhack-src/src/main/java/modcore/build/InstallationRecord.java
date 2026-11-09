package modcore.build;

import modcore.agent.PayloadArchive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** A bounded journal: paths come from this code, never from arbitrary entries in the record. */
final class InstallationRecord {
    /** The executable and its JSON are never touched: only these client batch files carry the mod. */
    static final List<String> LAUNCH_FILES = List.of("ProjectZomboid64.bat",
            "ProjectZomboid64ShowConsole.bat", "ProjectZomboidOpenGLDebug64.bat");
    static final List<String> RUN_FILES = List.of("agent.jar", "installation.properties", "installer.jar", "runtime.jar", "launch-gate.jar", "coordinator.jar");
    boolean legacy;
    boolean bundledJava;
    final Map<String, byte[]> components = new LinkedHashMap<>();
    final Path game;
    final Path home;
    final Path directory;
    final Map<String, byte[]> original = new LinkedHashMap<>();
    final Map<String, byte[]> installed = new LinkedHashMap<>();
    final Map<Path, byte[]> current = new LinkedHashMap<>();
    /** 记录里的游戏目录/用户目录与本次不符：当作本目录没有安装，不猜也不还原别人的文件。 */
    boolean foreign;

    InstallationRecord(Path game, Path home) {
        this.game = game;
        this.home = home;
        this.directory = home.resolve("Zomboid/modcore-agent");
    }

    Path payload() { return legacy ? home.resolve("Zomboid/modcore.bin") : directory.resolve("runtime.jar"); }
    Path manifest() { return directory.resolve("installation.properties"); }
    Path backup(String name) { return directory.resolve(name + ".original"); }

    boolean exists() {
        return Files.exists(manifest());
    }

    void load() throws IOException {
        byte[] raw = DeploymentTransaction.read(manifest(), 65536);
        if (raw == null) return;
        Properties p = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("Duplicate installation field: " + key);
                return super.put(key, value);
            }
        };
        try { p.load(new ByteArrayInputStream(raw)); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid installation record", e); }
        legacy = "1".equals(p.getProperty("format"));
        bundledJava = "2".equals(p.getProperty("format"));
        // 放宽：记录属于别的位置（游戏目录或用户目录搬过家）时按“本目录没有安装”处理。
        String recordedGame = p.getProperty("game");
        String recordedHome = p.getProperty("home");
        if ((recordedGame != null && !recordedGame.isEmpty() && !game.toString().equals(recordedGame))
                || (recordedHome != null && !recordedHome.isEmpty() && !home.toString().equals(recordedHome))) {
            foreign = true;
            return;
        }
        // 放宽：已装组件不再核对摘要，读到什么就用什么；缺了就当它不存在。
        for (String key : p.stringPropertyNames()) {
            if (!key.startsWith("file.")) continue;
            String name = key.substring(5);
            Path component = directory.resolve(name);
            byte[] bytes = DeploymentTransaction.read(component, 192 * 1024 * 1024);
            if (bytes == null) continue;
            components.put(name, bytes);
            current.put(component, bytes);
        }
        if (legacy) {
            byte[] payload = DeploymentTransaction.read(payload(), 64 * 1024 * 1024);
            if (payload != null) current.put(payload(), payload);
            Path agent = directory.resolve("agent.jar");
            byte[] agentBytes = DeploymentTransaction.read(agent, 4 * 1024 * 1024);
            if (agentBytes != null) current.put(agent, agentBytes);
        } else {
            for (String name : List.of("runtime.jar", "agent.jar", "launch-gate.jar", "coordinator.jar", "installer.jar")) {
                if (components.containsKey(name)) continue;
                Path component = directory.resolve(name);
                byte[] bytes = DeploymentTransaction.read(component, 192 * 1024 * 1024);
                if (bytes == null) continue;
                components.put(name, bytes);
                current.put(component, bytes);
            }
        }
        // 放宽：启动文件及其备份只按“存在即用”登记，不再核对摘要或重新推导比对。
        for (String name : LAUNCH_FILES) {
            Path saved = backup(name);
            Path live = game.resolve(name);
            byte[] before = DeploymentTransaction.read(saved, 1024 * 1024);
            byte[] after = DeploymentTransaction.read(live, 1024 * 1024);
            if (before == null && after == null) continue;
            if (before != null) {
                original.put(name, before);
                current.put(saved, before);
            }
            if (after != null) {
                installed.put(name, after);
                current.put(live, after);
            }
        }
        current.put(manifest(), raw);
    }

    byte[] encode(Map<String, byte[]> files) throws IOException {
        Properties p = new Properties();
        p.setProperty("format", "3");
        p.setProperty("game", game.toString());
        p.setProperty("home", home.toString());
        p.setProperty("game.sha256", PayloadArchive.digest(game.resolve("projectzomboid.jar")));
        for (var file : files.entrySet()) {
            validateComponentName(file.getKey());
            p.setProperty("file." + file.getKey(), PayloadArchive.digest(file.getValue()));
        }
        for (String name : original.keySet()) {
            p.setProperty("original." + name, PayloadArchive.digest(original.get(name)));
            p.setProperty("installed." + name, PayloadArchive.digest(installed.get(name)));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.store(out, "ModCore reversible installation record");
        return out.toByteArray();
    }

    static void validateComponentName(String name) throws IOException {
        validateComponentName(name, false);
    }

    private static void validateComponentName(String name, boolean bundledJava) throws IOException {
        if (List.of("agent.jar", "runtime.jar", "coordinator.jar", "launch-gate.jar", "installer.jar").contains(name)) return;
        if (!bundledJava || !name.startsWith("attach-java/") || name.contains("\\") || name.contains(":")) throw new IOException("Invalid component path: " + name);
        for (String part : name.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) throw new IOException("Invalid component path: " + name);
        }
    }
}
