/*
 * 自动驾驶唯一补丁点 (研判文档 §八): CarController.updateControls 头部门控分发。
 * 游戏版本升级只重核这一个文件 (updateControls = 纯键位搬运, 结构极简但版本敏感,
 * 与既有 CombatManager 补丁同族维护策略)。
 *
 * 注入体 (3 指令 + 分支):
 *   aload_0 → getfield vehicleObject
 *   → INVOKESTATIC EtherHack/drive/AutoDriveController.onUpdateControls(BaseVehicle)Z
 *   → IFEQ continue / RETURN
 * 激活: 控制器写 clientControls 后 return true → 直接 RETURN (跳过键位读取);
 * 未激活/接管放行: return false → IFEQ 落回原版键位读取 (按键当帧生效, 无死帧)。
 *
 * 头部分支注入与 patchHeadshotOnly 同款形态 (COMPUTE_FRAMES 下已验证可用);
 * Patch.injectIntoClass 自动打 @Injected 标记 (重复安装检测依赖)。
 */
package EtherHack.drive;

import EtherHack.utils.Logger;
import EtherHack.utils.Patch;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class CarControllerPatch {

    private static final String TARGET_CLASS = "zombie/core/physics/CarController";
    private static final String TARGET_METHOD = "updateControls";

    private CarControllerPatch() {
    }

    public static void install() {
        Logger.print("Patching CarController.updateControls with autopilot hook...");
        try {
            Patch.injectIntoClass(TARGET_CLASS, TARGET_METHOD, false, method -> {
                if (!method.desc.equals("()V")) {
                    return;
                }
                InsnList hook = new InsnList();
                LabelNode continueLabel = new LabelNode();
                hook.add(new VarInsnNode(25, 0)); // aload_0 (CarController this)
                hook.add(new FieldInsnNode(180, TARGET_CLASS, "vehicleObject",
                        "Lzombie/vehicles/BaseVehicle;"));
                hook.add(new MethodInsnNode(184, "EtherHack/drive/AutoDriveController",
                        "onUpdateControls", "(Lzombie/vehicles/BaseVehicle;)Z", false));
                hook.add(new JumpInsnNode(153, continueLabel)); // IFEQ → 原版键位路径
                hook.add(new InsnNode(177));                    // RETURN → 已接管
                hook.add(continueLabel);
                method.instructions.insert(hook);
                Logger.print("  [OK] Injected autopilot gate into CarController.updateControls()");
            });
        } catch (Exception e) {
            Logger.print("Warning: CarController autopilot injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }
}
