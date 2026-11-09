package modcore.core;

import modcore.BytecodeFixture;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LuaBridgeReadinessTest {
    @Test
    void failedRequiredModuleDoesNotMarkEnvironmentAndAlwaysEndsCapture() throws Exception {
        Class<?> bridge = isolatedBridge();
        Object instance = bridge.getConstructor().newInstance();
        Fields.env = readyEnvironment();
        Capture.depth = 0;
        Modules.failed = true;
        State.state = new RuntimeReadiness(event -> {});
        State.state.javaReady();
        assertThrows(InvocationTargetException.class, () -> bridge.getMethod("loadLua").invoke(instance));
        assertEquals(0, Capture.depth);
        assertNull(Fields.env.rawget("__etherEnvMarker"));
        assertEquals("FAILED", State.state.phase());
        Modules.failed = false;
        bridge.getMethod("loadLua").invoke(instance);
        assertEquals(Boolean.TRUE, Fields.env.rawget("__etherEnvMarker"));
        assertEquals("LUA_READY", State.state.phase());
        int loaded = Modules.calls;
        bridge.getMethod("loadLua").invoke(instance);
        assertEquals(loaded, Modules.calls);
        Fields.env = readyEnvironment();
        bridge.getMethod("loadLua").invoke(instance);
        assertEquals(2, State.state.generation());
        assertEquals(0, Capture.depth);
    }

    @Test
    void missingMenuRegistrationIsNotAcceptedAsReady() throws Exception {
        Class<?> bridge = isolatedBridge();
        Object instance = bridge.getConstructor().newInstance();
        Fields.env = readyEnvironment();
        ((KahluaTable) Fields.env.rawget("EtherKeyBinds")).rawset("registry", null);
        Modules.failed = false;
        State.state = new RuntimeReadiness(event -> {});
        State.state.javaReady();
        assertThrows(InvocationTargetException.class, () -> bridge.getMethod("loadLua").invoke(instance));
        assertNull(Fields.env.rawget("__etherEnvMarker"));
        assertEquals("FAILED", State.state.phase());
        assertEquals(0, Capture.depth);
    }

    private static Class<?> isolatedBridge() throws Exception {
        ClassNode source = BytecodeFixture.read("modcore/core/LuaBridge");
        ClassNode target = BytecodeFixture.type("fixture/BridgeReadiness");
        target.fields.addAll(source.fields);
        Map<String, String> names = Map.of("modcore/core/LuaBridge", target.name,
                "zombie/Lua/LuaManager", BytecodeFixture.owner(Fields.class),
                "modcore/core/PrivateGlobals", BytecodeFixture.owner(Capture.class),
                "modcore/core/LuaLoader", BytecodeFixture.owner(Modules.class),
                "modcore/core/RuntimeBootstrap", BytecodeFixture.owner(State.class));
        for (var method : source.methods) {
            if (!method.name.equals("<init>")) target.methods.add(BytecodeFixture.copy(method, names));
        }
        // The live constructor registers with game events; the fixture invokes the event directly.
        var constructor = target.methods.getFirst();
        constructor.instructions.insertBefore(constructor.instructions.getLast(), new org.objectweb.asm.tree.VarInsnNode(25, 0));
        constructor.instructions.insertBefore(constructor.instructions.getLast(), new org.objectweb.asm.tree.LdcInsnNode("modcore/lua/modcoreMenu.lua"));
        constructor.instructions.insertBefore(constructor.instructions.getLast(), new org.objectweb.asm.tree.FieldInsnNode(181,
                target.name, "pathToLuaMainFile", "Ljava/lang/String;"));
        return BytecodeFixture.define(target);
    }

    private static KahluaTable readyEnvironment() {
        var platform = J2SEPlatform.getInstance();
        KahluaTable env = platform.newTable();
        JavaFunction function = (frame, count) -> 0;
        for (String name : new String[]{"loadModuleScript", "getAccentUIColor", "resolveTexture", "getProjectLaunchId", "recordProjectUIOpened"}) {
            env.rawset(name, function);
        }
        KahluaTable menu = platform.newTable();
        menu.rawset("toggleMenu", function);
        env.rawset("EtherMain", menu);
        KahluaTable keys = platform.newTable();
        keys.rawset("onKeyPressed", function);
        KahluaTable registry = platform.newTable();
        KahluaTable binding = platform.newTable();
        binding.rawset("handler", function);
        registry.rawset("menu", binding);
        keys.rawset("registry", registry);
        env.rawset("EtherKeyBinds", keys);
        return env;
    }

    public static class Fields { public static KahluaTable env; }
    public static class Capture {
        static int depth;
        public static void beginCapture() { depth++; }
        public static void endCapture() { depth--; }
    }
    public static class Modules {
        static boolean failed;
        static int calls;
        public static Object loadRequired(String path) {
            calls++;
            if (failed) throw new IllegalStateException("required module failed: " + path);
            return null;
        }
        public static Object load(String path) { return null; }
        public static Object symbol(KahluaTable env, String name) { return env.rawget(name); }
    }
    public static class State {
        static RuntimeReadiness state;
        public static boolean beginLua(Object env) { return state.beginLua(env); }
        public static void luaReady(Object env) { state.luaReady(env); }
        public static void luaFailed(Object env, Throwable failure) { state.luaFailed(env, failure.toString()); }
    }
}
