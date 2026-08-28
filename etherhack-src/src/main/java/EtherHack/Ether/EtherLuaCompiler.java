/*
 * Decompiled with CFR 0.152.
 */
package EtherHack.Ether;

import EtherHack.utils.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class EtherLuaCompiler {
    private static EtherLuaCompiler instance;
    public boolean isBlockCompileDefaultLua = true;
    public boolean isBlockCompileLuaAboutEtherHack = true;
    public boolean isBlockCompileLuaWithBadWords = false;
    public ArrayList<String> whiteListPathCompiler = new ArrayList();
    public ArrayList<String> blackListWordsEtherUICompiler = new ArrayList();
    public String[] blackListWordsCompiler = new String[]{"logExploit", "LogExtender", "ISLogSystem", "writeLog", "sendLog", "PARP", "Bikinitools", "AVCS", "BTSE", "AntiCheat", "ISPerkLog", "getCore():quitToDesktop()", "KickPlayer", "kickPlayer", "playerKick", "PlayerKick", "banPlayer", "PlayerBan", "playerBan", "AnTiCheat"};
    public String[] stopDefaultLuaCompile = new String[]{"ISPerkLog"};

    public boolean isShouldLuaCompile(String var1) {
        for (String var3 : this.whiteListPathCompiler) {
            if (!var3.equals(var1)) continue;
            return true;
        }
        String var10 = "";
        try (BufferedReader var11 = new BufferedReader(new FileReader(var1));){
            StringBuilder var4 = new StringBuilder();
            while (true) {
                String var5;
                if ((var5 = var11.readLine()) == null) {
                    var10 = var4.toString();
                    break;
                }
                var4.append(var5).append("\n");
            }
        }
        catch (IOException var9) {
            var9.printStackTrace();
        }
        if (this.isBlockCompileLuaAboutEtherHack) {
            for (String var14 : this.blackListWordsEtherUICompiler) {
                if (!var10.contains(var14) || !var1.toLowerCase().contains("mod")) continue;
                Logger.printLog("File '" + var1 + "' is not allowed to compile. Contains the word: '" + var14 + "'");
                return false;
            }
        }
        if (this.isBlockCompileLuaWithBadWords) {
            for (String var6 : this.blackListWordsCompiler) {
                if (var10.contains(var6) && var1.toLowerCase().contains("mod")) {
                    Logger.printLog("File '" + var1 + "' is not allowed to compile. Contains the word: '" + var6 + "'");
                    return false;
                }
                if (!var1.toLowerCase().contains("mod") || !var1.toLowerCase().contains(var6.toLowerCase())) continue;
                Logger.printLog("File '" + var1 + "' is not allowed to compile. Contains the word in the file name: '" + var6 + "'");
                return false;
            }
        }
        if (this.isBlockCompileDefaultLua) {
            for (String var6 : this.stopDefaultLuaCompile) {
                if (!var1.toLowerCase().contains(var6.toLowerCase())) continue;
                Logger.printLog("File '" + var1 + "' is not allowed to compile. This is a standard logger - disable it!");
                return false;
            }
        }
        return true;
    }

    public void addWordToBlacklistLuaCompiler(String var1) {
        if (!this.blackListWordsEtherUICompiler.contains(var1)) {
            this.blackListWordsEtherUICompiler.add(var1);
        }
    }

    public void addPathToWhiteListLuaCompiler(String var1) {
        if (!this.whiteListPathCompiler.contains(var1)) {
            this.whiteListPathCompiler.add(var1);
        }
    }

    public void init() {
        Logger.printLog("Initializing EtherLuaCompiler...");
    }

    public static EtherLuaCompiler getInstance() {
        if (instance == null) {
            instance = new EtherLuaCompiler();
        }
        return instance;
    }
}
