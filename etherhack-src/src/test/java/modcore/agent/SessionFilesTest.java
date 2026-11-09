package modcore.agent;

import modcore.launch.SessionFiles;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SessionFilesTest {
    @Test
    void recordsRejectUnknownDuplicateMissingVersionAndOversizedFields() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        String valid = "version=1\nsession=" + "a".repeat(64) + "\npid=2\nstarted=3\nrequest=" + "b".repeat(64) + "\n";
        Path file = root.resolve("release.properties");
        for (String input : new String[]{valid + "unknown=yes\n", valid + "pid=4\n", valid.replace("version=1", "version=2"),
                valid.replace("pid=2\n", ""), valid + "#".repeat(32768)}) {
            Files.writeString(file, input);
            assertThrows(IOException.class, () -> SessionFiles.read(file, SessionFiles.RELEASE));
        }
        Files.writeString(file, valid);
        assertEquals(2, SessionFiles.positive(SessionFiles.read(file, SessionFiles.RELEASE), "pid"));
    }

    @Test
    void writeOnceRecordsCannotBeOverwrittenAndCannotEscapeSession() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Map<String, String> fields = Map.of("version", "1", "session", "a".repeat(64), "pid", "2", "started", "3", "request", "b".repeat(64));
        SessionFiles.write(root, "release.properties", fields, false);
        byte[] original = Files.readAllBytes(root.resolve("release.properties"));
        assertThrows(IOException.class, () -> SessionFiles.write(root, "release.properties", fields, false));
        assertThrows(IOException.class, () -> SessionFiles.write(root, "../release.properties", fields, false));
        assertArrayEquals(original, Files.readAllBytes(root.resolve("release.properties")));
    }

    @Test
    void malformedUtf8IsRejectedInsteadOfSilentlyReplacingBytes() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path file = root.resolve("state.properties");
        byte[] prefix = ("version=1\nsession=" + "a".repeat(64) + "\npid=2\nstarted=3\nrequest=" + "b".repeat(64)
                + "\nphase=WAITING_ATTACH\ncode=\ndetail=").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bytes = java.util.Arrays.copyOf(prefix, prefix.length + 1);
        bytes[prefix.length] = (byte) 0x80;
        Files.write(file, bytes);
        assertThrows(IOException.class, () -> SessionFiles.read(file, SessionFiles.STATE));
    }
}
