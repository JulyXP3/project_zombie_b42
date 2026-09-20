/*
 * RootCapacityPatch 的运行时被调用方 (一百一十九 结构性修复)。
 *
 * 铁律: **注入进游戏类的字节码所调用的类, 必须 ASM-free**。游戏类加载器解析被引用类
 * 时看不见 org.objectweb.asm — 若该类的方法描述符/签名里带 ASM 类型 (补丁类的
 * Consumer<MethodNode> / ShapeGuard 等), 类加载校验阶段即抛
 * NoClassDefFoundError: org/objectweb/asm/tree/AbstractInsnNode
 * (实测: 一百一十七 把 hook() 留在 RootCapacityPatch 里, 进游戏后
 * ISInventoryPage.update → ItemContainer.getEffectiveCapacity 每帧报错刷屏)。
 * 既有对照: AutoDriveController / ServerAntiCheatBypass / CoreMain 等 29 处注入目标
 * 全为 0 ASM 导入 —— 本类与 EarlyLoginRuntime 补上同款约束。
 */
package modcore.core;

public final class RootCapacityRuntime {

    private RootCapacityRuntime() {
    }

    /** 注入点判定 (高频调用, 保持极简): 开关开 + 本地玩家根背包 → 重写生效。 */
    public static boolean hook(Object container, Object chr) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main == null || main.CoreAPI == null || !main.CoreAPI.isUnlimitedCarry) {
                return false;
            }
            return container != null && container == zombie.characters.IsoPlayer.getInstance().getInventory();
        }
        catch (Throwable t) {
            return false;   // 任何异常都不改变原版行为
        }
    }
}
