package zombie;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

// L4b 自举存根: 编译后落位 zombie/coreboot.class。两阶段 (实测教训
// 2026-09-10: LuaManager.init 的 exposer 链会触发 ClimateManager 类初始
// 化 -> 已补丁的 LuaEventManager.triggerEvent 引用 modcore 类, 故解包必须
// 在 LuaManager.init 之前; 而初始化链依赖 LuaManager.env, 必须在其后):
//   boot() — LuaManager.init 之前: 读 bin 解密 -> 清旧 modcore\ -> 解包;
//   start() — LuaManager.init 之后: 反射续调 LuaCompiler/Logo/CoreMain。
//
// 生命周期: bin 与存根常驻不自删 — 安装一次永久生效, 每次启动覆盖解包;
// 游戏退出由 CoreMain 的 shutdown 钩子尽力清理 modcore\, 失败留待下次
// boot() 兜底。磁盘常态 = 4KB 存根 + 密文 bin (+ 上次残留 modcore\)。
// 游戏类对 modcore.* 的直调在解包之后惰性解析命中 (JVM 双亲委派下
// AppCL 只能从 classpath 解析, 不可运行时注入)。
// 注: 密钥随 bin 同文件 — 防护目标是磁盘零特征, 不是抗提取 (同级边界)。
public class coreboot {

    private static final String BIN_NAME = "modcore.bin";

    /** 阶段一: 解密+解包 (GameWindow.init 中 LuaManager.init 调用之前执行)。 */
    public static void boot() {
        try {
            Path bin = Paths.get(System.getProperty("user.home"), "Zomboid", BIN_NAME);
            if (!Files.exists(bin)) {
                return; // 未安装载荷: 静默退出, 游戏照常
            }
            byte[] all = Files.readAllBytes(bin);
            byte[] jarBytes = decrypt(all);

            // 先清上次解包残留 (明文目录不跨会话累积; 类尚在内存, 删除安全)
            deleteTreeQuietly(Paths.get("modcore"));

            // 解包到游戏目录 (classpath "./" 可见, 目录名中性):
            //   class -> AppCL 惰性解析; lua -> LuaLoader classpath 读取
            //   (L3 语义不变, KWRR Checksum 只扫 media/lua mod 目录树);
            //   translations/media -> 文件系统读取
            JarInputStream jin = new JarInputStream(new java.io.ByteArrayInputStream(jarBytes));
            JarEntry entry;
            while ((entry = jin.getNextJarEntry()) != null) {
                String name = entry.getName();
                boolean wanted = name.startsWith("modcore/")
                    && (name.endsWith(".class")
                        || name.startsWith("modcore/lua/")
                        || name.startsWith("modcore/translations/")
                        || name.startsWith("modcore/media/")
                        || name.equals("modcore/modcore.properties"));
                if (entry.isDirectory() || !wanted) {
                    continue;
                }
                Path out = Paths.get(name);
                Files.createDirectories(out.getParent());
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = jin.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }
                Files.write(out, bos.toByteArray());
            }
            jin.close();
            jarBytes = null;
        } catch (Throwable t) {
            // 任何失败: 静默, 游戏照常启动 (无痕优先)
        }
    }

    /** 阶段二: 初始化链 (LuaManager.init 之后执行; 依赖其 env/converter)。 */
    public static void start() {
        try {
            Class<?> compiler = Class.forName("modcore.core.LuaCompiler");
            Object compilerInst = compiler.getMethod("getInstance").invoke(null);
            compiler.getMethod("init").invoke(compilerInst);
            Class<?> main = Class.forName("modcore.core.CoreMain");
            Object coreMain = main.getMethod("getInstance").invoke(null);
            main.getMethod("init").invoke(coreMain);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static byte[] decrypt(byte[] data) throws Exception {
        byte[] key = new byte[32];
        byte[] iv = new byte[12];
        byte[] body = new byte[data.length - 44];
        System.arraycopy(data, 0, key, 0, 32);
        System.arraycopy(data, 32, iv, 0, 12);
        System.arraycopy(data, 44, body, 0, body.length);
        javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        c.init(javax.crypto.Cipher.DECRYPT_MODE,
            new javax.crypto.spec.SecretKeySpec(key, "AES"),
            new javax.crypto.spec.GCMParameterSpec(128, iv));
        return c.doFinal(body);
    }

    private static void deleteTreeQuietly(Path root) {
        try {
            if (Files.exists(root)) {
                Files.walk(root).sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Throwable ignored) {
                    }
                });
            }
        } catch (Throwable ignored) {
        }
    }
}
