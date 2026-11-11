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
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import sun.misc.Unsafe;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Stats;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.hit.AttackVars;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the real final critical decision with a deterministic RNG and no running world. */
class CriticalChancePatchTest {
    private static final String COMBAT = "zombie/CombatManager";
    private static final Path GAME_JAR = Path.of(System.getProperty("headshot.gameJar"));
    private CoreAPI previousApi;
    private CoreAPI api;
    private TestPlayer player;
    private HandWeapon weapon;
    private IsoZombie target;
    private boolean previousClient;
    private boolean previousServer;

    @BeforeEach
    void setUp() throws Exception {
        zombie.core.random.RandStandard.INSTANCE.init();
        previousApi = CoreMain.getInstance().CoreAPI;
        api = allocate(CoreAPI.class);
        CoreMain.getInstance().CoreAPI = api;
        previousClient = GameClient.client;
        previousServer = GameServer.server;
        GameClient.client = true;
        GameServer.server = false;
        player = allocate(TestPlayer.class);
        player.local = true;
        player.attack = new AttackVars();
        player.stats = allocate(Stats.class);
        player.chance = 25;
        weapon = allocate(HandWeapon.class);
        // Do not run HandWeapon's script rebinding, which requires a fully loaded inventory world.
        MethodHandles.privateLookupIn(InventoryItem.class, MethodHandles.lookup())
                .findVarHandle(InventoryItem.class, "scriptItem", Item.class).set(weapon, new Item());
        player.weapon = weapon;
        target = allocate(IsoZombie.class);
        resetPatch();
    }

    @AfterEach
    void tearDown() throws Exception {
        CoreMain.getInstance().CoreAPI = previousApi;
        GameClient.client = previousClient;
        GameServer.server = previousServer;
        resetPatch();
    }

    @Test
    void ordinaryMeleeAndFirearmsUseExactlyNinetyOfTheHundredRollValues() throws Exception {
        Method decision = executable(transform(readCombat()));
        api.isCritMax = true;
        for (WeaponType type : new WeaponType[]{WeaponType.ONE_HANDED, WeaponType.TWO_HANDED,
                WeaponType.HEAVY, WeaponType.SPEAR, WeaponType.HANDGUN, WeaponType.FIREARM}) {
            for (int chance : new int[]{0, 25, 90, 150}) {
                player.chance = chance;
                assertEquals(90, countCriticals(decision, type), "fixed chance: " + type + "/" + chance);
            }
        }
        assertTrue(decide(decision, WeaponType.FIREARM, 89));
        assertFalse(decide(decision, WeaponType.FIREARM, 90));
        player.behind = true;
        assertEquals(90, countCriticals(decision, WeaponType.ONE_HANDED), "behind bonus must not stack");
    }

    @Test
    void disablingTheFeaturePreservesTheVanillaRoll() throws Exception {
        Method decision = executable(transform(readCombat()));
        api.isCritMax = false;
        int[] input = {0, 25, 90, 150};
        int[] expected = {0, 25, 90, 100};
        for (int i = 0; i < input.length; i++) {
            player.chance = input[i];
            assertEquals(expected[i], countCriticals(decision, WeaponType.FIREARM));
        }
        api.isCritMax = true;
        assertEquals(90, countCriticals(decision, WeaponType.FIREARM));
        api.isCritMax = false;
        assertEquals(100, countCriticals(decision, WeaponType.FIREARM), "off must restore this attack's original chance");
    }

    @Test
    void handToHandAndSpecialWeaponRulesStayVanilla() throws Exception {
        Method decision = executable(transform(readCombat()));
        api.isCritMax = true;
        player.handToHand = true;
        assertEquals(25, countCriticals(decision, WeaponType.FIREARM), "holding a gun does not turn a shove into a shot");
        player.handToHand = false;
        player.stomp = true;
        assertEquals(25, countCriticals(decision, WeaponType.ONE_HANDED));
        player.stomp = false;
        player.attack.closeKill = true;
        assertEquals(25, countCriticals(decision, WeaponType.ONE_HANDED), "close kill is excluded independently of knife type");
        player.attack.closeKill = false;
        player.attack.doShove = true;
        assertEquals(25, countCriticals(decision, WeaponType.ONE_HANDED));
        player.attack.doShove = false;
        player.attack.doGrapple = true;
        assertEquals(25, countCriticals(decision, WeaponType.ONE_HANDED));
        player.attack.doGrapple = false;
        weapon.getTags().add(ItemTag.BARE_HANDS);
        assertEquals(25, countCriticals(decision, WeaponType.ONE_HANDED));
        weapon.getTags().clear();
        weapon.getTags().add(ItemTag.FAKE_WEAPON);
        assertEquals(25, countCriticals(decision, WeaponType.FIREARM));
        weapon.getTags().clear();
        assertEquals(25, countCriticals(decision, WeaponType.UNARMED));
        assertEquals(25, countCriticals(decision, WeaponType.THROWING));
        weapon.setAlwaysKnockdown(true);
        player.chance = 100;
        assertEquals(100, countCriticals(decision, WeaponType.ONE_HANDED));
        weapon.setAlwaysKnockdown(false);
        weapon.getTags().add(ItemTag.NO_CRITICALS);
        assertEquals(0, countCriticals(decision, WeaponType.FIREARM));
        weapon.getTags().clear();
        assertEquals(0, countCriticals(decision, WeaponType.KNIFE), "normal knife rule forbids criticals");
        player.attack.closeKill = true;
        player.behind = true;
        player.chance = 0;
        assertEquals(100, countCriticals(decision, WeaponType.KNIFE), "original behind close kill is forced true");
        player.stats.numChasingZombies = 2;
        assertEquals(0, countCriticals(decision, WeaponType.KNIFE), "original knife crowd restriction is forced false");
    }

