package modcore.core;

import modcore.BytecodeFixture;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserWaitTest {
    @Test
    void termsWaitIsReportedOnlyForTheActualCurrentState() throws Exception {
        var method = assertDoesNotThrow(() -> RuntimeBootstrap.class.getMethod("observeUserWait"),
                "the user-wait observer must exist");
        ClassNode source = BytecodeFixture.read("modcore/core/RuntimeBootstrap");
        ClassNode output = BytecodeFixture.type("fixture/UserWaitObserver");
        Map<String, String> remap = Map.of("zombie/GameWindow", BytecodeFixture.owner(Window.class),
                "zombie/gameStates/GameStateMachine", BytecodeFixture.owner(Machine.class),
                "modcore/core/RuntimeBootstrap", BytecodeFixture.owner(Recorder.class));
        output.methods.add(BytecodeFixture.copy(source.methods.stream().filter(m -> m.name.equals("observeUserWait")).findFirst().orElseThrow(), remap));
        Class<?> type = BytecodeFixture.define(output);
        Window.states = new Machine();
        Recorder.calls = 0;
        type.getMethod("observeUserWait").invoke(null);
        assertEquals(0, Recorder.calls);
        Window.states.current = new zombie.gameStates.TermsOfServiceState();
        type.getMethod("observeUserWait").invoke(null);
        assertEquals(1, Recorder.calls);
    }
    public static class Window { public static Machine states; }
    public static class Machine { public zombie.gameStates.GameState current; }
    public static class Recorder {
        static int calls;
        public static void waitingUser() { calls++; }
    }
}
