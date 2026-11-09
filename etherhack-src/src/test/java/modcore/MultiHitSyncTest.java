package modcore;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Executes the production synchronization block, replacing only the world-backed option API. */
class MultiHitSyncTest {
    @Test
    void enablingUpdatesADisabledOption() throws Exception {
        verify(false, true, false, true, 1);
    }

    @Test
    void disablingUpdatesAnEnabledOption() throws Exception {
        verify(true, false, false, false, 1);
    }

    @Test
    void unchangedOptionsAreNotWrittenAgain() throws Exception {
        verify(false, false, false, false, 0);
        verify(true, true, false, true, 0);
    }

    @Test
    void uncachedOptionIsResolvedBeforeSynchronizing() throws Exception {
        verify(false, true, true, true, 1);
    }

    private static void verify(boolean current, boolean enabled, boolean uncached,
                               boolean expected, int writes) throws Exception {
        Option option = new Option(current);
        Options.instance = new Options(option);
        Class<?> type = executableSync();
        Object api = type.getConstructor().newInstance();
        type.getField("isMultiHitZombies").setBoolean(api, enabled);
        type.getField("multiHitOption").set(api, uncached ? null : option);
        type.getMethod("update").invoke(api);
        assertEquals(expected, option.value);
        assertEquals(writes, Options.instance.writes);
        assertSame(option, type.getField("multiHitOption").get(api));
    }

    private static Class<?> executableSync() throws Exception {
        ClassNode source = new ClassNode();
        try (var stream = MultiHitSyncTest.class.getClassLoader()
                .getResourceAsStream("modcore/core/CoreAPI.class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(source, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        MethodNode update = source.methods.stream()
                .filter(method -> method.name.equals("updateLocalPlayerFeatures")).findFirst().orElseThrow();
        AbstractInsnNode start = null;
        AbstractInsnNode end = null;
        for (AbstractInsnNode instruction : update.instructions) {
            if (start == null && instruction instanceof FieldInsnNode field && field.name.equals("multiHitOption")) {
                start = instruction.getPrevious();
                assertEquals(Opcodes.ALOAD, start.getOpcode());
            }
            if (start != null && instruction instanceof MethodInsnNode call && call.name.equals("isTimedActionInstantCheat")) {
                end = instruction.getPrevious();
                assertEquals(Opcodes.ALOAD, end.getOpcode());
                break;
            }
        }
        assertNotNull(start);
        assertNotNull(end);
        String owner = "modcore/MultiHitSyncFixture";
        Map<String, String> names = Map.of(
                "modcore/core/CoreAPI", owner,
                "zombie/SandboxOptions", Options.class.getName().replace('.', '/'),
                "zombie/SandboxOptions$SandboxOption", OptionView.class.getName().replace('.', '/'),
                "zombie/config/ConfigOption", Option.class.getName().replace('.', '/'));
        MethodNode sync = new MethodNode(Opcodes.ACC_PUBLIC, "update", "()V", null, null);
        Map<LabelNode, LabelNode> labels = new HashMap<>();
        for (AbstractInsnNode instruction : update.instructions) {
            if (instruction instanceof LabelNode label) labels.put(label, new LabelNode());
        }
        for (AbstractInsnNode instruction = start; instruction != end; instruction = instruction.getNext()) {
            AbstractInsnNode copy = instruction.clone(labels);
            if (copy instanceof FieldInsnNode field) {
                field.owner = names.getOrDefault(field.owner, field.owner);
                field.desc = remap(field.desc, names);
            } else if (copy instanceof MethodInsnNode call) {
                call.owner = names.getOrDefault(call.owner, call.owner);
                call.desc = remap(call.desc, names);
            }
            sync.instructions.add(copy);
        }
        sync.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PUBLIC, "isMultiHitZombies", "Z", null, null).visitEnd();
        writer.visitField(Opcodes.ACC_PUBLIC, "multiHitOption", "L" + names.get("zombie/SandboxOptions$SandboxOption") + ";", null, null).visitEnd();
        var constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();
        sync.accept(writer);
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        return new ClassLoader(MultiHitSyncTest.class.getClassLoader()) {
            Class<?> define() { return defineClass(owner.replace('/', '.'), bytes, 0, bytes.length); }
        }.define();
    }

    private static String remap(String descriptor, Map<String, String> names) {
        for (var entry : names.entrySet()) {
            descriptor = descriptor.replace("L" + entry.getKey() + ";", "L" + entry.getValue() + ";");
        }
        return descriptor;
    }

    public interface OptionView {
        Option asConfigOption();
    }

    public static final class Option implements OptionView {
        boolean value;
        Option(boolean value) { this.value = value; }
        public Object getValueAsObject() { return value; }
        public Option asConfigOption() { return this; }
    }

    public static final class Options {
        public static Options instance;
        private final Option option;
        int writes;
        Options(Option option) { this.option = option; }
        public OptionView getOptionByName(String name) {
            assertEquals("MultiHitZombies", name);
            return option;
        }
        public void set(String name, Object value) {
            assertEquals("MultiHitZombies", name);
            option.value = (Boolean) value;
            writes++;
        }
    }
}
