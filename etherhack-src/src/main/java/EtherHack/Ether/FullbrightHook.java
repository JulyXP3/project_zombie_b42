package EtherHack.Ether;

import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;

/**
 * Fullbright (真-夜视) 注入助手 — 被 GamePatcher.patchFullbright 注入的
 * IsoGridSquare 字节码以**分支-free**方式调用。
 *
 * 为什么需要本类: 注入内联的 "if (isFullbright) return -1" 需要引入新的
 * JumpInsnNode+LabelNode 分支目标, 而 SafeClassWriter(2)=COMPUTE_FRAMES 在
 * IsoGridSquare 这种巨类上重算帧时 Frame.merge 会对无关类型合并
 * (如 org/joml/Vector3f <> zombie/vehicles/BaseVehicle) 抛
 * "Index -1 out of bounds" → 整个 IsoGridSquare.class 写盘失败 (历史遗留:
 * 该类因此从未成功落盘, 组①②从未生效)。分支-free 注入 (无条件调用本类 +
 * 位运算合并结果) 不产生任何新帧合并点, ASM 写出必然成功。
 */
public final class FullbrightHook {

    private FullbrightHook() {
    }

    /**
     * getVertLight 注入用: 返回覆盖值。
     * 真-夜视开 → -1 (0xFFFFFFFF = 纯白, 与原版日光满值一致);
     * 关 → 0, 注入侧用 IOR 合并: 0 | 原值 = 原值, 行为不变。
     */
    public static int vertLightOverride() {
        if (EtherMain.getInstance() == null) {
            return 0;
        }
        if (EtherMain.getInstance().etherAPI == null) {
            return 0;
        }
        return EtherMain.getInstance().etherAPI.isFullbright ? -1 : 0;
    }

    /**
     * cacheLightInfo 注入用: 真-夜视开 → 把该格缓存光照白化
     * (renderFloorInternal/FBORenderCell 用这份缓存调地板/物件颜色)。
     * 关 → 什么都不做。无条件调用, 条件在方法内部。
     */
    public static void cacheLightInfoHook(IsoGridSquare square) {
        if (EtherMain.getInstance() == null) {
            return;
        }
        if (EtherMain.getInstance().etherAPI == null) {
            return;
        }
        if (!EtherMain.getInstance().etherAPI.isFullbright) {
            return;
        }
        ColorInfo lightInfo = square.getLightInfo(IsoCamera.frameState.playerIndex);
        if (lightInfo == null) {
            return;
        }
        lightInfo.r = 1.0f;
        lightInfo.g = 1.0f;
        lightInfo.b = 1.0f;
        lightInfo.a = 1.0f;
    }
}
