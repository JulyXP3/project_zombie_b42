package modcore.core;

import java.util.Objects;
import java.util.function.Consumer;

/** Readiness belongs to one Lua environment; a completed Java start is not Lua readiness. */
public final class RuntimeReadiness {
    private final Consumer<Event> sink;
    private boolean javaReady;
    private Object environment;
    private long generation;
    private boolean loading;
    private boolean luaReady;
    private String phase = "GAME_STARTING";

    public RuntimeReadiness(Consumer<Event> sink) { this.sink = Objects.requireNonNull(sink); }

    public synchronized void javaReady() {
        if (javaReady) return;
        javaReady = true;
        emit("JAVA_READY", "");
    }

    public synchronized boolean beginLua(Object env) {
        if (!javaReady || env == null) throw new IllegalStateException("Java or Lua environment is not ready");
        if (environment != env) {
            environment = env;
            generation++;
            loading = false;
            luaReady = false;
            emit("JAVA_READY", "Lua environment changed");
        }
        if (loading || luaReady) return false;
        loading = true;
        return true;
    }

    public synchronized void luaReady(Object env) {
        requireEnvironment(env);
        if (!loading) throw new IllegalStateException("Lua readiness without a loading attempt");
        loading = false;
        luaReady = true;
        emit("LUA_READY", "");
    }

    public synchronized void luaFailed(Object env, String detail) {
        requireEnvironment(env);
        loading = false;
        luaReady = false;
        emit("FAILED", detail);
    }

    public synchronized void uiOpened(Object env) {
        requireEnvironment(env);
        if (!luaReady) throw new IllegalStateException("Panel opened before current Lua environment was ready");
        emit("UI_OPENED", "");
    }

    public synchronized String phase() { return phase; }
    public synchronized long generation() { return generation; }

    private void requireEnvironment(Object env) {
        if (env == null || env != environment) throw new IllegalStateException("Stale Lua environment");
    }

    private void emit(String next, String detail) {
        phase = next;
        sink.accept(new Event(next, generation, detail));
    }

    public record Event(String phase, long generation, String detail) {}
}
