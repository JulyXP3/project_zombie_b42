package EtherHack.Ether;

import EtherHack.utils.Logger;
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
 *   2. 游戏目录 EtherHack-*.jar zip 条目 (真机部署态: 安装器只把 .class 解包到
 *      游戏目录, jar 本身不在 classpath 上 — 纯 classpath 读取全部 404)。
 * 两种来源都不经过虚拟 FS; Lua 文件永不落地。
 *
 * L2 — 符号轮换: 加载时对源码做标识符替换 (Ether* / UI* 自定义全局 → 随机
 * 前缀), KWRR checkEtherHack 硬编码的 12 个符号全部落空, 且 new() 覆写失去
 * 目标 (方案 §7.2-1: L1 拦不住功能破坏, L2 是功能存活前提)。
 */
public final class EtherLuaLoader {

    /** 中性符号前缀: 构建期随机生成 (方案 §8.3-3), 从 jar 资源读取; 兜底 q0。 */
    private static final String PREFIX = loadPrefix();

    /** 符号映射: 源码标识符 → 运行时标识符。LinkedHashMap 保持替换顺序稳定。 */
    private static final Map<String, String> SYMBOL_MAP = new LinkedHashMap<>();

    /** 已加载模块 (防重复加载, 对应 RunLua 的 loaded 集合语义)。 */
    private static final List<String> LOADED = new ArrayList<>();

    /** 部署 jar 缓存 (游戏目录里的 EtherHack-*.jar, 找到一次后复用)。 */
    private static JarFile sourceJar;

    private static boolean initialized = false;

    /** 目标 env: 游戏里 = LuaManager.env; 冒烟探针注入独立 env (部署态验证)。 */
    public static KahluaTable targetEnv;
    public static LuaCaller targetCaller;
    public static KahluaThread targetThread;

    /** 打开资源: classpath 优先, 游戏目录部署 jar 兜底; 都没有返回 null。 */
    private static InputStream openResource(String path) {
        InputStream in = EtherLuaLoader.class.getClassLoader().getResourceAsStream(path);
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

    /** 定位部署 jar: 当前目录 (游戏根目录) 下的 EtherHack-*.jar。 */
    private static JarFile locateSourceJar() {
        try {
            java.io.File dir = new java.io.File(System.getProperty("user.dir"));
            java.io.File[] jars = dir.listFiles(
                    (d, name) -> name.startsWith("EtherHack-") && name.endsWith(".jar"));
            if (jars != null && jars.length > 0) {
                return new JarFile(jars[0]);
            }
        } catch (Exception e) {
            Logger.printLog("LuaLoader source jar locate failed: " + e.getMessage());
        }
        return null;
    }

    private static String loadPrefix() {
        try (InputStream in = openResource("EtherHack/lua-prefix.properties")) {
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
        return "q0"; // 资源缺失兜底 (仍非常识特征)
    }

    static {
        // KWRR checkEtherHack 点名的 12 个 + 其余我方全局 (Ether*)
        // 取值来源: 全 Lua 树 ^Ether\w*\s*= / ^function Ether\w*[:.] 扫描 (54 文件)
        String[] etherSymbols = {
            "EtherMain", "EtherAdminMenu", "EtherDebugMenu", "EtherEditWorldObjects",
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
            "EtherDriveCombatModule", "EtherCharacterCreation", "EtherCharacterPane"
        };
        for (String s : etherSymbols) {
            SYMBOL_MAP.put(s, PREFIX + s.substring(5)); // EtherMain → XMain
        }
        // UI* 自定义全局 (luaGlobals 白名单面上同属我方特征)
        String[] uiSymbols = {
            "UIButtonsPanel", "UICheckbox", "UIButton", "UISlider", "UIMechanics",
            "UIModalAddXP", "UIMovableMiniMap", "UIModalAddTrait", "UIHealth",
            "UIItemTables", "UIMap", "UISkillTable", "UITraitsTable",
            "UIRowBox", "UISectionHeader", "UIStatsEditor"
        };
        for (String s : uiSymbols) {
            SYMBOL_MAP.put(s, PREFIX + s.substring(2)); // UIButton → XButton
        }
    }

    private EtherLuaLoader() {
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
     * 长短注释整体跳过, 只替换代码位的全词标识符 — 否则 requireExtra
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

    /** 批量加载 (保持 EtherHackMenu 的注册顺序)。 */
    public static void loadAll(List<String> paths) {
        for (String p : paths) {
            load(p);
        }
    }

    /** Lua env 被 ResetLua 换新后由 EtherLuaManager 调: 模块缓存随 env 失效。 */
    static void resetLoaded() {
        LOADED.clear();
    }
}
