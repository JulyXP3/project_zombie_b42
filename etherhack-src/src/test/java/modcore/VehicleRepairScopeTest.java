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

    @Test
    void stagedRepairWaitsBetweenInstallationsAndFinishesAtFullCondition() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                door.condition = 42
                doorItem.condition = 42
                EtherRepairJob = newJob()
                now = 200
                repairTick()
                local firstTarget = EtherRepairJob.stageTarget
                door.item = nil
                now = 400
                repairTick()
                now = 850
                repairTick()
                local firstSync = syncCondition
                door.condition = 82
                door.item = doorItem
                now = 1050
                repairTick()
                local held = EtherRepairJob.holdUntilMs == 4050 and EtherRepairJob.steps == 0
                now = 1200
                repairTick()
                local noEarlyRetry = uninstalls == 1 and installs == 1
                now = 4050
                repairTick()
                local secondTarget = EtherRepairJob.stageTarget
                door.item = nil
                now = 4250
                repairTick()
                now = 4700
                repairTick()
                local secondSync = syncCondition
                door.condition = 100
                door.item = doorItem
                now = 4900
                repairTick()
                return firstTarget, firstSync, held, noEarlyRetry, secondTarget, secondSync,
                    EtherRepairJob.steps, uninstalls, installs
                """);
        assertArrayEquals(new Object[]{82.0, 82.0, true, true, 100.0, 100.0, 1.0, 2.0, 2.0}, result,
                "installed intermediate state is accepted before the retry deadline, then held before the next stage");
    }

    @Test
    void stagedRepairTargetsCoverDamagedAndNearlyRepairedParts() throws Exception {
        int[] starts = {0, 10, 42, 70, 97};
        Object[][] expected = {
                {"40,80,100", 1.0}, {"50,90,100", 1.0}, {"82,100", 1.0},
                {"100", 1.0}, {"100", 1.0}
        };
        for (int i = 0; i < starts.length; i++) {
            LuaTestRuntime lua = runtime();
            lua.execute("door.condition = " + starts[i] + "; doorItem.condition = " + starts[i]);
            Object[] result = lua.execute("""
                    EtherRepairJob = newJob()
                    local targets = {}
                    for stage = 1, 3 do
                        now = now + 3200
                        repairTick()
                        door.item = nil
                        now = now + 200
                        repairTick()
                        now = now + 450
                        repairTick()
                        table.insert(targets, string.format("%d", syncCondition))
                        door.condition = syncCondition
                        door.item = doorItem
                        now = now + 200
                        repairTick()
                        if EtherRepairJob.steps == 1 then break end
                    end
                    return table.concat(targets, ","), EtherRepairJob.steps
                    """);
            assertArrayEquals(expected[i], result, "stage sequence from condition " + starts[i]);
        }
    }

    @Test
    void gasTankWithAnInstalledItemIsSkippedWithoutSendingActions() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                engine.condition = 100
                gasTank.item = doorItem
                vehicleParts[#vehicleParts + 1] = gasTank
                EtherRepairJob = newJob()
                now = 200
                repairTick()
                return uninstalls, installs, syncs, EtherRepairJob.skip["GasTank"] == true,
                    EtherRepairJob.skipCount, gasTank.condition, gasTank.item == doorItem
                """);
        assertArrayEquals(new Object[]{0.0, 0.0, 0.0, true, 1.0, 50.0, true}, result,
                "the repair workflow leaves the installed gas tank unchanged; this does not prove all fuel values are finite");
    }

    @Test
    void leavingTheVehicleStopsTheStagedJob() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherRepairJob = newJob()
                player.getVehicle = function() return nil end
                now = 200
                repairTick()
                return EtherRepairJob == nil, uninstalls
                """);
        assertArrayEquals(new Object[]{true, 0.0}, result,
                "leaving the target vehicle stops the job before another action is sent");
    }

    @Test
    void leavingDuringTheStageHoldStopsImmediately() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherRepairJob = newJob()
                EtherRepairJob.holdUntilMs = 5000
                player.getVehicle = function() return nil end
                now = 200
                repairTick()
                return EtherRepairJob == nil, uninstalls, installs
                """);
        assertArrayEquals(new Object[]{true, 0.0, 0.0}, result,
                "the inter-stage delay must not hide a player leaving the vehicle");
    }

    @Test
    void changingVehiclesDuringTheStageHoldStopsImmediately() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                EtherRepairJob = newJob()
                EtherRepairJob.holdUntilMs = 5000
                player.getVehicle = function() return {} end
                now = 200
                repairTick()
                return EtherRepairJob == nil, uninstalls, installs
                """);
        assertArrayEquals(new Object[]{true, 0.0, 0.0}, result,
                "being seated in a different vehicle must not keep the old repair job running");
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
                player.getVehicle = function() return vehicle end
                doorItem = {
                    condition = 40,
                    getID = function() return 77 end,
                    getConditionMax = function() return 100 end,
                    getCondition = function(self) return self.condition end,
                }
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
                        if self.id == "DoorFrontLeft" or self.id == "DoorRearLeft" or self.id == "GasTank" then
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
                gasTank = makePart("GasTank", 50)
                vehicleParts = { door, engine, lightbar, truck }
                vehicle = {
                    getScript = function() return {} end,
                    getPartCount = function() return #vehicleParts end,
                    getPartByIndex = function(self, i) return vehicleParts[i + 1] end,
                }
                function newJob()
                    return { player = player, vehicle = vehicle, deadline = 100000,
                        nextCheckMs = 0, steps = 0, skip = {}, skipCount = 0, requireSeated = true }
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
