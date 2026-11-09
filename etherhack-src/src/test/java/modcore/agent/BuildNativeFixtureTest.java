package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Map;

import static modcore.agent.BuildEntryHarness.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The full build prepares a managed, isolated copy of the game's {@code jre64}
 * under the project temp folder and exports it as MODCORE_NATIVE_FIXTURE.
 * These tests cover creation, reuse, repair and every refusal case without
 * relying on a fixture being pre-set by the caller.
 */
class BuildNativeFixtureTest {
    private static final long YEAR_2000 = 946684800000L;

    @Test
    void createsTheFixtureWhenAbsentAndExportsItToGradle() throws Exception {
        Fixture fixture = fixture("native create");
        Map<String, String> before = snapshot(fixture.game());

        Path managed = firstRun(fixture);

        assertTrue(managed.startsWith(fixture.root().resolve("temp")),
                "the managed fixture must be inside the project temp folder: " + managed);
        assertNotEquals(fixture.game().resolve("jre64"), managed.resolve("jre64"),
                "the game runtime must be copied, not used in place");
        for (String relative : new String[]{"jre64/bin/java.exe", "jre64/lib/modules",
                "jre64/release", "jre64/conf/net.properties"}) {
            assertEquals(Files.readString(fixture.game().resolve(relative)),
                    Files.readString(managed.resolve(relative)),
                    "the complete game runtime must be copied: " + relative);
        }
        assertEquals(before, snapshot(fixture.game()), "the game folder must stay read-only");
    }

    @Test
    void reusesAValidFixtureWithoutRecopyingIt() throws Exception {
        Fixture fixture = fixture("native reuse");
        Path managed = firstRun(fixture);
        Path release = managed.resolve("jre64/release");
        markModified(release, FileTime.fromMillis(YEAR_2000));

        Result again = run(fixture, "build.bat", validEnvironment(fixture));

        assertEquals(0, again.exit(), again.output());
        assertEquals(managed, managedFixture(recording(fixture.source())),
                "the same managed fixture must be reused");
        assertTrue(Files.getLastModifiedTime(release).toMillis() < 1_000_000_000_000L,
                "a valid fixture must not be copied again: " + Files.getLastModifiedTime(release));
    }

    @Test
    void rebuildsWhenARequiredFixtureFileIsMissing() throws Exception {
        Fixture fixture = fixture("native repair");
        Path managed = firstRun(fixture);
        Files.delete(managed.resolve("jre64/lib/modules"));

        Result again = run(fixture, "build.bat", validEnvironment(fixture));

        assertEquals(0, again.exit(), again.output());
        Path exported = managedFixture(recording(fixture.source()));
        assertEquals(Files.readString(fixture.game().resolve("jre64/lib/modules")),
                Files.readString(exported.resolve("jre64/lib/modules")),
                "a missing managed file must be rebuilt from the game runtime");
    }

    @Test
    void updatesTheFixtureWhenTheGameRuntimeChanges() throws Exception {
        Fixture fixture = fixture("native update");
        firstRun(fixture);
        String updated = "IMPLEMENTOR=\"Azul Systems, Inc.\"\nJAVA_VERSION=\"25.0.6\"\nOS_ARCH=\"x86_64\"\n";
        Files.writeString(fixture.game().resolve("jre64/release"), updated);

        Result again = run(fixture, "build.bat", validEnvironment(fixture));

        assertEquals(0, again.exit(), again.output());
        Path exported = managedFixture(recording(fixture.source()));
        assertEquals(updated, Files.readString(exported.resolve("jre64/release")),
                "a changed game runtime must be copied into the managed fixture");
    }

    @Test
    void refusesUnknownFilesInsteadOfSilentlyOverwritingThem() throws Exception {
        Fixture fixture = fixture("native unknown");
        Path managed = firstRun(fixture);
        Path stray = Files.writeString(managed.resolve("jre64/stray.txt"), "not registered");

        Result again = run(fixture, "build.bat", validEnvironment(fixture));

        assertNotEquals(0, again.exit(), again.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(again, "an unregistered managed file must be refused", "unknown", "unregistered");
        assertEquals("not registered", Files.readString(stray), "the unknown file must be preserved for review");
    }

    @Test
    void refusesDigestDriftInsteadOfTreatingTheFixtureAsValid() throws Exception {
        Fixture fixture = fixture("native drift");
        Path managed = firstRun(fixture);
        Path release = managed.resolve("jre64/release");
        String tampered = Files.readString(release) + "# tampered";
        Files.writeString(release, tampered);

        Result again = run(fixture, "build.bat", validEnvironment(fixture));

        assertNotEquals(0, again.exit(), again.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(again, "a fixture whose digest drifted must be refused", "digest", "changed", "modified");
        assertEquals(tampered, Files.readString(release), "a drifted file must not be silently overwritten");
    }

    @Test
    void refusesReparsePointsInTheGameRuntimeSource() throws Exception {
        Fixture fixture = fixture("native reparse");
        Path target = Files.createDirectories(fixture.root().resolve("elsewhere"));
        Files.writeString(target.resolve("data.txt"), "outside");
        createJunction(fixture.game().resolve("jre64/linked"), target);

        Result result = run(fixture, "build.bat", validEnvironment(fixture));

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "a reparse point in the source runtime must be refused",
                "reparse", "link", "junction");
    }

    @Test
    void refusesWhenARequiredRuntimeFileIsInUse() throws Exception {
        Fixture fixture = fixture("native locked");
        Path managed = firstRun(fixture);
        Path sourceFile = fixture.game().resolve("jre64/lib/modules");
        deleteTree(managed.resolve("jre64"));
        Result result;
        try (FileChannel channel = FileChannel.open(sourceFile, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            result = run(fixture, "build.bat", validEnvironment(fixture));
        }

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "a runtime file that another process holds must be refused",
                "in use", "locked", "access", "cannot read", "used by another process", "being used",
                "cannot access", "占用", "正在使用", "另一进程", "另一个进程");
    }

    private static Path firstRun(Fixture fixture) throws Exception {
        Result result = run(fixture, "build.bat", validEnvironment(fixture));
        assertEquals(0, result.exit(), result.output());
        return managedFixture(recording(fixture.source()));
    }
}
