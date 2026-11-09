package modcore;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Executes production bytecode with only world and network boundaries replaced. */
public final class BytecodeFixture {
    private BytecodeFixture() {}

    public static ClassNode read(String owner) throws IOException {
        ClassNode node = new ClassNode();
        try (var stream = BytecodeFixture.class.getClassLoader().getResourceAsStream(owner + ".class")) {
            assertNotNull(stream, owner);
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return node;
    }

    public static MethodNode copy(MethodNode source, Map<String, String> names) {
        MethodNode method = new MethodNode(source.access, source.name, remap(source.desc, names), null, null);
        source.accept(method);
        method.visibleAnnotations = null;
        method.invisibleAnnotations = null;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field) {
                field.owner = names.getOrDefault(field.owner, field.owner);
                field.desc = remap(field.desc, names);
            } else if (instruction instanceof MethodInsnNode call) {
                call.owner = names.getOrDefault(call.owner, call.owner);
                call.desc = remap(call.desc, names);
            } else if (instruction instanceof TypeInsnNode type) {
                type.desc = names.getOrDefault(type.desc, type.desc);
            } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                dynamic.desc = remap(dynamic.desc, names);
            }
        }
        return method;
    }

    public static ClassNode type(String owner) {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = owner;
        node.superName = "java/lang/Object";
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(constructor);
        return node;
    }

    public static Class<?> define(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(BytecodeFixture.class.getClassLoader()) {
            Class<?> define() { return defineClass(node.name.replace('/', '.'), bytes, 0, bytes.length); }
        }.define();
    }

    public static String owner(Class<?> type) {
        return type.getName().replace('.', '/');
    }

    private static String remap(String descriptor, Map<String, String> names) {
        for (var entry : names.entrySet()) {
            descriptor = descriptor.replace("L" + entry.getKey() + ";", "L" + entry.getValue() + ";");
        }
        return descriptor;
    }
}
