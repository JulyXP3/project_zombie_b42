/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.characters.IsoPlayer
 *  zombie.characters.IsoZombie
 *  zombie.core.Core
 *  zombie.iso.IsoCamera
 *  zombie.iso.IsoUtils
 *  zombie.vehicles.BaseVehicle
 */
package EtherHack.utils;

import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.vehicles.BaseVehicle;

public class PlayerUtils {
    public static float getDistanceBetweenPlayerAndZombie(IsoPlayer var0, IsoZombie var1) {
        float var2 = var0.getX() - var1.getX();
        float var3 = var0.getY() - var1.getY();
        return (float)Math.sqrt(var2 * var2 + var3 * var3);
    }

    public static float getDistanceBetweenPlayerAndVehicle(IsoPlayer var0, BaseVehicle var1) {
        float var2 = var0.getX() - var1.getX();
        float var3 = var0.getY() - var1.getY();
        return (float)Math.sqrt(var2 * var2 + var3 * var3);
    }

    public static float getDistanceBetweenPlayers(IsoPlayer var0, IsoPlayer var1) {
        float var2 = var0.getX() - var1.getX();
        float var3 = var0.getY() - var1.getY();
        return (float)Math.sqrt(var2 * var2 + var3 * var3);
    }

    public static float getScreenPositionX(IsoPlayer var0) {
        int var1 = IsoCamera.frameState.playerIndex;
        float var2 = IsoUtils.XToScreen((float)var0.getX(), (float)var0.getY(), (float)var0.getZ(), (int)0);
        float var3 = Core.getInstance().getZoom(var1);
        var2 -= IsoCamera.getOffX();
        return var2 /= var3;
    }

    public static float getScreenPositionY(IsoPlayer var0) {
        int var1 = IsoCamera.frameState.playerIndex;
        float var2 = IsoUtils.YToScreen((float)var0.getX(), (float)var0.getY(), (float)var0.getZ(), (int)0);
        float var3 = Core.getInstance().getZoom(var1);
        var2 -= IsoCamera.getOffY();
        var2 -= (float)(128 / (2 / Core.getTileScale()));
        return var2 /= var3;
    }
}
