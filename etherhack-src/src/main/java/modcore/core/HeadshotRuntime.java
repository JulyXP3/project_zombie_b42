package modcore.core;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.physics.RagdollBodyPart;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;

/** 枪击部位与概率的计算点；不改暴击、伤害或武器字段，运行时无 ASM 依赖。 */
public final class HeadshotRuntime {
    private HeadshotRuntime() {
    }

    public static float selectHitChance(float original, HandWeapon weapon,
                                        IsoGameCharacter wielder, IsoMovingObject target) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main == null || main.CoreAPI == null) {
                return original;
            }
            // 保留原「百发百中」范围；只爆头新增的概率保障仅对本人射击僵尸生效。
            if (main.CoreAPI.isAlwaysHit) {
                return 100.0f;
            }
            if (main.CoreAPI.isHeadshotOnly && weapon != null && weapon.isRanged()
                    && wielder instanceof IsoPlayer player && player.isLocalPlayer()
                    && !player.isDoHandToHandAttack() && target instanceof IsoZombie) {
                return 100.0f;
            }
            return original;
        }
        catch (Throwable t) {
            return original;   // 身份/动作状态不可读时保留原版概率
        }
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
