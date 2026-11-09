package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SpawnQueueTest {
    @Test
    void unsupportedModesDoNotStartOrChangePendingRows() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherSpawnQueue.add("Base.Ham", "Ham", 2)
                local a = EtherSpawnQueue.start("take")
                local b = EtherSpawnQueue.start("unknown")
                local c = EtherSpawnQueue.start("poison")
                return a, b, c, EtherSpawnQueue.running,
                    EtherSpawnQueue.rows[1].status, EtherSpawnQueue.mode == nil, #sent
                """);
        assertArrayEquals(new Object[]{false, false, false, false, "pending", true, 0.0}, result);
    }

    @Test
    void fishingQueueWaitsForInventoryGrowthBeforeDispatchingTheNextItem() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherSpawnQueue.add("Base.Ham", "Ham", 2)
                EtherSpawnQueue.add("Base.Apple", "Apple", 1)
                EtherSpawnQueue.start("fish")
                inventory["Base.Ham"] = 1
                now = 500; EtherSpawnQueue.tick()
                now = 1499; EtherSpawnQueue.tick()
                local beforeNext = #sent
                now = 1500; EtherSpawnQueue.tick()
                inventory["Base.Apple"] = 1
                now = 2000; EtherSpawnQueue.tick()
                now = 3000; EtherSpawnQueue.tick()
                return beforeNext, #sent, sent[1], sent[2],
                    EtherSpawnQueue.running, EtherSpawnQueue.count()
                """);
        assertArrayEquals(new Object[]{1.0, 2.0, "Base.Ham", "Base.Apple", false, 0.0}, result);
    }

    @Test
    void stoppingTheQueuePreventsFurtherDispatch() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherSpawnQueue.add("Base.Ham", "Ham", 1)
                EtherSpawnQueue.add("Base.Apple", "Apple", 1)
                EtherSpawnQueue.start("fish")
                EtherSpawnQueue.stop()
                now = 90000; EtherSpawnQueue.tick()
                return #sent, EtherSpawnQueue.running, EtherSpawnQueue.rows[1].status,
                    EtherSpawnQueue.rows[2].status
                """);
        assertArrayEquals(new Object[]{1.0, false, "pending", "pending"}, result);
    }

    @Test
    void corpseBatchStillUsesAllPendingEntriesOnce() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherSpawnQueue.add("Base.Ham", "Ham", 2)
                EtherSpawnQueue.add("Base.Apple", "Apple", 3)
                EtherSpawnQueue.start("corpse")
                return #batches, batches[1], EtherSpawnQueue.running, EtherSpawnQueue.count()
                """);
        assertArrayEquals(new Object[]{1.0, "Base.Ham=2,Base.Apple=3", false, 0.0}, result);
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                function require() end
                function print() end
                function getTranslate(key) return key end
                function tr(key) return key end
                function isMultiplayer() return true end
                now = 0; sent = {}; batches = {}; inventory = {}
                function getTimestampMs() return now end
                local player = { getInventory = function()
                    return { getItemCount = function(self, id) return inventory[id] or 0 end }
                end }
                function getPlayer() return player end
                ISPanel = { derive = function() return {} end }
                Events = { OnTick = { Add = function() end } }
                EtherFishSpawn = { busy = false, epoch = 0, trigger = function(id)
                    table.insert(sent, id)
                    EtherFishSpawn.busy = true
                    return true
                end }
                function corpseSpawnPrepareBatch(items)
                    table.insert(batches, items)
                    return true
                end
                """);
        lua.resource("modcore/lua/components/ui/EtherSpawnQueue.lua");
        return lua;
    }
}
