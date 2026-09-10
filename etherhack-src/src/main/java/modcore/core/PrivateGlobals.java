package modcore.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;

/**
 * L1 辅助 (2026-09-10 封禁审计修正): 我方全局名私有清单 — 上报净化依据。
 *
 * 问题: KWRR verifyLuaGlobals 上报把客户端 _G 全部键名与服务器白名单比对,
 * 任何"白名单外"名字逐个写日志 (has unidentified lua globals [...]).
 * 我方 L2 轮换符号 (hYYu*) 与 ~250 个 @LuaMethod 全局方法名不在任何静态
 * 前缀内, 旧净化器 (Ether/UI/ServerSync/requireExtra) 拦不住 → 服务器白名单
 * 一旦被管理员种过, 全量暴露 (含 hackAdminAccess/bypassServerValidation 等
 * 自曝式方法名).
 *
 * 机制: 在三个时间窗前后对 _G 做差集 (beginCapture/endCapture):
 *   1. CoreMain.init    — LuaTranslator/CoreAPI/LuaBridge 构造窗;
 *   2. CoreAPI.loadAPI  — 全部 expose* 窗 (方法名);
 *   3. LuaBridge.loadLua — 我方 Lua 加载窗 (轮换符号 + ENV_MARKER)。
 * 差集即"我方注入的全局名", isPrivateGlobal() 暴露给 Lua 净化器查询。
 * 代价说明: 若第三方 mod 在捕获窗内加载, 其名字被误并入 — 仅表现为
 * "该名字不上报", 无功能影响。
 *
 * 注: 供 Lua 侧调用的 isPrivateGlobal() 在 ServerSyncBlocker (同包)。
 */
public final class PrivateGlobals {

    private static final Set<String> NAMES = ConcurrentHashMap.newKeySet();

    /** 捕获起点快照, 仅捕获期间非空 (单线程: 游戏主线程事件驱动)。 */
    private static Set<String> baseline;

    /** refresh 基准: 上次全量快照 (endCapture 后维护), 运行时补捕用。 */
    private static Set<String> lastCapture;

    private PrivateGlobals() {
    }

    /** 记录当前 _G 为基线 (此后新增的键视为我方)。 */
    public static synchronized void beginCapture() {
        baseline = snapshot();
    }

    /** 与基线做差集, 并入私有名单。 */
    public static synchronized void endCapture() {
        if (baseline == null) {
            return;
        }
        KahluaTable env = LuaManager.env;
        if (env != null) {
            KahluaTableIterator it = env.iterator();
            while (it.advance()) {
                Object key = it.getKey();
                if (key == null) {
                    continue;
                }
                String name = String.valueOf(key);
                if (!baseline.contains(name)) {
                    NAMES.add(name);
                }
            }
        }
        baseline = null;
        lastCapture = snapshot();
    }

    /**
     * 运行时补捕 (2026-09-10 兼容性加固): 与上次快照做差集, 捕获窗口外
     * (lazy init / 事件回调内) 创建的全局名。由 ReportSanitizer 在
     * verifyLuaGlobals/updateLuaGlobals 上报前触发一次 —— 恰是 _G 被枚举
     * 上交的同一时刻, 时间面完全对齐。未捕获过时为空操作。
     */
    public static synchronized void refresh() {
        if (lastCapture == null) {
            return;
        }
        Set<String> now = snapshot();
        for (String name : now) {
            if (!lastCapture.contains(name)) {
                NAMES.add(name);
            }
        }
        lastCapture = now;
    }

    /** 判断全局名是否为我方注入 (供 isPrivateGlobal @LuaMethod 调用)。 */
    public static boolean contains(String name) {
        return name != null && NAMES.contains(name);
    }

    private static Set<String> snapshot() {
        Set<String> out = new HashSet<>();
        KahluaTable env = LuaManager.env;
        if (env != null) {
            KahluaTableIterator it = env.iterator();
            while (it.advance()) {
                Object key = it.getKey();
                if (key != null) {
                    out.add(String.valueOf(key));
                }
            }
        }
        return out;
    }
}
