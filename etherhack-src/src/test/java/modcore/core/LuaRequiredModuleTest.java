package modcore.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class LuaRequiredModuleTest {
    @BeforeEach
    void setup() { environment(); }

    @AfterEach
    void reset() {
        LuaLoader.resetLoaded();
        LuaLoader.targetEnv = null;
        LuaLoader.targetCaller = null;
        LuaLoader.targetThread = null;
    }

    @Test
    void nilReturnIsSuccessfulAndRepeatedLoadDoesNotRepeatSideEffects() throws Exception {
        assertNull(required("modcore/tests/nil.lua"));
        assertEquals(1.0, LuaLoader.targetEnv.rawget("moduleCalls"));
        assertNull(required("modcore/tests/nil.lua"));
        assertEquals(1.0, LuaLoader.targetEnv.rawget("moduleCalls"));
    }

    @Test
    void missingCompileAndExecutionFailuresNameTheRequiredModule() throws Exception {
        for (String name : new String[]{"missing", "compile-error", "execution-error"}) {
            String path = "modcore/tests/" + name + ".lua";
            var failure = assertThrows(IllegalStateException.class, () -> required(path));
            assertTrue(failure.getMessage().contains(path), failure.toString());
        }
    }

    @Test
    void changedEnvironmentReloadsModulesAndFailedLoadCanRetry() throws Exception {
        required("modcore/tests/nil.lua");
        environment();
        required("modcore/tests/nil.lua");
        assertEquals(1.0, LuaLoader.targetEnv.rawget("moduleCalls"));
        assertThrows(IllegalStateException.class, () -> required("modcore/tests/retry.lua"));
        LuaLoader.targetEnv.rawset("canLoad", Boolean.TRUE);
        required("modcore/tests/retry.lua");
        required("modcore/tests/retry.lua");
        assertEquals(1.0, LuaLoader.targetEnv.rawget("retryCalls"));
    }

    private static Object required(String path) throws Exception {
        var method = assertDoesNotThrow(() -> LuaLoader.class.getMethod("loadRequired", String.class),
                "required modules need a distinct success/failure contract");
        try { return method.invoke(null, path); }
        catch (InvocationTargetException e) { throw (Exception) e.getCause(); }
    }

    private static KahluaTable environment() {
        var platform = J2SEPlatform.getInstance();
        var env = platform.newTable();
        env.rawset("_G", env);
        BaseLib.register(env);
        var thread = new KahluaThread(platform, env);
        thread.debugOwnerThread = Thread.currentThread();
        LuaLoader.targetEnv = env;
        LuaLoader.targetThread = thread;
        LuaLoader.targetCaller = new LuaCaller(new KahluaConverterManager());
        return env;
    }
}
