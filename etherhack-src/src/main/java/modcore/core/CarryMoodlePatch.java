/*
 * CarryMoodlePatch (一百三十三) — 无限负重的"HEAVY_LOAD 读取点归零"。
 *
 * 借鉴 = PienZ 源码 (`RootInventoryCapacity.java:36-39,115-149`): 他们除了改容量, 还改写了
 * `MoodlesUI.update` 里 4 处 `getMoodleLevel` → `visibleMoodleLevel`, 把 HEAVY_LOAD 图标恒置 0。
 * 我们做得更彻底一档: 直接打**读取点本体** `Moodles.getMoodleLevel(MoodleType)` ——
 * 开关开且是本地玩家的 Moodles 且类型 == HEAVY_LOAD 时返回 0。
 *
 * 为什么这样最好 (与本项目 一百二十二 的掉血排查同源):
 *   `HEAVY_LOAD` 档位是 B42 背负过重扣血的**唯一闸门** (`BodyDamage.Update:1970` 档≥3 才扣),
 *   同时驱动减速/耐力倍率/翻越摔落率等。把读取点归零 = 图标消失 + 扣血分支永不成立 +
 *   所有连带减速一并消失 —— 一处补丁覆盖"数值一致性"的全部面, 与
 *   `CarryWeightPatch` (分子清零) 形成**双重保险** (任一环节失效都不会掉血)。
 *
 * 门控 (外科式): 只对"本地玩家的 Moodles 实例"生效 —— 他人/僵尸的档位不受影响。
 *   (身份取不到时返回 false = 不干预, 失败开放, 无副作用。)
 *
 * 注入体: aload_0 → aload_1 → INVOKESTATIC CarryMoodleRuntime.zero(Obj,Obj)Z → IFEQ L →
 *         ICONST_0 → IRETURN; L: 原版体。Runtime 类 ASM-free (工程纪律第 8 条)。
 * 本补丁走 一百三十三 新版 `Patch.injectIntoClass(...)` 的 **ReadBack (hookOwner/hookName)**
 * 参数 → 注入后回读确认钩子确实进了字节码 (光"注入器跑过"不算数)。
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

public final class CarryMoodlePatch {

    private static final String TARGET_CLASS = "zombie/characters/Moodles/Moodles";
    private static final String TARGET_METHOD = "getMoodleLevel";
    private static final String TARGET_DESC = "(Lzombie/scripting/objects/MoodleType;)I";
    private static final String HOOK_OWNER = "modcore/core/CarryMoodleRuntime";
    private static final String HOOK_NAME = "zero";

    private CarryMoodlePatch() {
    }

    public static void install() {
        Logger.print("Patching Moodles.getMoodleLevel with HEAVY_LOAD read-point override...");
        try {
            Patch.injectIntoClass(TARGET_CLASS, TARGET_METHOD, false,
                    // ShapeGuard: 原版体 = `return this.moodles.get(moodleType).getLevel();` (desc + List.get 调用)
                    method -> {
                        if (!method.desc.equals(TARGET_DESC)) {
                            throw new IllegalStateException("unexpected getMoodleLevel desc: " + method.desc);
                        }
                    },
                    method -> {
                        InsnList hook = new InsnList();
                        LabelNode cont = new LabelNode();
                        hook.add(new VarInsnNode(25, 0));                        // aload_0 (this Moodles)
                        hook.add(new VarInsnNode(25, 1));                        // aload_1 (MoodleType)
                        hook.add(new MethodInsnNode(184, HOOK_OWNER, HOOK_NAME,
                                "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
                        hook.add(new JumpInsnNode(153, cont));                    // IFEQ → 原版
                        hook.add(new InsnNode(3));                                // ICONST_0
                        hook.add(new InsnNode(172));                              // IRETURN
                        hook.add(cont);
                        method.instructions.insert(hook);
                        Logger.print("  [OK] Injected HEAVY_LOAD read-point override");
                    },
                    HOOK_OWNER, HOOK_NAME);                                       // ReadBack (一百三十三)
        } catch (Exception e) {
            Logger.print("Warning: HEAVY_LOAD read-point injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }
}
