package modcore;

import modcore.core.CoreAPI;
import modcore.core.CoreMain;
import modcore.core.CriticalChancePatch;
import modcore.core.HeadshotRuntime;
import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import sun.misc.Unsafe;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.PlayerCheats;
import zombie.characters.ecs.ECSComponent;
import zombie.characters.skills.PerkFactory;
import zombie.core.physics.BallisticsController;
import zombie.core.random.RandAbstract;
import zombie.core.random.RandStandard;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises real probability, damage, armor and health calculations without a running world. */
public class HeadshotDamageTest {
    private static final String COMBAT = "zombie/CombatManager";
    private static final Path GAME_JAR = Path.of(System.getProperty("headshot.gameJar"));
    private CoreAPI previousApi;
    private CoreAPI api;
    private IsoPlayer[] previousPlayers;
    private int previousNumPlayers;
    private Random previousRandom;
    private boolean previousClient;
    private boolean previousServer;
    private TestPlayer player;
    private IsoZombie zombie;
    private TestTarget healthTarget;
    private HandWeapon weapon;

    @BeforeEach
    void setUp() throws Exception {
        previousApi = CoreMain.getInstance().CoreAPI;
        api = allocate(CoreAPI.class);
        CoreMain.getInstance().CoreAPI = api;
        player = allocate(TestPlayer.class);
        player.components = new HashMap<>();
        player.reaction = "Shot";
        zombie = allocate(IsoZombie.class);
        zombie.cheats = new PlayerCheats();
        healthTarget = allocate(TestTarget.class);
        weapon = allocate(HandWeapon.class);
        MethodHandles.privateLookupIn(InventoryItem.class, MethodHandles.lookup())
                .findVarHandle(InventoryItem.class, "scriptItem", Item.class).set(weapon, allocate(Item.class));
        weapon.setRanged(true);
        weapon.setHitChance(5);
        weapon.setAimingPerkHitChanceModifier(0);
        weapon.setMinSightRange(0);
        weapon.setMaxSightRange(10);
        weapon.setMinDamage(0.2f);
        weapon.setMaxDamage(0.2f);
        previousPlayers = IsoPlayer.players.clone();
        previousNumPlayers = IsoPlayer.numPlayers;
        IsoPlayer.players[0] = player;
        IsoPlayer.numPlayers = 1;
        previousRandom = (Random) randomHandle().get(RandStandard.INSTANCE);
        randomHandle().set(RandStandard.INSTANCE, new HighRoll());
        previousClient = GameClient.client;
        previousServer = GameServer.server;
        GameClient.client = false;
        GameServer.server = false;
        Patch.resetSession();
        Patch.setProjectZomboidJarPath(GAME_JAR.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        CoreMain.getInstance().CoreAPI = previousApi;
        if (previousPlayers != null) {
            System.arraycopy(previousPlayers, 0, IsoPlayer.players, 0, previousPlayers.length);
            IsoPlayer.numPlayers = previousNumPlayers;
            GameClient.client = previousClient;
            GameServer.server = previousServer;
            randomHandle().set(RandStandard.INSTANCE, previousRandom);
        }
        Patch.resetSession();
    }

    @Test
    void headshotOnlyPassesTheDamageRollAndActuallyReducesHealth() throws Throwable {
        Class<?> combat = executableCombat(transform("patchHeadshotOnly", "patchAlwaysHit"));
        Object manager = manager(combat);
        api.isHeadshotOnly = true;
        float chance = chance(combat, manager, player, zombie);
        Object hit = hitData(combat, manager, chance);
        float damage = damage(combat, manager, hit);
        zombie.setHealth(10.0f);
        applyZombieHealth(damage, ignored(hit));
        assertEquals(9.4f, zombie.getHealth(), 0.00001f, "check the real zombie health, not the reaction string");
        assertEquals(100.0f, chance);
        assertFalse(ignored(hit), "a roll of 99 must not suppress headshot damage");
        assertEquals(0.6f, damage, 0.00001f, "base damage 0.2 receives the original head multiplier");
        assertFalse(api.isAlwaysHit, "headshot-only must not enable the separate switch");
    }

    @Test
    void disablingBothSwitchesRestoresTheVanillaDamageRoll() throws Throwable {
        Class<?> combat = executableCombat(transform("patchHeadshotOnly", "patchAlwaysHit"));
        Object manager = manager(combat);
        float chance = chance(combat, manager, player, zombie);
        assertEquals(5.0f, chance);
        Object hit = hitData(combat, manager, chance);
        assertTrue(ignored(hit), "the deterministic roll of 99 must fail the vanilla chance of 5");
        zombie.setHealth(10.0f);
        applyZombieHealth(damage(combat, manager, hit), ignored(hit));
        assertEquals(10.0f, zombie.getHealth());
    }

    @Test
    void enablingBothSwitchesDoesNotMultiplyHeadshotDamageAgain() throws Throwable {
        Class<?> combat = executableCombat(transform("patchHeadshotOnly", "patchAlwaysHit"));
        Object manager = manager(combat);
        api.isHeadshotOnly = true;
        for (boolean alwaysHit : new boolean[]{false, true}) {
            api.isAlwaysHit = alwaysHit;
            Object hit = hitData(combat, manager, chance(combat, manager, player, zombie));
            zombie.setHealth(10.0f);
            applyZombieHealth(damage(combat, manager, hit), ignored(hit));
            assertEquals(9.4f, zombie.getHealth(), 0.00001f, "the second switch must not add damage");
        }
    }

    @Test
    void headshotProbabilityGuaranteeIsLimitedToTheLocalShooterAndZombies() throws Throwable {
        Class<?> combat = executableCombat(transform("patchHeadshotOnly", "patchAlwaysHit"));
        Object manager = manager(combat);
        api.isHeadshotOnly = true;
        TestPlayer other = allocate(TestPlayer.class);
        other.components = new HashMap<>();
        other.reaction = "Shot";
        assertFalse(other.isLocalPlayer());
        assertEquals(5.0f, chance(combat, manager, other, zombie));
        assertEquals(5.0f, chance(combat, manager, player, other), "player targets are not included");
        assertEquals(5.0f, chance(combat, manager, player, healthTarget), "non-zombie targets are not included");
        assertEquals(5.0f, chance(combat, manager, player, null), "missing targets are not included");
        assertEquals(100.0f, chance(combat, manager, player, zombie));
        api.isAlwaysHit = true;
        assertEquals(100.0f, chance(combat, manager, other, zombie), "existing always-hit behavior is preserved");
        assertEquals(100.0f, chance(combat, manager, player, other));
    }

    @Test
    void shovingAndGrapplingKeepTheirOriginalDamageRules() throws Throwable {
        Class<?> combat = executableCombat(transform("patchHeadshotOnly", "patchAlwaysHit"));
        Object manager = manager(combat);
        api.isHeadshotOnly = true;
        player.setDoShove(true);
        assertEquals(5.0f, chance(combat, manager, player, zombie));
        Object hit = hitData(combat, manager, 100.0f);
        assertTrue(ignored(hit), "even a passing roll must keep the original shove exemption");
        healthTarget.setHealth(10.0f);
        healthTarget.hitConsequences(weapon, player, ignored(hit), 0.6f, true);
        assertEquals(10.0f, healthTarget.getHealth());
        player.setDoShove(false);
        player.grappling = true;
        assertEquals(5.0f, chance(combat, manager, player, zombie));
    }

    @Test
    void armorAndInvulnerabilityStillApplyAfterTheChanceGuarantee() throws Throwable {
        Class<?> combat = executableCombat(transform("patchHeadshotOnly", "patchAlwaysHit"));
        Object manager = manager(combat);
        api.isHeadshotOnly = true;
        Object hit = hitData(combat, manager, chance(combat, manager, player, zombie));
        healthTarget.defense = 10.0f;
        float damage = damage(combat, manager, hit);
        assertEquals(0.42f, damage, 0.00001f, "default armor factor 2 gives 30 percent total protection");
        healthTarget.setHealth(10.0f);
        healthTarget.hitConsequences(weapon, player, ignored(hit), damage, true);
        assertEquals(9.58f, healthTarget.getHealth(), 0.00001f);
        healthTarget.invulnerable = true;
        healthTarget.setHealth(10.0f);
        healthTarget.hitConsequences(weapon, player, false, 0.6f, true);
        assertEquals(10.0f, healthTarget.getHealth(), "the feature must not bypass invulnerability");
    }

    @Test
    void combinedCombatInstallationKeepsFullDamageAndIsIdempotent() throws Throwable {
        ClassNode patched = transform("patchHeadshotOnly", "CriticalChancePatch", "patchAlwaysHit",
                "patchSuperMultiHit", "patchSuperMultiHitSuppression", "patchSuperMultiHitFullDamage");
        MethodNode damage = method(patched, "getHitDamage");
        int gates = 0;
        for (AbstractInsnNode insn : damage.instructions) {
            if (insn instanceof FieldInsnNode field && field.name.equals("isSuperMultiHit")) gates++;
        }
        assertEquals(1, gates, "the headshot patch must not skip the later full-damage patch");
        byte[] first = bytes(patched);
        runPatch("patchHeadshotOnly");
        runPatch("CriticalChancePatch");
        runPatch("patchAlwaysHit");
        runPatch("patchSuperMultiHit");
        runPatch("patchSuperMultiHitSuppression");
        runPatch("patchSuperMultiHitFullDamage");
        assertArrayEquals(first, bytes(patchCache().get(COMBAT)), "the complete combat sequence must be idempotent");
    }

    @Test
    void combinedInstallationStillRestoresTheActualMultiTargetDamageSplit() throws Throwable {
        ClassNode node = transform("patchHeadshotOnly", "CriticalChancePatch", "patchAlwaysHit",
                "patchSuperMultiHit", "patchSuperMultiHitSuppression", "patchSuperMultiHitFullDamage");
        // Use a fixed non-zero base input to test the real division and restore branch in isolation.
        for (String name : new String[]{"getDamage", "getRangeDel"}) {
            MethodNode method = method(node, name);
            Patch.clearBody(method);
            method.instructions.add(new InsnNode(Opcodes.FCONST_1));
            method.instructions.add(new InsnNode(Opcodes.FRETURN));
        }
        for (AbstractInsnNode insn : method(node, "getHitDamage").instructions) {
            if (insn instanceof MethodInsnNode call && call.name.equals("getMoodleLevel")) {
                call.setOpcode(Opcodes.INVOKESTATIC);
                call.owner = "modcore/HeadshotDamageTest";
                call.name = "neutralMoodle";
                call.desc = "(Lzombie/characters/Moodles/Moodles;Lzombie/scripting/objects/MoodleType;)I";
            }
        }
        Class<?> combat = executableCombat(node);
        Object manager = manager(combat);
        Object hit = hitData(combat, manager, 100.0f);
        weapon.setRanged(false);
        for (boolean enabled : new boolean[]{false, true, false}) {
            api.isSuperMultiHit = enabled;
            Object result = combat.getMethod("getHitDamage", IsoGameCharacter.class, HandWeapon.class,
                    IsoMovingObject.class, hit.getClass(), int.class).invoke(manager, player, weapon, null, hit, 3);
            float split = ((Number) result.getClass().getMethod("damageSplit").invoke(result)).floatValue();
            assertEquals(enabled ? 1.0f : 0.5f, split,
                    "fourth target: base 1 divided by (4 * 0.5), or restored to 1 when enabled");
        }
    }

    @Test
    void alwaysHitAlonePassesTheRollWithoutForcingTheHead() throws Throwable {
        Class<?> combat = executableCombat(transform("patchHeadshotOnly", "patchAlwaysHit"));
        Object manager = manager(combat);
        api.isAlwaysHit = true;
        Object hit = hitData(combat, manager, chance(combat, manager, player, zombie));
        assertFalse(ignored(hit));
        assertFalse(api.isHeadshotOnly);
        assertEquals(0.01f, damage(combat, manager, hit), 0.00001f,
                "the controlled non-critical original reaction chooses a leg, not the forced head");
        healthTarget.setHealth(10.0f);
        healthTarget.hitConsequences(weapon, player, false, damage(combat, manager, hit), true);
        assertEquals(9.99f, healthTarget.getHealth(), 0.00001f);
    }

    @Test
    void probabilityHelperKeepsDisabledInvalidAndNonShotInputsUnchanged() throws Exception {
        for (float original : new float[]{0.0f, 5.0f, 42.0f, 100.0f}) {
            api.isHeadshotOnly = false;
            assertEquals(original, HeadshotRuntime.selectHitChance(original, weapon, player, zombie));
            api.isHeadshotOnly = true;
            assertEquals(100.0f, HeadshotRuntime.selectHitChance(original, weapon, player, zombie));
            assertEquals(original, HeadshotRuntime.selectHitChance(original, null, player, zombie));
            assertEquals(original, HeadshotRuntime.selectHitChance(original, weapon, null, zombie));
            assertEquals(original, HeadshotRuntime.selectHitChance(original, weapon, player, null));
            weapon.setRanged(false);
            assertEquals(original, HeadshotRuntime.selectHitChance(original, weapon, player, zombie));
            weapon.setRanged(true);
            CoreMain.getInstance().CoreAPI = null;
            assertEquals(original, HeadshotRuntime.selectHitChance(original, weapon, player, zombie));
            CoreMain.getInstance().CoreAPI = api;
        }
        TestPlayer broken = allocate(BrokenPlayer.class);
        IsoPlayer.players[0] = broken;
        assertEquals(42.0f, HeadshotRuntime.selectHitChance(42.0f, weapon, broken, zombie));
        api.isAlwaysHit = true;
        assertEquals(100.0f, HeadshotRuntime.selectHitChance(42.0f, weapon, broken, zombie),
                "always-hit keeps its existing behavior independently of the headshot-only predicate");
    }

    @Test
    void probabilityPatchDetectsAMissingHookOnReapplication() throws Throwable {
        ClassNode node = transform("patchAlwaysHit");
        for (AbstractInsnNode insn : method(node, "calculateHitChanceData").instructions) {
            if (insn instanceof MethodInsnNode call && call.name.equals("selectHitChance")) {
                call.name = "missingChanceSelector";
                break;
            }
        }
        assertThrows(IllegalStateException.class, () -> runPatch("patchAlwaysHit"));
    }

    @Test
    void probabilityPatchRejectsDescriptorAndReturnShapeDrift() throws Throwable {
        for (int mutation = 0; mutation < 2; mutation++) {
            Patch.resetSession();
            Patch.setProjectZomboidJarPath(GAME_JAR.toString());
            ClassNode node = readCombat();
            MethodNode chance = method(node, "calculateHitChanceData");
            if (mutation == 0) {
                chance.desc = chance.desc.replace("Lzombie/iso/IsoMovingObject;", "Lzombie/iso/IsoObject;");
            } else {
                MethodInsnNode last = null;
                for (AbstractInsnNode insn : chance.instructions) {
                    if (insn instanceof MethodInsnNode call && call.owner.equals("zombie/CombatManager$HitChanceData")
                            && call.name.equals("<init>")) last = call;
                }
                assertNotNull(last);
                last.desc = "(FI)V";
            }
            byte[] before = bytes(node);
            patchCache().put(COMBAT, node);
            assertThrows(IllegalStateException.class, () -> runPatch("patchAlwaysHit"));
            assertArrayEquals(before, bytes(node), "a shape failure must not mutate the input");
        }
    }

    private float chance(Class<?> combat, Object manager, IsoGameCharacter owner, IsoMovingObject target)
            throws Exception {
        Object data = combat.getMethod("calculateHitChanceData", IsoGameCharacter.class,
                HandWeapon.class, IsoMovingObject.class, float.class).invoke(manager, owner, weapon, target, 10.0f);
        return ((Number) data.getClass().getMethod("hitChance").invoke(data)).floatValue();
    }

    private Object hitData(Class<?> combat, Object manager, float chance) throws Exception {
        return combat.getMethod("getHitData", IsoLivingCharacter.class, HandWeapon.class,
                IsoMovingObject.class, float.class).invoke(manager, player, weapon, zombie, chance);
    }

    private static boolean ignored(Object hit) throws Exception {
        assertNotNull(hit);
        return (boolean) hit.getClass().getMethod("isIgnoreDamage").invoke(hit);
    }

    private float damage(Class<?> combat, Object manager, Object hit) throws Exception {
        Object result = combat.getMethod("getHitDamage", IsoGameCharacter.class, HandWeapon.class,
                IsoMovingObject.class, hit.getClass(), int.class).invoke(manager, player, weapon, healthTarget, hit, 0);
        return ((Number) result.getClass().getMethod("damageSplit").invoke(result)).floatValue();
    }

    private void applyZombieHealth(float damage, boolean ignore) throws Throwable {
        // Retain the real superclass health gate and real zombie health field, not its AI tail.
        MethodHandles.privateLookupIn(IsoZombie.class, MethodHandles.lookup())
                .findSpecial(IsoGameCharacter.class, "hitConsequences", MethodType.methodType(void.class,
                        HandWeapon.class, IsoGameCharacter.class, boolean.class, float.class, boolean.class),
                        IsoZombie.class).invoke(zombie, weapon, player, ignore, damage, true);
    }

    private static Object manager(Class<?> combat) throws Throwable {
        return MethodHandles.privateLookupIn(combat, MethodHandles.lookup())
                .findConstructor(combat, MethodType.methodType(void.class)).invoke();
    }

    private ClassNode transform(String... patches) throws Throwable {
        patchCache().put(COMBAT, readCombat());
        for (String patch : patches) runPatch(patch);
        return patchCache().get(COMBAT);
    }

    private static void runPatch(String name) throws Throwable {
        if (name.equals("CriticalChancePatch")) {
            CriticalChancePatch.install();
        } else {
            MethodHandles.privateLookupIn(GamePatcher.class, MethodHandles.lookup())
                    .findVirtual(GamePatcher.class, name, MethodType.methodType(void.class)).invoke(new GamePatcher());
        }
        if (!Patch.getFailedClasses().isEmpty() || !Patch.getMissingTargets().isEmpty()) {
            throw new IllegalStateException(name + " rejected: " + Patch.getFailureDetails() + Patch.getMissingTargets());
        }
    }

    private static ClassNode readCombat() throws Exception {
        return readGameClass(COMBAT);
    }

    private static ClassNode readGameClass(String name) throws Exception {
        try (JarFile jar = new JarFile(GAME_JAR.toFile());
             InputStream in = jar.getInputStream(jar.getJarEntry(name + ".class"))) {
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.EXPAND_FRAMES);
            return node;
        }
    }

