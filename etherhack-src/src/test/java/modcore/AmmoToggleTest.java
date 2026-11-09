package modcore;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AmmoToggleTest {
    @Test
    void enabledAmmoRefillsTheGunAndInventoryMagazine() throws Exception {
        Fixture fixture = fixture();
        fixture.api.getClass().getField("isUnlimitedAmmo").setBoolean(fixture.api, true);
        fixture.update();
        assertEquals(6, fixture.gun.ammo);
        assertTrue(fixture.gun.chambered);
        assertEquals(6, fixture.magazine.ammo);
        assertEquals(2, Network.sent);
    }

    @Test
    void disabledAmmoLeavesConsumedRoundsAndAnEmptyChamberUntouched() throws Exception {
        Fixture fixture = fixture();
        fixture.api.getClass().getField("isUnlimitedAmmo").setBoolean(fixture.api, true);
        fixture.update();
        fixture.api.getClass().getField("isUnlimitedAmmo").setBoolean(fixture.api, false);
        fixture.gun.ammo = 2;
        fixture.gun.chambered = false;
        fixture.magazine.ammo = 1;
        Network.sent = 0;
        fixture.update();
        fixture.update();
        assertEquals(2, fixture.gun.ammo);
        assertFalse(fixture.gun.chambered);
        assertEquals(1, fixture.magazine.ammo);
        assertEquals(0, Network.sent);
    }

    @Test
    void disabledAmmoPreservesTheLastLegitimateChamberedRound() throws Exception {
        Fixture fixture = fixture();
        fixture.gun.chambered = true;
        fixture.update();
        assertEquals(0, fixture.gun.ammo);
        assertTrue(fixture.gun.chambered);
        assertEquals(0, Network.sent);
    }

    private static Fixture fixture() throws Exception {
        Network.sent = 0;
        String owner = "modcore/AmmoToggleFixture";
        Map<String, String> names = Map.of(
                "modcore/core/CoreAPI", owner,
                "zombie/characters/IsoPlayer", BytecodeFixture.owner(Player.class),
                "zombie/inventory/InventoryItem", BytecodeFixture.owner(Item.class),
                "zombie/inventory/types/HandWeapon", BytecodeFixture.owner(Weapon.class),
                "zombie/inventory/ItemContainer", BytecodeFixture.owner(Container.class),
                "zombie/network/PacketTypes$PacketType", BytecodeFixture.owner(PacketType.class),
                "zombie/network/packets/INetworkPacket", BytecodeFixture.owner(Network.class));
        ClassNode source = BytecodeFixture.read("modcore/core/CoreAPI");
        MethodNode update = source.methods.stream().filter(method -> method.name.equals("updateLocalPlayerFeatures"))
                .findFirst().orElseThrow();
        AbstractInsnNode start = null;
        AbstractInsnNode end = null;
        Map<LabelNode, LabelNode> labels = new HashMap<>();
        for (AbstractInsnNode instruction : update.instructions) {
            if (instruction instanceof LabelNode label) labels.put(label, new LabelNode());
            if (instruction instanceof FieldInsnNode field && field.name.equals("isUnlimitedAmmo")) {
                start = instruction.getPrevious();
            }
            if (start != null && instruction instanceof FieldInsnNode field && field.name.equals("isUnlimitedCondition")) {
                end = instruction.getPrevious();
                break;
            }
        }
        assertNotNull(start);
        assertNotNull(end);
        MethodNode tick = new MethodNode(Opcodes.ACC_PUBLIC, "update",
                "(Lzombie/characters/IsoPlayer;)V", null, null);
        for (AbstractInsnNode instruction = start; instruction != end; instruction = instruction.getNext()) {
            AbstractInsnNode copy = instruction.clone(labels);
            if (copy instanceof VarInsnNode variable && variable.var != 0) variable.var = 1;
            tick.instructions.add(copy);
        }
        tick.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassNode executable = BytecodeFixture.type(owner);
        executable.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "isUnlimitedAmmo", "Z", null, null));
        executable.methods.add(BytecodeFixture.copy(tick, names));
        for (String methodName : new String[]{"applyUnlimitedAmmo", "applyUnlimitedAmmoToInventory"}) {
            MethodNode method = source.methods.stream().filter(m -> m.name.equals(methodName)).findFirst().orElseThrow();
            MethodNode copy = BytecodeFixture.copy(method, names);
            for (AbstractInsnNode instruction : copy.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(BytecodeFixture.owner(Network.class))) {
                    call.itf = false;
                }
            }
            executable.methods.add(copy);
        }
        Class<?> type = BytecodeFixture.define(executable);
        Weapon gun = new Weapon();
        Item magazine = new Item();
        Player player = new Player(gun);
        player.inventory.items.add(magazine);
        return new Fixture(type.getConstructor().newInstance(), player, gun, magazine);
    }

    record Fixture(Object api, Player player, Weapon gun, Item magazine) {
        void update() throws Exception { api.getClass().getMethod("update", Player.class).invoke(api, player); }
    }

    public static class Item {
        public int ammo;
        public String getStringItemType() { return "Magazine"; }
        public Container getContainer() { return new Container(); }
        public int getCurrentAmmoCount() { return ammo; }
        public int getMaxAmmo() { return 6; }
        public void setCurrentAmmoCount(int value) { ammo = value; }
    }

    public static final class Weapon extends Item {
        public boolean chambered;
        public String getStringItemType() { return "RangedWeapon"; }
        public boolean haveChamber() { return true; }
        public boolean isRoundChambered() { return chambered; }
        public void setRoundChambered(boolean value) { chambered = value; }
    }

    public static final class Container {
        public final ArrayList<Item> items = new ArrayList<>();
        public ArrayList<Item> getItems() { return items; }
    }

    public static final class Player {
        final Weapon gun;
        final Container inventory = new Container();
        Player(Weapon gun) { this.gun = gun; }
        public Item getPrimaryHandItem() { return gun; }
        public Item getSecondaryHandItem() { return null; }
        public Container getInventory() { return inventory; }
    }

    public enum PacketType { SyncItemFields }

    public static final class Network {
        public static int sent;
        public static void send(PacketType type, Object... values) {
            assertEquals(PacketType.SyncItemFields, type);
            assertEquals(2, values.length);
            sent++;
        }
    }
}
