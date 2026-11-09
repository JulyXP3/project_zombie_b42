package modcore.agent;

import modcore.GamePatcher;
import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PreparedPatchTest {
    @AfterEach
    void reset() { Patch.resetSession(); }

    @Test
    void preparationReportsKnownDriftAndUsesRuntimeBootstrapWithoutInstalling() throws Exception {
        var prepare = assertDoesNotThrow(() -> GamePatcher.class.getMethod("prepareAgentPatches", Path.class));
        Object result;
        try {
            result = prepare.invoke(new GamePatcher(), Path.of(System.getProperty("headshot.gameJar")));
        } catch (InvocationTargetException failure) {
            throw new AssertionError("Preparation failed", failure.getCause());
        }
        @SuppressWarnings("unchecked") Map<String, byte[]> classes = (Map<String, byte[]>)
                result.getClass().getMethod("classes").invoke(result);
        @SuppressWarnings("unchecked") java.util.List<String> unavailable = (java.util.List<String>)
                result.getClass().getMethod("unavailable").invoke(result);
        assertEquals(java.util.Set.of(), new java.util.HashSet<>(unavailable),
                "every prepared patch must apply to this preview build");
        assertTrue(classes.containsKey("zombie/GameWindow"));
        ClassNode window = new ClassNode();
        new ClassReader(classes.get("zombie/GameWindow")).accept(window, 0);
        var init = window.methods.stream().filter(method -> method.name.equals("init")).findFirst().orElseThrow();
        java.util.List<String> order = new java.util.ArrayList<>();
        for (var instruction : init.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                assertNotEquals("zombie/coreboot", call.owner);
                if (call.owner.equals("modcore/core/RuntimeBootstrap")) order.add(call.name);
                if (call.owner.equals("zombie/Lua/LuaManager") && call.name.equals("init")) order.add("lua");
            }
        }
        assertEquals(java.util.List.of("boot", "lua", "start"), order);
        assertFalse(java.nio.file.Files.exists(Path.of("zombie/GameWindow.class")));
        assertFalse(java.nio.file.Files.exists(Path.of("zombie/coreboot.class")));
    }
}