    @Test
    void remotePlayersDoNotGetAnExtraRoll() throws Exception {
        Method decision = executable(transform(readCombat()));
        api.isCritMax = true;
        player.local = false;
        Roll.value = 0;
        Roll.calls = 0;
        assertFalse(invoke(decision, WeaponType.FIREARM));
        assertEquals(0, Roll.calls);
    }

    @Test
    void runtimeSelectorLeavesMissingNonZombieAndServerInputsUnchanged() throws Exception {
        Class<?> runtime = assertDoesNotThrow(() -> Class.forName("modcore.core.CriticalChanceRuntime"));
        Method select = runtime.getMethod("selectChance", float.class, IsoPlayer.class,
                WeaponType.class, IsoGameCharacter.class);
        api.isCritMax = true;
        assertEquals(90.0f, select.invoke(null, 15.0f, player, WeaponType.FIREARM, target));
        assertEquals(15.0f, select.invoke(null, 15.0f, null, WeaponType.FIREARM, target));
        assertEquals(15.0f, select.invoke(null, 15.0f, player, null, target));
        assertEquals(15.0f, select.invoke(null, 15.0f, player, WeaponType.FIREARM, null));
        assertEquals(15.0f, select.invoke(null, 15.0f, player, WeaponType.FIREARM, player));
        GameServer.server = true;
        assertEquals(15.0f, select.invoke(null, 15.0f, player, WeaponType.FIREARM, target));
        GameServer.server = false;
        player.weapon = null;
        assertEquals(15.0f, select.invoke(null, 15.0f, player, WeaponType.FIREARM, target));
        player.weapon = weapon;
        player.attack = null;
        assertEquals(15.0f, select.invoke(null, 15.0f, player, WeaponType.FIREARM, target));
        player.attack = new AttackVars();
        CoreMain.getInstance().CoreAPI = null;
        assertEquals(15.0f, select.invoke(null, 15.0f, player, WeaponType.FIREARM, target));
    }

