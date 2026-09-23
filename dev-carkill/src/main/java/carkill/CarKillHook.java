package carkill;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.physics.Bullet;
import zombie.input.GameKeyboard;
import zombie.vehicles.BaseVehicle;

/**
 * 运行期钩子。注入体只调用这里, 本类不含 ASM 类型。
 * 开关默认关; 只对玩家驾驶的车、且目标是僵尸生效。
 */
public final class CarKillHook {
    private static final float ZOMBIE_HIT_DAMAGE = 500.0f;
    private static final int KEY_BACKSLASH = 43; // org.lwjglx.input.Keyboard.KEY_BACKSLASH

    private static volatile boolean enabled;

    private CarKillHook() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
        System.out.println("[CarKill] " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /** GameKeyboard.update 每帧一次。只在按下沿切换, 文本输入中不抢键。 */
    public static void onKeyboardUpdate() {
        if (GameKeyboard.isKeyPressed(KEY_BACKSLASH)) {
            toggle();
            GameKeyboard.eatKeyPress(KEY_BACKSLASH);
        }
    }

    /**
     * calculateDamageFromVehicleImpact / RunOver 头部。
     * 秒杀且目标是僵尸 → 500; 其余 → NaN, 注入体放行原版。
     */
    public static float onZombieVehicleDamage(IsoGameCharacter chr, float impactSpeed) {
        // NOTE: 肇事车辆不在此方法参数里 (原版签名只有 impactSpeed), 无法按车门控;
        // 与主分支秒杀同语义: 开关开 + 目标是僵尸即 500。远程车辆的碰撞不在本机算,
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
     * 原生倍率是粘性的, 不能只 return; 必须主动上报无上限。
     * true = 已替换; false = 放行原版。
     */
    public static boolean onVelocityMultiplier(BaseVehicle vehicle) {
        if (!enabled || !isPlayerDriven(vehicle) || vehicle.getController() == null || vehicle.getScript() == null) {
            return false;
        }
        Bullet.setVehicleVelocityMultiplier(vehicle.vehicleId, 100000.0f, 1.0f);
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
}
