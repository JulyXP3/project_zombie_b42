package modcore.build;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** A running gate holds a shared lock; deployment must obtain exclusive ownership. */
final class DeploymentLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private DeploymentLock(FileChannel channel, FileLock lock) { this.channel = channel; this.lock = lock; }

    static DeploymentLock acquire(Path home) throws IOException {
        Path file = home.resolve("Zomboid/modcore-deployment.lock");
        DeploymentTransaction.checkPath(file);
        Files.createDirectories(file.getParent());
        FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) throw new IOException("Deployment is locked by a running launch or another installer");
            return new DeploymentLock(channel, lock);
        } catch (IOException | OverlappingFileLockException e) {
            channel.close();
            throw new IOException("Deployment is locked", e);
        }
    }

    @Override public void close() throws IOException {
        try { lock.release(); }
        finally { channel.close(); }
    }
}
