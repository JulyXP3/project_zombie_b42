/*
 * NoJamPatch (二百一十六, 用户拍板"做读取点补丁") — 「无卡壳」的读取点重写。
 *
 * 旧实现 (CoreAPI.updateLocalPlayerFeatures 的 OnRenderTick 每帧块, :899-914):
 * 主手是枪时每帧 setJamGunChance(0) + setJammed(false), 换枪/关闭时还原记下的原值。
 * 功能本身成立, 但有一个竞态窗口与每帧足迹:
 *   · **竞态 (本轮根因)**: 服务端也会跑同一个 onShoot (命中包路径
 *     network/fields/hit/Player.java:170 在服务端触发 OnWeaponSwingHitPoint), 用**服务端
 *     副本**的 jamGunChance 掷卡壳 (该字段不在任何同步载荷里, 客户端踩值传不上去),
 *     掷中后服务端经 syncHandWeaponFields (LuaManager:9687, 仅服务端能发) 把 isJammed
 *     广播下来 —— 客户端收到 setJammed(true) 后要等下一帧踩值才清, 撞在 ≤1 帧窗口里的
 *     开枪会被 ISReloadWeaponAction.canShoot:413 的 `weapon:isJammed()` 门吞掉。
 *   · **每帧成本**: 每帧一次 getStringItemType() (脚本表查找) + 两个字段写 + 记账分支。
 *
 * 修法 (项目原则"踩值被覆盖一律改读取点", 与 MaxWeight/耐力同构): 在
 * `zombie/inventory/types/HandWeapon` 两处**读取点**头部注入 —— 开关开且物品是本机玩家
 * 主/副手时, getJamGunChance() 直接返回 0f、isJammed() 直接返回 false。字段本身零改动,
 * 一切"卡壳"语义从读点收敛, 与字段被谁写入完全无关。
 *
 * 覆盖面 (2026-10-14 全量核实):
 *   · checkJam (唯一的掷骰函数) 全 jar 调用者只有两处 Lua, 且都以 `getJamGunChance() > 0`
 *     为前置门 — ISReloadWeaponAction.onShoot:518-519 (开枪喂弹, 客户端与服务端各跑一次)
 *     与 ISRackFirearm.rackBullet:65-66 (拉栓) → 读点返回 0 后两处掷骰全短路;
 *     checkJam 自身的 `chance == 0` 早退 (HandWeapon:2023) 落不到。
 *   · isJammed 消费点 = canShoot (ISReloadWeaponAction:413) / IsoPlayer:6731,6777 开枪门 /
 *     tooltip "卡壳"行 (HandWeapon:390) / 解卡菜单 (ISInventoryPaneContextMenu:1902) /
 *     拉栓逻辑 (ISRackFirearm:49,57,72) / 径向菜单 — 读点一次收敛, **含服务端广播回来的
 *     jammed** (客户端 processClient 写字段照旧, 只是再也没人读到 true)。
 *
 * 注入体 (与 MaxWeightPatch 同构):
 *   aload_0 → INVOKESTATIC NoJamRuntime.overrideChance/overrideJammed → IFEQ L →
 *   fconst_0/FRETURN (11/174) 或 iconst_0/IRETURN (3/172); L: 原版体。Runtime 类 ASM-free。
 * 安全: ShapeGuard (元数据过滤后断言恰为 `aload_0 / getfield / return` 三指令 —— 一百九十三
 * 定稿口径, 不对原始列表做长度/位置断言) + @Injected 幂等 + ReadBack 回读 + 关闭时
 * 读点返回原值 (零行为改变)。目标形状离线实检 (temp/NoJamGuardCheck, 对真实 jar 跑 ASM)。
 */
package modcore.core;

import modcore.utils.Logger;
import modcore.utils.Patch;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

public final class NoJamPatch {

    private static final String TARGET_CLASS = "zombie/inventory/types/HandWeapon";
    private static final String HOOK_OWNER = "modcore/core/NoJamRuntime";
    private static final String HOOK_DESC = "(Lzombie/inventory/types/HandWeapon;)Z";

    private NoJamPatch() {
    }

