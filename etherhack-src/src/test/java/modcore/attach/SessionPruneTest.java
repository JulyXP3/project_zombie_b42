package modcore.attach;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 会话目录配额：最多保留 3 份，运行中的对局永不删除。 */
class SessionPruneTest {

    @Test
    void keepsTheNewestThreeSessionsIncludingTheCurrentOne() throws Exception {
        Path root = Files.createTempDirectory("session-prune");
        try {
            for (int i = 1; i <= 4; i++) session(root, "session-old" + i, i);
            Path current = session(root, "session-current", 60);

            LaunchCoordinator.pruneSessions(root, current);

            assertEquals(3, count(root));
            assertTrue(Files.isDirectory(current), "current session must survive");
            assertTrue(Files.isDirectory(root.resolve("session-old4")), "newest old session must survive");
            assertTrue(Files.isDirectory(root.resolve("session-old3")), "second newest must survive");
            assertFalse(Files.exists(root.resolve("session-old1")), "oldest session must be removed");
            assertFalse(Files.exists(root.resolve("session-old2")), "second oldest must be removed");
        } finally {
            delete(root);
        }
    }

    @Test
    void aRunningSessionSurvivesEvenWhenItIsTheOldest() throws Exception {
        Path root = Files.createTempDirectory("session-prune-running");
        try {
            Path running = session(root, "session-running", 1);
            Files.writeString(running.resolve("state.properties"),
                    "pid=" + ProcessHandle.current().pid() + "\n"
                            + "started=" + ProcessHandle.current().info().startInstant().orElseThrow().toEpochMilli() + "\n");
            setModified(running, 1);
            for (int i = 1; i <= 3; i++) session(root, "session-new" + i, 10 + i);
            Path current = session(root, "session-current", 60);

            LaunchCoordinator.pruneSessions(root, current);

            assertTrue(Files.isDirectory(running), "a session whose game is still running must never be deleted");
            assertEquals(4, count(root), "current + two newest + the running one");
        } finally {
            delete(root);
        }
    }

    @Test
    void unrelatedEntriesAreIgnored() throws Exception {
        Path root = Files.createTempDirectory("session-prune-other");
        try {
            Files.writeString(root.resolve("notes.txt"), "keep");
            Files.createDirectory(root.resolve("other-folder"));
            for (int i = 1; i <= 4; i++) session(root, "session-old" + i, i);
            Path current = session(root, "session-current", 60);

            LaunchCoordinator.pruneSessions(root, current);

            assertTrue(Files.exists(root.resolve("notes.txt")));
            assertTrue(Files.isDirectory(root.resolve("other-folder")));
            assertEquals(3, count(root), "only the newest three sessions are kept");
        } finally {
            delete(root);
        }
    }

    private static Path session(Path root, String name, long modified) throws Exception {
        Path directory = Files.createDirectory(root.resolve(name));
        Files.writeString(directory.resolve("game.log"), "x");
        setModified(directory, modified);
        return directory;
    }

    private static void setModified(Path directory, long seconds) throws Exception {
        Files.setLastModifiedTime(directory, FileTime.fromMillis(seconds * 1000));
    }

    private static int count(Path root) throws Exception {
        try (var entries = Files.list(root)) {
            return (int) entries.filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("session-")).count();
        }
    }

    private static void delete(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
