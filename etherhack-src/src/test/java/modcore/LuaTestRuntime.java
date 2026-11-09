package modcore;

import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.j2se.MathLib;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.stdlib.StringLib;
import se.krka.kahlua.stdlib.TableLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Runs the game's Lua VM without loading game media, native libraries or a world. */
final class LuaTestRuntime {
    private final J2SEPlatform platform = J2SEPlatform.getInstance();
    final KahluaTable environment = platform.newTable();
    private final KahluaThread thread;

    LuaTestRuntime() {
        environment.rawset("_G", environment);
        MathLib.register(platform, environment);
        BaseLib.register(environment);
        StringLib.register(platform, environment);
        TableLib.register(platform, environment);
        thread = new KahluaThread(platform, environment);
        thread.debugOwnerThread = Thread.currentThread();
        KahluaTable table = (KahluaTable) environment.rawget("table");
        environment.rawset("ipairs", table.rawget("ipairs"));
        environment.rawset("pairs", table.rawget("pairs"));
        environment.rawset("assert", (JavaFunction) (frame, count) -> {
            Object condition = frame.get(0);
            if (condition == null || Boolean.FALSE.equals(condition)) {
                throw new AssertionError(count > 1 ? frame.get(1) : "Lua assertion failed");
            }
            return 0;
        });
        KahluaTable math = (KahluaTable) environment.rawget("math");
        math.rawset("max", (JavaFunction) (frame, count) -> {
            double value = ((Number) frame.get(0)).doubleValue();
            for (int i = 1; i < count; i++) value = Math.max(value, ((Number) frame.get(i)).doubleValue());
            return frame.push(value);
        });
        math.rawset("min", (JavaFunction) (frame, count) -> {
            double value = ((Number) frame.get(0)).doubleValue();
            for (int i = 1; i < count; i++) value = Math.min(value, ((Number) frame.get(i)).doubleValue());
            return frame.push(value);
        });
        // The external stdlib.lua supplies table.sort in a live game. Keep its comparator contract here.
        table.rawset("sort", (JavaFunction) (frame, count) -> {
            KahluaTable values = (KahluaTable) frame.get(0);
            Object compare = count > 1 ? frame.get(1) : null;
            var entries = new ArrayList<Object>();
            for (int i = 1; i <= values.len(); i++) entries.add(values.rawget(i));
            entries.sort((left, right) -> {
                if (compare == null) return left.toString().compareTo(right.toString());
                if (Boolean.TRUE.equals(thread.call(compare, new Object[]{left, right}))) return -1;
                return Boolean.TRUE.equals(thread.call(compare, new Object[]{right, left})) ? 1 : 0;
            });
            for (int i = 0; i < entries.size(); i++) values.rawset(i + 1, entries.get(i));
            return 0;
        });
    }

    Object[] execute(String source) throws IOException {
        var closure = LuaCompiler.loadstring(source, "test", environment);
        Object[] result = thread.pcall(closure, new Object[0]);
        assertEquals(Boolean.TRUE, result[0], () -> Arrays.toString(result));
        return Arrays.copyOfRange(result, 1, result.length);
    }

    void resource(String name) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(stream, name);
            execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
