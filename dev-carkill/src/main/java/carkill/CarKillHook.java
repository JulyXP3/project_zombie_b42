package carkill;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.physics.Bullet;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoObject;
import zombie.vehicles.BaseVehicle;

/**
 * 运行期钩子。注入体只调用这里, 本类不含 ASM 类型。
 * 开关默认关; 只对玩家驾驶的车、且目标是僵尸生效。
 */
public final class CarKillHook {
    private static final float ZOMBIE_HIT_DAMAGE = 99.0f; // 僵尸血 1.8-3.8 必死; 低于 AntiCheatHitDamage 的 >100 阈值, 留 1 点余量
    private static final int KEY_BACKSLASH = 43; // org.lwjglx.input.Keyboard.KEY_BACKSLASH
    private static final int KEY_EQUALS = 13;    // org.lwjglx.input.Keyboard.KEY_EQUALS ("=")

    private static volatile boolean enabled = false;
    /** 50% 不扣弹：默认关闭，按 = 切换。 */
    private static volatile boolean ammoSaveEnabled = false;

    private CarKillHook() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
        System.out.println("[CarKill] " + (enabled ? "ENABLED" : "DISABLED"));
    }

    public static boolean isAmmoSaveEnabled() {
        return ammoSaveEnabled;
    }

    /** 50% 不扣弹开关；默认关闭。 */
    public static void toggleAmmoSave() {
        ammoSaveEnabled = !ammoSaveEnabled;
        System.out.println("[CarKill] AmmoSave " + (ammoSaveEnabled ? "ENABLED" : "DISABLED"));
    }

    /** GameKeyboard.update 每帧一次。只在按下沿切换, 文本输入中不抢键。 */
    public static void onKeyboardUpdate() {
        if (GameKeyboard.isKeyPressed(KEY_BACKSLASH)) {
            toggle();
            GameKeyboard.eatKeyPress(KEY_BACKSLASH);
        }
        if (GameKeyboard.isKeyPressed(KEY_EQUALS)) {
            toggleAmmoSave();
            GameKeyboard.eatKeyPress(KEY_EQUALS);
        }
    }

    /**
     * calculateDamageFromVehicleImpact / RunOver 头部。
     * 秒杀且目标是僵尸 → 100; 其余 → NaN, 注入体放行原版。
     */
    public static float onZombieVehicleDamage(IsoGameCharacter chr, float impactSpeed) {
        // NOTE: 肇事车辆不在此方法参数里 (原版签名只有 impactSpeed), 无法按车门控;
        // 与主分支秒杀同语义: 开关开 + 目标是僵尸即 100。远程车辆的碰撞不在本机算,
        // 实际只影响自己驾驶的车; 步行时没有车辆撞击会进这个方法。
        if (enabled && chr instanceof IsoZombie) {
            return ZOMBIE_HIT_DAMAGE;
        }
        return Float.NaN;
    }

    /**
     * calculateDamageWithCharacter 头部。
     * 秒杀且目标是僵尸 → 0 (自伤报告不发); 其余 → MIN_VALUE, 注入体放行原版。
     */
    public static int onVehicleHitChrDamage(BaseVehicle vehicle, IsoGameCharacter chr) {
        if (enabled && isPlayerDriven(vehicle) && chr instanceof IsoZombie) {
            return 0;
        }
        return Integer.MIN_VALUE;
    }

    /** applyImpulseFromHitPedestrian 头部。true = 跳过冲量。 */
    public static boolean onHitPedestrianImpulse(BaseVehicle vehicle, IsoGameCharacter chr) {
        return enabled && isPlayerDriven(vehicle) && chr instanceof IsoZombie;
    }

    /**
     * updateVelocityMultiplier 头部。
     * 原生倍率是粘性的, 不能只 return; 必须主动上报 65 上限 (距 vanilla SpeedLimit 70 留 5 格/秒余量)。
     * true = 已替换; false = 放行原版。
     */
    public static boolean onVelocityMultiplier(BaseVehicle vehicle) {
        if (!enabled || !isPlayerDriven(vehicle) || vehicle.getController() == null || vehicle.getScript() == null) {
            return false;
        }
        Bullet.setVehicleVelocityMultiplier(vehicle.vehicleId, 65.0f, 1.0f);
        return true;
    }

    private static boolean isPlayerDriven(BaseVehicle vehicle) {
        if (vehicle == null) {
            return false;
        }
        IsoGameCharacter driver = vehicle.getDriver();
        return driver instanceof zombie.characters.IsoPlayer
                && ((zombie.characters.IsoPlayer) driver).isLocalPlayer();
    }

    /**
     * InventoryItem.setCurrentAmmoCount 头部。
     * 50% 概率跳过扣减 (弹药不消耗); 命中时调 syncItemFields 上行 SyncItemFields 让
     * 服务端认可, 否则服务端弹药会耗尽并触发 AntiCheatHitWeapon "not enough ammo" 踢人。
     * 默认关闭; 按 = 切换。
     */
    public static boolean onAmmoConsume(InventoryItem item, int newAmmo, int oldAmmo) {
        if (!ammoSaveEnabled) {
            return false;
        }
        if (newAmmo >= oldAmmo) {
            return false;
        }
        if (!(item instanceof HandWeapon) || !((HandWeapon)item).isAimedFirearm()) {
            return false;
        }
        if (!isLocalPlayerItem(item)) {
            return false;
        }
        if (Math.random() >= 0.5) {
            return false;
        }
        item.syncItemFields();
        return true;
    }

    /** 物品最外层容器属于本机玩家 (players[] 只含本机玩家, 房主/单机也安全)。 */
    private static boolean isLocalPlayerItem(InventoryItem item) {
        ItemContainer outer = item.getOutermostContainer();
        if (outer == null) {
            return false;
        }
        IsoObject parent = outer.getParent();
        if (!(parent instanceof IsoPlayer)) {
            return false;
        }
        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            if (IsoPlayer.players[pn] == parent) {
                return true;
            }
        }
        return false;
    }
}
