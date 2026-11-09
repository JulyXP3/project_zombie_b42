package modcore.core;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeReadinessTest {
    @Test
    void javaCompletionDoesNotClaimLuaOrVisiblePanelReadiness() throws Exception {
        Class<?> type = assertDoesNotThrow(() -> Class.forName("modcore.core.RuntimeReadiness"));
        Object state = type.getConstructor(java.util.function.Consumer.class).newInstance((java.util.function.Consumer<Object>) value -> {});
        type.getMethod("javaReady").invoke(state);
        assertEquals("JAVA_READY", type.getMethod("phase").invoke(state));
        assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> type.getMethod("uiOpened", Object.class).invoke(state, new Object()));
    }

    @Test
    void environmentGenerationPreventsStaleReadyAndDuplicateEvents() throws Exception {
        Class<?> type = assertDoesNotThrow(() -> Class.forName("modcore.core.RuntimeReadiness"));
        ArrayList<Object> events = new ArrayList<>();
        Object state = type.getConstructor(java.util.function.Consumer.class).newInstance((java.util.function.Consumer<Object>) events::add);
        Object first = new Object();
        Object second = new Object();
        type.getMethod("javaReady").invoke(state);
        assertEquals(true, type.getMethod("beginLua", Object.class).invoke(state, first));
        assertEquals(1L, type.getMethod("generation").invoke(state));
        type.getMethod("luaReady", Object.class).invoke(state, first);
        int count = events.size();
        assertEquals(false, type.getMethod("beginLua", Object.class).invoke(state, first));
        assertEquals(count, events.size());
        type.getMethod("uiOpened", Object.class).invoke(state, first);
        assertEquals("UI_OPENED", type.getMethod("phase").invoke(state));
        assertEquals(true, type.getMethod("beginLua", Object.class).invoke(state, second));
        assertEquals(2L, type.getMethod("generation").invoke(state));
        assertEquals("JAVA_READY", type.getMethod("phase").invoke(state));
        assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> type.getMethod("luaReady", Object.class).invoke(state, first));
        type.getMethod("luaFailed", Object.class, String.class).invoke(state, second, "module missing");
        assertEquals("FAILED", type.getMethod("phase").invoke(state));
        assertEquals(true, type.getMethod("beginLua", Object.class).invoke(state, second));
        type.getMethod("luaReady", Object.class).invoke(state, second);
        assertEquals("LUA_READY", type.getMethod("phase").invoke(state));
        assertEquals(2L, type.getMethod("generation").invoke(state));
    }
}
