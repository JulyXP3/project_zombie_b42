package modcore;

import modcore.core.CoreAPI;
import modcore.core.CoreMain;
import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import modcore.core.HeadshotRuntime;
import sun.misc.Unsafe;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.combat.HitReaction;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.RagdollBodyPart;
import zombie.inventory.types.HandWeapon;
import zombie.iso.Vector2;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the production transform and real processHit control flow, without a running world. */
public class HeadshotPatchTest {
    private static final String COMBAT = "zombie/CombatManager";
    private static final Path GAME_JAR = Path.of(System.getProperty("headshot.gameJar"));
    private CoreAPI previousApi;
    private CoreAPI api;
    private TestPlayer player;
    private TestTarget target;
    private HandWeapon weapon;

    @BeforeEach
    void setUp() throws Exception {
        previousApi = CoreMain.getInstance().CoreAPI;
        zombie.core.random.RandStandard.INSTANCE.init();
        api = allocate(CoreAPI.class);
        CoreMain.getInstance().CoreAPI = api;
        player = allocate(TestPlayer.class);
        player.reaction = "HeadLeft";
        target = allocate(TestTarget.class);
        weapon = allocate(HandWeapon.class);
        resetPatchCache();
        Patch.setProjectZomboidJarPath(GAME_JAR.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        CoreMain.getInstance().CoreAPI = previousApi;
        resetPatchCache();
    }

    @Test
    void headshotsMustNotChangeMeleeOrShoveReactions() throws Throwable {
        Method hit = executableProcessHit(transform(readCombat()));
        Object manager = allocate(hit.getDeclaringClass());
        String[] reactions = {"HeadLeft", "HeadLeft", "HeadLeft", "Uppercut", ""};
        boolean[] ranged = {false, false, true, true, true};
        boolean[] handToHand = {false, true, true, false, true};
        for (boolean headshot : new boolean[]{false, true}) {
            for (boolean crit : new boolean[]{false, true}) {
                api.isHeadshotOnly = headshot;
                api.isCritMax = crit;
                for (int i = 0; i < reactions.length; i++) {
                    player.reaction = reactions[i];
                    player.handToHand = handToHand[i];
                    player.setCriticalHit(crit);
                    player.critCalls = 0;
                    weapon.setRanged(ranged[i]);
                    target.reaction = null;
                    hit.invoke(manager, weapon, player, target);
                    assertEquals(reactions[i], target.reaction,
                            "non-shot reaction must remain unchanged: case=" + i
                                    + ", headshot=" + headshot + ", crit=" + crit);
                    assertEquals(crit, player.isCriticalHit(), "non-shot critical result must be preserved");
                    assertEquals(0, player.critCalls, "non-shot must not enter the shot critical roll");
                }
            }
        }
    }

    @Test
    void shotsKeepTheOriginalCriticalRollWithEitherHeadshotSetting() throws Throwable {
        Method hit = executableProcessHit(transform(readCombat()));
        Object manager = allocate(hit.getDeclaringClass());
        player.reaction = "Shot";
        weapon.setRanged(true);
        for (boolean headshot : new boolean[]{false, true}) {
            for (boolean crit : new boolean[]{false, true}) {
                api.isHeadshotOnly = headshot;
                api.isCritMax = crit;
                player.critChance = crit ? 100 : 0;
                player.critCalls = 0;
                boolean upstreamCrit = !crit;
                player.setCriticalHit(upstreamCrit);
                target.reaction = null;
                hit.invoke(manager, weapon, player, target);
                // b42.22 把暴击掷骰搬进 isCriticalHit(), processHit 不再调用它;
                // 这里守的是"只爆头不得改动上游暴击结论", 不是"必须在本方法内掷".
                assertEquals(0, player.critCalls, "the critical roll moved out of processHit in b42.22");
                assertEquals(upstreamCrit, player.isCriticalHit(), "headshot must not touch the upstream critical decision");
                boolean headReaction = target.reaction != null && target.reaction.startsWith("ShotHead");
                assertEquals(headshot, headReaction, "headshot only controls the shot body part");
            }
        }
    }

    @Test
    void shotLabelAloneMustNotOverrideMeleeOrHandToHandAttacks() throws Throwable {
        Method hit = executableProcessHit(transform(readCombat()));
        Object manager = allocate(hit.getDeclaringClass());
        api.isHeadshotOnly = true;
        player.reaction = "Shot";
        player.critChance = 0;
        for (int i = 0; i < 3; i++) {
            weapon.setRanged(i != 0);
            player.handToHand = i != 0;
            target.reaction = null;
            hit.invoke(manager, weapon, player, target);
            assertEquals("ShotChest", target.reaction, "melee/shove/grapple must keep the original body part");
        }
    }

    @Test
    void rejectsShapeDriftBeforeChangingTheMethod() throws Throwable {
        ClassNode original = readCombat();
        MethodNode method = method(original, "processHit");
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof org.objectweb.asm.tree.FieldInsnNode field
                    && field.owner.equals("zombie/combat/HitReaction") && field.name.equals("SHOT")) {
                field.name = "NONE";
                break;
            }
        }
        byte[] before = bytes(original);
        assertThrows(IllegalStateException.class, () -> transform(original));
        assertArrayEquals(before, bytes(original), "shape failure must leave the input untouched");
    }

    @Test
    void reapplyingThePatchDoesNotAddAnotherHook() throws Throwable {
        ClassNode first = transform(readCombat());
        byte[] before = bytes(first);
        ClassNode second = transform(first);
        assertArrayEquals(before, bytes(second));
    }

    @Test
    void helperPreservesOriginalForDisabledMissingOrUnusableInputs() throws Exception {
        weapon.setRanged(true);
        int head = RagdollBodyPart.BODYPART_HEAD.ordinal();
        for (int original : new int[]{0, 1, 2, 7, RagdollBodyPart.BODYPART_COUNT.ordinal()}) {
            api.isHeadshotOnly = false;
            assertEquals(original, HeadshotRuntime.selectTargetBodyPart(original, weapon, player));
            api.isHeadshotOnly = true;
            assertEquals(head, HeadshotRuntime.selectTargetBodyPart(original, weapon, player));
            assertEquals(original, HeadshotRuntime.selectTargetBodyPart(original, null, player));
            assertEquals(original, HeadshotRuntime.selectTargetBodyPart(original, weapon, null));
            assertEquals(original, HeadshotRuntime.selectTargetBodyPart(original, weapon, target));
            CoreMain.getInstance().CoreAPI = null;
            assertEquals(original, HeadshotRuntime.selectTargetBodyPart(original, weapon, player));
            CoreMain.getInstance().CoreAPI = api;
        }
        TestPlayer broken = allocate(BrokenPlayer.class);
        assertEquals(7, HeadshotRuntime.selectTargetBodyPart(7, weapon, broken));
        api.isHeadshotOnly = false;
        assertEquals(7, HeadshotRuntime.selectTargetBodyPart(7, weapon, player));
    }

    @Test
    void realHandToHandPredicateExcludesBothShovingAndGrappling() {
        api.isHeadshotOnly = true;
        weapon.setRanged(true);
        for (int mode = 1; mode <= 2; mode++) {
            player.setDoShove(mode == 1);
            player.grappling = mode == 2;
            assertEquals(7, HeadshotRuntime.selectTargetBodyPart(7, weapon, player));
        }
    }

    @Test
    void rejectsShiftedCriticalRollBodyStoreAndShotBoundary() throws Throwable {
        for (int mutation = 0; mutation < 3; mutation++) {
            resetPatchCache();
            ClassNode node = readCombat();
            MethodNode hit = method(node, "processHit");
            for (AbstractInsnNode insn : hit.instructions) {
                if (mutation == 0 && insn instanceof MethodInsnNode call && call.name.equals("processTargetedHit")) {
                    call.name = "processTargetedHitDrifted";
                    break;
                }
                if (mutation == 1 && insn instanceof FieldInsnNode field && field.name.equals("BODYPART_COUNT")) {
                    field.name = "BODYPART_HEAD";
                    break;
                }
                if (mutation == 2 && insn instanceof FieldInsnNode field && field.name.equals("SHOT")) {
                    AbstractInsnNode next = insn.getNext();
                    while (next.getOpcode() < 0) next = next.getNext();
                    ((JumpInsnNode) next).setOpcode(Opcodes.IF_ACMPEQ);
                    break;
                }
            }
            byte[] before = bytes(node);
            assertThrows(IllegalStateException.class, () -> transform(node));
            assertArrayEquals(before, bytes(node));
        }
    }

    @Test
    void rejectsHalfInstalledMarkerWithoutTheRuntimeHook() throws Throwable {
        ClassNode patched = transform(readCombat());
        MethodNode hit = method(patched, "processHit");
        for (AbstractInsnNode insn : hit.instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals("modcore/core/HeadshotRuntime")) {
                call.name = "missingHook";
                break;
            }
        }
        assertThrows(IllegalStateException.class, () -> transform(patched));
    }

    @Test
    void unmodifiedCalleesPassJvmVerificationOnFirstInvocation() throws Throwable {
        ClassNode node = transform(readCombat());
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Method hit = loadProcessHit(writer.toByteArray());
        Object manager = allocate(hit.getDeclaringClass());
        java.lang.reflect.InvocationTargetException failure = assertThrows(
                java.lang.reflect.InvocationTargetException.class,
                () -> hit.invoke(manager, null, null, null));
        assertInstanceOf(NullPointerException.class, failure.getCause());
        assertEquals("zombie.CombatManager", failure.getCause().getStackTrace()[0].getClassName());
        assertEquals("processHit", failure.getCause().getStackTrace()[0].getMethodName());
    }

    @Test
    void invalidBytecodeIsActuallyRejectedByTheJvm() throws Exception {
        ClassNode node = readCombat();
        replaceBody(method(node, "processHit"), new InsnNode(Opcodes.ACONST_NULL), new InsnNode(Opcodes.IRETURN));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertThrows(VerifyError.class, () -> {
            Method hit = loadProcessHit(writer.toByteArray());
            hit.invoke(allocate(hit.getDeclaringClass()), null, null, null);
        });
    }

    private static ClassNode readCombat() throws Exception {
        try (JarFile jar = new JarFile(GAME_JAR.toFile());
             InputStream in = jar.getInputStream(jar.getJarEntry(COMBAT + ".class"))) {
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.EXPAND_FRAMES);
            return node;
        }
    }

    private static ClassNode transform(ClassNode input) throws Throwable {
        Map<String, ClassNode> cache = patchCache();
        cache.put(COMBAT, input);
        MethodHandles.privateLookupIn(GamePatcher.class, MethodHandles.lookup())
                .findVirtual(GamePatcher.class, "patchHeadshotOnly", MethodType.methodType(void.class))
                .invoke(new GamePatcher());
        if (!Patch.getMissingTargets().isEmpty() || !Patch.getFailedClasses().isEmpty()) {
            throw new IllegalStateException("patch rejected: " + Patch.getMissingTargets() + Patch.getFailedClasses());
        }
        return cache.get(COMBAT);
    }

    private static Map<String, ClassNode> patchCache() throws Exception {
        VarHandle handle = MethodHandles.privateLookupIn(Patch.class, MethodHandles.lookup())
                .findStaticVarHandle(Patch.class, "classNodeMap", Map.class);
        @SuppressWarnings("unchecked") Map<String, ClassNode> cache = (Map<String, ClassNode>) handle.get();
        return cache;
    }

    private static void resetPatchCache() throws Exception {
        patchCache().clear();
        Patch.resetPatchReport();
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static Method executableProcessHit(ClassNode patched) throws Exception {
        // These callees require a live world, renderer or native physics. Keep processHit itself,
        // its critical roll, branch selection and processTargetedHit's real reaction selection intact.
        replaceBody(method(patched, "applyBlood"), new InsnNode(Opcodes.RETURN));
        replaceBody(method(patched, "setUsePhysicHitReaction"), new InsnNode(Opcodes.RETURN));
        for (MethodNode m : patched.methods) {
            if (m.name.equals("createCombatData")) replaceBody(m, new InsnNode(Opcodes.RETURN));
        }
        MethodNode resolve = method(patched, "resolveHitReaction");
        replaceBody(resolve,
                new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC,
                        "zombie/combat/HitReaction", "SHOT_CHEST", "Lzombie/combat/HitReaction;"),
                new InsnNode(Opcodes.ARETURN));
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        patched.accept(writer);
        return loadProcessHit(writer.toByteArray());
    }

    private static Method loadProcessHit(byte[] result) throws Exception {
        ClassLoader loader = new ClassLoader(HeadshotPatchTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals("zombie.CombatManager") && !name.startsWith("zombie.CombatManager$")) {
                    return super.loadClass(name, resolve);
                }
                Class<?> defined = findLoadedClass(name);
                if (defined == null) {
                    byte[] code = result;
                    if (!name.equals("zombie.CombatManager")) {
                        try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            if (in == null) throw new ClassNotFoundException(name);
                            code = in.readAllBytes();
                        } catch (java.io.IOException e) {
                            throw new ClassNotFoundException(name, e);
                        }
                    }
                    defined = defineClass(name, code, 0, code.length);
                }
                if (resolve) resolveClass(defined);
                return defined;
            }
        };
        return loader.loadClass("zombie.CombatManager")
                .getMethod("processHit", HandWeapon.class, IsoGameCharacter.class, IsoGameCharacter.class);
    }

    private static void replaceBody(MethodNode method, AbstractInsnNode... instructions) {
        Patch.clearBody(method);
        for (AbstractInsnNode instruction : instructions) method.instructions.add(instruction);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        VarHandle handle = MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup())
                .findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class);
        return type.cast(((Unsafe) handle.get()).allocateInstance(type));
    }

    public static class TestPlayer extends IsoPlayer {
        String reaction;
        boolean handToHand;
        boolean grappling;
        int critChance;
        int critCalls;

        public TestPlayer() { super(null); }
        @Override public String getVariableString(String name) { return reaction; }
        @Override public boolean isDoHandToHandAttack() { return handToHand || super.isDoHandToHandAttack(); }
        @Override public boolean isDoGrapple() { return grappling; }
        @Override public int calculateCritChance(IsoGameCharacter target) { critCalls++; return critChance; }
        @Override public BallisticsController getBallisticsController() { return null; }
        @Override public Vector2 getForwardDirection() { return new Vector2(1, 0); }
    }

    public static class BrokenPlayer extends TestPlayer {
        @Override public boolean isDoHandToHandAttack() { throw new IllegalStateException("unavailable attack state"); }
    }

    public static class TestTarget extends IsoGameCharacter {
        String reaction;

        public TestTarget() { super(null, 0, 0, 0); }
        @Override public void setHitReaction(String reaction) { this.reaction = reaction; }
        @Override public String getHitReaction() { return reaction; }
        @Override public boolean isAnimalRunningToDeathPosition() { return false; }
        @Override public boolean isHitFromBehind() { return false; }
        @Override public Vector2 getForwardDirection() { return new Vector2(-1, 0); }
    }
}
