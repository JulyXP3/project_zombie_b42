package modcore.core;

import modcore.utils.Patch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import java.util.ArrayList;
import java.util.List;

/** Validates the 42.22.2 movement method, then decorates every original vector return. */
public final class MovementSpeedPatch {
    private static final String TARGET = "zombie/characters/IsoPlayer";
    private static final String METHOD = "getDeferredMovement";
    private static final String DESC = "(Lzombie/iso/Vector2;Z)Lzombie/iso/Vector2;";
    private static final String HOOK = "modcore/core/MovementSpeedRuntime";
    private static final String HOOK_DESC = "(Lzombie/iso/Vector2;Lzombie/characters/IsoPlayer;)Lzombie/iso/Vector2;";
    private static final int[] ORIGINAL_OPS = {
        25,25,21,183,87,178,180,180,180,182,153,25,182,153,25,182,154,25,182,153,
        25,184,182,154,25,184,182,154,25,184,182,154,25,182,199,25,3,182,154,25,
        182,154,25,11,11,182,176,25,182,198,25,18,182,25,176,25,18,182,25,176
    };
    private MovementSpeedPatch() { }

    public static void install() {
        Patch.injectIntoClass(TARGET, METHOD, false, MovementSpeedPatch::requireOriginalShape,
                MovementSpeedPatch::injectReturns, HOOK, "apply");
        // Patch's generic marker check sees one call; this method needs all three returns protected.
        Patch.modifyClass(TARGET, node -> {
            MethodNode target = node.methods.stream()
                    .filter(m -> METHOD.equals(m.name) && DESC.equals(m.desc)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("movement target missing"));
            requireAllReturnHooks(target);
        });
    }

    public static void requireOriginalShape(MethodNode method) {
        if (!METHOD.equals(method.name) || !DESC.equals(method.desc))
            throw new IllegalStateException("movement method descriptor/name changed");
        List<AbstractInsnNode> real = new ArrayList<>();
        for (AbstractInsnNode node : method.instructions) if (node.getOpcode() >= 0) real.add(node);
        if (real.size() != ORIGINAL_OPS.length) throw new IllegalStateException("movement instruction count changed");
        for (int i=0; i<ORIGINAL_OPS.length; i++) if (real.get(i).getOpcode() != ORIGINAL_OPS[i])
            throw new IllegalStateException("movement opcode changed at " + i);
        if (!(real.get(0) instanceof VarInsnNode a) || a.var != 0
                || !(real.get(1) instanceof VarInsnNode b) || b.var != 1
                || !(real.get(2) instanceof VarInsnNode c) || c.var != 2)
            throw new IllegalStateException("movement parameter layout changed");
        int superCalls=0, scaleCalls=0, constants=0, returns=0;
        for (AbstractInsnNode node : real) {
            if (node.getOpcode() == Opcodes.ARETURN) returns++;
            if (node instanceof MethodInsnNode call) {
                if (call.getOpcode()==Opcodes.INVOKESPECIAL
                        && call.owner.equals("zombie/characters/IsoLivingCharacter")
                        && METHOD.equals(call.name) && DESC.equals(call.desc)) superCalls++;
                if (call.owner.equals("zombie/iso/Vector2") && call.name.equals("scale") && call.desc.equals("(F)V")) scaleCalls++;
            }
            if (node instanceof LdcInsnNode ldc && ldc.cst instanceof Float f && (f==2.5f || f==7.5f)) constants++;
        }
        if (superCalls!=1 || scaleCalls!=2 || constants!=2 || returns!=3)
            throw new IllegalStateException("movement source calls/returns changed");
    }

    private static void injectReturns(MethodNode method) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node.getOpcode() != Opcodes.ARETURN) continue;
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "apply", HOOK_DESC, false));
            method.instructions.insertBefore(node, hook);
        }
        requireAllReturnHooks(method);
    }

    private static void requireAllReturnHooks(MethodNode method) {
        int returns=0, calls=0;
        for (AbstractInsnNode node : method.instructions) {
            if (node instanceof MethodInsnNode call && HOOK.equals(call.owner) && "apply".equals(call.name)) calls++;
            if (node.getOpcode() != Opcodes.ARETURN) continue;
            returns++;
            if (!(node.getPrevious() instanceof MethodInsnNode call)
                    || call.getOpcode()!=Opcodes.INVOKESTATIC || !HOOK.equals(call.owner)
                    || !"apply".equals(call.name) || !HOOK_DESC.equals(call.desc)
                    || !(call.getPrevious() instanceof VarInsnNode receiver)
                    || receiver.getOpcode()!=Opcodes.ALOAD || receiver.var!=0)
                throw new IllegalStateException("movement return missing exact runtime hook");
        }
        if (returns!=3 || calls!=3) throw new IllegalStateException("movement return hook count changed");
    }
}
