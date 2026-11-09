package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** 2026-11-09 改: 拆→拉满→装回链路; 没有安装表的部件(引擎等)跳过。 */
class VehicleRepairScopeTest {

    @Test
    void engineWithoutInstallTableIsSkippedAndJobFinishes() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherRepairJob = newJob()
                now = 200
                repairTick()
                now = 400
                repairTick()
                return uninstalls, installs, EtherRepairJob == nil, skipped
                """);
        assertArrayEquals(new Object[]{0.0, 0.0, true, 1.0}, result,
                "a damaged engine has no install table and must be skipped, not uninstalled");
    }

    @Test
    void damagedDoorStartsWithUninstall() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                door.condition = 40
                EtherRepairJob = newJob()
                now = 200
                repairTick()
                return uninstalls, uninstallPart, EtherRepairJob.phase, EtherRepairJob.itemId
                """);
        assertArrayEquals(new Object[]{1.0, "DoorFrontLeft", "uninstall", 77.0}, result,
                "the chain starts by uninstalling the damaged part and keeps its item id");
    }

    @Test
    void installPhaseRestoresItemConditionThenInstalls() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                door.condition = 40
                door.item = nil
                EtherRepairJob = newJob()
                EtherRepairJob.target = door
                EtherRepairJob.itemId = 77
                EtherRepairJob.phase = "install"
                EtherRepairJob.tries = 0
                EtherRepairJob.phaseDeadline = 0
                now = 200
                repairTick()
                return syncs, syncCondition, installs, installPart
                """);
        assertArrayEquals(new Object[]{1.0, 100.0, 1.0, "DoorFrontLeft"}, result,
                "the item condition must be reset to max before the reinstall");
    }

    @Test
    void repairedPartCompletesAndClearsTarget() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                door.condition = 100
                door.item = {}
                EtherRepairJob = newJob()
                EtherRepairJob.target = door
                EtherRepairJob.phase = "install"
                EtherRepairJob.phaseDeadline = 999999
                now = 200
                repairTick()
                return EtherRepairJob.steps, EtherRepairJob.target == nil, uninstalls, installs
                """);
        assertArrayEquals(new Object[]{1.0, true, 0.0, 0.0}, result,
                "a part at 100 with its item back counts as repaired and is not touched again");
    }

    @Test
    void damagedLightbarUsesTheVanillaAction() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                lightbar.condition = 40
                EtherRepairJob = newJob()
                now = 200
                repairTick()
                now = 3000
                repairTick()
                now = 6000
                repairTick()
                return lightbars, lightbarPart
                """);
        assertArrayEquals(new Object[]{1.0, "lightbar"}, result,
                "the lightbar has no install table and must use the vanilla lightbar action");
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("""
                function getText(key) return key end
                Perks = { Mechanics = 1 }
                Events = { OnTick = { Add = function(callback) repairTick = callback end } }
                now = 0
                function getTimestampMs() return now end
                player.getPerkLevel = function() return 0 end
                uninstalls = 0
                installs = 0
                syncs = 0
                lightbars = 0
                skipped = 0
                uninstallPart = nil
                installPart = nil
                syncCondition = nil
                lightbarPart = nil
                local realPrint = print
                print = function(msg)
                    if type(msg) == "string" and string.find(msg, "skip Engine", 1, true) then
                        skipped = skipped + 1
                    end
                end
                HaloTextHelper = {
                    addText = function() end,
                    getColorRed = function() return {} end,
                    getColorGreen = function() return {} end,
                }
                EtherFormPanel = {
                    BOX_PAD_X = 10, BOX_PAD_Y = 3, ROW_STEP = 26, SECTION_GAP = 12, ENTRY_W = 100,
                    derive = function(self) return setmetatable({}, { __index = self }) end,
                    addWidget = function() end, addChild = function() end,
                    addSpacer = function() end, addModule = function() end,
                    _rowContentW = function() return 700 end, _anchor = function() end,
                }
                player.isDead = function() return false end
                player.getVehicle = function() return nil end
                doorItem = { getConditionMax = function() return 100 end }
                player.getInventory = function()
                    return { getItemById = function(self, id)
                        if id == 77 then return doorItem end
                        return nil
                    end }
                end
                token = { getDisplayName = function() return "Token" end }
                local function makePart(id, condition)
                    local part = { id = id, condition = condition, item = nil }
                    function part:getId() return self.id end
                    function part:getCondition() return self.condition end
                    function part:getInventoryItem() return self.item end
                    function part:getVehicle() return vehicle end
                    function part:getTable(name)
                        if self.id == "DoorFrontLeft" or self.id == "DoorRearLeft" then
                            return { skills = "Mechanics:4" }
                        end
                        return nil
                    end
                    function part:getItemContainer()
                        return { getItems = function()
                            return { size = function() return 1 end, get = function() return token end }
                        end }
                    end
                    return part
                end
                door = makePart("DoorFrontLeft", 100)
                door.item = { getID = function() return 77 end }
                engine = makePart("Engine", 50)
                lightbar = makePart("lightbar", 100)
                truck = makePart("TruckBed", 100)
                local parts = { door, engine, lightbar, truck }
                vehicle = {
                    getScript = function() return {} end,
                    getPartCount = function() return #parts end,
                    getPartByIndex = function(self, i) return parts[i + 1] end,
                }
                function newJob()
                    return { player = player, vehicle = vehicle, deadline = 100000,
                        nextCheckMs = 0, steps = 0, skip = {}, skipCount = 0 }
                end
                function vehicleRepairSendUninstall(part, time)
                    uninstalls = uninstalls + 1; uninstallPart = part:getId(); return "sent"
                end
                function vehicleRepairSendInstall(part, item, time)
                    installs = installs + 1; installPart = part:getId(); return "sent"
                end
                function vehicleRepairSyncItemCondition(item, condition)
                    syncs = syncs + 1; syncCondition = condition; return "sent"
                end
                function vehicleRepairSendLightbar(part, item, time)
                    lightbars = lightbars + 1; lightbarPart = part:getId(); return "sent"
                end
                """);
        lua.resource("modcore/lua/components/panels/EtherVehiclePanel.lua");
        return lua;
    }
}
