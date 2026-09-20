/*
 * 无限负重 - 根背包容量重写 (一百一十七, 参考 PienZ 的 RootInventoryCapacity 方案)。
 *
 * 背景: 我方旧实现靠"本地 maxWeight 踩值 + 每 50ms 重发 PlayerDamagePacket (20/s)"
 * 压制服务端重算 —— 高频发包既是网络噪声也是限流/日志面。PienZ 的做法是**零包**:
 * 运行时重写 ItemContainer.getEffectiveCapacity, 仅对本地玩家根背包返回
 * Integer.MAX_VALUE (拖拽上限 = 容量校验的本地读点), 服务端侧另有一次低频同步。
 *
 * 本补丁 (一百一十九 结构调整): 本类只做**安装** (含 ASM); 运行时钩子移到
 * `core/RootCapacityRuntime` (ASM-free) —— 注入体调用的是 Runtime 类。
 * 铁律: 被注入代码引用的类必须 ASM-free, 否则游戏类加载器解析时报
 * NoClassDefFoundError: org/objectweb/asm/tree/AbstractInsnNode (一百一十九 实测教训)。
 *
 * 注入体: aload_0, aload_1 → INVOKESTATIC RootCapacityRuntime.hook → IFEQ continue
 *         → LDC MAX_INT → IRETURN。ShapeGuard 校验 desc + getCapacity/hasTrait 特征;
 * @Injected 幂等标记 (Patch.injectIntoClass 自动打)。
 */
package modcore.core;

import modcore.utils.Logger;
import modcore.utils.Patch;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class RootCapacityPatch {

    private static final String TARGET_CLASS = "zombie/inventory/ItemContainer";
    private static final String TARGET_METHOD = "getEffectiveCapacity";

    private RootCapacityPatch() {
    }

    public static void install() {
        Logger.print("Patching ItemContainer.getEffectiveCapacity with root-capacity override...");
        try {
            Patch.injectIntoClass(TARGET_CLASS, TARGET_METHOD, false,
                    // ShapeGuard (注入前): 原版体 = getCapacity() 起手 + ORGANIZED/DISORGANIZED
                    // 特长分支 (hasTrait)。两处特征缺一即判版本漂移, 拒绝注入。
                    method -> {
                        if (!method.desc.equals("(Lzombie/characters/IsoGameCharacter;)I")) {
                            throw new IllegalStateException("unexpected getEffectiveCapacity desc: " + method.desc);
                        }
                        boolean hasCapacity = false;
                        boolean hasTrait = false;
                        for (int i = 0; i < method.instructions.size(); i++) {
                            if (!(method.instructions.get(i) instanceof MethodInsnNode)) continue;
                            MethodInsnNode insn = (MethodInsnNode) method.instructions.get(i);
                            if (insn.name.equals("getCapacity")) hasCapacity = true;
                            if (insn.name.equals("hasTrait")) hasTrait = true;
                        }
                        if (!hasCapacity || !hasTrait) {
                            throw new IllegalStateException("getEffectiveCapacity shape changed "
                                    + "(capacity=" + hasCapacity + ", trait=" + hasTrait + ")");
                        }
                    },
                    method -> {
                        InsnList hook = new InsnList();
                        LabelNode continueLabel = new LabelNode();
                        hook.add(new VarInsnNode(25, 0));                       // aload_0 (ItemContainer this)
                        hook.add(new VarInsnNode(25, 1));                       // aload_1 (IsoGameCharacter chr)
                        hook.add(new MethodInsnNode(184, "modcore/core/RootCapacityRuntime",
                                "hook", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
                        hook.add(new JumpInsnNode(153, continueLabel));          // IFEQ → 原版路径
                        hook.add(new LdcInsnNode(Integer.MAX_VALUE));            // 重写生效
                        hook.add(new InsnNode(172));                             // IRETURN
                        hook.add(continueLabel);
                        method.instructions.insert(hook);
                        Logger.print("  [OK] Injected root-capacity override into ItemContainer.getEffectiveCapacity()");
                    });
        } catch (Exception e) {
            Logger.print("Warning: root-capacity injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }
}
