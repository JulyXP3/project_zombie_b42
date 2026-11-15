package modcore;

import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import zombie.characters.NetworkCharacterAI;
import zombie.characters.NetworkPlayerAI;
import zombie.network.GameClient;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TeleportClimbPatchTest {
    @AfterEach
    void reset() { Patch.resetSession(); }

    @Test
    void postUpdateGateReceivesItsActualReceiverAndRejectsUnrecognizedBody() throws Exception {
        patch();
        MethodNode method = method(cache().get("zombie/characters/NetworkCharacterAI"), "postUpdate");
        assertTrue(calls(method, "suppressActive", "(Lzombie/characters/NetworkCharacterAI;)Z"));
        assertTrue(calls(method, "observePostUpdate", "(Lzombie/characters/NetworkCharacterAI;)V"));
        Patch.resetSession();
        Patch.setProjectZomboidJarPath(gameJar());
        Patch.modifyClass("zombie/characters/NetworkCharacterAI", node ->
                method(node, "postUpdate").instructions.insert(new InsnNode(Opcodes.NOP)));
        byte[] before = Patch.exportModifiedClasses().get("zombie/characters/NetworkCharacterAI");
        invokePatch();
        assertTrue(Patch.getFailedClasses().contains("zombie/characters/NetworkCharacterAI"));
        assertArrayEquals(before, Patch.exportModifiedClasses().get("zombie/characters/NetworkCharacterAI"));
    }

    @Test
    void movementGateKeepsOriginalCodeAndRequiresBothRuntimeHooks() throws Exception {
        patch();
        MethodNode method = method(cache().get("zombie/network/GameClient"), "sendPlayer2");
        assertTrue(calls(method, "routePlayerUpdate", "(Lzombie/characters/IsoPlayer;)Z"));
        assertTrue(calls(method, "observePlayerSender", "()V"));
        MethodNode fence = method(cache().get("zombie/network/packets/service/TimeSyncPacket"), "processClient");
        assertTrue(calls(fence, "acknowledgeFence", "(JLzombie/core/raknet/UdpConnection;)V"));
        assertTrue(java.util.Arrays.stream(method.instructions.toArray())
                .anyMatch(i -> i instanceof MethodInsnNode call && call.name.equals("sendToServer")));
    }

    @Test
    void repeatedApplicationIsByteForByteIdempotentAndBothGatesArePrepared() throws Exception {
        patch();
        byte[] state = Patch.exportModifiedClasses().get("zombie/characters/NetworkCharacterAI");
        byte[] movement = Patch.exportModifiedClasses().get("zombie/network/GameClient");
        invokePatch();
        assertArrayEquals(state, Patch.exportModifiedClasses().get("zombie/characters/NetworkCharacterAI"));
        assertArrayEquals(movement, Patch.exportModifiedClasses().get("zombie/network/GameClient"));
    }

    @Test
    void transformedMethodsExecuteWithTheActualReceiverAndDoNotLeaveBadFrames() throws Exception {
        zombie.core.random.RandStandard.INSTANCE.init();
        patch();
        ClassNode originalState = cache().get("zombie/characters/NetworkCharacterAI");
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        originalState.accept(writer);
        ClassLoader loader = isolated("zombie.characters.NetworkCharacterAI", writer.toByteArray());
        Class<?> type = loader.loadClass("zombie.characters.NetworkCharacterAI");
        Object receiver = allocateConcreteCopy(type, originalState);
        try {
            receiver.getClass().getMethod("postUpdate").invoke(receiver);
            fail("null character should reach the untouched original local branch");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertInstanceOf(NullPointerException.class, e.getCause());
        }
        ClassNode movement = cache().get("zombie/network/GameClient");
        writer = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        movement.accept(writer);
        Class<?> client = isolated("zombie.network.GameClient", writer.toByteArray()).loadClass("zombie.network.GameClient");
        Object instance = BytecodeFixture.allocate(client);
        client.getMethod("sendPlayer2", zombie.characters.IsoPlayer.class).invoke(instance, new Object[]{null});
    }

    private static Object allocateConcreteCopy(Class<?> parent, ClassNode source) throws Exception {
        ClassNode concrete = new ClassNode();
        concrete.version = Opcodes.V25;
        concrete.access = Opcodes.ACC_PUBLIC;
        concrete.name = "modcore/tests/ExecutableNetworkAI";
        concrete.superName = "zombie/characters/NetworkCharacterAI";
        for (MethodNode method : source.methods) {
            if ((method.access & Opcodes.ACC_ABSTRACT) == 0) continue;
            MethodNode implementation = new MethodNode(Opcodes.ACC_PUBLIC, method.name, method.desc, null, null);
            if (method.desc.endsWith("V")) implementation.instructions.add(new InsnNode(Opcodes.RETURN));
            else {
                implementation.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                implementation.instructions.add(new InsnNode(Opcodes.ARETURN));
            }
            concrete.methods.add(implementation);
        }
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        concrete.accept(writer);
        byte[] code = writer.toByteArray();
        Class<?> type = new ClassLoader(parent.getClassLoader()) {
            Class<?> define() { return defineClass("modcore.tests.ExecutableNetworkAI", code, 0, code.length); }
        }.define();
        return BytecodeFixture.allocate(type);
    }

    private static ClassLoader isolated(String name, byte[] bytes) {
        return new ClassLoader(TeleportClimbPatchTest.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String requested, boolean resolve) throws ClassNotFoundException {
                if (!requested.equals(name) && !requested.startsWith("modcore.core.TeleportClimbRuntime")) {
                    return super.loadClass(requested, resolve);
                }
                Class<?> defined = findLoadedClass(requested);
                if (defined == null) {
                    byte[] code = bytes;
                    if (!requested.equals(name)) {
                        try (var in = getParent().getResourceAsStream(requested.replace('.', '/') + ".class")) {
                            if (in == null) throw new ClassNotFoundException(requested);
                            code = in.readAllBytes();
                        } catch (java.io.IOException e) { throw new ClassNotFoundException(requested, e); }
                    }
                    defined = defineClass(requested, code, 0, code.length);
                }
                if (resolve) resolveClass(defined);
                return defined;
            }
        };
    }

    private static void patch() throws Exception {
        Patch.resetSession();
        Patch.setProjectZomboidJarPath(gameJar());
        invokePatch();
        assertTrue(Patch.getFailedClasses().isEmpty(), Patch.getFailureDetails().toString());
    }

    private static void invokePatch() throws Exception {
        GamePatcher patcher = new GamePatcher();
        try {
            MethodHandles.privateLookupIn(GamePatcher.class, MethodHandles.lookup())
                    .findVirtual(GamePatcher.class, "patchTeleportStateSuppress", MethodType.methodType(void.class))
                    .invokeWithArguments(patcher);
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ClassNode> cache() throws Exception {
        VarHandle handle = MethodHandles.privateLookupIn(Patch.class, MethodHandles.lookup())
                .findStaticVarHandle(Patch.class, "classNodeMap", Map.class);
        return (Map<String, ClassNode>) handle.get();
    }

    private static MethodNode method(ClassNode node, String name) {
        assertNotNull(node);
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    private static boolean calls(MethodNode method, String name, String descriptor) {
        return java.util.Arrays.stream(method.instructions.toArray()).anyMatch(i -> i instanceof MethodInsnNode call
                && call.owner.equals("modcore/core/TeleportClimbRuntime") && call.name.equals(name) && call.desc.equals(descriptor));
    }

    private static String gameJar() {
        return System.getProperty("headshot.gameJar", Path.of("lib/projectzomboid.jar").toAbsolutePath().toString());
    }
}
