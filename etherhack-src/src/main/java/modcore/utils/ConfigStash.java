package modcore.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 配置加密暂存 (二百七十一/二百七十二) — %LOCALAPPDATA%\app_cache.dat 单文件 AES-GCM 密文。
 *
 * 两个写入方共用同一份实现:
 *  · 安装器 (GamePatcher): 卸载删目录前收档 / 重装时回填缺失文件;
 *  · 运行时 (二百七十三, 事件驱动 — 替代已删除的 60s 定时器): 各配置写入点
 *    (saveConfig / setPanelSize / deleteConfig / setLanguage / AutoDrive 驾驶参数)
 *    落盘后即刷新 + 会话边界 (OnMainMenuEnter/OnResetLua 的 loadAPI) 兜底 —
 *    无定时器、零常驻后台 I/O, 配置一变暂存即新; 这让纯批处理卸载
 *    (install.bat 无 jar 模式 / uninstall.bat, 不经 Java) 也能保住配置。
 *
 * 格式同 modcore.bin: 32B 随机 key + 12B GCM IV + 密文(Properties→XML→Base64, 键=平面文件名)。
 * logs 永不暂存。空目录不落盘 (防止把既有暂存清成空包)。本类必须保持 ASM-free (运行时加载)。
 */
public final class ConfigStash {

    private static final String STASH_NAME = "app_cache.dat";

    /** 运行时错误只记一次 (60s 节拍下防刷屏)。 */
    private static boolean runtimeErrorLogged = false;

    private ConfigStash() {
    }

    public static Path stashPath() {
        String local = System.getenv("LOCALAPPDATA");
        Path dir = local != null && !local.isEmpty()
            ? Paths.get(local)
            : Paths.get(System.getProperty("user.home"), "AppData", "Local");
        return dir.resolve(STASH_NAME);
    }

    /** 把 configDir 下全部文件打包加密写入暂存。runtime=true 时静默 (游戏内节拍调用)。
     *  @return 打包写入的文件数 (0 = 无配置可存, 未动暂存); -1 = 失败 */
    public static int saveSnapshot(File configDir, boolean runtime) {
        try {
            if (configDir == null || !Files.isDirectory(configDir.toPath(), new LinkOption[0])) {
                return 0;
            }
            Properties bundle = new Properties();
            try (Stream<Path> files = Files.list(configDir.toPath())) {
                for (Path p : files.filter(f -> Files.isRegularFile(f, new LinkOption[0]))
                        .collect(Collectors.toList())) {
                    bundle.setProperty(p.getFileName().toString(),
                        java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(p)));
                }
            }
            if (bundle.isEmpty()) {
                return 0;   // 空目录不落盘 — 不能把既有暂存清成空包
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bundle.storeToXML(out, "s", "UTF-8");
            byte[] key = new byte[32];
            new java.security.SecureRandom().nextBytes(key);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(key, "AES"));
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(out.toByteArray());
            Path stash = stashPath();
            Files.createDirectories(stash.getParent());
            ByteArrayOutputStream bin = new ByteArrayOutputStream();
            bin.write(key);
            bin.write(iv);
            bin.write(encrypted);
            // 先写 tmp 再原子替换, 游戏中途被杀也只留旧暂存不留半份
            Path tmp = stash.resolveSibling(STASH_NAME + ".tmp");
            Files.write(tmp, bin.toByteArray());
            Files.move(tmp, stash, StandardCopyOption.REPLACE_EXISTING);
            return bundle.size();
        }
        catch (Exception e) {
            if (runtime) {
                if (!runtimeErrorLogged) {
                    runtimeErrorLogged = true;
                    Logger.printLog("[ConfigStash] snapshot failed (once per session): " + e);
                }
                return -1;
            }
            System.err.println("[ModCore] Config stash failed (configs will be lost): " + e);
            return -1;
        }
    }

    /**
     * 回填缺失的配置文件 (已存在的一律不动 — 重装刷新路径天然跳过)。
     *
     * @return 还原的文件数; -1 = 暂存不可读 (陈旧/异物/损坏)
     */
    public static int restoreMissing(File configDir) {
        Path stash = stashPath();
        if (!Files.exists(stash, new LinkOption[0])) {
            return 0;
        }
        try {
            byte[] blob = Files.readAllBytes(stash);
            byte[] key = java.util.Arrays.copyOfRange(blob, 0, 32);
            byte[] iv = java.util.Arrays.copyOfRange(blob, 32, 44);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(key, "AES"),
                new javax.crypto.spec.GCMParameterSpec(128, iv));
            byte[] plain = cipher.doFinal(blob, 44, blob.length - 44);
            Properties bundle = new Properties();
            bundle.loadFromXML(new ByteArrayInputStream(plain));
            Files.createDirectories(configDir.toPath());
            int restored = 0;
            for (String name : bundle.stringPropertyNames()) {
                if (name.contains("/") || name.contains("\\") || name.contains("..")) {
                    continue;   // 暂存键 = 平面文件名, 拒绝任何路径形态
                }
                Path target = configDir.toPath().resolve(name);
                if (Files.exists(target, new LinkOption[0])) {
                    continue;
                }
                Files.write(target, java.util.Base64.getDecoder().decode(bundle.getProperty(name)));
                ++restored;
            }
            return restored;
        }
        catch (Exception e) {
            return -1;
        }
    }

    /** 暂存用完即删 (一次性)。 */
    public static void discard() {
        try {
            Files.deleteIfExists(stashPath());
            Files.deleteIfExists(stashPath().resolveSibling(STASH_NAME + ".tmp"));
        }
        catch (IOException ignored) {
        }
    }
}
