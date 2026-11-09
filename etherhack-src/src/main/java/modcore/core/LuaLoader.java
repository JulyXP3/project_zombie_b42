package modcore.core;

import modcore.utils.Logger;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;
import se.krka.kahlua.vm.LuaClosure;
import zombie.Lua.LuaManager;

/**
 * 红队 L2+L3: Lua 加载器 (fileless + 符号轮换)。
 *
 * L3 — fileless: 资源读取 + LuaCompiler.loadstring 编译进 LuaManager.env,
 * 完全不走 RunLua / ZomboidFileSystem 虚拟文件系统 — getLoadedLua / checksumLua
 * 枚举不到我方任何文件 (方案 §3-L3)。
 *
 * 资源只从当前运行时的 classloader 读取；Agent 模式由载荷内存索引提供。
 * 不再扫描 CWD 部署 jar 或明文资源目录，避免跨版本混用。
 *
 * L2 — 符号轮换: 加载时对源码做标识符替换 (Ether* / UI* 自定义全局 → 随机
 * 前缀), KWRR checkmodcore 硬编码的 12 个符号全部落空, 且 new() 覆写失去
 * 目标 (方案 §7.2-1: L1 拦不住功能破坏, L2 是功能存活前提)。
 */
public final class LuaLoader {

    /** 构建资源缺失时明确失败，不降级到固定前缀。 */
    private static final String PREFIX = loadPrefix();

    /** 符号映射: 源码标识符 → 运行时标识符。LinkedHashMap 保持替换顺序稳定。 */
    private static final Map<String, String> SYMBOL_MAP = new LinkedHashMap<>();

    /** 构建符号表来源；在 static 块之前声明，避免覆盖初始化结果。 */
    private static String symbolSource = "unknown";

    /** 已加载模块 (防重复加载, 对应 RunLua 的 loaded 集合语义)。 */
    private static final List<String> LOADED = new ArrayList<>();
    private static KahluaTable loadedEnvironment;
    private static final java.util.Set<String> LOADING = new java.util.HashSet<>();

    private static boolean initialized = false;

    /** 目标 env: 游戏里 = LuaManager.env; 冒烟探针注入独立 env (部署态验证)。 */
    public static KahluaTable targetEnv;
    public static LuaCaller targetCaller;
    public static KahluaThread targetThread;

    /** Resources come only from this runtime's loader; never scan a second deployment. */
    private static InputStream openResource(String path) {
        return modcore.utils.RuntimeResources.open(path);
    }

    private static String loadPrefix() {
        try (InputStream in = openResource("modcore/lua-prefix.properties")) {
            if (in != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(in);
                String p = props.getProperty("lua.prefix", "").trim();
                // Lua 标识符合法性: 字母开头 (生成器只产字母, 双保险)
                if (p.matches("[A-Za-z][A-Za-z0-9_]*")) {
                    return p;
                }
            }
        } catch (Exception ignored) {
        }
        throw new ExceptionInInitializerError("Valid modcore/lua-prefix.properties is required");
    }

