package modcore;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Executes the production cycle with world, clock and network boundaries replaced. */
public class WornSpawnFlowTest {
    @Test
    void wearRequestPassesBothThePlayerAndTheClothingToTheConstructor() throws Exception {
        Class<?> api = runtime();
        assertEquals("started", start(api));
        assertSame(Player.instance, Actions.action.rawget("character"));
        assertSame(Player.instance.trigger, Actions.action.rawget("item"));
        assertSame(Player.instance, Actions.owner);
        assertTrue(Player.instance.food.getActualWeight() >= 60.0f);
    }

    @Test
    void failedSendRestoresFoodAndRemovesOnlyTheCyclesGhost() throws Exception {
        Class<?> api = runtime();
        Actions.failSend = true;
        Item hat = wearHat();
        assertEquals("send-failed", api.getMethod("wornSpawnStart", String.class).invoke(null, "Base.Target"));
        assertNull(Player.instance.getWornItem(Location.TSHIRT));
        assertSame(hat, Player.instance.getWornItem(Location.HAT));
        assertEquals(0.2f, Player.instance.food.getActualWeight(), 0.001f);
        assertEquals("idle", api.getMethod("wornSpawnStatus").invoke(null));
    }

    @Test
    void timeoutRemovesTheGhostAndAllowsTheSameClothingToBeUsedAgain() throws Exception {
        Class<?> api = runtime();
        assertEquals("started", start(api));
        Clock.now += 6001;
        assertEquals("timeout", poll(api));
        assertNull(Player.instance.getWornItem(Location.TSHIRT));
        assertEquals(0.2f, Player.instance.food.getActualWeight(), 0.001f);
        assertEquals(1, Packets.sent.stream().filter(packet -> packet.type == PacketType.GeneralAction).count());
        assertEquals("started", start(api));
    }

    @Test
    void aChangedWornSlotAndDoneReceiptDoNotProveAnItemDropped() throws Exception {
        Class<?> api = runtime();
        assertEquals("started", start(api));
        Receipt.value = 1;
        Player.instance.worn.setItem(Location.TSHIRT, Player.instance.trigger);
        assertEquals("waiting-drop", poll(api));
        assertEquals(0.2f, Player.instance.food.getActualWeight(), 0.001f);
        Clock.now += 6001;
        assertEquals("timeout", poll(api));
        assertSame(Player.instance.trigger, Player.instance.getWornItem(Location.TSHIRT));
    }

    @Test
    void completionNeedsTheMatchingGroundItemAndTheRealClothing() throws Exception {
        Class<?> api = runtime();
        assertEquals("started", start(api));
        Item ghost = Player.instance.getWornItem(Location.TSHIRT);
        Receipt.value = 1;
        Player.instance.square.world.add(new WorldItem(new Item("Base.Target", ghost.id + 1, null)));
        Player.instance.square.world.add(new WorldItem(new Item("Base.Other", ghost.id, null)));
        Player.instance.worn.setItem(Location.TSHIRT, Player.instance.trigger);
        assertEquals("waiting-drop", poll(api));
        Player.instance.square.world.add(new WorldItem(new Item("Base.Target", ghost.id, null)));
        assertEquals("done", poll(api));
        assertEquals(0.2f, Player.instance.food.getActualWeight(), 0.001f);
        assertEquals(0, Packets.sent.stream().filter(packet -> packet.type == PacketType.GeneralAction).count());
    }

    @Test
    void aGroundUpdateCanArriveBeforeTheWearUpdateWithoutBeingRemoved() throws Exception {
        Class<?> api = runtime();
        assertEquals("started", start(api));
        Item ghost = Player.instance.getWornItem(Location.TSHIRT);
        Receipt.value = 1;
        Player.instance.square.world.add(new WorldItem(new Item("Base.Target", ghost.id, null)));
        assertEquals("waiting-drop", poll(api));
        Player.instance.worn.setItem(Location.TSHIRT, Player.instance.trigger);
        assertEquals("done", poll(api));
        assertEquals(1, Player.instance.square.world.size());
    }

    @Test
    void rejectedActionRestoresWeightWithoutRemovingTheRealClothing() throws Exception {
        Class<?> api = runtime();
        assertEquals("started", start(api));
        Player.instance.worn.setItem(Location.TSHIRT, Player.instance.trigger);
        Receipt.value = 2;
        assertEquals("rejected", poll(api));
        assertSame(Player.instance.trigger, Player.instance.getWornItem(Location.TSHIRT));
        assertEquals(0.2f, Player.instance.food.getActualWeight(), 0.001f);
    }

