/*
 * Decompiled with CFR 0.152.
 * 
 *  Could not load the following classes:
 *  se.krka.kahlua.vm.KahluaTable
 *  se.krka.kahlua.vm.KahluaTableIterator
 */
package EtherHack.Ether;

import EtherHack.utils.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;

public class EtherTranslator {
    private static final String TRANSLATIONS_PATH = "EtherHack/translations";
    private static final String LANGUAGE_CONFIG_PATH = "EtherHack/config/language.properties";
    private Map translations;
    private String currentLanguage;

    public EtherTranslator() {
        Logger.printLog("Initializing EtherTranslator...");
        this.translations = new HashMap();
    }

    public void setLanguage(String var1) {
        this.currentLanguage = var1;
        Logger.printLog("EtherHack language set to: " + var1);
        try {
            File configDir = new File("EtherHack/config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            Properties props = new Properties();
            props.setProperty("language", var1);
            try (FileOutputStream out = new FileOutputStream(LANGUAGE_CONFIG_PATH)) {
                props.store(out, (String)null);
            }
        }
        catch (IOException e) {
            Logger.printLog("Error while saving language: " + e.getMessage());
        }
    }

    public String getCurrentLanguage() {
        if (this.currentLanguage != null) {
            return this.currentLanguage;
        }
        return "EN";
    }

    private String loadSavedLanguage() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(LANGUAGE_CONFIG_PATH)) {
            props.load(in);
            String lang = props.getProperty("language");
            if (lang != null && !lang.isEmpty()) {
                return lang;
            }
        }
        catch (Exception e) {
        }
        return null;
    }

    public void loadTranslations() {
        this.currentLanguage = this.loadSavedLanguage();
        File var1 = new File(TRANSLATIONS_PATH);
        File[] var2 = var1.listFiles(EtherTranslator::lambda$loadTranslations$0);
        if (var2 == null) {
            Logger.printLog("Failed to load translations: no files found.");
        } else {
            File[] var3 = var2;
            int var4 = var2.length;
            for (int var5 = 0; var5 < var4; ++var5) {
                File var6 = var3[var5];
                String var7 = var6.getName().replace(".txt", "");
                HashMap<String, String> var8 = new HashMap<String, String>();
                try (BufferedReader var9 = new BufferedReader(new FileReader(var6));){
                    String var10;
                    while ((var10 = var9.readLine()) != null) {
                        String[] var11;
                        if (var10.trim().isEmpty() || !var10.contains("=") || (var11 = var10.split("=", 2)).length < 2) continue;
                        String var12 = var11[0].trim();
                        String var13 = var11[1].trim();
                        if (var13.endsWith(",")) {
                            var13 = var13.substring(0, var13.length() - 1);
                        }
                        var13 = var13.replaceAll("\"", "");
                        var8.put(var12, var13);
                    }
                }
                catch (Exception var16) {
                    Logger.printLog("Failed to load translation file: " + var6.getName());
                    var16.printStackTrace();
                }
                this.translations.put(var7, var8);
            }
        }
    }

    public String getTranslate(String var1) {
        return this.getTranslate(var1, null);
    }

    public String getTranslate(String var1, KahluaTable var2) {
        String var5;
        if (var1 == null) {
            Logger.printLog("The translation key value was not obtained!");
            return "???";
        }
        String var3 = this.getCurrentLanguage();
        Map var4 = (Map)this.translations.get(var3);
        if (var4 == null) {
            Logger.printLog("No translations for language code: " + var3);
            var4 = (Map)this.translations.get("EN");
            if (var4 == null) {
                return var1;
            }
        }
        if ((var5 = (String)var4.get(var1)) == null) {
            Logger.printLog("No translation for key: " + var1 + " for language: " + var3);
            return var1;
        }
        if (var2 != null && !var2.isEmpty()) {
            KahluaTableIterator var8 = var2.iterator();
            while (var8.advance()) {
                String var6 = var8.getKey().toString();
                String var7 = var8.getValue().toString();
                var5 = var5.replace("{" + var6 + "}", var7);
            }
        }
        var5 = var5.replace("<br>", "\n");
        return var5;
    }

    private static boolean lambda$loadTranslations$0(File var0, String var1) {
        return var1.endsWith(".txt");
    }
}
