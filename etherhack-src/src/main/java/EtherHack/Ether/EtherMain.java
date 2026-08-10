/*
 * Decompiled with CFR 0.152.
 */
package EtherHack.Ether;

import EtherHack.Ether.EtherAPI;
import EtherHack.Ether.EtherCredits;
import EtherHack.Ether.EtherLuaManager;
import EtherHack.Ether.EtherTranslator;
import EtherHack.utils.Logger;

public class EtherMain {
    private static EtherMain instance;
    public EtherTranslator etherTranslator;
    public EtherCredits etherCredits;
    public EtherLuaManager etherLuaManager;
    public EtherAPI etherAPI;

    private EtherMain() {
    }

    public void init() {
        try {
            Logger.printLog("Initializing DeiClient...");
            Logger.printLog("Creating EtherTranslator...");
            this.etherTranslator = new EtherTranslator();
            Logger.printLog("Loading translations...");
            this.etherTranslator.loadTranslations();
            Logger.printLog("Creating EtherCredits...");
            this.etherCredits = new EtherCredits();
            Logger.printLog("Creating EtherAPI...");
            this.etherAPI = new EtherAPI();
            Logger.printLog("EtherAPI created successfully");
            Logger.printLog("Creating EtherLuaManager...");
            this.etherLuaManager = new EtherLuaManager();
            Logger.printLog("DeiClient initialization completed!");
        }
        catch (Throwable e) {
            Logger.crash("Failed to initialize DeiClient", e);
            System.err.println("CRITICAL ERROR during DeiClient initialization:");
            e.printStackTrace(System.err);
            throw new RuntimeException("Critical initialization failure", e);
        }
    }

    public static EtherMain getInstance() {
        if (instance == null) {
            instance = new EtherMain();
        }
        return instance;
    }
}