    static {
        // 构建生成的符号表必须随载荷提供；不再扫描其他部署或使用手写回退。
        boolean buildTime = false;
        try (InputStream in = openResource("modcore/symbols.txt")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || SYMBOL_MAP.containsKey(line)) {
                            continue;
                        }
                        mapSymbol(line);
                    }
                }
                buildTime = true;
            }
        } catch (Exception ignored) {
        }

        if (buildTime && !SYMBOL_MAP.isEmpty()) {
            symbolSource = "build-time scan (" + SYMBOL_MAP.size() + ")";
            Logger.printLog("L2 symbols loaded: " + SYMBOL_MAP.size() + " (" + symbolSource + ")");
        } else {
            throw new ExceptionInInitializerError("Non-empty modcore/symbols.txt is required");
        }
    }

    /** 符号表来源描述 (L2 自检口): 供 ServerSyncBlocker#luaSymbolSource 与 SelfProbe 读取。 */
    public static String getSymbolSource() {
        return symbolSource;
    }


    /** 符号 → 随机前缀变体 (去掉 Ether/UI 特征头, 统一走词法合法前缀)。 */
    private static void mapSymbol(String s) {
        if (s.startsWith("Ether")) {
            SYMBOL_MAP.put(s, PREFIX + s.substring(5));
        } else if (s.startsWith("UI")) {
            SYMBOL_MAP.put(s, PREFIX + s.substring(2));
        } else {
            SYMBOL_MAP.put(s, PREFIX + s);
        }
    }

    private LuaLoader() {
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            // 编译错误细节进日志 (对齐 RunLuaInternal 的日志行为)
            LuaCompiler.rewriteEvents = false;
        }
    }

    /**
     * L2 符号替换 (字符串/注释感知): 逐字符扫描, 字符串字面量 (含转义) 与
     * 长短注释整体跳过, 只替换代码位的全词标识符 — 否则 loadModuleScript
     * (".../EtherTheme.lua") 里的符号也会被 \b 正则误改 → 路径 404。
     */
    private static String rewriteSymbols(String source) {
        StringBuilder out = new StringBuilder(source.length() + 256);
        StringBuilder word = new StringBuilder(64);
        int i = 0;
        int len = source.length();
        while (i < len) {
            char c = source.charAt(i);
            // 字符串字面量: 原样拷贝, 处理 \\ 与 \" 转义
            if (c == '"' || c == '\'') {
                char quote = c;
                out.append(c);
                ++i;
                while (i < len) {
                    char sc = source.charAt(i);
                    out.append(sc);
                    ++i;
                    if (sc == '\\' && i < len) {
                        out.append(source.charAt(i));
                        ++i;
                    } else if (sc == quote) {
                        break;
                    }
                }
                continue;
            }
            // 长注释 --[[ ... ]]
            if (c == '-' && i + 1 < len && source.charAt(i + 1) == '-'
                    && i + 3 < len && source.charAt(i + 2) == '[' && source.charAt(i + 3) == '[') {
                int end = source.indexOf("]]", i + 4);
                end = end == -1 ? len : end + 2;
                out.append(source, i, end);
                i = end;
                continue;
            }
            // 短注释 -- ... 行尾
            if (c == '-' && i + 1 < len && source.charAt(i + 1) == '-') {
                int end = source.indexOf('\n', i);
                end = end == -1 ? len : end;
                out.append(source, i, end);
                i = end;
                continue;
            }
            // 标识符: 攒词查表
            if (Character.isJavaIdentifierStart(c)) {
                word.setLength(0);
                while (i < len && Character.isJavaIdentifierPart(source.charAt(i))) {
                    word.append(source.charAt(i));
                    ++i;
                }
                String w = word.toString();
                String mapped = SYMBOL_MAP.get(w);
                out.append(mapped != null ? mapped : w);
                continue;
            }
            out.append(c);
            ++i;
        }
        return out.toString();
    }

    /**
     * L3 fileless 加载: classpath 读源 → 符号替换 → loadstring → protectedCall。
     * 返回模块返回值 (无返回值返回 null), 加载失败返回 null 并记日志。
     */
    public static Object load(String resourcePath) {
        try { return loadRequired(resourcePath); }
        catch (IllegalStateException e) {
            Logger.error(e.getMessage(), e);
            return null;
        }
    }

    /** 必需模块：nil 是正常返回；读取、编译或执行失败必须抛出带路径的异常。 */
    public static Object loadRequired(String resourcePath) {
        ensureInitialized();
        String key = resourcePath.replace("\\", "/");
        KahluaTable env = targetEnv != null ? targetEnv : LuaManager.env;
        if (env == null) throw new IllegalStateException("Lua environment missing for required module: " + key);
        if (loadedEnvironment != env) {
            resetLoaded();
            loadedEnvironment = env;
        }
        if (LOADED.contains(key)) return null;
        if (!LOADING.add(key)) throw new IllegalStateException("Cyclic required Lua module: " + key);
        try {
            String source;
            try (InputStream in = openResource(key)) {
                if (in == null) throw new java.io.IOException("Resource not found");
                source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            LuaCaller caller = targetCaller != null ? targetCaller : LuaManager.caller;
            KahluaThread thread = targetThread != null ? targetThread : LuaManager.thread;
            LuaClosure closure = LuaCompiler.loadstring(rewriteSymbols(source), key, env);
            LuaReturn ret = caller.protectedCall(thread, closure, new Object[0]);
            if (!ret.isSuccess()) {
                throw new IllegalStateException(ret.getErrorString() + "\n" + ret.getLuaStackTrace(), ret.getJavaException());
            }
            LOADED.add(key);
            return ret.isEmpty() ? null : ret.getFirst();
        } catch (Exception e) {
            throw new IllegalStateException("Required Lua module failed: " + key, e);
        } finally { LOADING.remove(key); }
    }

    static Object symbol(KahluaTable env, String name) {
        return env.rawget(SYMBOL_MAP.getOrDefault(name, name));
    }

    /** 批量加载 (保持 modcoreMenu 的注册顺序)。 */
    public static void loadAll(List<String> paths) {
        for (String p : paths) {
            load(p);
        }
    }

    /** Lua env 被 ResetLua 换新后由 LuaManager 调: 模块缓存随 env 失效。 */
    static void resetLoaded() {
        LOADED.clear();
        LOADING.clear();
        loadedEnvironment = null;
    }
}
