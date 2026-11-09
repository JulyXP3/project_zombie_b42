package modcore.agent;

import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PatchExportTest {
    @AfterEach
    void clearSession() throws Exception {
        cache().clear();
        Patch.resetPatchReport();
    }

    @Test
    void exportedBytesIncludeOnlyExplicitlyModifiedClasses() throws Exception {
        reset();
        String target = "zombie/inventory/ItemContainer";
        Patch.modifyClass(target, node -> node.access |= Opcodes.ACC_SYNTHETIC);
        @SuppressWarnings("unchecked") Map<String, byte[]> exported = (Map<String, byte[]>)
                assertDoesNotThrow(() -> Patch.class.getMethod("exportModifiedClasses").invoke(null));
        assertEquals(java.util.Set.of(target), exported.keySet());
        ClassNode output = new ClassNode();
        new ClassReader(exported.get(target)).accept(output, 0);
        assertNotEquals(0, output.access & Opcodes.ACC_SYNTHETIC);
        assertFalse(java.nio.file.Files.exists(Path.of(target + ".class")));
    }

    @Test
    void failedInjectorDoesNotLeakPartialMutationsIntoExport() throws Exception {
        reset();
        String target = "zombie/inventory/ItemContainer";
        Patch.modifyClass(target, node -> node.access |= Opcodes.ACC_SYNTHETIC);
        int before = methodSize(cache().get(target), "getCapacityWeight");
        assertThrows(IllegalStateException.class, () -> Patch.injectIntoClass(target, "getCapacityWeight", false, method -> {
            method.instructions.insert(new InsnNode(Opcodes.NOP));
            throw new IllegalStateException("fixture rejection");
        }));
        assertEquals(before, methodSize(cache().get(target), "getCapacityWeight"),
                "a rejected mutation must not damage a previously accepted patch");
        assertFalse(Patch.getFailedClasses().isEmpty(), "the caught rejection must remain visible");
    }

    @Test
    void newPreparationSessionCannotReusePreviousVersionClasses() throws Exception {
        reset();
        Patch.modifyClass("zombie/inventory/ItemContainer", node -> node.access |= Opcodes.ACC_SYNTHETIC);
        assertFalse(cache().isEmpty());
        assertDoesNotThrow(() -> Patch.class.getMethod("resetSession").invoke(null));
        assertTrue(cache().isEmpty());
        assertTrue(Patch.getFailedClasses().isEmpty());
        assertTrue(Patch.getMissingTargets().isEmpty());
    }

    private static int methodSize(ClassNode node, String name) {
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow().instructions.size();
    }

    private static void reset() throws Exception {
        cache().clear();
        Patch.resetPatchReport();
        Patch.setProjectZomboidJarPath(System.getProperty("headshot.gameJar"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ClassNode> cache() throws Exception {
        return (Map<String, ClassNode>) MethodHandles.privateLookupIn(Patch.class, MethodHandles.lookup())
                .findStaticVarHandle(Patch.class, "classNodeMap", Map.class).get();
    }
}
