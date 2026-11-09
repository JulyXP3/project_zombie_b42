/*
 * Decompiled with CFR 0.152.
 * 
 *  Could not load the following classes:
 *  se.krka.kahlua.vm.KahluaTable
 *  se.krka.kahlua.vm.KahluaTableIterator
 */
package modcore.core;

import modcore.utils.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;

public class LuaTranslator {
    private static final String TRANSLATIONS_PATH = "modcore/translations";
    private static final String LANGUAGE_CONFIG_FILENAME = "language.properties";
    private Map translations;
    private String currentLanguage;

    public LuaTranslator() {
        Logger.printLog("Initializing LuaTranslator...");
        this.translations = new HashMap();
    }

    public void setLanguage(String var1) {
        this.currentLanguage = var1;
        Logger.printLog("modcore language set to: " + var1);
        try {
            File languageFile = new File(CoreAPI.configDir(), LANGUAGE_CONFIG_FILENAME);
            Properties props = new Properties();
            props.setProperty("language", var1);
            try (FileOutputStream out = new FileOutputStream(languageFile)) {
                props.store(out, (String)null);
                modcore.utils.ConfigStash.saveSnapshot(CoreAPI.configDir(), true);   // 二百七十三: 落盘即暂存
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
        try (FileInputStream in = new FileInputStream(new File(CoreAPI.configDir(), LANGUAGE_CONFIG_FILENAME))) {
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
        try {
            for (String path : modcore.utils.RuntimeResources.list(TRANSLATIONS_PATH + "/")) {
                if (!path.endsWith(".txt")) continue;
                String language = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
                HashMap<String, String> values = new HashMap<>();
                try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(
                        modcore.utils.RuntimeResources.require(path), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty() || !line.contains("=")) continue;
                        String[] pair = line.split("=", 2);
                        String value = pair[1].trim();
                        if (value.endsWith(",")) value = value.substring(0, value.length() - 1);
                        values.put(pair[0].trim(), value.replace("\"", ""));
                    }
                }
                this.translations.put(language, values);
            }
            if (!this.translations.containsKey("EN")) throw new IOException("English translation resource missing");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load translation resources", e);
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
