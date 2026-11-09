package modcore.agent;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Map;

import static modcore.agent.BuildEntryHarness.*;
import static org.junit.jupiter.api.Assertions.*;

class FastBuildEntryTest {
    @Test
    void fastBuildUsesTheExternalJdkAndSkipsTestsAndPreparation() throws Exception {
        Fixture fixture = fixture("fast entry");

        Result result = run(fixture, "build-fast.bat", validEnvironment(fixture));

        assertEquals(0, result.exit(), result.output());
        Map<String, String> call = recording(fixture.source());
        assertEquals(realJdk().toString(), call.get("JAVA_HOME"), result.output());
        assertMinimalBuild(call.get("GRADLE_ARGS"));
        assertFalse(call.containsKey(NATIVE_FIXTURE), "the fast entry must not export a native fixture: " + call);
        assertNoManagedFixture(fixture);
        assertTrue(result.output().contains("not a verified delivery package"), result.output());
        assertFalse(result.output().contains("Build and tests passed."), result.output());
    }

    @Test
    void fastBuildRefusesAnInvalidExplicitJdkInsteadOfFallingBack() throws Exception {
        Fixture fixture = fixture("fast bad jdk");

        Result result = run(fixture, "build-fast.bat", Map.of(
                JAVA_OVERRIDE, fixture.root().resolve("missing jdk").toString(),
                "JAVA_HOME", realJdk().toString(),
                "PATH", realJdk().resolve("bin") + ";" + pathWithPowerShell()));

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "an invalid explicit JDK must be rejected without falling back", "JDK");
    }

    @Test
    void fastBuildPropagatesGradleFailureWithoutClaimingSuccess() throws Exception {
        Fixture fixture = fixture("fast failure", 17);

        Result result = run(fixture, "build-fast.bat", validEnvironment(fixture));

        assertEquals(17, result.exit(), result.output());
        assertTrue(Files.exists(fixture.source().resolve(CALL_FILE)), result.output());
        assertFalse(result.output().contains("Fast build finished:"), result.output());
    }

    @Test
    void fastBuildRefusesMissingGameDependencyBeforeInvokingGradle() throws Exception {
        Fixture fixture = fixture("fast dependency");
        Files.delete(fixture.source().resolve("lib/projectzomboid.jar"));

        Result result = run(fixture, "build-fast.bat", validEnvironment(fixture));

        assertNotEquals(0, result.exit(), result.output());
        assertGradleNotInvoked(fixture.source());
        assertMessage(result, "a missing compile dependency must be named", "projectzomboid.jar");
    }

    private static void assertMinimalBuild(String arguments) {
        assertTrue(arguments.contains("clean jar"), arguments);
        assertTrue(arguments.contains("-x test"), arguments);
        assertFalse(arguments.contains("assemble"), arguments);
        assertFalse(arguments.contains("packageLoadout"), arguments);
        assertFalse(arguments.contains("skipDeploymentTests"), arguments);
    }
}
