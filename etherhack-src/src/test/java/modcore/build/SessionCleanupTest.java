package modcore.build;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 卸载清理会话目录：只删 session-*，运行中的对局与非会话条目保持不动。 */
class SessionCleanupTest {

    @Test
    void uninstallRemovesFinishedSessionsAndKeepsTheRunningOne() throws Exception {
        Path root = Files.createTempDirectory("session-cleanup");
        try {
            session(root, "session-dead1");
            session(root, "session-dead2");
            Path deadWithState = session(root, "session-dead3");
            Files.writeString(deadWithState.resolve("state.properties"), "pid=4000000\nstarted=1\n");
            Path running = session(root, "session-running");
            Files.writeString(running.resolve("state.properties"),
                    "pid=" + ProcessHandle.current().pid() + "\n"
                            + "started=" + ProcessHandle.current().info().startInstant().orElseThrow().toEpochMilli() + "\n");
            Files.writeString(root.resolve("notes.txt"), "keep");
            Files.createDirectory(root.resolve("not-a-session"));

            AgentInstaller.removeSessionDirectories(root);

            assertFalse(Files.exists(root.resolve("session-dead1")));
            assertFalse(Files.exists(root.resolve("session-dead2")));
            assertFalse(Files.exists(deadWithState), "a session whose process is gone must be removed");
            assertTrue(Files.isDirectory(running), "a session whose game is still running must be kept");
            assertTrue(Files.exists(root.resolve("notes.txt")));
            assertTrue(Files.isDirectory(root.resolve("not-a-session")));
            assertEquals(2, count(root), "only the running session and the unrelated folder remain");
        } finally {
            delete(root);
        }
    }

    @Test
    void aMissingSessionsFolderIsNotAnError() throws Exception {
        Path root = Files.createTempDirectory("session-cleanup-missing");
        try {
            AgentInstaller.removeSessionDirectories(root.resolve("modcore-sessions"));
            assertTrue(Files.isDirectory(root));
        } finally {
            delete(root);
        }
    }

    private static Path session(Path root, String name) throws Exception {
        Path directory = Files.createDirectory(root.resolve(name));
        Files.writeString(directory.resolve("game.log"), "x");
        return directory;
    }

    private static int count(Path root) throws Exception {
        try (var entries = Files.list(root)) {
            return (int) entries.filter(Files::isDirectory).count();
        }
    }

    private static void delete(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
