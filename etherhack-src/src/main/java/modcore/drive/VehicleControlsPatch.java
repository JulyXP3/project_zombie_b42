/*
 * 自动驾驶唯一补丁点 (研判文档 §八; 一百一十二上移): BaseVehicle.updateControls 头部门控。
 * 游戏版本升级只重核这一个文件 (updateControls = 纯门控转发, 结构极简但版本敏感,
 * 与既有 CombatManager 补丁同族维护策略)。
 *
 * 为什么在 BaseVehicle 而不是 CarController (一百一十二 修复):
 * 原注入点在 CarController.updateControls 头部, 但原版 BaseVehicle.updateControls 在
 * 调用它之前有一道门 —— 驾驶员 isBlockMovement() 直接 return (M 大地图/模态 UI 开着时
 * 置位, ISWorldMap.lua setBlockMovement)。地图一开, 我们的钩子整段不被调用,
 * clientControls 冻结在开图前最后一帧 (油门/舵角恒定, 无避障无边界刹停) → 实测
 * "看大地图时乱开"。上移到 BaseVehicle.updateControls 头部 (这道门之前) 后, 开图/
 * 模态 UI 期间照常控制; 未激活 return false 落回原版路径 (含原版自己的门), 原版
 * 行为零改变。瞄准/换弹冻结 (IsoPlayer.update 更上游的 haveControl 门) 不在本补丁
 * 范围内, 见 analysis/自动驾驶/车辆自动驾驶-实测修复方案4。
 *
 * 注入体 (4 指令 + 分支):
 *   aload_0 (BaseVehicle this)
 *   → INVOKESTATIC modcore/drive/AutoDriveController.onUpdateControls(BaseVehicle)Z
 *   → IFEQ continue / RETURN
 * 激活: 控制器写 clientControls 后 return true → 直接 RETURN (跳过原版门与键位读取);
 * 未激活/接管放行: return false → IFEQ 落回原版路径 (按键当帧生效, 无死帧)。
 *
 * 形状校验 (工程纪律 1, Patch H1 ShapeGuard): 目标方法必须仍是"门控转发"形状 —
 * 含 isBlockMovement 门调用与尾部的 CarController.updateControls 转发调用; 游戏版本
 * 变更改了结构 → 抛错不盲注 (自动驾驶失效但不碰游戏字节码)。
 * Patch.injectIntoClass 自动打 @Injected 标记 (重复安装检测依赖)。
 */
package modcore.drive;

import modcore.utils.Logger;
import modcore.utils.Patch;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class VehicleControlsPatch {

    private static final String TARGET_CLASS = "zombie/vehicles/BaseVehicle";
    private static final String TARGET_METHOD = "updateControls";

    private VehicleControlsPatch() {
    }

    public static void install() {
        Logger.print("Patching BaseVehicle.updateControls with autopilot gate...");
        try {
            Patch.injectIntoClass(TARGET_CLASS, TARGET_METHOD, false,
                    // ShapeGuard (注入前): 原版体 = getController 判空 → isOperational →
                    // isBlockMovement 门 → 尾部转发 CarController.updateControls。
                    // 两处特征调用缺一即拒绝注入 (版本漂移保护)。
                    method -> {
                        if (!method.desc.equals("()V")) {
                            throw new IllegalStateException("unexpected updateControls desc: " + method.desc);
                        }
                        boolean hasBlockMovementGate = false;
                        boolean hasControllerForward = false;
                        for (int i = 0; i < method.instructions.size(); i++) {
                            if (!(method.instructions.get(i) instanceof MethodInsnNode)) continue;
                            MethodInsnNode insn = (MethodInsnNode) method.instructions.get(i);
                            if (insn.name.equals("isBlockMovement")) hasBlockMovementGate = true;
                            if (insn.name.equals("updateControls") && insn.owner.contains("CarController")) {
                                hasControllerForward = true;
                            }
                        }
                        if (!hasBlockMovementGate || !hasControllerForward) {
                            throw new IllegalStateException("updateControls shape changed "
                                    + "(blockMovementGate=" + hasBlockMovementGate
                                    + ", controllerForward=" + hasControllerForward + ")");
                        }
                    },
                    method -> {
                        InsnList hook = new InsnList();
                        LabelNode continueLabel = new LabelNode();
                        hook.add(new VarInsnNode(25, 0)); // aload_0 (BaseVehicle this)
                        hook.add(new MethodInsnNode(184, "modcore/drive/AutoDriveController",
                                "onUpdateControls", "(Lzombie/vehicles/BaseVehicle;)Z", false));
                        hook.add(new JumpInsnNode(153, continueLabel)); // IFEQ → 原版路径
                        hook.add(new InsnNode(177));                    // RETURN → 已接管
                        hook.add(continueLabel);
                        method.instructions.insert(hook);
                        Logger.print("  [OK] Injected autopilot gate into BaseVehicle.updateControls()");
                    });
        } catch (Exception e) {
            Logger.print("Warning: VehicleControls autopilot injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }
}
