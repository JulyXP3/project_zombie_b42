/*
 * EnduranceStatPatch (一百三十九) — 无限耐力的"读取点归零"。
 *
 * 起因 (用户实测): 开「无限耐力」时**图标会一瞬间闪出来又消失** —— 逐帧踩值 (`onTickUpdate` 每 tick
 * `stats.set(ENDURANCE, 1.0)`) 与游戏自身的**每帧扣减**在赛跑: 扣减发生 → moodle 当帧读到低值
 * (图标闪出) → 下一 tick 我们才补回 1.0 (图标消失)。这与负重那次 (一百三十三) 是同一类问题,
 * 解法也照抄: **打读取点**, 而不是踩字段。
 *
 * 打点: `zombie/characters/Stats.get(CharacterStat)F` 头部 —— 开关开 + 本机玩家的 Stats + 类型为
 * `CharacterStat.ENDURANCE` 时直接返回 1.0f。
 * 效果: 所有读者 (moodle 档位/图标、冲刺与动作的耐力判定、UI 条) 一律看到"满耐力",
 * 与游戏何时扣减、扣减多少**完全无关** → 图标不再闪、耐力判定不再触发。
 *
 * 保留 `onTickUpdate` 里的每 tick 踩值作**兜底** (与读点重写不冲突: 一个管"读到什么", 一个管"存着什么")。
 *
 * 注入体: aload_0 → aload_1 → INVOKESTATIC EnduranceStatRuntime.override(Obj,Obj)Z → IFEQ L →
 *         FCONST_1 → FRETURN; L: 原版体。Runtime 类 ASM-free (工程纪律第 8 条);
 *         走 一百三十三 的 ReadBack 参数 (hookOwner/hookName) 注入后回读校验。
 */
package modcore.core;

import modcore.utils.Logger;
import modcore.utils.Patch;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class EnduranceStatPatch {

    private static final String TARGET_CLASS = "zombie/characters/Stats";
    private static final String TARGET_METHOD = "get";
    private static final String TARGET_DESC = "(Lzombie/characters/CharacterStat;)F";
    private static final String HOOK_OWNER = "modcore/core/EnduranceStatRuntime";
    private static final String HOOK_NAME = "override";

    private EnduranceStatPatch() {
    }

    public static void install() {
        Logger.print("Patching Stats.get with ENDURANCE read-point override...");
        try {
            Patch.injectIntoClass(TARGET_CLASS, TARGET_METHOD, false,
                    method -> {
                        if (!method.desc.equals(TARGET_DESC)) {
                            throw new IllegalStateException("unexpected Stats.get desc: " + method.desc);
                        }
                    },
                    method -> {
                        InsnList hook = new InsnList();
                        LabelNode cont = new LabelNode();
                        hook.add(new VarInsnNode(25, 0));                        // aload_0 (this Stats)
                        hook.add(new VarInsnNode(25, 1));                        // aload_1 (CharacterStat)
                        hook.add(new MethodInsnNode(184, HOOK_OWNER, HOOK_NAME,
                                "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
                        hook.add(new JumpInsnNode(153, cont));                    // IFEQ → 原版
                        hook.add(new InsnNode(12));                               // FCONST_1
                        hook.add(new InsnNode(174));                              // FRETURN
                        hook.add(cont);
                        method.instructions.insert(hook);
                        Logger.print("  [OK] Injected ENDURANCE read-point override");
                    },
                    HOOK_OWNER, HOOK_NAME);
        } catch (Exception e) {
            Logger.print("Warning: ENDURANCE read-point injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }
}
