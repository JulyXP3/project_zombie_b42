package modcore.agent;

import modcore.BytecodeFixture;
import modcore.GamePatcher;
import modcore.utils.Patch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CombatPacketGateTest {
    @AfterEach
    void reset() {
        Patch.resetSession();
        Patch.resetPatchReport();
        Main.instance = null;
    }

    @Test
    void disabledMultiHitStillReportsTheShot() throws Exception {
        verify(false, false, false, 1, 0);
    }

    @Test
    void missingRuntimeStillReportsTheShot() throws Exception {
        var method = executableGate();
        Main.instance = null;
        Network.sent = 0;
        Filter.calls = 0;
        method.invoke(null, new Weapon(false), List.of());
        assertEquals(1, Network.sent);
        assertEquals(0, Filter.calls);
    }

    @Test
    void firearmsKeepTheServerAmmoConsumptionNotification() throws Exception {
        verify(true, true, false, 1, 0);
    }

    @Test
    void enabledMeleeStillSuppressesAnOwnerOnlyHit() throws Exception {
        verify(true, false, false, 0, 1);
    }

    @Test
    void enabledMeleeStillReportsRemainingTargets() throws Exception {
        verify(true, false, true, 1, 1);
    }

    private static void verify(boolean enabled, boolean firearm, boolean remaining,
                               int sent, int calls) throws Exception {
        var method = executableGate();
        Main.instance = new Main();
        Main.instance.CoreAPI = new Api();
        Main.instance.CoreAPI.isSuperMultiHit = enabled;
        Network.sent = 0;
        Filter.calls = 0;
        Filter.remaining = remaining;
        method.invoke(null, new Weapon(firearm), List.of());
        assertEquals(sent, Network.sent, "shot notification count");
        assertEquals(calls, Filter.calls, "melee payload filter count");
    }

    @SuppressWarnings("unchecked")
    private static java.lang.reflect.Method executableGate() throws Exception {
        Patch.resetSession();
        Patch.resetPatchReport();
        Patch.setProjectZomboidJarPath(System.getProperty("headshot.gameJar"));
        new GamePatcher().patchSuperMultiHitSuppression();
        assertTrue(Patch.getFailedClasses().isEmpty(), Patch.getFailureDetails().toString());
        var handle = MethodHandles.privateLookupIn(Patch.class, MethodHandles.lookup())
                .findStaticVarHandle(Patch.class, "classNodeMap", Map.class);
        ClassNode source = ((Map<String, ClassNode>) handle.get()).get("zombie/CombatManager");
        assertNotNull(source);
        MethodNode attack = source.methods.stream()
                .filter(method -> method.name.equals("attackCollisionCheck")).findFirst().orElseThrow();
        AbstractInsnNode anchor = null;
        AbstractInsnNode start = null;
        MethodInsnNode filter = null;
        for (AbstractInsnNode instruction : attack.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("modcore/core/CoreMain")) {
                if (start == null) start = instruction;
            }
            if (instruction instanceof MethodInsnNode call && call.owner.equals("modcore/core/HitPacketFilter")) {
                filter = call;
            }
            if (filter != null && instruction instanceof FieldInsnNode field && field.name.equals("PlayerHit")) {
                anchor = instruction;
                break;
            }
        }
        assertNotNull(start);
        assertNotNull(filter);
        assertNotNull(anchor);
        AbstractInsnNode load = filter.getPrevious();
        while (load.getOpcode() < 0) load = load.getPrevious();
        int listSlot = ((VarInsnNode) load).var;
        MethodInsnNode send = null;
        for (AbstractInsnNode instruction = anchor; instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("zombie/network/packets/INetworkPacket")
                    && call.name.equals("send")) {
                send = call;
                break;
            }
        }
        assertNotNull(send);
        Map<LabelNode, LabelNode> labels = new HashMap<>();
        for (AbstractInsnNode instruction : attack.instructions) {
            if (instruction instanceof LabelNode label) labels.put(label, new LabelNode());
        }
        MethodNode gate = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run",
                "(Lzombie/inventory/types/HandWeapon;Ljava/util/List;)V", null, null);
        for (AbstractInsnNode instruction = start; instruction != anchor; instruction = instruction.getNext()) {
            AbstractInsnNode copy = instruction.clone(labels);
            if (copy instanceof VarInsnNode variable) {
                variable.var = variable.var == listSlot ? 1 : variable.var == 2 ? 0 : variable.var;
            }
            gate.instructions.add(copy);
        }
        gate.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                BytecodeFixture.owner(Network.class), "report", "()V", false));
        AbstractInsnNode afterSend = send.getNext();
        while (afterSend != null && afterSend.getOpcode() < 0) {
            gate.instructions.add(afterSend.clone(labels));
            afterSend = afterSend.getNext();
        }
        gate.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassNode fixture = BytecodeFixture.type("modcore/agent/CombatPacketGateFixture");
        fixture.methods.add(BytecodeFixture.copy(gate, Map.of(
                "modcore/core/CoreMain", BytecodeFixture.owner(Main.class),
                "modcore/core/CoreAPI", BytecodeFixture.owner(Api.class),
                "modcore/core/HitPacketFilter", BytecodeFixture.owner(Filter.class),
                "zombie/inventory/types/HandWeapon", BytecodeFixture.owner(Weapon.class))));
        return BytecodeFixture.define(fixture).getMethod("run", Weapon.class, List.class);
    }

    public static final class Main {
        public static Main instance;
        public Api CoreAPI;
        public static Main getInstance() { return instance; }
    }

    public static final class Api {
        public boolean isSuperMultiHit;
    }

    public static final class Weapon {
        private final boolean firearm;
        Weapon(boolean firearm) { this.firearm = firearm; }
        public boolean isAimedFirearm() { return firearm; }
        public boolean isRanged() { return firearm; }
    }

    public static final class Filter {
        public static int calls;
        public static boolean remaining;
        public static boolean filter(List<?> hits) {
            calls++;
            return remaining;
        }
    }

    public static final class Network {
        public static int sent;
        public static void report() { sent++; }
    }
}
