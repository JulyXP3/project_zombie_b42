/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.core.Color
 */
package EtherHack.utils;

import EtherHack.utils.ColorUtils;
import java.util.Properties;
import zombie.core.Color;

public class ConfigUtils {
    public static boolean getBooleanFromConfig(Properties var0, String var1, boolean var2) {
        String var3 = var0.getProperty(var1);
        return var3 != null ? Boolean.parseBoolean(var3) : var2;
    }

    public static Color getColorFromConfig(Properties var0, String var1, Color var2) {
        String var3 = var0.getProperty(var1);
        return var3 != null ? ColorUtils.stringToColor(var3) : var2;
    }

    public static int getIntFromConfig(Properties var0, String var1, int var2) {
        String var3 = var0.getProperty(var1);
        if (var3 == null) {
            return var2;
        }
        try {
            return Integer.parseInt(var3.trim());
        }
        catch (NumberFormatException var5) {
            return var2;
        }
    }

    public static float getFloatFromConfig(Properties var0, String var1, float var2) {
        String var3 = var0.getProperty(var1);
        if (var3 == null) {
            return var2;
        }
        try {
            return Float.parseFloat(var3.trim());
        }
        catch (NumberFormatException var5) {
            return var2;
        }
    }
}
