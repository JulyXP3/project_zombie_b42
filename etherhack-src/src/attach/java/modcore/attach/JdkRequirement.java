package modcore.attach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Checks the caller's JDK without adopting or modifying it. */
public final class JdkRequirement {
    private JdkRequirement() {}

    public static void main(String[] args) {
        try {
            if (args.length != 0) throw new IOException("The JDK check takes no arguments");
            System.out.println("External JDK 25 ready: " + requireCurrent());
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            System.exit(1);
        }
    }

    public static Path requireCurrent() throws IOException {
        checkVersion(Runtime.version().feature());
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!System.getProperty("os.name", "").startsWith("Windows")
                || (!arch.equals("amd64") && !arch.equals("x86_64"))
                || !System.getProperty("sun.arch.data.model", "").equals("64")) {
            throw new IOException("Install a Windows x64 full JDK 25; this Java architecture is unsupported");
        }
        for (String module : List.of("jdk.attach", "jdk.compiler")) {
            if (ModuleLayer.boot().findModule(module).isEmpty()) {
                throw new IOException("Install a full JDK 25: required module " + module + " is missing");
            }
        }
        Path home = Path.of(System.getProperty("java.home")).toRealPath();
        for (String tool : List.of("java.exe", "javac.exe")) {
            if (!Files.isRegularFile(home.resolve("bin").resolve(tool))) {
                throw new IOException("Install a full JDK 25: " + tool + " is missing from " + home);
            }
        }
        return home.resolve("bin/java.exe");
    }

    private static void checkVersion(int feature) throws IOException {
        if (feature != 25) throw new IOException("A full JDK 25 is required; detected Java " + feature);
    }
}
