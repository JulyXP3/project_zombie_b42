package modcore.core;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.network.GameServer;
import zombie.network.fields.hit.AttackVars;
import zombie.scripting.objects.ItemTag;

/** 普通武器攻击的最终暴击概率；沿用原版的一次掷骰，运行时无 ASM 依赖。 */
public final class CriticalChanceRuntime {
    private CriticalChanceRuntime() {
    }

    public static float selectChance(float original, IsoPlayer player, WeaponType type, IsoGameCharacter target) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main == null || main.CoreAPI == null || !main.CoreAPI.isCritMax) {
                return original;
            }
            if (GameServer.server || player == null || !player.isLocalPlayer() || !(target instanceof IsoZombie)
                    || type == null || type == WeaponType.UNARMED || type == WeaponType.KNIFE
                    || type == WeaponType.THROWING || player.isDoHandToHandAttack() || player.isShoveStompAnim()) {
                return original;
            }
            AttackVars attack = player.getAttackVars();
            if (attack == null || attack.closeKill || attack.doShove || attack.doGrapple
                    || !(player.getPrimaryHandItem() instanceof HandWeapon weapon)
                    || weapon.isBareHands() || weapon.isAlwaysKnockdown()
                    || weapon.hasTag(ItemTag.NO_CRITICALS) || weapon.hasTag(ItemTag.FAKE_WEAPON)) {
                return original;
            }
            return 90.0f;
        } catch (Throwable failure) {
            return original;   // 身份/武器/动作状态不可读时保留原版判定
        }
    }
}
