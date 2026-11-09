package modcore.agent;

import modcore.core.CoreAPI;
import modcore.core.LuaMethods;
import org.junit.jupiter.api.Test;
import zombie.ZomboidFileSystem;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationLocationTest {
    @Test
    void userConfigCreationDoesNotMoveOrDeleteLegacyFilesInWorkingDirectory() throws Exception {
        Path root = PayloadArchiveTest.sandbox();
        String previous = ZomboidFileSystem.instance.getCacheDir();
        Path legacy = Path.of("modcore/config");
        Files.createDirectories(legacy);
        Path marker = Files.createTempFile(legacy, "fixture-", ".properties");
        Files.writeString(marker, "fixture=true\n");
        try {
            ZomboidFileSystem.instance.setCacheDir(root.resolve("user-cache").toString());
            Path user = CoreAPI.configDir().toPath();
            assertTrue(user.startsWith(root));
            assertEquals("fixture=true\n", Files.readString(marker));
            assertFalse(Files.exists(user.resolve(marker.getFileName())));
            Files.writeString(user.resolve("only-user-config.properties"), "x=1\n");
            assertTrue(LuaMethods.getConfigList().contains("only-user-config"));
            assertFalse(LuaMethods.getConfigList().contains(marker.getFileName().toString().replace(".properties", "")));
        } finally {
            ZomboidFileSystem.instance.setCacheDir(previous);
            Files.deleteIfExists(marker);
        }
    }
}
