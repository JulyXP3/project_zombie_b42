package modcore.agent;

import modcore.GamePatcher;
import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Anchors for the two combat patches that moved in b42.22. These run against the real game
 * bytecode so a shape change in a future preview fails here instead of silently disabling a
 * feature.
 */
class CombatPatchAnchorTest {
    @AfterEach
    void reset() {
        Patch.resetSession();
        Patch.resetPatchReport();
    }

    @Test
    void alwaysHitOverwritesTheLocalChanceInsteadOfTheRemovedFieldWrite() throws Exception {
        run("patchAlwaysHit");
        MethodNode method = patched("zombie/CombatManager", "calculateHitChanceData");
        assertEquals(1, count(method, "isAlwaysHit"), "the switch must gate the overwrite exactly once");
        assertEquals(1, count(method, "LDC 100.0"), "the forced chance must be injected exactly once");
        assertTrue(forcedValuePrecedes(method, "LDC 100.0", Opcodes.FSTORE),
                "the forced chance must be stored back into the hit chance local");
    }

    @Test
    void fullDamageRestoresTheSplitAfterTheMovedCalculation() throws Exception {
        run("patchSuperMultiHitFullDamage");
        MethodNode method = patched("zombie/CombatManager", "getHitDamage");
        assertEquals(1, count(method, "isSuperMultiHit"), "the switch must gate the restore exactly once");
        assertTrue(forcedValuePrecedes(method, "FLOAD", Opcodes.FSTORE),
                "the restore must load the damage local and store it into the split local");
    }

    @Test
    void ownerZombieSuppressionFiltersThePayloadInsteadOfDroppingThePacket() throws Exception {
        run("patchSuperMultiHitSuppression");
        MethodNode method = patched("zombie/CombatManager", "attackCollisionCheck");
        assertEquals(1, count(method, "isSuperMultiHit"), "the switch must gate the filter exactly once");
        assertEquals(1, count(method, "HitPacketFilter"), "the payload must be filtered exactly once");
        assertTrue(filterCallIsGuarded(method),
                "the filter result must be loaded and branched on right after the call");
    }

    @Test
    void reapplyingEitherPatchDoesNotAddASecondHook() throws Exception {
        run("patchAlwaysHit");
        run("patchSuperMultiHitFullDamage");
        Patch.resetSession();
        run("patchAlwaysHit");
        run("patchSuperMultiHitFullDamage");
        assertEquals(1, count(patched("zombie/CombatManager", "calculateHitChanceData"), "isAlwaysHit"));
        assertEquals(1, count(patched("zombie/CombatManager", "getHitDamage"), "isSuperMultiHit"));
    }

    private static void run(String name) throws Exception {
        Method target = GamePatcher.class.getDeclaredMethod(name);
        target.setAccessible(true);
        GamePatcher patcher = new GamePatcher();
        // Patch reads the original class from the real game JAR.
        Patch.setProjectZomboidJarPath(System.getProperty("headshot.gameJar"));
        target.invoke(patcher);
        assertTrue(Patch.getFailedClasses().isEmpty(),
                name + " failed: " + Patch.getFailureDetails());
    }

    @SuppressWarnings("unchecked")
    private static MethodNode patched(String owner, String name) throws Exception {
        var handle = java.lang.invoke.MethodHandles.privateLookupIn(Patch.class, java.lang.invoke.MethodHandles.lookup())
                .findStaticVarHandle(Patch.class, "classNodeMap", java.util.Map.class);
        var classes = (java.util.Map<String, ClassNode>) handle.get();
        ClassNode node = classes.get(owner);
        assertNotNull(node, owner + " was not patched");
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    /** True when the given injected value is immediately consumed by the expected store. */
    private static boolean forcedValuePrecedes(MethodNode method, String value, int storeOpcode) {
        boolean[] found = {false};
        for (AbstractInsnNode insn : method.instructions) {
            boolean valueHit = switch (value) {
                case "LDC 100.0" -> insn instanceof LdcInsnNode ldc && Float.valueOf(100.0f).equals(ldc.cst);
                case "FLOAD" -> insn.getOpcode() == Opcodes.FLOAD;
                default -> false;
            };
            if (!valueHit) continue;
            AbstractInsnNode next = insn.getNext();
            while (next != null && next.getOpcode() < 0) next = next.getNext();
            if (next != null && next.getOpcode() == storeOpcode) found[0] = true;
        }
        return found[0];
    }

    /** The injected call must be preceded by loading the list and followed by a branch on its result. */
    private static boolean filterCallIsGuarded(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions) {
            if (!(insn instanceof MethodInsnNode call) || !call.owner.equals("modcore/core/HitPacketFilter")) continue;
            AbstractInsnNode load = insn.getPrevious();
            while (load != null && load.getOpcode() < 0) load = load.getPrevious();
            AbstractInsnNode branch = insn.getNext();
            while (branch != null && branch.getOpcode() < 0) branch = branch.getNext();
            return load != null && load.getOpcode() == Opcodes.ALOAD
                    && branch != null && branch.getOpcode() == Opcodes.IFEQ;
        }
        return false;
    }

    private static int count(MethodNode method, String what) {
        int total = 0;
        for (AbstractInsnNode insn : method.instructions) {
            boolean hit = switch (what) {
                case "isAlwaysHit" -> insn instanceof FieldInsnNode f && f.name.equals("isAlwaysHit");
                case "isSuperMultiHit" -> insn instanceof FieldInsnNode f && f.name.equals("isSuperMultiHit");
                case "HitPacketFilter" -> insn instanceof MethodInsnNode m && m.owner.equals("modcore/core/HitPacketFilter");
                case "LDC 100.0" -> insn instanceof LdcInsnNode ldc && Float.valueOf(100.0f).equals(ldc.cst);
                case "FSTORE" -> insn.getOpcode() == Opcodes.FSTORE;
                default -> false;
            };
            if (hit) total++;
        }
        return total;
    }
}
