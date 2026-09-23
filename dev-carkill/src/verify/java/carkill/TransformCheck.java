package carkill;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

public final class TransformCheck {
    private static int failures;

    public static void main(String[] args) throws Exception {
        Path gameJar = Path.of(args[0]);
        ClassLoader hierarchy = new java.net.URLClassLoader(
                new java.net.URL[]{ gameJar.toUri().toURL() }, TransformCheck.class.getClassLoader());
        CarKillInjector.setHierarchyLoader(hierarchy);
        try (JarFile jar = new JarFile(gameJar.toFile())) {
            check(transform(jar, "zombie/characters/IsoGameCharacter.class"),
                    "calculateDamageFromVehicleImpact", "(F)F",
                    "carkill/CarKillHook", "onZombieVehicleDamage",
                    "(Lzombie/characters/IsoGameCharacter;F)F");
            check(transform(jar, "zombie/characters/IsoGameCharacter.class"),
                    "calculateDamageFromVehicleRunOver", "(F)F",
                    "carkill/CarKillHook", "onZombieVehicleDamage",
                    "(Lzombie/characters/IsoGameCharacter;F)F");
            check(transform(jar, "zombie/vehicles/BaseVehicle.class"),
                    "calculateDamageWithCharacter", "(Lzombie/characters/IsoGameCharacter;)I",
                    "carkill/CarKillHook", "onVehicleHitChrDamage",
                    "(Lzombie/vehicles/BaseVehicle;Lzombie/characters/IsoGameCharacter;)I");
            check(transform(jar, "zombie/vehicles/BaseVehicle.class"),
                    "applyImpulseFromHitPedestrian", "(Lzombie/characters/IsoGameCharacter;)V",
                    "carkill/CarKillHook", "onHitPedestrianImpulse",
                    "(Lzombie/vehicles/BaseVehicle;Lzombie/characters/IsoGameCharacter;)Z");
            check(transform(jar, "zombie/vehicles/BaseVehicle.class"),
                    "updateVelocityMultiplier", "()V",
                    "carkill/CarKillHook", "onVelocityMultiplier",
                    "(Lzombie/vehicles/BaseVehicle;)Z");
            check(transform(jar, "zombie/input/GameKeyboard.class"),
                    "update", "()V",
                    "carkill/CarKillHook", "onKeyboardUpdate", "()V");
            assertAbsent(transform(jar, "zombie/input/GameKeyboard.class"), 220);
        }
        checkHookSemantics();
        if (failures != 0) {
            throw new IllegalStateException(failures + " transform check(s) failed");
        }
        System.out.println("[CarKill] transform check passed");
    }

    private static byte[] transform(JarFile jar, String entry) throws Exception {
        byte[] original = jar.getInputStream(jar.getJarEntry(entry)).readAllBytes();
        // NOTE: loader arg is null here, so the transformer keeps the hierarchy loader
        // set in main() instead of overwriting it.
        byte[] transformed = new CarKillInjector.CarKillTransformer()
                .transform(null, entry.substring(0, entry.length() - 6), null, null, original);
        if (transformed == null) {
            fail(entry + " was not transformed");
            return original;
        }
        ClassLoader hierarchy = CarKillInjector.hierarchyLoader;
        CheckClassAdapter.verify(new ClassReader(transformed), hierarchy, false,
                new java.io.PrintWriter(System.out));
        return transformed;
    }

    private static void check(byte[] bytes, String method, String desc,
                              String owner, String name, String hookDesc) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        MethodNode target = null;
        for (MethodNode candidate : node.methods) {
            if (method.equals(candidate.name) && desc.equals(candidate.desc)) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            fail("missing " + method + desc);
            return;
        }
        boolean found = false;
        for (AbstractInsnNode insn = target.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode call
                    && call.getOpcode() == 184
                    && owner.equals(call.owner)
                    && name.equals(call.name)
                    && hookDesc.equals(call.desc)) {
                found = true;
                break;
            }
        }
        if (!found) {
            fail(method + " missing " + name + hookDesc);
            return;
        }
        System.out.println("[OK] " + method + desc);
    }

    private static void assertAbsent(byte[] bytes, int value) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer integer && integer == value) {
                    fail("unexpected constant " + value + " in " + method.name);
                }
            }
        }
    }

    private static void checkHookSemantics() {
        if (CarKillHook.isEnabled()) {
            fail("hook must start disabled");
        }
        if (!Float.isNaN(CarKillHook.onZombieVehicleDamage(null, 10.0f))) {
            fail("disabled damage hook must pass through");
        }
        if (CarKillHook.onVehicleHitChrDamage(null, null) != Integer.MIN_VALUE) {
            fail("disabled self-damage hook must pass through");
        }
        if (CarKillHook.onHitPedestrianImpulse(null, null) || CarKillHook.onVelocityMultiplier(null)) {
            fail("disabled impulse or velocity hook must pass through");
        }
        CarKillHook.toggle();
        if (!CarKillHook.isEnabled()) {
            fail("toggle did not enable");
        }
        if (!Float.isNaN(CarKillHook.onZombieVehicleDamage(null, 10.0f))) {
            fail("enabled hook changed a non-zombie target");
        }
        if (CarKillHook.onVehicleHitChrDamage(null, null) != Integer.MIN_VALUE
                || CarKillHook.onHitPedestrianImpulse(null, null)
                || CarKillHook.onVelocityMultiplier(null)) {
            fail("enabled hook changed an untargeted vehicle");
        }
        CarKillHook.toggle();
        System.out.println("[OK] hook defaults and guards");
    }

    private static void fail(String message) {
        failures++;
        System.err.println("[FAIL] " + message);
    }
}
