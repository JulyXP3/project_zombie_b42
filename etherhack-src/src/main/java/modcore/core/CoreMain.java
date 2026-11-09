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
            Logger.printLog("ModCore initialization completed!");
        }
        catch (Throwable e) {
            Logger.crash("Failed to initialize ModCore", e);
            System.err.println("CRITICAL ERROR during ModCore initialization:");
            e.printStackTrace(System.err);
            throw new RuntimeException("Critical initialization failure", e);
        } finally {
            PrivateGlobals.endCapture();
        }
    }

    public static CoreMain getInstance() {
        if (instance == null) {
            instance = new CoreMain();
        }
        return instance;
    }
}
