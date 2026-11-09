package modcore.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

record LaunchGateFixture(Path root, Path game, Path runtime, Path agent, String gameDigest) {
    static LaunchGateFixture create() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path classes = Files.createDirectories(root.resolve("fixture-classes"));
        var sources = List.of(
                new Source("fixture.game.Main", """
                        package fixture.game;
                        public class Main {
                            static {
                                try { java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("modcore.session"), "game-initialized"), "yes"); }
                                catch (Exception e) { throw new ExceptionInInitializerError(e); }
                            }
                            public static void main(String[] args) throws Exception {
                                var root = java.nio.file.Path.of(System.getProperty("modcore.session"));
                                var calls = root.resolve("main-calls");
                                int count = java.nio.file.Files.exists(calls) ? Integer.parseInt(java.nio.file.Files.readString(calls)) : 0;
                                java.nio.file.Files.writeString(calls, Integer.toString(count + 1));
                                ClassLoader cl = ClassLoader.getSystemClassLoader();
                                if (Main.class.getClassLoader() != cl || Thread.currentThread().getContextClassLoader() != cl)
                                    throw new AssertionError("game/context loader differs");
                                if (!Thread.currentThread().getName().equals("main")) throw new AssertionError("wrong main thread");
                                if (Target.class.getClassLoader() != cl || modcore.fixture.Helper.class.getClassLoader() != cl
                                        || Class.forName(System.getProperty("fixture.agentClass", "fixture.attach.GateAgent")).getClassLoader() != cl)
                                    throw new AssertionError("duplicate type universe");
                                if (Target.value() != 73 || Peer.value() != 4) throw new AssertionError("transform missing");
                                var source = Target.class.getProtectionDomain().getCodeSource();
                                if (source.getCertificates() == null || source.getCertificates().length == 0) throw new AssertionError("signer lost");
                                if (!java.util.Arrays.equals(source.getCertificates(), Peer.class.getProtectionDomain().getCodeSource().getCertificates()))
                                    throw new AssertionError("signers differ");
                                if (!source.getLocation().equals(Main.class.getProtectionDomain().getCodeSource().getLocation()))
                                    throw new AssertionError("source changed");
                                if (!Main.class.getPackage().isSealed()) throw new AssertionError("sealed package lost");
                                for (String name : java.util.List.of("modcore/data/probe.txt", "fixture/game/original.txt")) {
                                    try (var in = cl.getResourceAsStream(name)) {
                                        if (in == null || !new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).equals("probe-data"))
                                            throw new AssertionError("resource missing: " + name);
                                    }
                                }
                                if (!System.getProperty("java.class.path").equals(java.nio.file.Path.of(source.getLocation().toURI()).toString()))
                                    throw new AssertionError("classpath changed");
                                for (String arg : java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                                    if (arg.startsWith("-javaagent:") || arg.startsWith("-agentpath:") || arg.startsWith("-agentlib:"))
                                        throw new AssertionError("startup agent argument");
                                }
                                for (String name : java.util.List.of("java.desktop", "java.sql", "java.management", "java.instrument")) {
                                    if (ModuleLayer.boot().findModule(name).isEmpty()) throw new AssertionError("default module missing: " + name);
                                }
                                if (!java.util.Arrays.equals(args, new String[]{"first argument", "中文参数"})) throw new AssertionError("arguments changed");
                                var failures = new java.util.concurrent.ConcurrentLinkedQueue<Throwable>();
                                var workers = new java.util.ArrayList<Thread>();
                                for (int i = 0; i < 8; i++) workers.add(new Thread(() -> {
                                    try { for (int j = 0; j < 20; j++) {
                                        if (Class.forName("modcore.fixture.Helper", false, cl) != modcore.fixture.Helper.class)
                                            throw new AssertionError("identity changed");
                                    } } catch (Throwable t) { failures.add(t); }
                                }));
                                for (var worker : workers) worker.start();
                                for (var worker : workers) worker.join();
                                if (!failures.isEmpty()) throw new AssertionError(failures.toString());
                                System.out.println("SIGNED-GATE-PASS");
                                if (Boolean.getBoolean("fixture.waitForUser")) {
                                    var module = ModuleLayer.boot().findModule("modcore.launch").orElseThrow();
                                    var gate = Class.forName(module, "modcore.launch.LaunchGate");
                                    gate.getMethod("runtimeState", String.class, long.class, String.class)
                                        .invoke(null, "WAITING_USER", 0L, "fixture user confirmation");
                                    while (!java.nio.file.Files.exists(root.resolve("user-done"))) Thread.sleep(20);
                                    System.out.println("USER-WAIT-FINISHED");
                                }
                            }
                        }
                        """),
                new Source("fixture.game.Target", """
                        package fixture.game;
                        public class Target {
                            public static int value() { return 7; }
                            public static class Nested { public static int value() { return 1; } }
                        }
                        """),
                new Source("fixture.game.Peer", "package fixture.game; public class Peer { public static int value() { return 4; } }"),
                new Source("modcore.fixture.Helper", "package modcore.fixture; public class Helper { public static int value() { return 72 + fixture.game.Target.Nested.value(); } }"),
                new Source("fixture.attach.GateAgent", """
                        package fixture.attach;
                        public final class GateAgent {
                            public static void agentmain(String options, java.lang.instrument.Instrumentation inst) throws Exception {
                                Thread.sleep(Long.getLong("fixture.attachHoldMs", 0L));
                                var request = java.nio.file.Path.of(options).toRealPath();
                                var props = new java.util.Properties();
                                try (var reader = java.nio.file.Files.newBufferedReader(request)) { props.load(reader); }
                                var module = ModuleLayer.boot().findModule("modcore.launch").orElseThrow();
                                var gate = Class.forName(module, "modcore.launch.LaunchGate");
                                try {
                                    if ("preloaded".equals(System.getProperty("fixture.fault")))
                                        Class.forName("fixture.game.Target", false, ClassLoader.getSystemClassLoader());
                                    for (var type : inst.getAllLoadedClasses()) {
                                        if (type.getName().startsWith("fixture.game.")) throw new IllegalStateException("target already loaded");
                                    }
                                    System.out.println("PRELOADED_TARGETS=0");
                                    var jar = new java.util.jar.JarFile(props.getProperty("runtime"));
                                    byte[] patch;
                                    try (var in = jar.getInputStream(jar.getJarEntry("META-INF/target.class"))) { patch = in.readAllBytes(); }
                                    inst.appendToSystemClassLoaderSearch(jar);
                                    var applied = new java.util.concurrent.atomic.AtomicBoolean();
                                    inst.addTransformer(new java.lang.instrument.ClassFileTransformer() {
                                        public byte[] transform(ClassLoader loader, String name, Class<?> redefining,
                                                java.security.ProtectionDomain domain, byte[] bytes) {
                                            if (!name.equals("fixture/game/Target")) return null;
                                            if (redefining != null || loader != ClassLoader.getSystemClassLoader()) return null;
                                            applied.set(true);
                                            return patch.clone();
                                        }
                                    }, false);
                                    Runnable preflight = () -> {
                                        try {
                                            if (!Thread.currentThread().getName().equals("main")) throw new IllegalStateException("wrong preflight thread");
                                            if ("preflight".equals(System.getProperty("fixture.fault"))) throw new IllegalStateException("deliberate preflight failure");
                                            for (String name : java.util.List.of("fixture.game.Main", "fixture.game.Target", "modcore.fixture.Helper")) {
                                                var type = Class.forName(name, false, ClassLoader.getSystemClassLoader());
                                                type.getDeclaredMethods(); type.getDeclaredFields(); type.getDeclaredConstructors();
                                            }
                                            if (!applied.get()) throw new IllegalStateException("transform missing");
                                        } catch (Exception e) { throw new IllegalStateException(e); }
                                    };
                                    String digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(request)));
                                    var process = ProcessHandle.current();
                                    var install = gate.getMethod("install", String.class, long.class, long.class, String.class, String.class, Runnable.class);
                                    install.invoke(null, props.getProperty("session"), process.pid(), process.info().startInstant().orElseThrow().toEpochMilli(),
                                            digest, "fixture.game.Main", preflight);
                                    if ("duplicate".equals(System.getProperty("fixture.fault"))) {
                                        install.invoke(null, props.getProperty("session"), process.pid(), process.info().startInstant().orElseThrow().toEpochMilli(),
                                                digest, "fixture.game.Main", preflight);
                                    }
                                } catch (Throwable e) {
                                    gate.getMethod("fail", String.class, String.class).invoke(null, "AGENT_REJECTED", e.toString());
                                    throw new IllegalStateException(e);
                                }
                            }
                        }
                        """));
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        try (var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            assertTrue(compiler.getTask(null, manager, null,
                    List.of("-d", classes.toString(), "-encoding", "UTF-8"), null, sources).call());
        }
        Path game = root.resolve("signed-game.jar");
        Manifest gameManifest = manifest();
        gameManifest.getMainAttributes().put(Attributes.Name.SEALED, "true");
        try (var out = new JarOutputStream(Files.newOutputStream(game), gameManifest)) {
            addTree(out, classes, "fixture/game");
            entry(out, "fixture/game/original.txt", "probe-data".getBytes(StandardCharsets.UTF_8));
        }
        Path key = root.resolve("fixture.p12");
        command(root, List.of(tool("keytool"), "-genkeypair", "-alias", "fixture", "-keyalg", "EC", "-dname",
                "CN=LaunchGateFixture", "-validity", "3650", "-storetype", "PKCS12", "-keystore", key.toString(),
                "-storepass", "fixture-password", "-keypass", "fixture-password", "-noprompt"));
        command(root, List.of(tool("jarsigner"), "-keystore", key.toString(), "-storepass", "fixture-password", game.toString(), "fixture"));
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(classes.resolve("fixture/game/Target.class"))).accept(node, 0);
        var method = node.methods.stream().filter(m -> m.name.equals("value")).findFirst().orElseThrow();
        method.instructions = new InsnList();
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "modcore/fixture/Helper", "value", "()I", false));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.localVariables = null;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Path runtime = root.resolve("runtime.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(runtime), manifest())) {
            addTree(out, classes, "modcore/fixture");
            entry(out, "modcore/data/probe.txt", "probe-data".getBytes(StandardCharsets.UTF_8));
            entry(out, "META-INF/target.class", writer.toByteArray());
        }
        Path agent = root.resolve("fixture-agent.jar");
        Manifest agentManifest = manifest();
        agentManifest.getMainAttributes().putValue("Agent-Class", "fixture.attach.GateAgent");
        try (var out = new JarOutputStream(Files.newOutputStream(agent), agentManifest)) { addTree(out, classes, "fixture/attach"); }
        return new LaunchGateFixture(root, game, runtime, agent, PayloadArchive.digest(game));
    }

    Path productionRuntime(boolean wrongInput) throws Exception {
        var entries = new java.util.LinkedHashMap<String, byte[]>();
        byte[] patch;
        try (var jar = new java.util.jar.JarFile(runtime.toFile())) {
            for (String name : List.of("modcore/fixture/Helper.class", "modcore/data/probe.txt")) {
                try (var in = jar.getInputStream(jar.getJarEntry(name))) { entries.put(name, in.readAllBytes()); }
            }
            try (var in = jar.getInputStream(jar.getJarEntry("META-INF/target.class"))) { patch = in.readAllBytes(); }
        }
        entries.put(PayloadArchive.PATCH_ROOT + "fixture/game/Target.class", patch);
        java.util.Properties metadata = new java.util.Properties();
        metadata.setProperty("format.version", "2");
        metadata.setProperty("game.sha256", gameDigest);
        metadata.setProperty("main.class", "fixture.game.Main");
        try (var jar = new java.util.jar.JarFile(game.toFile()); var in = jar.getInputStream(jar.getJarEntry("fixture/game/Target.class"))) {
            metadata.setProperty("original.fixture/game/Target", wrongInput ? "0".repeat(64) : PayloadArchive.digest(in.readAllBytes()));
        }
        for (var entry : entries.entrySet()) metadata.setProperty("entry." + entry.getKey(), PayloadArchive.digest(entry.getValue()));
        var config = new java.io.ByteArrayOutputStream();
        metadata.store(config, null);
        entries.put(PayloadArchive.METADATA, config.toByteArray());
        return PlainPayloadTest.write(Files.createTempDirectory(root, "production-runtime-"), entries);
    }

    List<Path> runtimes() {
        String nativeRoot = System.getenv("MODCORE_NATIVE_FIXTURE");
        assertNotNull(nativeRoot, "G0 requires the copied game JRE as well as a development JDK");
        return List.of(Path.of(tool("java")), Path.of(nativeRoot, "jre64", "bin", "java.exe"));
    }

    private static Manifest manifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return manifest;
    }

    private static void addTree(JarOutputStream out, Path root, String prefix) throws Exception {
        try (var files = Files.walk(root.resolve(prefix))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                entry(out, root.relativize(file).toString().replace('\\', '/'), Files.readAllBytes(file));
            }
        }
    }

    private static void entry(JarOutputStream out, String name, byte[] bytes) throws Exception {
        out.putNextEntry(new JarEntry(name));
        out.write(bytes);
        out.closeEntry();
    }

    private static String tool(String name) { return Path.of(System.getProperty("java.home"), "bin", name + ".exe").toString(); }

    private static void command(Path root, List<String> command) throws Exception {
        Path log = Files.createTempFile(root, "tool-", ".log");
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
            fail("Fixture tool timed out");
        }
        assertEquals(0, process.exitValue(), new String(Files.readAllBytes(log),
                java.nio.charset.Charset.forName(System.getProperty("native.encoding", "UTF-8"))));
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        Source(String name, String text) {
            super(URI.create("string:///" + name.replace('.', '/') + ".java"), Kind.SOURCE);
            this.text = text;
        }
        @Override public CharSequence getCharContent(boolean ignored) { return text; }
    }
}
