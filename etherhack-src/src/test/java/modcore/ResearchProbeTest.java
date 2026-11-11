package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2026-11-11 研判探针: 僵尸挂载单发探针 + 搜寻池只读扫描。
 * 这些测试只验证客户端 Lua 探针的边界与记录逻辑; 服务端归属语义必须实机验证。
 */
class ResearchProbeTest {

    @Test
    void attachProbeSendsTheFirstInventoryItemToTheNearestZombie() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherResearchProbe.attachNearest()
                local rec = EtherResearchProbe.attach
                return EtherResearchProbe.message, rec.itemId, rec.zombieId, rec.location,
                    attachCalls, attachItem == probeItem, attachZombie == probeZombie, attachLocation
                """);
        assertArrayEquals(new Object[]{"UI_ResearchProbe_AttachSent", 42.0, 7.0, "Belt Right",
                1.0, true, true, "Belt Right"}, result,
                "the probe sends one forged attached-item packet and records the receipt");
    }

    @Test
    void attachProbeRefusesToRunWithoutAZombieOrMultiplayer() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.execute("zombies = {}");
        Object[] noZombie = lua.execute("""
                EtherResearchProbe.attachNearest()
                return EtherResearchProbe.message, EtherResearchProbe.attach, attachCalls
                """);
        assertArrayEquals(new Object[]{"UI_ResearchProbe_NoZombie", null, 0.0}, noZombie,
                "no nearby zombie means nothing is sent");

        LuaTestRuntime lua2 = runtime();
        lua2.execute("multiplayerEnabled = false");
        Object[] notMp = lua2.execute("""
                EtherResearchProbe.attachNearest()
                return EtherResearchProbe.message, EtherResearchProbe.attach, attachCalls
                """);
        assertArrayEquals(new Object[]{"UI_ResearchProbe_NeedMP", null, 0.0}, notMp,
                "the attach probe is multiplayer-only");
    }

    @Test
    void checkProbeReportsBothSidesOfTheAttachedItem() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.execute("""
                EtherResearchProbe.attachNearest()
                corpseContainer.findId = 42
                EtherResearchProbe.checkAttached()
                """);
        Object[] result = lua.execute("""
                return EtherResearchProbe.message, lastCheck.invHas, lastCheck.invSameType,
                    lastCheck.corpses, lastCheck.corpseHits
                """);
        assertArrayEquals(new Object[]{"UI_ResearchProbe_VerdictDup", true, 1.0, 1.0, 1.0}, result,
                "item still in our inventory and found in a corpse must be reported as a duplicate reference");
    }

    @Test
    void checkProbeReportsACorpseOnlyHitAsAMove() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.execute("""
                EtherResearchProbe.attachNearest()
                invItems = {}
                corpseContainer.findId = 42
                EtherResearchProbe.checkAttached()
                """);
        Object[] result = lua.execute("""
                return EtherResearchProbe.message, lastCheck.invHas, lastCheck.corpseHits
                """);
        assertArrayEquals(new Object[]{"UI_ResearchProbe_VerdictMove", false, 1.0}, result,
                "if our inventory no longer has the item, the corpse hit reads as a move");
    }

    @Test
    void checkProbeRefusesToRunWithoutAPriorAttach() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherResearchProbe.checkAttached()
                return EtherResearchProbe.message, lastCheck
                """);
        assertArrayEquals(new Object[]{"UI_ResearchProbe_NoAttach", null}, result,
                "the check needs a prior attach receipt");
    }

    @Test
    void poolScanSummarisesTheServerSentForageRecords() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherResearchProbe.scanPool()
                return EtherResearchProbe.message, EtherResearchProbe.lastScan.total,
                    EtherResearchProbe.lastScan.rare, EtherResearchProbe.lastScan.printed
                """);
        assertArrayEquals(new Object[]{"UI_ResearchProbe_PoolResult", 2.0, 1.0, 2.0}, result,
                "the scan reads the client copy of the server pool; Ammunition/JunkWeapons count as rare");
    }

    @Test
    void poolScanReportsAnEmptyPoolWithoutTouchingTheNetwork() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.execute("poolIcons = {}");
        Object[] result = lua.execute("""
                EtherResearchProbe.scanPool()
                return EtherResearchProbe.message, EtherResearchProbe.lastScan
                """);
        assertArrayEquals(new Object[]{"UI_ResearchProbe_NoPool", null}, result,
                "an empty pool is reported instead of printing nothing");
    }

    /** 游戏集合是 Java List (size/get 0 基); 替身必须同形。 */
    private static String listStub() {
        return """
                function makeList(arr)
                    local list = {}
                    function list:size() return #arr end
                    function list:get(i) return arr[i + 1] end
                    return list
                end
                """;
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute(listStub() + """
                multiplayerEnabled = true
                function isClient() return true end
                function isMultiplayer() return multiplayerEnabled end
                attachCalls = 0
                attachItem = nil
                attachZombie = nil
                attachLocation = nil
                function zombieAttachSend(zombie, item, location)
                    attachCalls = attachCalls + 1
                    attachItem = item
                    attachZombie = zombie
                    attachLocation = location
                    return "sent"
                end
                AttachedLocations = { getGroup = function(name)
                    return { getLocation = function(self, id) return { id = id } end }
                end }
                probeItem = {
                    id = 42,
                    getID = function(self) return self.id end,
                    getFullType = function() return "Base.Rock" end,
                    getDisplayName = function() return "Rock" end,
                }
                plankItem = { getFullType = function() return "Base.Plank" end }
                invItems = { probeItem, plankItem }
                player.getX = function() return 0 end
                player.getY = function() return 0 end
                player.getZ = function() return 0 end
                player.getInventory = function()
                    return {
                        getItems = function() return makeList(invItems) end,
                        getItemById = function(self, id)
                            for _, it in ipairs(invItems) do
                                if it.getID ~= nil and it.getID(it) == id then return it end
                            end
                            return nil
                        end,
                    }
                end
                probeZombie = {
                    dead = false,
                    x = 5, y = 5, onlineId = 7,
                    isDead = function(self) return self.dead end,
                    getX = function(self) return self.x end,
                    getY = function(self) return self.y end,
                    getOnlineID = function(self) return self.onlineId end,
                }
                zombies = { probeZombie }
                corpseContainer = { findId = nil }
                corpseBody = { getItemContainer = function() return corpseContainer end }
                function corpseContainer:getItemWithID(id)
                    if self.findId == id then return probeItem end
                    return nil
                end
                function corpseContainer:getItems() return makeList({}) end
                corpseSquare = { getDeadBodys = function() return makeList({ corpseBody }) end }
                poolIcons = {
                    axe = { itemType = "Base.Axe", catName = "JunkWeapons", count = 1, x = 10, y = 10 },
                    rock = { itemType = "Base.Rock", catName = "Stones", count = 2, x = 3, y = 4 },
                }
                ISSearchManager = { getManager = function()
                    return { forageIcons = poolIcons }
                end }
                lastCheck = nil
                function getCell()
                    return {
                        getZombieList = function() return makeList(zombies) end,
                        getGridSquare = function(self, x, y, z)
                            if x == 0 and y == 0 then return corpseSquare end
                            return nil
                        end,
                    }
                end
                """);
        lua.resource("modcore/lua/components/ui/EtherResearchProbe.lua");
        return lua;
    }
}