    @Test
    void completionCanBeConfirmedAfterThePlayerMovesToAnAdjacentSquare() throws Exception {
        Class<?> api = runtime();
        assertEquals("started", start(api));
        Item ghost = Player.instance.getWornItem(Location.TSHIRT);
        Player.instance.square = new Square(11, 20);
        Receipt.value = 1;
        Player.instance.worn.setItem(Location.TSHIRT, Player.instance.trigger);
        Player.instance.square.world.add(new WorldItem(new Item("Base.Target", ghost.id, null)));
        assertEquals("done", poll(api));
    }

    @Test
    void anExclusiveWornItemIsNotSilentlyRemovedByTheGhost() throws Exception {
        Class<?> api = runtime();
        Item hat = wearHat();
        Player.instance.worn.group.exclusive = true;
        assertEquals("no-cloth", api.getMethod("wornSpawnStart", String.class).invoke(null, "Base.Target"));
        assertSame(hat, Player.instance.getWornItem(Location.HAT));
        assertEquals(0.2f, Player.instance.food.getActualWeight(), 0.001f);
        assertTrue(Packets.sent.isEmpty());
    }

    @Test
    void aConnectionThatIsNotReadyIsTreatedAsMultiplayerOnly() throws Exception {
        Class<?> api = runtime();
        Client.connection.ready = false;
        assertEquals("not-mp", api.getMethod("wornSpawnStart", String.class).invoke(null, "Base.Target"));
        assertTrue(Packets.sent.isEmpty(), "nothing may be sent before the client connection is ready");
    }

    @Test
    void cancellingTwiceDoesNotRepeatNetworkCleanupOrRemoveOtherClothing() throws Exception {
        Class<?> api = runtime();
        Item hat = wearHat();
        assertEquals("started", start(api));
        assertEquals("restored", api.getMethod("wornSpawnRestore").invoke(null));
        int sent = Packets.sent.size();
        assertEquals("restored", api.getMethod("wornSpawnRestore").invoke(null));
        assertEquals(sent, Packets.sent.size());
        assertNull(Player.instance.getWornItem(Location.TSHIRT));
        assertSame(hat, Player.instance.getWornItem(Location.HAT));
        assertEquals(0.2f, Player.instance.food.getActualWeight(), 0.001f);
    }

    @Test
    void disconnectRestoresLocalWeightWithoutSendingIntoAnotherSession() throws Exception {
        Class<?> api = runtime();
        assertEquals("started", start(api));
        Player previous = Player.instance;
        int sent = Packets.sent.size();
        Player.instance = new Player();
        Client.connection = new Connection();
        assertEquals("no-player", poll(api));
        assertEquals(sent, Packets.sent.size());
        assertEquals(0.2f, previous.food.getActualWeight(), 0.001f);
        assertNull(previous.getWornItem(Location.TSHIRT));
        assertEquals(0.2f, Player.instance.food.getActualWeight(), 0.001f);
    }

    private static Object start(Class<?> api) throws Exception {
        Log.lines.clear();
        Object status = api.getMethod("wornSpawnStart", String.class).invoke(null, "Base.Target");
        if (!"started".equals(status)) {
            throw new AssertionError("wornSpawnStart returned " + status + "; log=" + Log.lines);
        }
        return status;
    }

    private static Object poll(Class<?> api) throws Exception {
        return api.getMethod("wornSpawnPoll").invoke(null);
    }

    private static Item wearHat() {
        Item hat = new Clothing("Base.Hat", 103, Location.HAT);
        Player.instance.inventory.add(hat);
        Player.instance.worn.setItem(Location.HAT, hat);
        return hat;
    }

