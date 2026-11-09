package modcore.agent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentTransactionTest {
    @Test
    void failedSecondChangeRollsBackFirstWithoutReplacingConcurrentChanges() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        Path first = root.resolve("first");
        Path second = root.resolve("second");
        Files.writeString(first, "old");
        Files.writeString(second, "changed-elsewhere");
        Map<Path, byte[]> before = new LinkedHashMap<>();
        before.put(first, "old".getBytes());
        before.put(second, "expected".getBytes());
        Map<Path, byte[]> after = new LinkedHashMap<>();
        after.put(first, "new".getBytes());
        after.put(second, "new".getBytes());
        Class<?> transaction = assertDoesNotThrow(() -> Class.forName("modcore.build.DeploymentTransaction"));
        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> transaction.getMethod("apply", Map.class, Map.class).invoke(null, before, after));
        assertInstanceOf(java.io.IOException.class, failure.getCause());
        assertEquals("old", Files.readString(first));
        assertEquals("changed-elsewhere", Files.readString(second));
        try (var files = Files.list(root)) { assertEquals(2, files.count()); }
    }
}
