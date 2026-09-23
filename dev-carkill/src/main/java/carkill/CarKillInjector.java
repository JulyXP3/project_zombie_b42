package carkill;

import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class CarKillInjector {
    private static final String HOOK = "carkill/CarKillHook";

    private CarKillInjector() {
    }

    public static void install(Instrumentation inst) {
        inst.addTransformer(new CarKillTransformer(), true);
        if (!inst.isRetransformClassesSupported()) {
            System.out.println("[CarKill] startup transformer installed");
            return;
        }
        retransform(inst, "zombie.characters.IsoGameCharacter");
        retransform(inst, "zombie.vehicles.BaseVehicle");
        retransform(inst, "zombie.input.GameKeyboard");
    }

    private static void retransform(Instrumentation inst, String name) {
        try {
            // NOTE: never Class.forName() here. During premain the game may be loading
            // these classes on another thread; forcing definition races it and dies with
            // LinkageError "duplicate class definition". Only touch already-loaded classes;
            // the transformer (registered retransformation-capable) catches the rest at
            // first definition automatically.
            for (Class<?> loaded : inst.getAllLoadedClasses()) {
                if (loaded.getName().equals(name)) {
                    if (inst.isModifiableClass(loaded)) {
                        inst.retransformClasses(loaded);
                    }
                    return;
                }
            }
            // Not loaded yet: the transformer catches the class at first definition.
        } catch (Throwable t) {
            System.err.println("[CarKill] retransform failed: " + name);
            t.printStackTrace();
        }
    }

    // Hierarchy loader for COMPUTE_FRAMES. Set from the target class loader on every
    // transform; the offline TransformCheck sets it to a URLClassLoader over the game jar.
    static volatile ClassLoader hierarchyLoader;

    static void setHierarchyLoader(ClassLoader loader) {
        hierarchyLoader = loader;
    }

    static final class CarKillTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain pd, byte[] bytes) throws IllegalClassFormatException {
            return transformClass(loader, className, bytes);
        }

        @Override
        public byte[] transform(Module module, ClassLoader loader, String className,
                                Class<?> classBeingRedefined, ProtectionDomain pd, byte[] bytes)
                throws IllegalClassFormatException {
            return transformClass(loader, className, bytes);
        }

        private byte[] transformClass(ClassLoader loader, String className, byte[] bytes) {
            if (className == null || bytes == null) {
                return null;
            }
            if (loader != null) {
                hierarchyLoader = loader;
            }
            // Fail-open: a transformer bug must never kill game startup. Any failure
            // here returns null (original bytes) and only logs.
            try {
                switch (className) {
                    case "zombie/characters/IsoGameCharacter":
                        return rewrite(bytes, this::injectCharacter);
                    case "zombie/vehicles/BaseVehicle":
                        return rewrite(bytes, this::injectVehicle);
                    case "zombie/input/GameKeyboard":
                        return rewrite(bytes, this::injectKeyboard);
                    default:
                        return null;
                }
            } catch (Throwable t) {
                System.err.println("[CarKill] transform skipped: " + className);
                t.printStackTrace();
                return null;
            }
        }

        private byte[] rewrite(byte[] bytes, java.util.function.Consumer<ClassNode> inject) {
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            inject.accept(node);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES) {
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    try {
                        return commonSuper(type1, type2);
                    } catch (Throwable t) {
                        return "java/lang/Object";
                    }
                }
            };
            node.accept(writer);
            return writer.toByteArray();
        }

        /**
         * Byte-based common superclass (mirrors ASM's own algorithm, but resolves the
         * hierarchy from raw class bytes via {@link #hierarchyLoader} resources instead
         * of {@code Class.forName}.
         *
         * <p>forName must NEVER be called from inside a transformer: resolving a class
         * that is currently being defined re-enters defineClass and dies with
         * LinkageError "duplicate class definition", killing game startup.
         * Resource reads define nothing and are safe at any point.
         */
        private String commonSuper(String type1, String type2) throws Exception {
            if (type1.equals(type2)) {
                return type1;
            }
            int dims1 = arrayDims(type1);
            int dims2 = arrayDims(type2);
            if (dims1 != dims2) {
                return "java/lang/Object";
            }
            if (dims1 > 0) {
                String merged = commonSuper(type1.substring(dims1), type2.substring(dims2));
                if (merged.length() == 1) {
                    if (!merged.equals(type1.substring(dims1))) {
                        return "java/lang/Object";
                    }
                    StringBuilder rebuilt = new StringBuilder();
                    for (int i = 0; i < dims1; i++) {
                        rebuilt.append('[');
                    }
                    return rebuilt.append(merged).toString();
                }
                if ("java/lang/Object".equals(merged)) {
                    return "java/lang/Object";
                }
                StringBuilder rebuilt = new StringBuilder();
                for (int i = 0; i < dims1; i++) {
                    rebuilt.append('[');
                }
                return rebuilt.append('L').append(merged).append(';').toString();
            }
            if (type1.length() == 1 || type2.length() == 1) {
                return "java/lang/Object";
            }
            if (isInterface(type1) || isInterface(type2)) {
                return "java/lang/Object";
            }
            Set<String> supers = new HashSet<>();
            for (String current = type1; current != null; current = readSuperName(current)) {
                if (!supers.add(current)) {
                    break;
                }
                if ("java/lang/Object".equals(current)) {
                    break;
                }
            }
            for (String current = type2; current != null; current = readSuperName(current)) {
                if (supers.contains(current)) {
                    return current;
                }
                if ("java/lang/Object".equals(current)) {
                    break;
                }
            }
            return "java/lang/Object";
        }

        private int arrayDims(String internalName) {
            int dims = 0;
            while (dims < internalName.length() && internalName.charAt(dims) == '[') {
                dims++;
            }
            return dims;
        }

        private boolean isInterface(String internalName) throws Exception {
            ClassReader reader = readClassBytes(internalName);
            return reader != null && (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0;
        }

        /** Direct superclass from raw bytes; null at Object, on interfaces, or when unreadable. */
        private String readSuperName(String internalName) throws Exception {
            if ("java/lang/Object".equals(internalName)) {
                return null;
            }
            ClassReader reader = readClassBytes(internalName);
            if (reader == null) {
                return null;
            }
            if ((reader.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                return null;
            }
            return reader.getSuperName();
        }

        private ClassReader readClassBytes(String internalName) throws Exception {
            ClassLoader loader = hierarchyLoader;
            InputStream in = loader != null
                    ? loader.getResourceAsStream(internalName + ".class")
                    : ClassLoader.getSystemResourceAsStream(internalName + ".class");
            if (in == null) {
                return null;
            }
            try {
                return new ClassReader(in);
            } finally {
                in.close();
            }
        }

        private void injectCharacter(ClassNode node) {
            boolean impact = false;
            boolean runOver = false;
            for (MethodNode method : node.methods) {
                if (!"(F)F".equals(method.desc)) {
                    continue;
                }
                if ("calculateDamageFromVehicleImpact".equals(method.name)) {
                    injectZombieDamage(method);
                    impact = true;
                } else if ("calculateDamageFromVehicleRunOver".equals(method.name)) {
                    injectZombieDamage(method);
                    runOver = true;
                }
            }
            require(impact, "IsoGameCharacter.calculateDamageFromVehicleImpact (F)F");
            require(runOver, "IsoGameCharacter.calculateDamageFromVehicleRunOver (F)F");
        }

        private void injectVehicle(ClassNode node) {
            boolean damage = false;
            boolean impulse = false;
            boolean velocity = false;
            for (MethodNode method : node.methods) {
                if ("calculateDamageWithCharacter".equals(method.name)
                        && "(Lzombie/characters/IsoGameCharacter;)I".equals(method.desc)) {
                    injectSelfDamage(method);
                    damage = true;
                } else if ("applyImpulseFromHitPedestrian".equals(method.name)
                        && "(Lzombie/characters/IsoGameCharacter;)V".equals(method.desc)) {
                    injectImpulse(method);
                    impulse = true;
                } else if ("updateVelocityMultiplier".equals(method.name) && "()V".equals(method.desc)) {
                    injectVelocity(method);
                    velocity = true;
                }
            }
            require(damage, "BaseVehicle.calculateDamageWithCharacter (IsoGameCharacter)I");
            require(impulse, "BaseVehicle.applyImpulseFromHitPedestrian (IsoGameCharacter)V");
            require(velocity, "BaseVehicle.updateVelocityMultiplier ()V");
        }

        private void injectKeyboard(ClassNode node) {
            boolean found = false;
            for (MethodNode method : node.methods) {
                if ("update".equals(method.name) && "()V".equals(method.desc)) {
                    InsnList hook = new InsnList();
                    hook.add(new MethodInsnNode(184, HOOK, "onKeyboardUpdate", "()V", false));
                    method.instructions.insert(hook);
                    found = true;
                }
            }
            require(found, "GameKeyboard.update ()V");
        }

        private void injectZombieDamage(MethodNode method) {
            InsnList hook = new InsnList();
            LabelNode replace = new LabelNode();
            hook.add(new VarInsnNode(25, 0));
            hook.add(new VarInsnNode(23, 1));
            hook.add(new MethodInsnNode(184, HOOK, "onZombieVehicleDamage",
                    "(Lzombie/characters/IsoGameCharacter;F)F", false));
            hook.add(new InsnNode(89));
            hook.add(new MethodInsnNode(184, "java/lang/Float", "isNaN", "(F)Z", false));
            hook.add(new JumpInsnNode(153, replace));
            hook.add(new InsnNode(87));
            method.instructions.insert(hook);
            method.instructions.add(replace);
            method.instructions.add(new InsnNode(174));
        }

        private void injectSelfDamage(MethodNode method) {
            InsnList hook = new InsnList();
            LabelNode original = new LabelNode();
            hook.add(new VarInsnNode(25, 0));
            hook.add(new VarInsnNode(25, 1));
            hook.add(new MethodInsnNode(184, HOOK, "onVehicleHitChrDamage",
                    "(Lzombie/vehicles/BaseVehicle;Lzombie/characters/IsoGameCharacter;)I", false));
            hook.add(new InsnNode(89));
            hook.add(new LdcInsnNode(Integer.MIN_VALUE));
            hook.add(new JumpInsnNode(159, original));
            hook.add(new InsnNode(172));
            hook.add(original);
            hook.add(new InsnNode(87));
            method.instructions.insert(hook);
        }

        private void injectImpulse(MethodNode method) {
            InsnList hook = new InsnList();
            LabelNode original = new LabelNode();
            hook.add(new VarInsnNode(25, 0));
            hook.add(new VarInsnNode(25, 1));
            hook.add(new MethodInsnNode(184, HOOK, "onHitPedestrianImpulse",
                    "(Lzombie/vehicles/BaseVehicle;Lzombie/characters/IsoGameCharacter;)Z", false));
            hook.add(new JumpInsnNode(153, original));
            hook.add(new InsnNode(177));
            hook.add(original);
            method.instructions.insert(hook);
        }

        private void injectVelocity(MethodNode method) {
            InsnList hook = new InsnList();
            LabelNode original = new LabelNode();
            hook.add(new VarInsnNode(25, 0));
            hook.add(new MethodInsnNode(184, HOOK, "onVelocityMultiplier",
                    "(Lzombie/vehicles/BaseVehicle;)Z", false));
            hook.add(new JumpInsnNode(153, original));
            hook.add(new InsnNode(177));
            hook.add(original);
            method.instructions.insert(hook);
        }

        private void require(boolean found, String what) {
            if (!found) {
                // Soft-fail: transformClass catches this and falls back to original bytes.
                // A missing method (game update changed signatures) must degrade to
                // vanilla, never crash startup.
                throw new IllegalStateException("injection skipped: " + what);
            }
        }
    }
}
