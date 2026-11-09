package modcore.core;

import modcore.annotations.LuaEvents;
import modcore.annotations.SubscribeLuaEvent;
import modcore.utils.EventSubscriber;
import modcore.utils.Logger;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

public class LuaBridge {
    public final String pathToLuaMainFile = "modcore/lua/modcoreMenu.lua";

    /** env 生命周期标记: 活在 Lua env 里, ResetLua 清 env 时随之消失 (实测教训:
     *  进服 OnResetLua 后模块缓存未失效, 55 模块全部空转, 面板打不开)。 */
    private static final String ENV_MARKER = "__etherEnvMarker";

    public LuaBridge() {
        EventSubscriber.register(this);
    }

    @LuaEvents(value={@SubscribeLuaEvent(eventName="OnResetLua"), @SubscribeLuaEvent(eventName="OnMainMenuEnter")})
    public void loadLua() {
        KahluaTable env = LuaManager.env;
        if (env == null) {
            return;
        }
        if (env.rawget(ENV_MARKER) != null || !RuntimeBootstrap.beginLua(env)) {
            return;
        }
        // 缓存按环境失效；同一环境失败重试保留已经成功的模块，避免重复注册事件。
        PrivateGlobals.beginCapture();
        try {
            Logger.printLog("Loading required Lua modules...");
            for (String resource : java.util.List.of("modcore/lua/fixes/ReportSanitizer.lua",
                    "modcore/lua/fixes/ISChatFix.lua", "modcore/lua/fixes/ServerSyncBlocker.lua",
                    "modcore/lua/fixes/ExtraInfoMask.lua", pathToLuaMainFile)) {
                LuaLoader.loadRequired(resource);
            }
            verifyReady(env);
            // SelfProbe不是就绪门槛，只报告自身覆盖的检查。
            LuaLoader.load("modcore/lua/fixes/SelfProbe.lua");
            RuntimeBootstrap.luaReady(env);
            env.rawset(ENV_MARKER, Boolean.TRUE);
        } catch (RuntimeException | Error failure) {
            env.rawset(ENV_MARKER, null);
            RuntimeBootstrap.luaFailed(env, failure);
            Logger.error("Required Lua initialization failed", failure);
            throw failure;
        } finally {
            PrivateGlobals.endCapture();
        }
    }

    static void verifyReady(KahluaTable env) {
        for (String name : java.util.List.of("loadModuleScript", "getAccentUIColor", "resolveTexture",
                "getProjectLaunchId", "recordProjectUIOpened")) {
            requireFunction(env.rawget(name), "API " + name);
        }
        Object main = LuaLoader.symbol(env, "EtherMain");
        Object bindings = LuaLoader.symbol(env, "EtherKeyBinds");
        if (!(main instanceof KahluaTable menu) || !(bindings instanceof KahluaTable keys)) {
            throw new IllegalStateException("Required menu or key binding module is missing");
        }
        requireFunction(menu.rawget("toggleMenu"), "menu toggle");
        requireFunction(keys.rawget("onKeyPressed"), "key dispatcher");
        if (!(keys.rawget("registry") instanceof KahluaTable registry)
                || !(registry.rawget("menu") instanceof KahluaTable binding)) {
            throw new IllegalStateException("Menu key binding is not registered");
        }
        requireFunction(binding.rawget("handler"), "menu key handler");
    }

    private static void requireFunction(Object value, String name) {
        if (!(value instanceof se.krka.kahlua.vm.LuaClosure) && !(value instanceof se.krka.kahlua.vm.JavaFunction)) {
            throw new IllegalStateException("Required function missing: " + name);
        }
    }
}
