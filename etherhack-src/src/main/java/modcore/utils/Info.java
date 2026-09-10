/*
 * Decompiled with CFR 0.152.
 */
package modcore.utils;

import java.io.IOException;
import java.util.Properties;

public class Info {
    private static final String APP_VERSION;
    public static final String APP_GUI_TITLE;
    public static final String APP_CREDITS_TITLE;
    public static final String APP_WINDOW_TITLE_SUFFIX;
    public static final String APP_NAME = "ModCore";
    public static final String APP_AUTHOR = "Quzile";
    public static final String APP_TAG = "[ModCore]: ";
    public static final String APP_CREDITS_AUTHOR = "Author: Quzile";

    static {
        Properties var0 = new Properties();
        try {
            var0.load(Info.class.getClassLoader().getResourceAsStream("modcore/modcore.properties"));
            APP_VERSION = var0.getProperty("version").replace("'", "");
        }
        catch (IOException var1) {
            throw new ExceptionInInitializerError("Unable to load version from modcore.properties");
        }
        APP_GUI_TITLE = "ModCore (" + APP_VERSION + ")";
        APP_CREDITS_TITLE = "Author: Quzile Updated By: july  (" + APP_VERSION + ")";
        APP_WINDOW_TITLE_SUFFIX = " by ModCore  (" + APP_VERSION + ")";
    }
}
