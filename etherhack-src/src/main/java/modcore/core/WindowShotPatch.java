/*
 * WindowShotPatch (二百一十七, 用户拍板实现方案 A) — 「关窗射击」的门控重定向。
 *
 * 机制 (研判见 temp/交接文档-车内关窗射击研判(2026-10-15).md; 行号基准 = analysis/pz_sync_analysis):
 *   原版车内攻击的唯一门控在 IsoPlayer.updateWhileInVehicle() (源码 :4113-4126):
 *     if (door != null && (window = door.findWindow()) != null && !window.isHittable()) canAttack = true;
 *   VehicleWindow.isHittable() (VehicleWindow.java:67) = 未碎 && 未摇下 && (无 itemType 或有 item),
 *   即「完好关闭的车窗 = hittable = 不能车内攻击」。isHittable() 本体有 6 个调用点 (僵尸砸窗
 *   AttackVehicleState / 车内淋雨 Clothing / 车辆受击部位选择 BaseVehicle:10185 / window.damage
 *   自身守卫 / 本门控), **绝不能改本体** —— 本补丁只做调用点定向 redirect:
 *   把 updateWhileInVehicle() 内那一处
 *     aload <window槽> / INVOKEVIRTUAL VehicleWindow.isHittable()Z / ifne
 *   的 invokevirtual 换成
 *     INVOKESTATIC WindowShotRuntime.overrideHittable(Lzombie/vehicles/VehicleWindow;)Z
 *   栈形态天然兼容 (objectref 变静态参数), 后面的 ifne (即原版 `!`) 不动; 钩子返回 false =
 *   「窗不可命中」→ canAttack = true。其余 5 个调用点与 isHittable 本体零改动。
 *
 * ShapeGuard (一百九十三 口径): desc 匹配 + 元数据过滤 (Label/LineNumber/Frame) 后断言
 *   ① 恰 1 处 INVOKEVIRTUAL zombie/vehicles/VehicleWindow.isHittable ()Z;
 *   ② 其前一条真实指令为 ALOAD (window 引用压栈);
 *   ③ 其后一条真实指令为 IFEQ/IFNE (原版 `!` 的取反跳转)。
 * 零处/多处/邻位不符 = 版本漂移, 拒绝注入 (不盲注)。@Injected 幂等 + ReadBack 回读校验。
 * Runtime 类 ASM-free (纪律 8); 离线实检 temp/WindowShotGuardCheck (对真实 jar 跑 ASM,
 * 落盘 + 首次执行 JVM 校验 + 负向对照)。
 *
 * 语义边界: 钩子只在开关开 + 本机玩家手持远程武器 (HandWeapon.isRanged) 时返回 false
 * (限枪械, 不放开车内近战/推搡穿关窗); 其余一切情形回退 window.isHittable() 原版值。
 * 子弹伤窗路径的最终校验以实测定论 (研判 §4 情形 1/2)。
 */
package modcore.core;

import modcore.utils.Logger;
import modcore.utils.Patch;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public final class WindowShotPatch {

    private static final String TARGET_CLASS = "zombie/characters/IsoPlayer";
    private static final String TARGET_METHOD = "updateWhileInVehicle";
    private static final String TARGET_DESC = "()V";
    private static final String GATE_OWNER = "zombie/vehicles/VehicleWindow";
    private static final String GATE_NAME = "isHittable";
    private static final String GATE_CALL_DESC = "()Z";
    private static final String HOOK_OWNER = "modcore/core/WindowShotRuntime";
    private static final String HOOK_NAME = "overrideHittable";
    private static final String HOOK_DESC = "(Lzombie/vehicles/VehicleWindow;)Z";

    private WindowShotPatch() {
    }

    public static void install() {
        Logger.print("Patching IsoPlayer in-vehicle attack gate (window shot)...");
        try {
            Patch.injectIntoClass(TARGET_CLASS, TARGET_METHOD, false,
                    WindowShotPatch::requireGateShape,
                    method -> {
                        for (int i = 0; i < method.instructions.size(); i++) {
                            AbstractInsnNode n = method.instructions.get(i);
                            if (isGateCall(n)) {
                                method.instructions.set(n,
                                        new MethodInsnNode(184, HOOK_OWNER, HOOK_NAME, HOOK_DESC, false));
                                Logger.print("  [OK] Redirected in-vehicle window-hittable gate call");
                                return;
                            }
                        }
                        throw new IllegalStateException("window-hittable gate call not found at injection time");
                    },
                    HOOK_OWNER, HOOK_NAME);
        }
        catch (Exception e) {
            Logger.print("Warning: window-shot injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /**
     * ShapeGuard: 元数据过滤后断言「恰 1 处门控调用 + 前 ALOAD 后 IFEQ/IFNE」。
     * 不对原始 instructions 列表断长度/位置 (一百九十三 教训)。
     */
    private static void requireGateShape(MethodNode method) {
        if (!method.desc.equals(TARGET_DESC)) {
            throw new IllegalStateException("unexpected IsoPlayer.updateWhileInVehicle desc: " + method.desc);
        }
        List<AbstractInsnNode> real = new ArrayList<AbstractInsnNode>();
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode n = method.instructions.get(i);
            if (n instanceof LabelNode || n instanceof LineNumberNode || n instanceof FrameNode) continue;
            real.add(n);
        }
        int gateIdx = -1;
        for (int i = 0; i < real.size(); i++) {
            if (isGateCall(real.get(i))) {
                if (gateIdx != -1) {
                    throw new IllegalStateException("IsoPlayer.updateWhileInVehicle shape changed "
                            + "(multiple window-hittable gate calls)");
                }
                gateIdx = i;
            }
        }
        if (gateIdx == -1) {
            throw new IllegalStateException("IsoPlayer.updateWhileInVehicle shape changed "
                    + "(window-hittable gate call not found)");
        }
        boolean prevOk = gateIdx > 0 && real.get(gateIdx - 1).getOpcode() == 25;   // aload <window>
        AbstractInsnNode next = gateIdx + 1 < real.size() ? real.get(gateIdx + 1) : null;
        boolean nextOk = next instanceof JumpInsnNode
                && (next.getOpcode() == 153 || next.getOpcode() == 154);            // ifeq/ifne (原版 `!`)
        if (!prevOk || !nextOk) {
            throw new IllegalStateException("IsoPlayer.updateWhileInVehicle shape changed "
                    + "(unexpected instructions around the window-hittable gate call)");
        }
    }

    private static boolean isGateCall(AbstractInsnNode n) {
        if (!(n instanceof MethodInsnNode)) {
            return false;
        }
        MethodInsnNode call = (MethodInsnNode) n;
        return call.getOpcode() == 182
                && GATE_OWNER.equals(call.owner)
                && GATE_NAME.equals(call.name)
                && GATE_CALL_DESC.equals(call.desc);
    }
}
