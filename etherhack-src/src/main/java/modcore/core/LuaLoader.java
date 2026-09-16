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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
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
 * 资源来源 (部署形态适配, 实测教训 §9.3):
 *   1. classpath (开发态 / fatjar 直跑);
 *   2. 游戏目录 modcore-*.jar zip 条目 (真机部署态: 安装器只把 .class 解包到
 *      游戏目录, jar 本身不在 classpath 上 — 纯 classpath 读取全部 404)。
 * 两种来源都不经过虚拟 FS; Lua 文件永不落地。
 *
 * L2 — 符号轮换: 加载时对源码做标识符替换 (Ether* / UI* 自定义全局 → 随机
 * 前缀), KWRR checkmodcore 硬编码的 12 个符号全部落空, 且 new() 覆写失去
 * 目标 (方案 §7.2-1: L1 拦不住功能破坏, L2 是功能存活前提)。
 */
public final class LuaLoader {

    /** 中性符号前缀: 构建期随机生成 (方案 §8.3-3), 从 jar 资源读取; 兜底 q0。 */
    private static final String PREFIX = loadPrefix();

    /** 符号映射: 源码标识符 → 运行时标识符。LinkedHashMap 保持替换顺序稳定。 */
    private static final Map<String, String> SYMBOL_MAP = new LinkedHashMap<>();

    /**
     * 符号表来源 (L2 自检口, 2026-09-14 部署断链修正): 构建期扫描 = 正常;
     * 运行时扫描 = symbols.txt 没落地但游戏目录有 Lua 树 (功能保住, 部署有洞);
     * 手写兜底 = 已降级 (滞后于 Lua 树, 新增符号会裸奔)。
     * 注意: 必须在 static 块之前声明 — 否则声明处的初始化会在 static 块之后执行,
     * 把 static 块算好的值覆盖回初始值。
     */
    private static String symbolSource = "unknown";

    /** 已加载模块 (防重复加载, 对应 RunLua 的 loaded 集合语义)。 */
    private static final List<String> LOADED = new ArrayList<>();

    /** 部署 jar 缓存 (游戏目录里的 modcore-*.jar, 找到一次后复用)。 */
    private static JarFile sourceJar;

    private static boolean initialized = false;

    /** 目标 env: 游戏里 = LuaManager.env; 冒烟探针注入独立 env (部署态验证)。 */
    public static KahluaTable targetEnv;
    public static LuaCaller targetCaller;
    public static KahluaThread targetThread;

