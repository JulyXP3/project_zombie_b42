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
        if (env.rawget(ENV_MARKER) != null) {
            // 同一 env 内重复触发 (无重置): 跳过, 对齐 RunLua 的 loaded 缓存语义
            // (重复执行会双注册 OnKeyPressed 等事件)
            return;
        }
        // env 已被 ResetLua 换新 (标记消失): 清模块缓存后全量重载
        LuaLoader.resetLoaded();
        // L1 修正: 捕获窗 3 — 我方 Lua 加载期注入的轮换符号 + ENV_MARKER
        PrivateGlobals.beginCapture();
        Logger.printLog("Loading Lua (fileless)...");
        // L1 (ReportSanitizer) 必须最先加载: 它要捕获 sendClientCommand 的
        // 原始函数引用 — 任何后续 mod 加载前就位 (方案 §3-L1)
        LuaLoader.load("modcore/lua/fixes/ReportSanitizer.lua");
        // 自证探针: 复刻蓝队检测原语本地自测 (只 print 本地日志, 无网络)
        LuaLoader.load("modcore/lua/fixes/SelfProbe.lua");
        // L3: fileless 加载, 不走 RunLua/虚拟 FS — getLoadedLua 枚举不到
        LuaLoader.load("modcore/lua/fixes/ISChatFix.lua");
        LuaLoader.load("modcore/lua/fixes/ServerSyncBlocker.lua");
        // 主入口: 内部 etherModules 列表经 loadModuleScript 全走 LuaLoader
        LuaLoader.load("modcore/lua/modcoreMenu.lua");
        env.rawset(ENV_MARKER, Boolean.TRUE);
        PrivateGlobals.endCapture();
    }
}