    @Test
    void reapplyingDoesNotDuplicateTheHookAndPartialInstallationIsRejected() throws Exception {
        ClassNode first = transform(readCombat());
        byte[] before = bytes(first);
        assertArrayEquals(before, bytes(transform(first)));
        for (AbstractInsnNode insn : critical(first).instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals("modcore/core/CriticalChanceRuntime")) {
                call.name = "missingHook";
                break;
            }
        }
        assertThrows(IllegalStateException.class, () -> transform(first));
    }

    @Test
    void shapeDriftIsRejectedBeforeChangingTheInput() throws Exception {
        ClassNode drifted = readCombat();
        for (AbstractInsnNode insn : critical(drifted).instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals("zombie/core/random/Rand")
                    && call.name.equals("Next") && call.desc.equals("(I)I")) {
                AbstractInsnNode bound = insn.getPrevious();
                while (bound.getOpcode() < 0) bound = bound.getPrevious();
                ((IntInsnNode) bound).operand = 99;
                break;
            }
        }
        byte[] before = bytes(drifted);
        assertThrows(IllegalStateException.class, () -> transform(drifted));
        assertArrayEquals(before, bytes(drifted));
        resetPatch();
        ClassNode wrongSlot = readCombat();
        for (AbstractInsnNode insn : critical(wrongSlot).instructions) {
            if (insn.getOpcode() == Opcodes.FCMPG && insn.getPrevious() instanceof VarInsnNode load) {
                load.var++;
                break;
            }
        }
        assertThrows(IllegalStateException.class, () -> transform(wrongSlot));
    }

    private int countCriticals(Method decision, WeaponType type) throws Exception {
        int count = 0;
        for (int roll = 0; roll < 100; roll++) {
            if (decide(decision, type, roll)) count++;
        }
        return count;
    }

    private boolean decide(Method decision, WeaponType type, int roll) throws Exception {
        Roll.value = roll;
        Roll.calls = 0;
        boolean result = invoke(decision, type);
        assertEquals(1, Roll.calls, "one original RNG draw per local decision");
        return result;
    }

    private boolean invoke(Method method, WeaponType type) throws Exception {
        Object manager;
        try {
            manager = MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup())
                    .findConstructor(method.getDeclaringClass(), MethodType.methodType(void.class)).invoke();
        } catch (Throwable failure) {
            throw new IllegalStateException("cannot create combat decision fixture", failure);
        }
        return (boolean) method.invoke(manager, player, type, target, 1.0f);
    }

    private static ClassNode transform(ClassNode input) throws Exception {
        patchCache().put(COMBAT, input);
        Class<?> patch = assertDoesNotThrow(() -> Class.forName("modcore.core.CriticalChancePatch"),
                "the approved final-chance patch must be present");
        try {
            patch.getMethod("install").invoke(null);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            throw new IllegalStateException("patch rejected", failure.getCause());
        }
        if (!Patch.getMissingTargets().isEmpty() || !Patch.getFailedClasses().isEmpty()) {
            throw new IllegalStateException("patch rejected: " + Patch.getMissingTargets() + Patch.getFailedClasses());
        }
        return patchCache().get(COMBAT);
    }

    private static ClassNode readCombat() throws Exception {
        try (JarFile jar = new JarFile(GAME_JAR.toFile());
             InputStream in = jar.getInputStream(jar.getJarEntry(COMBAT + ".class"))) {
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.EXPAND_FRAMES);
            return node;
        }
    }

    private static MethodNode critical(ClassNode node) {
        return node.methods.stream().filter(m -> m.name.equals("isCriticalHit")).findFirst().orElseThrow();
    }

    private static byte[] bytes(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static Method executable(ClassNode node) throws Exception {
        for (AbstractInsnNode insn : critical(node).instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals("zombie/core/random/Rand")
                    && call.name.equals("Next") && call.desc.equals("(I)I")) {
                call.owner = "modcore/CriticalChancePatchTest$Roll";
                call.name = "next";
            }
        }
        byte[] code = bytes(node);
        ClassLoader loader = new ClassLoader(CriticalChancePatchTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals("zombie.CombatManager") && !name.startsWith("zombie.CombatManager$")) {
                    return super.loadClass(name, resolve);
                }
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    byte[] data = code;
                    if (!name.equals("zombie.CombatManager")) {
                        try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            if (in == null) throw new ClassNotFoundException(name);
                            data = in.readAllBytes();
                        } catch (java.io.IOException e) {
                            throw new ClassNotFoundException(name, e);
                        }
                    }
                    loaded = defineClass(name, data, 0, data.length);
                }
                if (resolve) resolveClass(loaded);
                return loaded;
            }
        };
        return loader.loadClass("zombie.CombatManager").getMethod("isCriticalHit", IsoPlayer.class,
                WeaponType.class, IsoGameCharacter.class, float.class);
    }

    private static Map<String, ClassNode> patchCache() throws Exception {
        VarHandle handle = MethodHandles.privateLookupIn(Patch.class, MethodHandles.lookup())
                .findStaticVarHandle(Patch.class, "classNodeMap", Map.class);
        @SuppressWarnings("unchecked") Map<String, ClassNode> cache = (Map<String, ClassNode>) handle.get();
        return cache;
    }

    private static void resetPatch() {
        Patch.resetSession();
        Patch.setProjectZomboidJarPath(GAME_JAR.toString());
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        VarHandle handle = MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup())
                .findStaticVarHandle(Unsafe.class, "theUnsafe", Unsafe.class);
        return type.cast(((Unsafe) handle.get()).allocateInstance(type));
    }

    public static final class Roll {
        public static int value;
        public static int calls;

        public static int next(int bound) {
            assertEquals(100, bound);
            calls++;
            return value;
        }
    }

    public static class TestPlayer extends IsoPlayer {
        boolean local;
        boolean handToHand;
        boolean stomp;
        boolean behind;
        int chance;
        InventoryItem weapon;
        AttackVars attack;
        Stats stats;

        public TestPlayer() { super(null); }
        @Override public boolean isLocalPlayer() { return local; }
        @Override public boolean isDoHandToHandAttack() { return handToHand; }
        @Override public boolean isDoShove() { return handToHand; }
        @Override public boolean isShoveStompAnim() { return stomp; }
        @Override public boolean isAttackFromBehind() { return behind; }
        @Override public int calculateCritChance(IsoGameCharacter target) { return chance; }
        @Override public InventoryItem getPrimaryHandItem() { return weapon; }
        @Override public AttackVars getAttackVars() { return attack; }
        @Override public Stats getStats() { return stats; }
    }
}
