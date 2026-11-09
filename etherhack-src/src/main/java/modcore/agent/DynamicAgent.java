package modcore.agent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

/** Installs only into the module gate created by this launch session. */
final class DynamicAgent {
    private static JarFile runtime;

    private DynamicAgent() {}

    static synchronized void install(String options, Instrumentation instrumentation) throws Exception {
        Module module = ModuleLayer.boot().findModule("modcore.launch")
                .orElseThrow(() -> new IOException("Controlled launch module is missing"));
        Class<?> gate = Class.forName(module, "modcore.launch.LaunchGate");
        if (gate == null || gate.getClassLoader() != ClassLoader.getSystemClassLoader()) {
            throw new IOException("Unexpected launch gate loader");
        }
        try {
            if (runtime != null) throw new IOException("Dynamic agent already installed");
            Path directory = Path.of(System.getProperty("modcore.session")).toRealPath();
            Path requestFile = Path.of(options).toRealPath();
            if (!requestFile.equals(directory.resolve("request.properties"))) throw new IOException("Agent request outside owned session");
            Class<?> protocol = Class.forName(module, "modcore.launch.SessionFiles");
            @SuppressWarnings("unchecked") Set<String> fields = (Set<String>) protocol.getField("REQUEST").get(null);
            Properties request = (Properties) protocol.getMethod("read", Path.class, Set.class).invoke(null, requestFile, fields);
            protocol.getMethod("verifyArtifacts", Properties.class).invoke(null, request);
            Path agent = Path.of(AgentMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            if (!agent.equals(Path.of(request.getProperty("agent")).toRealPath())) throw new IOException("Agent source mismatch");
            Path gateSource = Path.of(gate.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            if (!gateSource.equals(Path.of(request.getProperty("gate")).toRealPath())) throw new IOException("Gate source mismatch");
            Path game = Path.of(request.getProperty("game")).toRealPath();
            Path payload = Path.of(request.getProperty("runtime")).toRealPath();
            PayloadArchive archive = PayloadArchive.readRuntime(payload);
            if (!PayloadArchive.digest(game).equals(archive.gameDigest())) throw new IOException("Game digest mismatch");
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            if (AgentMain.class.getClassLoader() != loader) throw new IOException("Agent did not use system loader");
            Set<String> targets = new HashSet<>(archive.patchNames());
            targets.add(archive.mainClass().replace('.', '/'));
            for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
                if (targets.contains(loaded.getName().replace('.', '/'))) throw new IOException("Game target already loaded: " + loaded.getName());
            }
            for (String name : targets) {
                for (Path root : new HashSet<>(java.util.List.of(Path.of("").toAbsolutePath(), game.getParent()))) {
                    if (Files.exists(root.resolve(name + ".class")) || Files.exists(root.resolve("zombie/coreboot.class"))) {
                        throw new IOException("Legacy override class exists: " + name);
                    }
                }
            }
            for (String name : archive.resourceNames("modcore/")) {
                if (loader.getResource(name) != null) throw new IOException("Runtime resource already visible: " + name);
            }
            runtime = new JarFile(payload.toFile(), true);
            instrumentation.appendToSystemClassLoaderSearch(runtime);
            GameTransformer transformer = new GameTransformer(loader, game, archive);
            instrumentation.addTransformer(transformer, false);
            Runnable preflight = () -> {
                try { verify(loader, game, payload, archive, transformer); }
                catch (Exception e) { throw new IllegalStateException("Runtime preflight failed", e); }
            };
            ProcessHandle current = ProcessHandle.current();
            gate.getMethod("install", String.class, long.class, long.class, String.class, String.class, Runnable.class)
                    .invoke(null, request.getProperty("session"), current.pid(), current.info().startInstant().orElseThrow().toEpochMilli(),
                            PayloadArchive.digest(requestFile), archive.mainClass(), preflight);
        } catch (Throwable failure) {
            Throwable cause = failure instanceof InvocationTargetException invocation ? invocation.getCause() : failure;
            gate.getMethod("fail", String.class, String.class).invoke(null, "AGENT_REJECTED", cause.toString());
            throw new IOException("Dynamic agent installation failed", cause);
        }
    }

    private static void verify(ClassLoader loader, Path game, Path payload, PayloadArchive archive,
                               GameTransformer transformer) throws Exception {
        if (!Thread.currentThread().getName().equals("main") || Thread.currentThread().getContextClassLoader() != loader) {
            throw new IOException("Preflight must run on the original main thread with the system context loader");
        }
        try (JarFile original = new JarFile(game.toFile(), true)) {
            for (String name : archive.patchNames()) {
                Class<?> type = link(name.replace('/', '.'), loader);
                requireSource(type, game);
                var entry = original.getJarEntry(name + ".class");
                if (entry == null) throw new IOException("Original class missing: " + name);
                try (var in = original.getInputStream(entry)) {
                    if (!PayloadArchive.digest(in.readAllBytes()).equals(archive.originalDigest(name))) throw new IOException("Original digest changed: " + name);
                }
                Certificate[] expected = entry.getCertificates();
                Certificate[] actual = type.getProtectionDomain().getCodeSource().getCertificates();
                if (expected == null || expected.length == 0 || !Arrays.equals(expected, actual)) throw new IOException("Original signer lost: " + name);
            }
        }
        int linked = 0;
        for (String name : archive.resourceNames("modcore/")) {
            if (name.endsWith(".class")) {
                requireSource(link(name.substring(0, name.length() - 6).replace('/', '.'), loader), payload);
                linked++;
            } else {
                try (var actual = loader.getResourceAsStream(name); var expected = archive.open(name)) {
                    if (actual == null || !Arrays.equals(actual.readAllBytes(), expected.readAllBytes())) throw new IOException("Runtime resource mismatch: " + name);
                }
            }
        }
        transformer.requireComplete();
        requireSource(link(archive.mainClass(), loader), game);
        System.out.println("[ModCore Agent] Verified " + archive.patchNames().size() + " prepared class transformations; " + linked + " runtime classes linked");
    }

    private static Class<?> link(String name, ClassLoader loader) throws Exception {
        Class<?> type = Class.forName(name, false, loader);
        if (type.getClassLoader() != loader) throw new IOException("Class loader identity mismatch: " + name);
        type.getDeclaredMethods();
        type.getDeclaredConstructors();
        type.getDeclaredFields();
        return type;
    }

    private static void requireSource(Class<?> type, Path expected) throws Exception {
        if (!Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(expected)) {
            throw new IOException("Class source mismatch: " + type.getName());
        }
    }
}