    /** 打开资源: classpath 优先, 游戏目录部署 jar 兜底; 都没有返回 null。 */
    private static InputStream openResource(String path) {
        InputStream in = LuaLoader.class.getClassLoader().getResourceAsStream(path);
        if (in != null) {
            return in;
        }
        try {
            if (sourceJar == null) {
                sourceJar = locateSourceJar();
            }
            if (sourceJar != null) {
                ZipEntry entry = sourceJar.getEntry(path);
                if (entry != null) {
                    return sourceJar.getInputStream(entry);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 定位部署 jar: 当前目录 (游戏根目录) 下的 modcore-*.jar。 */
    private static JarFile locateSourceJar() {
        try {
            java.io.File dir = new java.io.File(System.getProperty("user.dir"));
            java.io.File[] jars = dir.listFiles(
                    (d, name) -> name.startsWith("modcore-") && name.endsWith(".jar"));
            if (jars != null && jars.length > 0) {
                return new JarFile(jars[0]);
            }
        } catch (Exception e) {
            Logger.printLog("LuaLoader source jar locate failed: " + e.getMessage());
        }
        return null;
    }

    private static String loadPrefix() {
        try (InputStream in = openResource("modcore/lua-prefix.properties")) {
            if (in != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(in);
                String p = props.getProperty("lua.prefix", "").trim();
                // Lua 标识符合法性: 字母开头 (生成器只产字母, 双保险)
                if (!p.isEmpty() && Character.isJavaIdentifierStart(p.charAt(0))) {
                    return p;
                }
            }
        } catch (Exception ignored) {
        }
        // 2026-09-14 部署断链修正: 资源没随载荷落地时旧实现静默退化 —— 现明确告警,
        // 否则「每构建随机前缀」在生产上一直是固定字符串而无人察觉。
        Logger.warn("L2 lua-prefix.properties missing (classpath & deploy jar) - deploy gap: "
                + "falling back to fixed prefix \"q0\", per-build random prefix lost");
        return "q0"; // 资源缺失兜底 (仍非常识特征)
    }

    static {
        // 符号表三来源 (2026-09-14 部署断链修正): 构建期机械扫描 (modcore/symbols.txt,
        // 主) → 运行时扫描 (游戏目录 modcore/lua/**, 兜底; 与构建期规则逐字相同) →
        // 手写清单 (最后手段, 天然滞后于 Lua 树)。
        // 2026-09-10 封禁事件教训: 手写清单漏 EtherMain/EtherDriveModule_addTo/
        // EtherDriveCombatModule_addTo → 全局裸奔被 KWRR "if EtherMain" 探测抓到。
        // 机械扫描规则: ^\s*(Ether|UI)\w+\s*[=.] 与 ^\s*function\s+(Ether|UI)\w+。
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

        if (buildTime) {
            symbolSource = "build-time scan (" + SYMBOL_MAP.size() + ")";
            Logger.printLog("L2 symbols loaded: " + SYMBOL_MAP.size() + " (" + symbolSource + ")");
        } else {
            // symbols.txt 没随载荷落地 (旧白名单漏发) 或读取失败: 先告警, 再就地扫
            // 游戏目录的 Lua 树把机械结果补回来 —— 手写清单只在两条机械来源都拿不
            // 到时才兜底, 免得上线后新增的符号静默裸奔 (EtherPick 就是这么漏的)。
            Logger.warn("L2 symbols.txt missing (classpath & deploy jar) - deploy gap: "
                    + "falling back to runtime scan of modcore/lua");
            int scanned = scanRuntimeSymbols();
            if (!SYMBOL_MAP.isEmpty()) {
                symbolSource = "runtime scan (" + SYMBOL_MAP.size() + ")";
                Logger.printLog("L2 symbols loaded: " + SYMBOL_MAP.size()
                        + " (runtime scan over " + scanned + " lua files)");
            } else {
                Logger.warn("L2 runtime scan found nothing under modcore/lua - "
                        + "using handwritten fallback (symbols added later may leak)");
                // 兜底清单 (两条机械来源都拿不到时仍覆盖 KWRR 点名的核心面板符号)
                // 取值来源: 全 Lua 树 ^Ether\w*\s*= / ^function Ether\w*[:.] 扫描 (54 文件);
                // 含 2026-09-10 补齐的 EtherMain / EtherDriveModule_addTo 变体。
                String[] etherSymbols = {
                    "EtherMain", "CoreMain", "EtherAdminMenu", "EtherDebugMenu", "EtherEditWorldObjects",
                    "EtherEditInventoryItem", "EtherCharacterPanel", "EtherCombatPanel",
                    "EtherExploitPanel", "EtherInfoPanel", "EtherItemCreator",
                    "EtherMapPanel", "EtherPlayerEditor", "EtherSettingsPanel",
                    "EtherVisualsPanel", "EtherVehiclePanel", "EtherFunPanel",
                    "EtherExchangePanel", "EtherFarmingPanel", "EtherLootRollPanel",
                    "EtherRadarPanel", "EtherTrapSpawn", "EtherCharacterBoostPanel",
                    "EtherKeyBindsPanel", "EtherKeyBinds", "EtherFormPanel", "EtherTheme",
                    "EtherI18n", "EtherItemSearch", "EtherTrapPOC", "EtherFishSpawn",
                    "EtherRadioXp", "EtherExchange", "EtherAmmoFarm",
                    "EtherTempWeapon", "EtherContainerPOC", "EtherDriveModule",
                    "EtherDriveModule_addTo", "EtherDriveCombatModule",
                    "EtherDriveCombatModule_addTo", "EtherCharacterCreation", "EtherCharacterPane"
                };
                for (String s : etherSymbols) {
                    mapSymbol(s); // EtherMain → XMain
                }
                // UI* 自定义全局 (luaGlobals 白名单面上同属我方特征)
                String[] uiSymbols = {
                    "UIButtonsPanel", "UICheckbox", "UIButton", "UISlider", "UIMechanics",
                    "UIModalAddXP", "UIMovableMiniMap", "UIModalAddTrait", "UIHealth",
                    "UIItemTables", "UIMap", "UISkillTable", "UITraitsTable",
                    "UIRowBox", "UISectionHeader", "UIStatsEditor"
                };
                for (String s : uiSymbols) {
                    mapSymbol(s); // UIButton → XButton
                }
                symbolSource = "handwritten fallback (" + SYMBOL_MAP.size() + ")";
                Logger.printLog("L2 symbols loaded: " + SYMBOL_MAP.size() + " (" + symbolSource + ")");
            }
        }
    }

    /** 符号表来源描述 (L2 自检口): 供 ServerSyncBlocker#luaSymbolSource 与 SelfProbe 读取。 */
    public static String getSymbolSource() {
        return symbolSource;
    }

    /**
     * 运行时符号扫描兜底 (2026-09-14 L2 部署断链修正): symbols.txt 没能随载荷
     * 落地时, 就地读游戏目录 (进程工作目录) 下 modcore/lua 树里的 .lua, 用与构建期
     * build.gradle.kts#generateLuaSymbols 逐字相同的两条规则提取符号。
     * 纯只读: 目录不存在 / 读取失败都只返回 0, 不抛异常 (不得影响游戏启动)。
     *
     * @return 实际读到的 .lua 文件数 (0 = 目录缺失或扫描失败)
     */
    private static int scanRuntimeSymbols() {
        int files = 0;
        try {
            java.nio.file.Path root = java.nio.file.Paths.get(
                    System.getProperty("user.dir"), "modcore", "lua");
            if (!java.nio.file.Files.isDirectory(root)) {
                return 0;
            }
            java.util.regex.Pattern reAssign = java.util.regex.Pattern.compile(
                    "(?m)^\\s*(Ether\\w+|UI\\w+)\\s*[=.]");
            java.util.regex.Pattern reFunc = java.util.regex.Pattern.compile(
                    "(?m)^\\s*function\\s+(Ether\\w+|UI\\w+)[\\s(.:]");
            java.util.List<java.nio.file.Path> luaFiles = new java.util.ArrayList<>();
            try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(root)) {
                walk.filter(java.nio.file.Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".lua"))
                        .forEach(luaFiles::add);
            }
            for (java.nio.file.Path p : luaFiles) {
                String text = new String(java.nio.file.Files.readAllBytes(p), StandardCharsets.UTF_8);
                java.util.regex.Matcher ma = reAssign.matcher(text);
                while (ma.find()) {
                    mapSymbol(ma.group(1));
                }
                java.util.regex.Matcher mf = reFunc.matcher(text);
                while (mf.find()) {
                    mapSymbol(mf.group(1));
                }
                ++files;
            }
        } catch (Throwable t) {
            // 扫描失败无碍: 交由手写清单兜底
            Logger.warn("L2 runtime symbol scan failed: " + t.getMessage());
        }
        return files;
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
        ensureInitialized();
        String key = resourcePath.replace("\\", "/");
        if (LOADED.contains(key)) {
            return null;
        }

        String source;
        try (InputStream in = openResource(key)) {
            if (in == null) {
                Logger.error("Lua resource not found (classpath & deploy jar): " + key);
                return null;
            }
            StringBuilder sb = new StringBuilder(8192);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                char[] buf = new char[4096];
                int n;
                while ((n = reader.read(buf)) > 0) {
                    sb.append(buf, 0, n);
                }
            }
            source = sb.toString();
        } catch (Exception e) {
            Logger.error("Error reading Lua resource: " + key, e);
            return null;
        }

        try {
            KahluaTable env = targetEnv != null ? targetEnv : LuaManager.env;
            LuaCaller caller = targetCaller != null ? targetCaller : LuaManager.caller;
            KahluaThread thread = targetThread != null ? targetThread : LuaManager.thread;
            String rewritten = rewriteSymbols(source);
            LuaClosure closure = LuaCompiler.loadstring(rewritten, key, env);
            LuaReturn ret = caller.protectedCall(thread, closure, new Object[0]);
            if (!ret.isSuccess()) {
                Logger.error("Lua load failed: " + key + "\n"
                        + ret.getErrorString() + "\n" + ret.getLuaStackTrace(),
                        ret.getJavaException());
                return null;
            }
            LOADED.add(key);
            return ret.isEmpty() ? null : ret.getFirst();
        } catch (Exception e) {
            Logger.error("Error loading Lua (fileless): " + key, e);
            return null;
        }
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
    }
}
