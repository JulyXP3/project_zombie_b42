/*
 * 伪·自动驾驶安装期注入器 (GamePatcher 唯一调用, 运行期零引用)。
 * 运行期助手 = BulletNoClipHook (onCalcPhysics / refreshChunks / 战损与冲量钩子)
 * — 二者必须分离: 安装器只解包 EtherHack 前缀条目到游戏目录, ASM 库 (本类依赖)
 * 从不落地, 运行期类一旦引用 ASM 类型即 NoClassDefFoundError (实测教训)。
 *
 * 四处注入 (目标类均在 patchFiles: iso/IsoChunk + vehicles/BaseVehicle +
 * characters/IsoGameCharacter), 注入体统一 "钩子返回哨兵 → 放行原版, 否则替换" 形态:
 * ① IsoChunk.calcPhysics (III[I)V       → onCalcPhysics            (boolean 替换/放行)
 * ② BaseVehicle.calculateDamageWithCharacter
 *    (Lzombie/characters/IsoGameCharacter;)I
 *                                       → onVehicleHitChrDamage    (int, MIN=哨兵)
 * ③ IsoGameCharacter.calculateDamageFromVehicleImpact (F)F
 * ④ IsoGameCharacter.calculateDamageFromVehicleRunOver (F)F
 *                                       → onZombieVehicleDamage    (float, NaN=哨兵)
 * ⑤ BaseVehicle.applyImpulseFromHitPedestrian
 *    (Lzombie/characters/IsoGameCharacter;)V
 *                                       → onHitPedestrianImpulse   (boolean 跳过/放行)
 * 描述符均为 javap 亲验 (calcPhysics 首版误数隐式 this 的教训见研判 §六)。
 */
package EtherHack.drive;

