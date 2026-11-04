package modcore.core;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.core.physics.RagdollBodyPart;
import zombie.inventory.types.HandWeapon;

/** 枪击分支的命中部位读取点；不改暴击、受击状态或武器字段，运行时无 ASM 依赖。 */
public final class HeadshotRuntime {
    private HeadshotRuntime() {
    }

    public static int selectTargetBodyPart(int original, HandWeapon weapon, IsoGameCharacter wielder) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main == null || main.CoreAPI == null || !main.CoreAPI.isHeadshotOnly) {
                return original;
            }
            if (weapon == null || !weapon.isRanged()
                    || !(wielder instanceof IsoLivingCharacter living) || living.isDoHandToHandAttack()) {
                return original;
            }
            return RagdollBodyPart.BODYPART_HEAD.ordinal();
        }
        catch (Throwable t) {
            return original;   // 身份/动作状态不可读时回到原版命中部位
        }
    }
}
