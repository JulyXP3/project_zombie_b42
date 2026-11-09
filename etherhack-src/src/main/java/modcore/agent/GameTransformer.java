package modcore.agent;

import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Applies prepared bytes only to the expected original input and application loader. */
final class GameTransformer implements ClassFileTransformer {
    private final ClassLoader application;
    private final Path gameJar;
    private final PayloadArchive archive;
    private final Set<String> targets;
    private final Set<String> applied = ConcurrentHashMap.newKeySet();
    private final Map<String, String> failures = new ConcurrentHashMap<>();

    GameTransformer(ClassLoader application, Path gameJar, PayloadArchive archive) {
        this.application = application;
        this.gameJar = gameJar;
        this.archive = archive;
        this.targets = Set.copyOf(archive.patchNames());
    }

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> redefining,
                            ProtectionDomain domain, byte[] input) {
        if (loader != application || name == null || !targets.contains(name)) return null;
        try {
            if (redefining != null) throw new IllegalStateException("Only initial definitions are supported");
            if (domain == null || domain.getCodeSource() == null
                    || !Path.of(domain.getCodeSource().getLocation().toURI()).toRealPath().equals(gameJar)) {
                throw new IllegalStateException("Unexpected game class source");
            }
            if (!PayloadArchive.digest(input).equals(archive.originalDigest(name))) {
                throw new IllegalStateException("Original class digest mismatch");
            }
            byte[] patched = archive.patchBytes(name);
            applied.add(name);
            return patched;
        } catch (Exception e) {
            failures.put(name, e.toString());
            // Instrumentation ignores transformer exceptions. Launcher checks this report before main.
            return null;
        }
    }

    void requireComplete() {
        if (!failures.isEmpty()) throw new IllegalStateException("Transformation failed: " + failures);
        for (String name : archive.patchNames()) {
            if (!applied.contains(name)) throw new IllegalStateException("Transformation missing: " + name);
        }
    }
}
