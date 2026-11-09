package modcore.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Compare-before-write with atomic file replacement and reverse-order rollback. */
public final class DeploymentTransaction {
    private DeploymentTransaction() {}

    public static void apply(Map<Path, byte[]> before, Map<Path, byte[]> after) throws IOException {
        if (!before.keySet().equals(after.keySet())) throw new IOException("Transaction file sets differ");
        List<Path> applied = new ArrayList<>();
        try {
            for (Path file : before.keySet()) {
                replace(file, before.get(file), after.get(file));
                applied.add(file);
            }
        } catch (IOException failure) {
            for (int i = applied.size() - 1; i >= 0; i--) {
                Path file = applied.get(i);
                try {
                    replace(file, after.get(file), before.get(file));
                } catch (IOException rollback) {
                    failure.addSuppressed(new IOException("Rollback requires manual review: " + file, rollback));
                }
            }
            throw failure;
        }
    }

    static void checkPath(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        for (Path part : absolute) {
            current = current.resolve(part);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) continue;
            BasicFileAttributes attributes = Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || attributes.isOther() || !current.toRealPath().equals(current)) {
                throw new IOException("Linked or redirected deployment path is not supported: " + current);
            }
        }
    }

    static byte[] read(Path file, int limit) throws IOException {
        checkPath(file);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return null;
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > limit) {
            throw new IOException("Unexpected file type or size: " + file);
        }
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(limit + 1);
            if (bytes.length > limit) throw new IOException("File exceeds the size limit: " + file);
            return bytes;
        }
    }

    private static void replace(Path file, byte[] expected, byte[] replacement) throws IOException {
        if (!Arrays.equals(expected, read(file, 64 * 1024 * 1024))) {
            throw new IOException("File changed; refusing to overwrite: " + file);
        }
        if (replacement == null) {
            Files.deleteIfExists(file);
            return;
        }
        checkPath(file.getParent());
        Files.createDirectories(file.getParent());
        Path staging = Files.createTempFile(file.getParent(), ".modcore-write-", ".tmp");
        try {
            Files.write(staging, replacement);
            checkPath(file);
            if (!Arrays.equals(expected, read(file, 64 * 1024 * 1024))) {
                throw new IOException("File changed during staging: " + file);
            }
            Files.move(staging, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staging);
        }
    }
}
