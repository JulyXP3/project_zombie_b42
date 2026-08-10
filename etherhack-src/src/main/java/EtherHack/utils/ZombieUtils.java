/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.characters.IsoZombie
 *  zombie.core.Core
 *  zombie.iso.IsoCamera
 *  zombie.iso.IsoUtils
 */
package EtherHack.utils;

import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;

public class ZombieUtils {
    public static float getScreenPositionX(IsoZombie var0) {
        int var1 = IsoCamera.frameState.playerIndex;
        float var2 = IsoUtils.XToScreen((float)var0.getX(), (float)var0.getY(), (float)var0.getZ(), (int)0);
        float var3 = Core.getInstance().getZoom(var1);
        var2 -= IsoCamera.getOffX();
        return var2 /= var3;
    }

    public static float getScreenPositionY(IsoZombie var0) {
        int var1 = IsoCamera.frameState.playerIndex;
        float var2 = IsoUtils.YToScreen((float)var0.getX(), (float)var0.getY(), (float)var0.getZ(), (int)0);
        float var3 = Core.getInstance().getZoom(var1);
        var2 -= IsoCamera.getOffY();
        var2 -= (float)(128 / (2 / Core.getTileScale()));
        return var2 /= var3;
    }
}
