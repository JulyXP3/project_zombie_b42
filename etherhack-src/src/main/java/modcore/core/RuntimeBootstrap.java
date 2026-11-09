package modcore.core;

import modcore.utils.RuntimeResources;
import java.io.IOException;
import java.io.InputStream;

/** Preserves the two initialization phases without extracting a runtime directory. */
public final class RuntimeBootstrap {
    private static boolean prepared;
    private static boolean started;
    private static final RuntimeReadiness READINESS = new RuntimeReadiness(RuntimeBootstrap::record);
    private static String launchId;
    private static boolean waitingUser;

    private RuntimeBootstrap() {}

    @modcore.annotations.SubscribeLuaEvent(eventName="OnPostUIDraw")
    public static void observeUserWait() {
        if (zombie.GameWindow.states != null && zombie.GameWindow.states.current != null
                && zombie.GameWindow.states.current.getClass().getName().equals("zombie.gameStates.TermsOfServiceState")) {
            waitingUser();
        }
    }

    public static synchronized void waitingUser() {
        if (waitingUser || !READINESS.phase().equals("JAVA_READY")) return;
        waitingUser = true;
        record(new RuntimeReadiness.Event("WAITING_USER", READINESS.generation(), "Terms of service require user confirmation"));
    }

    public static synchronized void boot() {
        if (prepared) return;
        try {
            for (String name : java.util.List.of("modcore/modcore.properties", "modcore/symbols.txt",
                    "modcore/lua-prefix.properties", "modcore/resources.index")) {
                try (InputStream in = RuntimeResources.require(name)) {
                    if (in.read() == -1) throw new IOException("Empty runtime resource: " + name);
                }
            }
            prepared = true;
        } catch (IOException e) {
            throw new IllegalStateException("Runtime resource preflight failed", e);
        }
    }

    public static synchronized void start() {
        if (started) return;
        boot();
        try {
            LuaCompiler.getInstance().init();
            CoreMain.getInstance().init();
            started = true;
            READINESS.javaReady();
            modcore.utils.EventSubscriber.register(new RuntimeBootstrap());
        } catch (RuntimeException | Error e) {
            record(new RuntimeReadiness.Event("FAILED", 0, e.toString()));
            throw e;
        }
    }

    static boolean beginLua(Object env) { return READINESS.beginLua(env); }
    static void luaReady(Object env) { READINESS.luaReady(env); }
    static void luaFailed(Object env, Throwable failure) { READINESS.luaFailed(env, failure.toString()); }

    public static void uiOpened() { READINESS.uiOpened(zombie.Lua.LuaManager.env); }

    public static synchronized String launchId() {
        if (launchId != null) return launchId;
        String session = System.getProperty("modcore.sessionId", "unmanaged");
        String build = System.getProperty("modcore.buildId", "unknown");
        launchId = build.substring(0, Math.min(8, build.length())) + "/" + session.substring(0, Math.min(8, session.length()));
        return launchId;
    }

    private static void record(RuntimeReadiness.Event event) {
        String line = "[ModCore Runtime] " + event.phase() + " generation=" + event.generation()
                + " launch=" + launchId() + (event.detail().isEmpty() ? "" : " detail=" + event.detail());
        System.out.println(line);
        Module module = ModuleLayer.boot().findModule("modcore.launch").orElse(null);
        if (module == null) return;
        try {
            Class<?> gate = Class.forName(module, "modcore.launch.LaunchGate");
            gate.getMethod("runtimeState", String.class, long.class, String.class)
                    .invoke(null, event.phase(), event.generation(), event.detail());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not record runtime readiness", e);
        }
    }
}