    private static Class<?> runtime() throws Exception {
        Player.instance = new Player();
        Client.client = true;
        Client.connection = new Connection();
        Clock.now = 1000;
        Packets.sent.clear();
        Actions.action = null;
        Actions.owner = null;
        Actions.failSend = false;
        Receipt.value = 0;
        Lua.reset();
        Map<String, String> names = Map.ofEntries(
                Map.entry("modcore/core/WornSpawnAPI", "modcore/fixture/WornSpawnFlow"),
                Map.entry("modcore/core/TimedActionReceipt", BytecodeFixture.owner(Receipt.class)),
                Map.entry("modcore/utils/Logger", BytecodeFixture.owner(Log.class)),
                Map.entry("java/lang/System", BytecodeFixture.owner(Clock.class)),
                Map.entry("zombie/characters/IsoPlayer", BytecodeFixture.owner(Player.class)),
                Map.entry("zombie/characters/WornItems/WornItems", BytecodeFixture.owner(WornItems.class)),
                Map.entry("zombie/characters/WornItems/BodyLocationGroup", BytecodeFixture.owner(Group.class)),
                Map.entry("zombie/characters/WornItems/BodyLocation", BytecodeFixture.owner(Location.class)),
                Map.entry("zombie/inventory/InventoryItem", BytecodeFixture.owner(Item.class)),
                Map.entry("zombie/inventory/ItemContainer", BytecodeFixture.owner(Inventory.class)),
                Map.entry("zombie/inventory/types/Clothing", BytecodeFixture.owner(Clothing.class)),
                Map.entry("zombie/inventory/types/Food", BytecodeFixture.owner(Food.class)),
                Map.entry("zombie/inventory/InventoryItemFactory", BytecodeFixture.owner(Factory.class)),
                Map.entry("zombie/scripting/ScriptManager", BytecodeFixture.owner(Scripts.class)),
                Map.entry("zombie/scripting/objects/Item", BytecodeFixture.owner(Script.class)),
                Map.entry("zombie/scripting/objects/ItemBodyLocation", BytecodeFixture.owner(Location.class)),
                Map.entry("zombie/network/GameClient", BytecodeFixture.owner(Client.class)),
                Map.entry("zombie/core/raknet/UdpConnection", BytecodeFixture.owner(Connection.class)),
                Map.entry("zombie/network/PacketTypes$PacketType", BytecodeFixture.owner(PacketType.class)),
                Map.entry("zombie/network/packets/INetworkPacket", BytecodeFixture.owner(Packets.class)),
                Map.entry("zombie/core/ActionManager", BytecodeFixture.owner(Actions.class)),
                Map.entry("zombie/Lua/LuaManager", BytecodeFixture.owner(Lua.class)),
                Map.entry("zombie/iso/IsoGridSquare", BytecodeFixture.owner(Square.class)),
                Map.entry("zombie/iso/IsoCell", BytecodeFixture.owner(Cell.class)),
                Map.entry("zombie/iso/objects/IsoWorldInventoryObject", BytecodeFixture.owner(WorldItem.class)));
        ClassNode source = BytecodeFixture.read("modcore/core/WornSpawnAPI");
        ClassNode target = BytecodeFixture.type("modcore/fixture/WornSpawnFlow");
        target.methods.clear();
        for (FieldNode field : source.fields) {
            String descriptor = field.desc;
            for (var entry : names.entrySet()) {
                descriptor = descriptor.replace("L" + entry.getKey() + ";", "L" + entry.getValue() + ";");
            }
            target.fields.add(new FieldNode(field.access, field.name, descriptor, null, field.value));
        }
        for (var method : source.methods) target.methods.add(BytecodeFixture.copy(method, names));
        return BytecodeFixture.define(target);
    }

