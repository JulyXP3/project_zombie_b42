/*
 * Decompiled with CFR 0.152.
 */
package modcore.core;

import modcore.core.CoreAPI;
import modcore.core.LuaBridge;
import modcore.core.LuaTranslator;
import modcore.utils.Logger;

public class CoreMain {
    private static CoreMain instance;
    public LuaTranslator LuaTranslator;
    public LuaBridge LuaBridge;
    public CoreAPI CoreAPI;

    private CoreMain() {
    }

    public void init() {
        try {
            // L1 修正: 捕获窗 1 — LuaTranslator/CoreAPI/LuaBridge 构造期注入的全局名
            PrivateGlobals.beginCapture();
            Logger.printLog("Initializing ModCore...");
            Logger.printLog("Creating LuaTranslator...");
            this.LuaTranslator = new LuaTranslator();
            Logger.printLog("Loading translations...");
            this.LuaTranslator.loadTranslations();
            Logger.printLog("Creating CoreAPI...");
            this.CoreAPI = new CoreAPI();
            Logger.printLog("CoreAPI created successfully");
            Logger.printLog("Creating LuaBridge...");
            this.LuaBridge = new LuaBridge();
            PrivateGlobals.endCapture();
            Logger.printLog("ModCore initialization completed!");
            this.registerCleanupHook();
        }
        catch (Throwable e) {
            Logger.crash("Failed to initialize ModCore", e);
            System.err.println("CRITICAL ERROR during ModCore initialization:");
            e.printStackTrace(System.err);
            throw new RuntimeException("Critical initialization failure", e);
        }
    }

    /**
     * L4b 退出清理: 游戏退出时尽力删除解包残留的明文 modcore\ 目录;
     * 失败 (文件锁等) 留待下次启动 coreboot.boot() 兜底清理。
     */
    private void registerCleanupHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                java.nio.file.Path root = java.nio.file.Paths.get("modcore");
                if (java.nio.file.Files.exists(root)) {
                    java.nio.file.Files.walk(root)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                java.nio.file.Files.deleteIfExists(p);
                            } catch (Throwable ignored) {
                            }
                        });
                }
            } catch (Throwable ignored) {
            }
        }));
    }

    public static CoreMain getInstance() {
        if (instance == null) {
            instance = new CoreMain();
        }
        return instance;
    }
}