    private static Class<?> executableCombat(ClassNode node) throws Exception {
        for (MethodNode method : node.methods) {
            if (method.name.equals("applyBlood") || method.name.equals("createCombatData")
                    || method.name.equals("setUsePhysicHitReaction")) {
                Patch.clearBody(method);
                method.instructions.add(new InsnNode(Opcodes.RETURN));
            }
            if (method.name.equals("getPainPenalty") || method.name.equals("getMoodlesPenalty")) {
                Patch.clearBody(method);
                method.instructions.add(new InsnNode(Opcodes.FCONST_0));
                method.instructions.add(new InsnNode(Opcodes.FRETURN));
            }
            if (method.name.equals("calculateHitChanceData")) {
                // Pose needs live zombie AI. Only replace pose lookup, not the chance calculation.
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof MethodInsnNode call && call.name.equals("isProne")
                            && call.desc.equals("()Z")) {
                        call.setOpcode(Opcodes.INVOKESTATIC);
                        call.owner = "modcore/HeadshotDamageTest";
                        call.name = "standingTarget";
                        call.desc = "(Lzombie/iso/IsoMovingObject;)Z";
                    }
                }
            }
            if (method.name.equals("calculateHitData")) {
                // Keep the real roll and exemption flag in getHitData; omit location/native geometry.
                Patch.clearBody(method);
                method.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.NEW, COMBAT + "$HitData"));
                method.instructions.add(new InsnNode(Opcodes.DUP));
                method.instructions.add(new InsnNode(Opcodes.ICONST_0));
                method.instructions.add(new InsnNode(Opcodes.ICONST_0));
                method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
                method.instructions.add(new InsnNode(Opcodes.ICONST_0));
                method.instructions.add(new InsnNode(Opcodes.ICONST_1));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, COMBAT + "$HitData", "<init>",
                        "(BZZZZ)V", false));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));
            }
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        byte[] transformed = writer.toByteArray();
        ClassNode sandbox = readGameClass("zombie/SandboxOptions");
        MethodNode constructor = method(sandbox, "<init>");
        AbstractInsnNode mediaTail = null;
        for (AbstractInsnNode insn : constructor.instructions) {
            if (insn instanceof FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
                    && field.owner.equals("zombie/sandbox/CustomSandboxOptions") && field.name.equals("instance")) {
                mediaTail = insn;
                break;
            }
        }
        assertNotNull(mediaTail, "sandbox initialization must keep declarations but omit media/mod loading");
        constructor.instructions.insertBefore(mediaTail, new InsnNode(Opcodes.RETURN));
        while (mediaTail != null) {
            AbstractInsnNode next = mediaTail.getNext();
            constructor.instructions.remove(mediaTail);
            mediaTail = next;
        }
        constructor.localVariables = null;
        ClassWriter sandboxWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        sandbox.accept(sandboxWriter);
        byte[] sandboxBytes = sandboxWriter.toByteArray();
        ClassLoader loader = new ClassLoader(HeadshotDamageTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals("zombie.CombatManager") && !name.startsWith("zombie.CombatManager$")
                        && !name.equals("zombie.SandboxOptions") && !name.startsWith("zombie.SandboxOptions$")) {
                    return super.loadClass(name, resolve);
                }
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    byte[] code = name.equals("zombie.SandboxOptions") ? sandboxBytes : transformed;
                    if (!name.equals("zombie.CombatManager") && !name.equals("zombie.SandboxOptions")) {
                        try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            if (in == null) throw new ClassNotFoundException(name);
                            code = in.readAllBytes();
                        } catch (java.io.IOException e) {
                            throw new ClassNotFoundException(name, e);
                        }
                    }
                    loaded = defineClass(name, code, 0, code.length);
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        };
        return loader.loadClass("zombie.CombatManager");
    }

    public static boolean standingTarget(IsoMovingObject target) { return false; }

    public static int neutralMoodle(zombie.characters.Moodles.Moodles moodles,
                                    zombie.scripting.objects.MoodleType type) { return 0; }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ClassNode> patchCache() throws Exception {
        return (Map<String, ClassNode>) MethodHandles.privateLookupIn(Patch.class, MethodHandles.lookup())
                .findStaticVarHandle(Patch.class, "classNodeMap", Map.class).get();
    }

    private static VarHandle randomHandle() throws Exception {
        return MethodHandles.privateLookupIn(RandAbstract.class, MethodHandles.lookup())
                .findVarHandle(RandAbstract.class, "rand", Random.class);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Unsafe unsafe = (Unsafe) MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup())
                .findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class).get();
        return type.cast(unsafe.allocateInstance(type));
    }

    private static class HighRoll extends Random {
        @Override public int nextInt(int bound) { return bound - 1; }
        @Override public float nextFloat() { return 0.5f; }
    }

    public static class TestPlayer extends IsoPlayer {
        String reaction;
        boolean grappling;
        HashMap<Class<? extends ECSComponent>, ECSComponent> components;
        public TestPlayer() { super(null); }
        @Override public String getVariableString(String name) { return reaction; }
        @Override public boolean isDoGrapple() { return grappling; }
        @Override public int getPerkLevel(PerkFactory.Perk perk) { return 0; }
        @Override public boolean hasTrait(CharacterTrait trait) { return false; }
        @Override public float getWornItemsVisionModifier() { return 1.0f; }
        @Override public Vector2 getLookVector(Vector2 vector) { return vector.set(1, 0); }
        @Override public Vector2 getForwardDirection() { return new Vector2(1, 0); }
        @Override public BallisticsController getBallisticsController() { return null; }
        @Override public HashMap<Class<? extends ECSComponent>, ECSComponent> getECSComponentMap() { return components; }
    }

    public static class BrokenPlayer extends TestPlayer {
        @Override public boolean isDoHandToHandAttack() {
            throw new IllegalStateException("unavailable attack state");
        }
    }

    public static class TestTarget extends IsoGameCharacter {
        float defense;
        boolean invulnerable;
        String reaction;
        public TestTarget() { super(null, 0, 0, 0); }
        @Override public boolean isProne() { return false; }
        @Override public Vector2 getForwardDirection() { return new Vector2(-1, 0); }
        @Override public void setHitReaction(String value) { reaction = value; }
        @Override public String getHitReaction() { return reaction; }
        @Override public float getBodyPartClothingDefense(Integer part, boolean bite, boolean bullet) { return defense; }
        @Override public void addBlood(BloodBodyPartType part, boolean scratch, boolean bite, boolean all) { }
        @Override public boolean helmetFall(boolean head) { return false; }
        @Override public boolean isInvulnerable() { return invulnerable; }
    }
}
