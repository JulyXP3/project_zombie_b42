package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Exercises the repair controller with independently delayed action and inventory updates. */
class VehicleRepairScopeTest {
    @Test
    void damagedPartIsClaimedBeforeTheFirstUninstall() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                return claims, uninstalls, claimBeforeSend, lockedId
                """, 1.0, 1.0, true, 77.0);
    }

    @Test
    void partsAtOrAboveTheTargetAreNotLoweredOrUninstalled() throws Exception {
        for (int condition : new int[]{95, 97, 100}) {
            assertRun("startJob(" + condition + "); tick(200); return uninstalls, syncs, door.condition",
                    0.0, 0.0, (double) condition);
        }
    }

    @Test
    void gasTankLightbarAndUnsupportedPartsNeverSendRepairActions() throws Exception {
        assertRun("""
                startJob(100)
                engine.condition = 40
                gasTank.condition = 40
                lightbar.condition = 40
                for i = 1, 8 do tick(i * 200) end
                return uninstalls, installs, syncs, lightbars, EtherRepairJob == nil
                """, 0.0, 0.0, 0.0, 0.0, true);
    }

    @Test
    void unfinishedUninstallIsNotResentAfterTheOldRetryDeadline() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                tick(1600)
                tick(3000)
                return uninstalls, installs, acks, lockedId
                """, 1.0, 0.0, 0.0, 77.0);
    }

    @Test
    void localUninstallUpdateDoesNotReplaceTheActionReceipt() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                observeUninstalled()
                tick(500)
                tick(1800)
                return installs, syncs, acks
                """, 0.0, 0.0, 0.0);
    }

    @Test
    void doneReceiptWaitsForTheUninstalledItemToArrive() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                receipt = 'done'
                door.item = nil
                tick(500)
                tick(1800)
                return installs, syncs, EtherRepairJob ~= nil, lockedId
                """, 0.0, 0.0, true, 77.0);
    }

    @Test
    void confirmedUninstallWritesNinetyFiveBeforeOneInstallation() throws Exception {
        assertRun("""
                startJob(47)
                beginInstall()
                return syncCondition, syncs, installs, inventory.item.condition, acks
                """, 95.0, 1.0, 1.0, 95.0, 1.0);
    }

    @Test
    void failedConditionSyncDoesNotSendAnInstall() throws Exception {
        assertRun("""
                startJob(47)
                syncResult = 'no-container'
                beginInstall()
                return installs, syncs, EtherRepairJob == nil
                """, 0.0, 1.0, true);
    }

    @Test
    void anInstalledServerItemWithStaleClientStateIsNeverResent() throws Exception {
        assertRun("""
                startJob(47)
                beginInstall()
                receipt = 'done'
                -- Server consumed it, but the client still sees its original inventory object.
                tick(1200)
                tick(2400)
                tick(4000)
                return installs, syncs, lockedId, EtherRepairJob ~= nil
                """, 1.0, 1.0, 77.0, true);
    }

    @Test
    void installationWaitsForBothPartAndInventoryUpdates() throws Exception {
        assertRun("""
                startJob(47)
                beginInstall()
                receipt = 'done'
                door.condition = 95
                door.item = makeItem(77, 95)
                tick(1200)
                local held = lockedId == 77 and acks == 1
                inventory.item = nil
                tick(1400)
                tick(1800)
                return held, lockedId, installs, EtherRepairJob == nil, door.condition
                """, true, null, 1.0, true, 95.0);
    }

    @Test
    void delayedInventoryRemovalWithoutAPartUpdateDoesNotFinish() throws Exception {
        assertRun("""
                startJob(47)
                beginInstall()
                receipt = 'done'
                inventory.item = nil
                tick(1200)
                tick(2400)
                return installs, lockedId, EtherRepairJob ~= nil
                """, 1.0, 77.0, true);
    }

    @Test
    void failedInstallRetriesOnlyAfterAReplacementItemArrives() throws Exception {
        assertRun("""
                startJob(47)
                beginInstall()
                receipt = 'done'
                tick(1200)
                local beforeReplacement = installs
                inventory.item = makeItem(77, 87)
                inventory.item.container = inventory
                tick(1400)
                tick(1800)
                return beforeReplacement, installs, syncCondition, inventory.item.condition, acks
                """, 1.0, 2.0, 95.0, 95.0, 2.0);
    }

    @Test
    void failedUninstallRetriesOnlyAfterReceiptAndChangedPartState() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                receipt = 'done'
                tick(600)
                local beforeUpdate = uninstalls
                door.condition = 41
                door.item.condition = 41
                tick(1200)
                tick(1400)
                return beforeUpdate, uninstalls, acks
                """, 1.0, 2.0, 1.0);
    }

    @Test
    void changingTheTargetItemStopsInsteadOfInstallingAnotherItem() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                receipt = 'done'
                door.item = makeItem(88, 90)
                tick(600)
                return installs, EtherRepairJob == nil, quarantined
                """, 0.0, true, true);
    }

    @Test
    void cancelDuringInstallationKeepsTheLeaseUntilOwnershipIsConfirmed() throws Exception {
        assertRun("""
                startJob(47)
                beginInstall()
                checkbox.callback(false)
                tick(1200)
                local held = lockedId == 77 and releases == 0 and not checkbox.checked
                receipt = 'done'
                inventory.item = nil
                door.item = makeItem(77, 95)
                door.condition = 95
                tick(1400)
                return held, installs, lockedId, EtherRepairJob == nil
                """, true, 1.0, null, true);
    }

    @Test
    void leavingDuringUninstallNeverStartsInstallation() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                player.vehicle = nil
                tick(600)
                local held = lockedId == 77
                receipt = 'done'
                observeUninstalled()
                tick(1200)
                return held, installs, lockedId, EtherRepairJob == nil
                """, true, 0.0, null, true);
    }

    @Test
    void lostReceiptQuarantinesTheItemInsteadOfReleasingItOnTimeout() throws Exception {
        assertRun("""
                startJob(47)
                beginInstall()
                tick(60000)
                local stopped = EtherRepairJob == nil
                checkbox.callback(true)
                tick(60400)
                return stopped, quarantined, lockedId, installs, EtherRepairJob == nil
                """, true, true, 77.0, 1.0, true);
    }

    @Test
    void missingActionIsNotTreatedAsAnAcknowledgedRejection() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                receipt = 'missing'
                tick(600)
                return acks, uninstalls, quarantined, lockedId, EtherRepairJob == nil
                """, 0.0, 1.0, true, 77.0, true);
    }

    @Test
    void acknowledgedRejectionStopsWithoutRetrying() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                receipt = 'rejected'
                tick(600)
                return acks, uninstalls, installs, lockedId, EtherRepairJob == nil
                """, 1.0, 1.0, 0.0, null, true);
    }

    @Test
    void disconnectClearsTheOldSessionWithoutAnotherSend() throws Exception {
        assertRun("""
                startJob(47)
                beginInstall()
                online = false
                tick(1200)
                return installs, syncs, lockedId, EtherRepairJob == nil
                """, 1.0, 1.0, null, true);
    }

    @Test
    void oldInventoryObjectMovedToAnotherContainerIsNotInstalled() throws Exception {
        assertRun("""
                startJob(47)
                tick(200)
                receipt = 'done'
                observeUninstalled()
                inventory.item.container = {}
                tick(600)
                tick(1200)
                return installs, syncs, EtherRepairJob == nil
                """, 0.0, 0.0, true);
    }

    @Test
    void localReadinessAboveTargetIsNotReportedAsTheRequestedRepair() throws Exception {
        assertRun("""
                startJob(47)
                beginInstall()
                receipt = 'done'
                inventory.item = nil
                door.item = makeItem(77, 100)
                door.condition = 100
                tick(1200)
                return installs, syncs, EtherRepairJob == nil, lastMessage
                """, 1.0, 1.0, true, "UI_VehiclePanel_RepairFloatConflict");
    }

    @Test
    void unsupportedPartIsSkippedWithoutQuarantine() throws Exception {
        assertRun("""
                vehicleRepairClaim = function() return 'unsupported' end
                startJob(47)
                tick(200)
                tick(400)
                return uninstalls, quarantined, EtherRepairJob == nil, releases
                """, 0.0, false, true, 0.0);
    }

    private static void assertRun(String source, Object... expected) throws Exception {
        assertArrayEquals(expected, runtime().execute(source));
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("""
                function getText(key) return key end
                Perks = { Mechanics = 1 }
                Events = { OnTick = { Add = function(callback) repairTick = callback end } }
                now = 0
                online = true
                function getTimestampMs() return now end
                function isMultiplayer() return online end
                player.isDead = function() return false end
                player.getPerkLevel = function() return 10 end
                uninstalls = 0; installs = 0; syncs = 0; lightbars = 0
                claims = 0; acks = 0; releases = 0; quarantined = false
                receipt = 'idle'; lockedId = nil; syncResult = 'sent'
                lastMessage = nil
                print = function() end
                function tr(key, params) return key end
                HaloTextHelper = {
                    addText = function(owner, text) lastMessage = text end,
                    getColorRed = function() return {} end,
                    getColorGreen = function() return {} end,
                }
                EtherFormPanel = {
                    BOX_PAD_X = 10, BOX_PAD_Y = 3, ROW_STEP = 26, SECTION_GAP = 12,
                    derive = function(self) return setmetatable({}, { __index = self }) end,
                    addWidget = function() end, addChild = function() end,
                    addSpacer = function() end, _rowContentW = function() return 700 end,
                    _anchor = function() end,
                    addModule = function(self, key, height, builder) builder(0, 0, 700) end,
                }
                UICheckbox = { new = function(self, x, y, title, checked, callback)
                    return { checked = checked, callback = callback,
                        setCheked = function(self, value) self.checked = value end }
                end }
                ModuleHint = { new = function(self, x, y, w, text)
                    return { x=x, y=y, width=w, text=text, lines={text},
                        initialise=function() end, instantiate=function() end }
                end }
                function isVehicleInstantStart() return false end
                function makeItem(id, condition)
                    return { id=id, condition=condition,
                        getID=function(self) return self.id end,
                        getFullType=function() return 'Base.NormalCarSeat' end,
                        getCondition=function(self) return self.condition end,
                        getConditionMax=function() return 100 end,
                        getContainer=function(self) return self.container end }
                end
                inventory = { item=nil }
                function inventory:getItemById(id)
                    if self.item and self.item.id == id then return self.item end
                    return nil
                end
                inventory.getItemWithID = inventory.getItemById
                player.getInventory = function() return inventory end
                function makePart(id, condition, installable)
                    return { id=id, condition=condition, item=makeItem(77, condition),
                        getId=function(self) return self.id end,
                        getCondition=function(self) return self.condition end,
                        getInventoryItem=function(self) return self.item end,
                        getTable=function(self, name)
                            if installable then return { skills='Mechanics:1' } end
                            return nil
                        end,
                        getVehicle=function() return vehicle end,
                        getItemContainer=function() return nil end }
                end
                door = makePart('SeatFrontLeft', 100, true)
                engine = makePart('Engine', 100, false)
                gasTank = makePart('GasTank', 100, true)
                lightbar = makePart('lightbar', 100, false)
                parts = { door, engine, gasTank, lightbar }
                vehicle = {
                    getScript=function() return {} end,
                    getPartCount=function() return #parts end,
                    getPartByIndex=function(self, i) return parts[i + 1] end,
                    isDriver=function() return true end,
                    isStopped=function() return true end,
                }
                player.vehicle = vehicle
                player.getVehicle = function(self) return self.vehicle end
                function vehicleRepairSessionState()
                    if not online then lockedId = nil; receipt = 'idle'; return 'offline' end
                    if quarantined then return 'quarantined' end
                    return 'ready'
                end
                function vehicleRepairClaim(part)
                    if quarantined or lockedId ~= nil then return 'busy' end
                    claims = claims + 1; lockedId = part.item.id
                    return 'ready'
                end
                function vehicleRepairRelease()
                    if receipt ~= 'idle' or quarantined then return 'pending' end
                    releases = releases + 1; lockedId = nil
                    return 'released'
                end
                function vehicleRepairQuarantine() quarantined = true end
                function vehicleRepairActionStatus() return receipt end
                function vehicleRepairFinishAction()
                    if receipt ~= 'done' and receipt ~= 'rejected' then return 'pending' end
                    acks = acks + 1; receipt = 'idle'; return 'ready'
                end
                function vehicleRepairSendUninstall(part, time)
                    claimBeforeSend = lockedId == 77
                    uninstalls = uninstalls + 1; receipt = 'pending'; return 'sent'
                end
                function vehicleRepairSendInstall(part, item, time)
                    installs = installs + 1; receipt = 'pending'; return 'sent'
                end
                function vehicleRepairSyncItemCondition(item, condition)
                    syncs = syncs + 1; syncCondition = condition
                    if syncResult == 'sent' then item.condition = condition end
                    return syncResult
                end
                function vehicleRepairSendLightbar() lightbars = lightbars + 1; return 'sent' end
                function observeUninstalled()
                    door.item = nil
                    inventory.item = makeItem(77, door.condition)
                    inventory.item.container = inventory
                end
                function tick(time) now = time; repairTick() end
                function startJob(condition)
                    door.condition = condition; door.item.condition = condition
                    checkbox.callback(true)
                end
                function beginInstall()
                    tick(200)
                    receipt = 'done'
                    observeUninstalled()
                    tick(500)
                    tick(950)
                end
                """);
        lua.resource("modcore/lua/components/panels/EtherVehiclePanel.lua");
        lua.execute("""
                local panel = setmetatable({}, { __index = EtherVehiclePanel })
                panel:build()
                checkbox = EtherVehiclePanel.checkboxByKey['UI_VehiclePanel_RepairAuto']
                """);
        return lua;
    }
}