    public enum Location { TSHIRT, HAT }
    public enum PacketType { ItemStats, SyncClothing, GeneralAction }
    public static class Connection {
        /** 客户端就绪标志；isFullyConnected 只在服务端置位，故意不提供。 */
        public boolean ready = true;
        public boolean isReady() { return ready; }
    }
    public static class Client {
        public static boolean client;
        public static Connection connection;
    }
    public static class Clock {
        public static long now;
        public static long currentTimeMillis() { return now; }
        public static long nanoTime() { return now * 1_000_000; }
    }
    public static class Log {
        public static final List<String> lines = new ArrayList<>();
        public static void printLog(String line) { lines.add(line); }
    }
    public static class Item {
        public final String type;
        public int id;
        public final Location location;
        public Inventory container;
        public Item(String type, int id, Location location) {
            this.type = type;
            this.id = id;
            this.location = location;
        }
        public String getFullType() { return type; }
        public int getID() { return id; }
        public void setID(int id) { this.id = id; }
        public Location getBodyLocation() { return location; }
        public Inventory getContainer() { return container; }
    }
    public static class Clothing extends Item {
        public Clothing(String type, int id, Location location) { super(type, id, location); }
    }
    public static class Food extends Item {
        public float hunger = -0.1f;
        public float thirst;
        public float weight = 0.2f;
        public Food() { super("Base.Food", 102, null); }
        public float getHungChange() { return hunger; }
        public float getThirstChange() { return thirst; }
        public float getActualWeightUnmodded() { return weight; }
        public float getActualWeight() { return 0.2f * hunger * 100.0f / -10.0f; }
        public void setHungChange(float value) { hunger = value; }
        public void setThirstChange(float value) { thirst = value; }
        public void setActualWeight(float value) { weight = value; }
        public Script getScriptItem() { return new Script(); }
        public String getReplaceOnUseFullType() { return null; }
    }
    public static class Script {
        public float getHungerChange() { return -10.0f; }
        public float getThirstChange() { return 0.0f; }
        public float getActualWeight() { return 0.2f; }
    }
    public static class Scripts {
        public static final Scripts instance = new Scripts();
        public Script FindItem(String name) { return null; }
    }
    public static class Factory {
        public static Item CreateItem(String name) { return new Item(name, 0, null); }
    }
    public static class Inventory {
        public final ArrayList<Item> items = new ArrayList<>();
        public void add(Item item) { items.add(item); item.container = this; }
        public ArrayList<Item> getItems() { return items; }
        public Item getItemWithID(int id) {
            return items.stream().filter(item -> item.id == id).findFirst().orElse(null);
        }
        public Item getItemWithIDRecursiv(int id) { return getItemWithID(id); }
    }
    public static class Group {
        public boolean exclusive;
        /** 未登记部位返回 null；与游戏里 BodyLocationGroup.getLocation 的语义一致。 */
        public Location getLocation(Location location) { return location; }
        public boolean isExclusive(Location first, Location second) { return exclusive && first != second; }
    }
    public static class WornItems {
        public final Group group = new Group();
        public final Map<Location, Item> items = new EnumMap<>(Location.class);
        public Group getBodyLocationGroup() { return group; }
        public int size() { return items.size(); }
        public Item getItemByIndex(int index) { return new ArrayList<>(items.values()).get(index); }
        public Location getLocation(Item item) { return item.location; }
        public void setItem(Location location, Item item) { items.put(location, item); }
        public Item getItemById(int id) {
            return items.values().stream().filter(item -> item.id == id).findFirst().orElse(null);
        }
        public void remove(Item item) { items.values().removeIf(value -> value == item); }
    }
    public static class Player {
        public static Player instance;
        public final Inventory inventory = new Inventory();
        public final WornItems worn = new WornItems();
        public final Item trigger = new Clothing("Base.Tshirt", 101, Location.TSHIRT);
        public final Food food = new Food();
        public Square square = new Square();
        public Player() { inventory.add(trigger); inventory.add(food); }
        public static Player getInstance() { return instance; }
        public Inventory getInventory() { return inventory; }
        public WornItems getWornItems() { return worn; }
        public Item getWornItem(Location location) { return worn.items.get(location); }
        public Square getCurrentSquare() { return square; }
    }
    public static class Square {
        public final ArrayList<WorldItem> world = new ArrayList<>();
        public final Cell cell = new Cell(this);
        public final int x;
        public final int y;
        public Square() { this(10, 20); }
        public Square(int x, int y) { this.x = x; this.y = y; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return 0; }
        public Cell getCell() { return cell; }
        public ArrayList<WorldItem> getWorldObjects() { return world; }
    }
    public static class Cell {
        public final Square square;
        public Cell(Square square) { this.square = square; }
        public Square getGridSquare(int x, int y, int z) {
            return x == square.x && y == square.y && z == 0 ? square : null;
        }
    }
    public static class WorldItem {
        public final Item item;
        public WorldItem(Item item) { this.item = item; }
        public Item getItem() { return item; }
    }
    public static class Receipt {
        public static int value;
        public static int state(KahluaTable action) { return action == Actions.action ? value : 3; }
        public static void remove(KahluaTable action) {
            if (Actions.action == action) Actions.action = null;
        }
    }
    public static class Actions {
        public static KahluaTable action;
        public static Player owner;
        public static boolean failSend;
        public static Actions getInstance() { return new Actions(); }
        public byte createNetTimedAction(Player player, KahluaTable table) {
            action = table;
            owner = player;
            return failSend ? 0 : (byte)7;
        }
        public static void remove(Player player, byte id) { action = null; }
    }
    public static class Lua {
        public static final LuaCaller caller = new LuaCaller(new KahluaConverterManager());
        public static KahluaThread thread;
        public static KahluaTable actionClass;
        public static Object constructor;
        public static void reset() {
            var platform = J2SEPlatform.getInstance();
            thread = new KahluaThread(platform, platform.newTable());
            thread.debugOwnerThread = Thread.currentThread();
            actionClass = platform.newTable();
            constructor = (JavaFunction) (frame, count) -> {
                KahluaTable action = platform.newTable();
                action.setMetatable(actionClass);
                action.rawset("character", count > 1 ? frame.get(1) : null);
                action.rawset("item", count > 2 ? frame.get(2) : null);
                return frame.push(action);
            };
        }
        public static Object get(Object name) { return actionClass; }
        public static Object getFunctionObject(String name) { return constructor; }
    }
    public record SentPacket(PacketType type, List<Object> values) {}
    public interface Packets {
        List<SentPacket> sent = new ArrayList<>();
        static void send(PacketType type, Object... values) {
            sent.add(new SentPacket(type, List.of(values.clone())));
        }
    }
}
