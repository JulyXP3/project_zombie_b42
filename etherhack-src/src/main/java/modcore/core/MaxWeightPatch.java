/*
 * MaxWeightPatch (一百九十一 重建) — 无限负重的"读取点重写"。
 *
 * 症状 (用户实测): 多人下「无限负重」时不时被服务端"打回原形" —— 容量数字闪回原版、
 * 放置判定瞬间失效。根因: maxWeight 是多来源写入的缓存字段, 旧实现 (OnRenderTick 每帧
 * `setMaxWeight(10000)` 踩值) 在与两个覆盖源赛跑:
 *   ① 服务端 PlayerDamagePacket (4a0e9546ec 起纯下行) 周期推回, 客户端 parse 无条件
 *      setMaxWeight(vanilla 值) (PlayerDamagePacket.java:46);
 *   ② vanilla BodyDamage 自重算 setMaxWeight(base×weightMod−减益)×maxWeightDelta
 *      (BodyDamage.java:1779-1785, 纯客户端行为, 与服务端无关)。
 * 覆盖发生在踩值之后、UI 读取之前的窗口即"回退" (服务端 ~2s 一推 → "时不时")。
 *
 * 修法: 在 `IsoGameCharacter.getMaxWeight()I` **读取点**注入 (项目原则"踩值被覆盖一律
 * 改读取点"; 与 EnduranceStatPatch/Stats.get 同构) — 无限负重开启且对象是本地玩家时
 * 直接返回 10000, 所有读者 (容量显示/放置判定/速度/代谢/坠落比值) 与字段被谁写入无关。
 * 关闭时零行为改变; 只影响本地玩家; ShapeGuard + @Injected 幂等 + ReadBack 回读校验。
 *
 * 沿革: 一百二十一 首建 (掉血误判期) → 一百二十二 随误判回退删除 (掉血案与 carry 无关,
 * 已结案冻结) → 一百九十一 因"服务端推回竞态"新症状重建 (本地踩值随之退役)。
 *
 * 注入体: aload_0 → INVOKESTATIC MaxWeightRuntime.override → IFEQ L → LDC 10000 →
 * IRETURN; L: 原版体。(Runtime 类 ASM-free — 工程纪律第 8 条。)
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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

public final class MaxWeightPatch {

    private static final String TARGET_CLASS = "zombie/characters/IsoGameCharacter";
    private static final String TARGET_METHOD = "getMaxWeight";
    private static final String TARGET_DESC = "()I";
    private static final int OVERRIDE_VALUE = 10000;
    private static final String HOOK_OWNER = "modcore/core/MaxWeightRuntime";
    private static final String HOOK_NAME = "override";

    private MaxWeightPatch() {
    }

    public static void install() {
        Logger.print("Patching IsoGameCharacter.getMaxWeight with read-point override...");
        try {
            Patch.injectIntoClass(TARGET_CLASS, TARGET_METHOD, false,
                    // ShapeGuard (注入前, 一百九十三 定稿): 原版体 = `return this.maxWeight;`
                    // 真实指令恰 3 条 (aload_0 / getfield maxWeight:I / ireturn), 但 ASM 的
                    // instructions 列表还含元数据节点 —— 本方法实测 6 节点 (首尾 LabelNode +
                    // LineNumberNode line=3856 + 3 条指令), **不能对原始列表做位置/长度断言**
                    // (一百九十一 首版断言 size==4 逐位、复核首版断言 size==3, 双双被元数据
                    // 节点否决; 离线实检 = temp/MaxWeightGuardCheck 对真实 jar 跑 ASM)。
                    // 正确做法: 剔除 Label/LineNumber/Frame 后断言恰为 3 条规范形。
                    method -> {
                        if (!method.desc.equals(TARGET_DESC)) {
                            throw new IllegalStateException("unexpected getMaxWeight desc: " + method.desc);
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
                                && "maxWeight".equals(((FieldInsnNode) real.get(1)).name)
                                && "I".equals(((FieldInsnNode) real.get(1)).desc)
                                && real.get(2) instanceof InsnNode
                                && real.get(2).getOpcode() == 172;               // IRETURN
                        if (!shapeOk) {
                            throw new IllegalStateException("getMaxWeight shape changed (not `return this.maxWeight;`)");
                        }
                    },
                    method -> {
                        InsnList hook = new InsnList();
                        LabelNode cont = new LabelNode();
                        hook.add(new VarInsnNode(25, 0));                        // aload_0 (this)
                        hook.add(new MethodInsnNode(184, HOOK_OWNER, HOOK_NAME,
                                "(Ljava/lang/Object;)Z", false));
                        hook.add(new JumpInsnNode(153, cont));                    // IFEQ → 原版
                        hook.add(new LdcInsnNode(OVERRIDE_VALUE));                // 重写值
                        hook.add(new InsnNode(172));                              // IRETURN
                        hook.add(cont);
                        method.instructions.insert(hook);
                        Logger.print("  [OK] Injected read-point maxWeight override");
                    },
                    HOOK_OWNER, HOOK_NAME);
        } catch (Exception e) {
            Logger.print("Warning: maxWeight read-point injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }
}
