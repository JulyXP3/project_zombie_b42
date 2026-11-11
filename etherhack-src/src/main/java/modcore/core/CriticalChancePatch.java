package modcore.core;

import modcore.utils.Patch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/** 在 b42.22 最终暴击掷骰前选取概率，不改原版 RNG 和掷骰后的特殊规则。 */
public final class CriticalChancePatch {
    private static final String TARGET = "zombie/CombatManager";
    private static final String OWNER = "modcore/core/CriticalChanceRuntime";
    private static final String METHOD_DESC = "(Lzombie/characters/IsoPlayer;Lzombie/inventory/types/WeaponType;"
            + "Lzombie/characters/IsoGameCharacter;F)Z";
    private static final String HOOK_DESC = "(FLzombie/characters/IsoPlayer;Lzombie/inventory/types/WeaponType;"
            + "Lzombie/characters/IsoGameCharacter;)F";

    private CriticalChancePatch() {
    }

    public static void install() {
        Patch.injectIntoClass(TARGET, "isCriticalHit", false, CriticalChancePatch::locateRoll, method -> {
            RollPoint point = locateRoll(method);
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.FLOAD, point.chanceSlot()));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 3));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, OWNER, "selectChance", HOOK_DESC, false));
            hook.add(new VarInsnNode(Opcodes.FSTORE, point.chanceSlot()));
            method.instructions.insertBefore(point.bound(), hook);
        }, OWNER, "selectChance");
    }

    private record RollPoint(AbstractInsnNode bound, int chanceSlot) {}

    private static RollPoint locateRoll(MethodNode method) {
        if (!METHOD_DESC.equals(method.desc)) {
            throw new IllegalStateException("unexpected isCriticalHit descriptor: " + method.desc);
        }
        List<AbstractInsnNode> code = new ArrayList<>();
        for (AbstractInsnNode insn : method.instructions) {
            if (insn.getOpcode() >= 0) code.add(insn);
        }
        int roll = -1;
        int base = -1;
        int returns = 0;
        for (int i = 0; i < code.size(); i++) {
            AbstractInsnNode insn = code.get(i);
            if (isCall(insn, Opcodes.INVOKESTATIC, "zombie/core/random/Rand", "Next", "(I)I")) {
                if (roll != -1) throw new IllegalStateException("multiple critical rolls");
                roll = i;
            }
            if (isCall(insn, Opcodes.INVOKEVIRTUAL, "zombie/characters/IsoPlayer", "calculateCritChance",
                    "(Lzombie/characters/IsoGameCharacter;)I")) {
                if (base != -1) throw new IllegalStateException("multiple base critical calculations");
                base = i;
            }
            if (insn.getOpcode() == Opcodes.IRETURN) returns++;
        }
        if (base < 0 || roll < 6 || base + 2 >= roll || roll + 8 >= code.size() || returns != 1
                || code.get(base + 1).getOpcode() != Opcodes.I2F
                || !(code.get(base + 2) instanceof VarInsnNode baseStore) || baseStore.getOpcode() != Opcodes.FSTORE
                || !(code.get(roll - 1) instanceof IntInsnNode bound) || bound.getOpcode() != Opcodes.BIPUSH || bound.operand != 100
                || code.get(roll + 1).getOpcode() != Opcodes.I2F
                || !isVar(code.get(roll + 2), Opcodes.FLOAD, baseStore.var)
                || code.get(roll + 3).getOpcode() != Opcodes.FCMPG
                || !(code.get(roll + 4) instanceof JumpInsnNode compare) || compare.getOpcode() != Opcodes.IFGE
                || code.get(roll + 5).getOpcode() != Opcodes.ICONST_1
                || !(code.get(roll + 6) instanceof JumpInsnNode join) || join.getOpcode() != Opcodes.GOTO
                || code.get(roll + 7).getOpcode() != Opcodes.ICONST_0
                || !(code.get(roll + 8) instanceof VarInsnNode resultStore) || resultStore.getOpcode() != Opcodes.ISTORE
                || nextOpcode(compare.label) != code.get(roll + 7) || nextOpcode(join.label) != resultStore) {
            throw new IllegalStateException("isCriticalHit base/roll/comparison shape changed");
        }
        if (!(code.get(roll - 6) instanceof FieldInsnNode client) || client.getOpcode() != Opcodes.GETSTATIC
                || !client.owner.equals("zombie/network/GameClient") || !client.name.equals("client") || !client.desc.equals("Z")
                || !(code.get(roll - 5) instanceof JumpInsnNode singlePlayer) || singlePlayer.getOpcode() != Opcodes.IFEQ
                || nextOpcode(singlePlayer.label) != bound
                || !isVar(code.get(roll - 4), Opcodes.ALOAD, 1)
                || !isCall(code.get(roll - 3), Opcodes.INVOKEVIRTUAL, "zombie/characters/IsoPlayer", "isLocalPlayer", "()Z")
                || !(code.get(roll - 2) instanceof JumpInsnNode remote) || remote.getOpcode() != Opcodes.IFEQ
                || method.instructions.indexOf(remote.label) <= method.instructions.indexOf(resultStore)) {
            throw new IllegalStateException("isCriticalHit local-player gate changed");
        }
        int forbiddenTags = 0;
        int closeKills = 0;
        int knifeRules = 0;
        for (int i = roll + 9; i < code.size(); i++) {
            AbstractInsnNode insn = code.get(i);
            if (insn instanceof FieldInsnNode field) {
                if (field.getOpcode() == Opcodes.GETSTATIC && field.owner.equals("zombie/scripting/objects/ItemTag")
                        && field.name.equals("NO_CRITICALS")) forbiddenTags++;
                if (field.getOpcode() == Opcodes.GETFIELD && field.owner.equals("zombie/network/fields/hit/AttackVars")
                        && field.name.equals("closeKill") && field.desc.equals("Z")) closeKills++;
                if (field.getOpcode() == Opcodes.GETSTATIC && field.owner.equals("zombie/inventory/types/WeaponType")
                        && field.name.equals("KNIFE")) knifeRules++;
            }
        }
        if (forbiddenTags != 1 || closeKills != 3 || knifeRules != 2
                || !isVar(code.get(code.size() - 2), Opcodes.ILOAD, resultStore.var)
                || code.get(code.size() - 1).getOpcode() != Opcodes.IRETURN) {
            throw new IllegalStateException("isCriticalHit special-rule/return shape changed");
        }
        return new RollPoint(bound, baseStore.var);
    }

    private static boolean isCall(AbstractInsnNode insn, int opcode, String owner, String name, String desc) {
        return insn instanceof MethodInsnNode call && call.getOpcode() == opcode
                && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc);
    }

    private static boolean isVar(AbstractInsnNode insn, int opcode, int slot) {
        return insn instanceof VarInsnNode var && var.getOpcode() == opcode && var.var == slot;
    }

    private static AbstractInsnNode nextOpcode(AbstractInsnNode insn) {
        AbstractInsnNode next = insn;
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }
}