    public static void install() {
        Logger.print("Patching HandWeapon jam read-points (no-jam)...");
        installChance();
        installJammed();
    }

    /** getJamGunChance()F 头部: 命中时返回 0f (原版两处 `getJamGunChance() > 0` 掷骰门直接短路)。 */
    private static void installChance() {
        try {
            Patch.injectIntoClass(TARGET_CLASS, "getJamGunChance", false,
                    method -> requireGetterShape(method, "()F", "jamGunChance", "F", 174),   // FRETURN
                    method -> {
                        InsnList hook = new InsnList();
                        LabelNode cont = new LabelNode();
                        hook.add(new VarInsnNode(25, 0));                      // aload_0 (this)
                        hook.add(new MethodInsnNode(184, HOOK_OWNER, "overrideChance", HOOK_DESC, false));
                        hook.add(new JumpInsnNode(153, cont));                 // IFEQ → 原版
                        hook.add(new InsnNode(11));                            // fconst_0
                        hook.add(new InsnNode(174));                           // freturn
                        hook.add(cont);
                        method.instructions.insert(hook);
                        Logger.print("  [OK] Injected read-point jamGunChance override");
                    },
                    HOOK_OWNER, "overrideChance");
        }
        catch (Exception e) {
            Logger.print("Warning: jamGunChance read-point injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /** isJammed()Z 头部: 命中时返回 false (开枪门/tooltip/解卡菜单/拉栓逻辑全覆盖)。 */
    private static void installJammed() {
        try {
            Patch.injectIntoClass(TARGET_CLASS, "isJammed", false,
                    method -> requireGetterShape(method, "()Z", "isJammed", "Z", 172),        // IRETURN
                    method -> {
                        InsnList hook = new InsnList();
                        LabelNode cont = new LabelNode();
                        hook.add(new VarInsnNode(25, 0));                      // aload_0 (this)
                        hook.add(new MethodInsnNode(184, HOOK_OWNER, "overrideJammed", HOOK_DESC, false));
                        hook.add(new JumpInsnNode(153, cont));                 // IFEQ → 原版
                        hook.add(new InsnNode(3));                             // iconst_0
                        hook.add(new InsnNode(172));                           // ireturn
                        hook.add(cont);
                        method.instructions.insert(hook);
                        Logger.print("  [OK] Injected read-point isJammed override");
                    },
                    HOOK_OWNER, "overrideJammed");
        }
        catch (Exception e) {
            Logger.print("Warning: isJammed read-point injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /**
     * ShapeGuard (一百九十三 定稿口径): 剔除 Label/LineNumber/Frame 元数据后断言恰为
     * `aload_0 / getfield <field>:<desc> / <return>` 三条规范形; 不符抛错拒注 (不盲注)。
     * (真实 jar 实测: getJamGunChance 原始列表含元数据节点, 绝不可对原始列表断言长度/位置)
     */
    private static void requireGetterShape(MethodNode method, String expectDesc, String fieldName,
                                           String fieldDesc, int returnOpcode) {
        if (!method.desc.equals(expectDesc)) {
            throw new IllegalStateException("unexpected " + fieldName + " desc: " + method.desc);
        }
        List<AbstractInsnNode> real = new ArrayList<AbstractInsnNode>();
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode n = method.instructions.get(i);
            if (n instanceof LabelNode || n instanceof LineNumberNode || n instanceof FrameNode) continue;
            real.add(n);
        }
        boolean shapeOk = real.size() == 3
                && real.get(0) instanceof VarInsnNode
                && ((VarInsnNode) real.get(0)).var == 0          // aload_0
                && real.get(0).getOpcode() == 25
                && real.get(1) instanceof FieldInsnNode
                && real.get(1).getOpcode() == 180                // GETFIELD
                && fieldName.equals(((FieldInsnNode) real.get(1)).name)
                && fieldDesc.equals(((FieldInsnNode) real.get(1)).desc)
                && real.get(2) instanceof InsnNode
                && real.get(2).getOpcode() == returnOpcode;
        if (!shapeOk) {
            throw new IllegalStateException(fieldName + " shape changed (not `return this." + fieldName + ";`)");
        }
    }
}
