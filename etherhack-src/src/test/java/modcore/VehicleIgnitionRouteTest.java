package modcore;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.FieldNode;
import se.krka.kahlua.vm.KahluaTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The server accepts any key in the ignition; verify the client only uses that vanilla path. */
public class VehicleIgnitionRouteTest {
    @Test
    void anyHeldKeyIsInsertedAndTheServerStartCommandFollows() throws Exception {
        Class<?> api = runtime();
        Player.instance.vehicle = new Vehicle();
        Player.instance.inventory.items.add(new Key(4242));
        assertEquals("sent", api.getMethod("vehicleIgnitionStart").invoke(null));
        assertEquals(2, Calls.commands.size());
        assertEquals("putKeyInIgnition", Calls.commands.get(0));
        assertSame(Player.instance.inventory.items.get(0), Calls.values.get(0));
        assertEquals(-1, Calls.values.get(1));
        assertEquals("startEngine", Calls.commands.get(1));
    }

    @Test
    void aKeyInsideAKeyRingIsMovedWithItsRingIdInsteadOfBeingCopied() throws Exception {
        Class<?> api = runtime();
        Player.instance.vehicle = new Vehicle();
        Ring ring = new Ring();
        ring.inventory.items.add(new Key(77));
        Player.instance.inventory.items.add(ring);
        assertEquals("sent", api.getMethod("vehicleIgnitionStart").invoke(null));
        assertSame(ring.inventory.items.get(0), Calls.values.get(0));
        assertEquals(9001, Calls.values.get(1), "the ring id lets the server remove the key from the ring");
    }

    @Test
    void missingKeyOrWrongSeatSendsNothing() throws Exception {
        Class<?> api = runtime();
        Player.instance.vehicle = new Vehicle();
        assertEquals("no-key", api.getMethod("vehicleIgnitionStart").invoke(null));
        assertTrue(Calls.commands.isEmpty());
        Player.instance.inventory.items.add(new Key(1));
        Player.instance.vehicle = null;
        assertEquals("no-driver", api.getMethod("vehicleIgnitionStart").invoke(null));
        assertTrue(Calls.commands.isEmpty());
    }

    @Test
    void singlePlayerSendsNothing() throws Exception {
        Class<?> api = runtime();
        Client.client = false;
        Player.instance.vehicle = new Vehicle();
        Player.instance.inventory.items.add(new Key(1));
        assertEquals("not-mp", api.getMethod("vehicleIgnitionStart").invoke(null));
        assertTrue(Calls.commands.isEmpty());
    }

    private static Class<?> runtime() throws Exception {
        Client.client = true;
        Calls.commands.clear();
        Calls.values.clear();
        Player.instance = new Player();
        Player.instance.inventory = new Inventory();
        Map<String, String> names = Map.ofEntries(
                Map.entry("modcore/core/VehicleIgnitionAPI", "modcore/fixture/VehicleIgnitionRoute"),
                Map.entry("modcore/utils/Logger", BytecodeFixture.owner(Log.class)),
                Map.entry("zombie/characters/IsoPlayer", BytecodeFixture.owner(Player.class)),
                Map.entry("zombie/characters/IsoGameCharacter", BytecodeFixture.owner(Player.class)),
                Map.entry("zombie/inventory/InventoryItem", BytecodeFixture.owner(Item.class)),
                Map.entry("zombie/inventory/ItemContainer", BytecodeFixture.owner(Inventory.class)),
                Map.entry("zombie/inventory/types/Key", BytecodeFixture.owner(Key.class)),
                Map.entry("zombie/inventory/types/InventoryContainer", BytecodeFixture.owner(Ring.class)),
                Map.entry("zombie/network/GameClient", BytecodeFixture.owner(Client.class)),
                Map.entry("zombie/vehicles/BaseVehicle", BytecodeFixture.owner(Vehicle.class)));
        var source = BytecodeFixture.read("modcore/core/VehicleIgnitionAPI");
        var target = BytecodeFixture.type("modcore/fixture/VehicleIgnitionRoute");
        target.methods.clear();
        for (FieldNode field : source.fields) {
            String descriptor = field.desc;
            for (var entry : names.entrySet()) descriptor = descriptor.replace("L" + entry.getKey() + ";", "L" + entry.getValue() + ";");
            target.fields.add(new FieldNode(field.access, field.name, descriptor, null, field.value));
        }
        for (var method : source.methods) target.methods.add(BytecodeFixture.copy(method, names));
        return BytecodeFixture.define(target);
    }

    public static class Client {
        public static boolean client;
        public static final Client instance = new Client();
        public void sendClientCommandV(Player player, String module, String command, Object... values) {
            Calls.commands.add(command);
            for (int i = 0; i < values.length; i += 2) {
                if ("key".equals(values[i])) Calls.values.add(values[i + 1]);
                if ("container".equals(values[i])) Calls.values.add(((Number) values[i + 1]).intValue());
            }
        }
        public void sendClientCommand(Player player, String module, String command, KahluaTable args) {
            Calls.commands.add(command);
        }
    }
    public static class Calls {
        public static final List<String> commands = new ArrayList<>();
        public static final List<Object> values = new ArrayList<>();
    }
    public static class Log {
        public static void printLog(String message) {}
        public static void error(String message, Throwable error) { throw new AssertionError(message, error); }
    }
    public static class Item {}
    public static class Key extends Item {
        public final int id;
        public Key(int id) { this.id = id; }
    }
    public static class Inventory {
        public final ArrayList<Item> items = new ArrayList<>();
        public ArrayList<Item> getItems() { return items; }
    }
    public static class Ring extends Item {
        public final Inventory inventory = new Inventory();
        public int getID() { return 9001; }
        public String getType() { return "KeyRing"; }
        public Inventory getInventory() { return inventory; }
    }
    public static class Vehicle {
        public boolean isDriver(Player player) { return true; }
    }
    public static class Player {
        public static Player instance;
        public Vehicle vehicle;
        public Inventory inventory;
        public static Player getInstance() { return instance; }
        public Vehicle getVehicle() { return vehicle; }
        public Inventory getInventory() { return inventory; }
    }
}