import EtherHack.utils.Logger;
import EtherHack.utils.Patch;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class BulletNoClipPatch {

    private static final String HOOK = "EtherHack/drive/BulletNoClipHook";

    private BulletNoClipPatch() {
    }

    public static void install() {
        Logger.print("Patching autopilot no-clip & combat gates...");
        try {
            installWorldNoClipGate();
            installVehicleSelfDamageGate();
            installZombieDamageBoost();
            installHitImpulseGate();
        } catch (Exception e) {
            Logger.print("Warning: autopilot no-clip injection failed: " + e.getMessage());
            Logger.logException(e);
        }
    }

    /** ① IsoChunk.calcPhysics 头部: 激活期只保留 Floor 形状。 */
    private static void installWorldNoClipGate() {
        final boolean[] injected = {false};
        Patch.injectIntoClass("zombie/iso/IsoChunk", "calcPhysics", false, method -> {
            if (!method.desc.equals("(III[I)V")) {
                return;
            }
            injected[0] = true;
            // aload_0/iload×3/aload 4 → onCalcPhysics(IsoChunk,III,[I)Z → ifeq 原版 / return
            InsnList hook = new InsnList();
            LabelNode continueLabel = new LabelNode();
            hook.add(new VarInsnNode(25, 0));
            hook.add(new VarInsnNode(21, 1));
            hook.add(new VarInsnNode(21, 2));
            hook.add(new VarInsnNode(21, 3));
            hook.add(new VarInsnNode(25, 4));
            hook.add(new MethodInsnNode(184, HOOK, "onCalcPhysics",
                    "(Lzombie/iso/IsoChunk;III[I)Z", false));
            hook.add(new JumpInsnNode(153, continueLabel)); // IFEQ → 原版装配
            hook.add(new InsnNode(177));                    // RETURN → 已替换
            hook.add(continueLabel);
            method.instructions.insert(hook);
            Logger.print("  [OK] IsoChunk.calcPhysics no-clip gate");
        });
        require(injected, "IsoChunk.calcPhysics");
    }

    /** ② 车辆撞击角色自伤 → 激活期 0 (伤害报告不发出)。 */
    private static void installVehicleSelfDamageGate() {
        final boolean[] injected = {false};
        Patch.injectIntoClass("zombie/vehicles/BaseVehicle", "calculateDamageWithCharacter",
                false, method -> {
                    if (!method.desc.equals("(Lzombie/characters/IsoGameCharacter;)I")) {
                        return;
                    }
                    injected[0] = true;
                    // 结果 != MIN_VALUE → ireturn; == MIN_VALUE (哨兵) → pop 后原版
                    InsnList hook = new InsnList();
                    LabelNode continueLabel = new LabelNode();
                    hook.add(new VarInsnNode(25, 0)); // aload_0 this
                    hook.add(new VarInsnNode(25, 1)); // aload_1 chr
                    hook.add(new MethodInsnNode(184, HOOK, "onVehicleHitChrDamage",
                            "(Lzombie/vehicles/BaseVehicle;Lzombie/characters/IsoGameCharacter;)I", false));
                    hook.add(new InsnNode(89));       // DUP
                    hook.add(new LdcInsnNode(Integer.MIN_VALUE));
                    hook.add(new JumpInsnNode(159, continueLabel)); // IF_ICMPEQ → 原版
                    hook.add(new InsnNode(172));                    // IRETURN (替换值)
                    hook.add(continueLabel);
                    hook.add(new InsnNode(87));       // POP (丢弃哨兵)
                    method.instructions.insert(hook);
                    Logger.print("  [OK] BaseVehicle.calculateDamageWithCharacter gate");
                });
        require(injected, "BaseVehicle.calculateDamageWithCharacter");
    }

    /** ③/④ 僵尸被撞伤害 → 激活期 500 (首触即死); 玩家/动物/未激活放行原版。 */
    private static void installZombieDamageBoost() {
        String[] methods = {"calculateDamageFromVehicleImpact", "calculateDamageFromVehicleRunOver"};
        for (String name : methods) {
            final boolean[] injected = {false};
            Patch.injectIntoClass("zombie/characters/IsoGameCharacter", name, false, method -> {
                if (!method.desc.equals("(F)F")) {
                    return;
                }
                injected[0] = true;
                // 结果非 NaN → freturn (替换值); NaN (哨兵) → pop 后原版
                InsnList hook = new InsnList();
                LabelNode returnLabel = new LabelNode();
                hook.add(new VarInsnNode(25, 0)); // aload_0 this
                hook.add(new VarInsnNode(23, 1)); // fload_1 impactSpeed
                hook.add(new MethodInsnNode(184, HOOK, "onZombieVehicleDamage",
                        "(Lzombie/characters/IsoGameCharacter;F)F", false));
                hook.add(new InsnNode(89));       // DUP
                hook.add(new MethodInsnNode(184, "java/lang/Float", "isNaN", "(F)Z", false));
                hook.add(new JumpInsnNode(153, returnLabel)); // IFEQ (非 NaN) → freturn
                hook.add(new InsnNode(87));       // POP (NaN → 走原版)
                method.instructions.insert(hook);
                // 原版自有返回在其后; 追加替换值返回出口
                method.instructions.add(returnLabel);
                method.instructions.add(new InsnNode(174)); // FRETURN
                Logger.print("  [OK] IsoGameCharacter." + name + " gate");
            });
            require(injected, "IsoGameCharacter." + name);
        }
    }

    /** ⑤ 撞击减速冲量 → 激活期跳过 (尸群顶不停车)。 */
    private static void installHitImpulseGate() {
        final boolean[] injected = {false};
        Patch.injectIntoClass("zombie/vehicles/BaseVehicle", "applyImpulseFromHitPedestrian",
                false, method -> {
                    if (!method.desc.equals("(Lzombie/characters/IsoGameCharacter;)V")) {
                        return;
                    }
                    injected[0] = true;
                    InsnList hook = new InsnList();
                    LabelNode continueLabel = new LabelNode();
                    hook.add(new VarInsnNode(25, 0)); // aload_0 this
                    hook.add(new VarInsnNode(25, 1)); // aload_1 chr
                    hook.add(new MethodInsnNode(184, HOOK, "onHitPedestrianImpulse",
                            "(Lzombie/vehicles/BaseVehicle;Lzombie/characters/IsoGameCharacter;)Z", false));
                    hook.add(new JumpInsnNode(153, continueLabel)); // IFEQ → 原版
                    hook.add(new InsnNode(177));                    // RETURN → 已跳过
                    hook.add(continueLabel);
                    method.instructions.insert(hook);
                    Logger.print("  [OK] BaseVehicle.applyImpulseFromHitPedestrian gate");
                });
        require(injected, "BaseVehicle.applyImpulseFromHitPedestrian");
    }

    /** 注入落空显式炸出 (描述符不匹配会静默跳过 — calcPhysics 误数教训)。 */
    private static void require(boolean[] injected, String what) {
        if (!injected[0]) {
            throw new RuntimeException("injection skipped (descriptor mismatch): " + what);
        }
    }
}
