package modcore.agent;

import com.sun.tools.attach.VirtualMachine;
import modcore.launch.SessionFiles;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class IntegrityComparisonTest {
    @Test
    void unmodifiedReportHasSameJarAgentAndClasspathResultsAfterControlledAttach() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path game = Path.of(System.getProperty("headshot.gameJar"));
        Path gate = Path.of(System.getProperty("gate.jarFile"));
        Path runtime = root.resolve("runtime.jar");
        modcore.build.AgentLoadout.prepareRuntime(game, Path.of(System.getProperty("agent.payloadJar")), runtime);
        Path observer = observer(root, gate);
        String fixture = System.getenv("MODCORE_NATIVE_FIXTURE");
        assertNotNull(fixture);
        Path java = Path.of(fixture, "jre64/bin/java.exe");
        Properties baseline = run(root, java, game, gate, runtime, observer, false);
        Properties attached = run(root, java, game, gate, runtime, observer, true);
        assertEquals("ORIGINAL", baseline.getProperty("status"));
        assertEquals("0", baseline.getProperty("AGENT"));
        assertEquals("1", baseline.getProperty("CLASSPATH_EXTRA"));
        assertEquals(baseline, attached, "the original report must not gain project agent or classpath counts");
    }

    private static Properties run(Path root, Path java, Path game, Path gate, Path runtime, Path observer, boolean attach) throws Exception {
        Path work = Files.createTempDirectory(root, attach ? "attached-" : "baseline-");
        Files.write(work.resolve("original-extra.class"), new byte[]{0});
        Path agent = Path.of(System.getProperty("agent.jarFile"));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("version", "1"); fields.put("session", "c".repeat(64)); fields.put("timeout", "30000");
        for (var file : Map.of("game", game, "gate", gate, "runtime", runtime, "agent", agent).entrySet()) {
            fields.put(file.getKey(), file.getValue().toString());
            fields.put(file.getKey() + ".sha256", PayloadArchive.digest(file.getValue()));
        }
        SessionFiles.write(work, "request.properties", fields, false);
        Process process = new ProcessBuilder(List.of(java.toString(), "--module-path", gate + ";" + observer,
                "--add-modules", "ALL-DEFAULT", "-XX:+EnableDynamicAgentLoading", "-Dmodcore.verifyOnly=true",
                "-Dmodcore.session=" + work, "-Duser.home=" + work, "-Djava.io.tmpdir=" + work,
                "-Dprobe.attach=" + attach, "-cp", ".;" + game, "-m", "integrity.observer/probe.Observer"))
                .directory(work.toFile()).redirectErrorStream(true).redirectOutput(work.resolve("run.log").toFile()).start();
        try {
            if (attach) {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                while (!Files.exists(work.resolve("state.properties")) && process.isAlive() && System.nanoTime() < deadline) Thread.sleep(20);
                Properties state = SessionFiles.read(work.resolve("state.properties"), SessionFiles.STATE);
                assertEquals(process.pid(), SessionFiles.positive(state, "pid"));
                assertEquals(process.info().startInstant().orElseThrow().toEpochMilli(), SessionFiles.positive(state, "started"));
                VirtualMachine vm = VirtualMachine.attach(Long.toString(process.pid()));
                try { vm.loadAgent(agent.toString(), work.resolve("request.properties").toString()); }
                finally { vm.detach(); }
                SessionFiles.write(work, "release.properties", Map.of("version", "1", "session", fields.get("session"),
                        "pid", state.getProperty("pid"), "started", state.getProperty("started"), "request", state.getProperty("request")), false);
            }
            assertTrue(process.waitFor(45, TimeUnit.SECONDS));
            assertEquals(0, process.exitValue(), new String(Files.readAllBytes(work.resolve("run.log"))));
            Properties result = new Properties();
            try (var in = Files.newInputStream(work.resolve("report.properties"))) { result.load(in); }
            return result;
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }

    private static Path observer(Path root, Path gate) throws Exception {
        Path source = Files.createDirectories(root.resolve("observer-source/probe"));
        Path module = source.getParent().resolve("module-info.java");
        Files.writeString(module, "module integrity.observer { requires modcore.launch; }");
        Path main = source.resolve("Observer.java");
        Files.writeString(main, """
                package probe;
                public class Observer {
                    public static void main(String[] args) throws Exception {
                        if (Boolean.getBoolean("probe.attach")) modcore.launch.LaunchGate.main(new String[0]);
                        Class<?> type = Class.forName("zombie.core.IntegrityReport", true, ClassLoader.getSystemClassLoader());
                        var done = new java.util.concurrent.CountDownLatch(1);
                        type.getMethod("collectInBackground", Runnable.class).invoke(null, (Runnable) done::countDown);
                        if (!done.await(30, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("report timeout");
                        Object report = type.getMethod("current").invoke(null);
                        var values = new java.util.Properties();
                        values.setProperty("status", type.getMethod("getStatus").invoke(report).toString());
                        Class<?> problem = Class.forName("zombie.core.IntegrityReport$Problem");
                        for (Object kind : problem.getEnumConstants()) {
                            var details = (java.util.List<?>) type.getMethod("getProblem", problem).invoke(report, kind);
                            values.setProperty(kind.toString(), Integer.toString(details.size()));
                        }
                        try (var out = java.nio.file.Files.newOutputStream(java.nio.file.Path.of("report.properties"))) { values.store(out, null); }
                    }
                }
                """);
        Path classes = Files.createDirectories(root.resolve("observer-classes"));
        int exit = javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null, "--module-path", gate.toString(),
                "-d", classes.toString(), "-encoding", "UTF-8", module.toString(), main.toString());
        assertEquals(0, exit);
        Path jar = root.resolve("observer.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(jar)); var files = Files.walk(classes)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                out.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, out); out.closeEntry();
            }
        }
        return jar;
    }
}
