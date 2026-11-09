package modcore.agent;

import modcore.BytecodeFixture;
import modcore.GamePatcher;
import modcore.utils.Patch;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BootstrapFailureTest {
    @Test
    void bootstrapFailuresPropagateInsteadOfContinuingIntoGameInitialization() throws Exception {
        ClassNode input = BytecodeFixture.type("zombie/GameWindow");
        MethodNode init = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "init", "()V", null, null);
        init.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "zombie/Lua/LuaManager", "init", "()V", false));
        init.instructions.add(new InsnNode(Opcodes.RETURN));
        input.methods.add(init);
        MethodNode display = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "InitDisplay", "()V", null, null);
        display.instructions.add(new LdcInsnNode("Project Zomboid"));
        display.instructions.add(new InsnNode(Opcodes.POP));
        display.instructions.add(new InsnNode(Opcodes.RETURN));
        input.methods.add(display);
        Patch.resetSession();
        try {
            var lookup = java.lang.invoke.MethodHandles.privateLookupIn(Patch.class, java.lang.invoke.MethodHandles.lookup());
            @SuppressWarnings("unchecked") Map<String, ClassNode> nodes = (Map<String, ClassNode>)
                    lookup.findStaticVarHandle(Patch.class, "classNodeMap", Map.class).get();
            nodes.put("zombie/GameWindow", input);
            new GamePatcher().patchGameWindow();
            MethodNode patched = nodes.get("zombie/GameWindow").methods.stream()
                    .filter(method -> method.name.equals("init")).findFirst().orElseThrow();
            ClassNode output = BytecodeFixture.type("fixture/BootstrapFailure");
            output.methods.add(BytecodeFixture.copy(patched, Map.of("zombie/coreboot", BytecodeFixture.owner(Boot.class),
                    "zombie/Lua/LuaManager", BytecodeFixture.owner(Lua.class))));
            Class<?> type = BytecodeFixture.define(output);
            Boot.failBoot = true;
            Lua.called = false;
            var early = assertThrows(InvocationTargetException.class, () -> type.getMethod("init").invoke(null));
            assertEquals("boot failed", early.getCause().getMessage());
            assertFalse(Lua.called);
            Boot.failBoot = false;
            var late = assertThrows(InvocationTargetException.class, () -> type.getMethod("init").invoke(null));
            assertEquals("start failed", late.getCause().getMessage());
            assertTrue(Lua.called);
        } finally { Patch.resetSession(); }
    }

    public static class Boot {
        static boolean failBoot;
        public static void boot() { if (failBoot) throw new IllegalStateException("boot failed"); }
        public static void start() { throw new IllegalStateException("start failed"); }
    }
    public static class Lua {
        static boolean called;
        public static void init() { called = true; }
    }
}
