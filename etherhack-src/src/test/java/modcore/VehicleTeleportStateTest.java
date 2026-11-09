package modcore;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 三百三十: 汽车传送落位判定的状态机回归 (只跑生产 UIMap 的传送区段, 不加载游戏/世界).
 *
 * 覆盖冻结、按时间下落、区块环、负写返回码与落地观察失效；不启动游戏或原生库。
 */
class VehicleTeleportStateTest {

    @Test
    void frozenHeightIsRejectedInsteadOfReportedAsArrived() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                mode = "frozen"
                vehicle.x, vehicle.y, vehicle.z = 0, 0, 0
                hops, rollbacks = 0, 0
                assert(UIMap:startVehicleTeleport(52.3, 0))
                for i = 1, 400 do clock = i * 20; tick() end
                return UIMap.lastTeleportCancel, hops, rollbacks
                """);
        assertArrayEquals(new Object[]{"frozen", 1.0, 1.0}, result,
                "a vehicle whose height never changed must be rolled back, not reported as arrived");
    }

    @Test
    void normalFallStillArrives() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                mode = "falling"
                vehicle.x, vehicle.y, vehicle.z = 0, 0, 0
                hops, rollbacks = 0, 0
                assert(UIMap:startVehicleTeleport(5, 0))
                for i = 1, 400 do clock = i * 20; tick() end
                return UIMap.lastTeleportCancel, hops, rollbacks, vehicle.x
                """);
        assertArrayEquals(new Object[]{"done", 1.0, 0.0, 5.0}, result,
                "elapsed-time falling sequence must still settle and arrive");
    }

    @Test
    void missingChunkRingNeverStartsAHop() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                mode = "blocked"
                vehicle.x, vehicle.y, vehicle.z = 0, 0, 0
                hops, rollbacks = 0, 0
                assert(UIMap:startVehicleTeleport(5, 0))
                for i = 1, 700 do clock = i * 20; tick() end
                return UIMap.lastTeleportCancel, hops, rollbacks
                """);
        assertArrayEquals(new Object[]{"dest-unloaded", 0.0, 0.0}, result,
                "a destination whose 3x3 ring is not loaded must not be hopped to");
    }

    @Test
    void rejectedPhysicsWriteAbortsWithoutCountingTheHop() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                mode = "reject"
                vehicle.x, vehicle.y, vehicle.z = 0, 0, 0
                hops, rollbacks = 0, 0
                assert(UIMap:startVehicleTeleport(5, 0))
                for i = 1, 200 do clock = i * 20; tick() end
                return UIMap.lastTeleportCancel, hops, rollbacks
                """);
        assertArrayEquals(new Object[]{"hop", 0.0, 0.0}, result,
                "a rejected physics write must abort the trip instead of showing progress");
    }

    @Test
    void slowHighFrameRateFallCannotReportArrivalAfterTwoHundredMilliseconds() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                mode = "slow"
                assert(UIMap:startVehicleTeleport(5, 0))
                for i = 1, 400 do clock = i * 8; tick() end
                return UIMap.lastTeleportCancel or "pending", hops, rollbacks
                """);
        assertArrayEquals(new Object[]{"pending", 1.0, 0.0}, result,
                "an unfinished fall must remain pending even after a position-only verify would pass");
    }

    @Test
    void changedRingOrDisabledPhysicsRollsBackAnAlreadyStartedFall() throws Exception {
        for (String mode : new String[]{"ring-lost", "off-again", "not-local"}) {
            LuaTestRuntime lua = runtime();
            Object[] result = lua.execute("""
                    mode = "%s"
                    assert(UIMap:startVehicleTeleport(5, 0))
                    for i = 1, 150 do clock = i * 20; tick() end
                    return UIMap.lastTeleportCancel, rollbacks, vehicle.x
                    """.formatted(mode));
            assertArrayEquals(new Object[]{"off-again".equals(mode) ? "frozen" : "verify", 1.0, 0.0}, result,
                    "loss of simulation preconditions must restore the starting point: " + mode);
        }
    }

    @Test
    void cancelDuringObservationRestoresTheStartingPoint() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                assert(UIMap:startVehicleTeleport(5, 0))
                for i = 1, 85 do clock = i * 20; tick() end
                function isKeyDown() return true end
                clock = 1720; tick()
                return UIMap.lastTeleportCancel, rollbacks, vehicle.x
                """);
        assertArrayEquals(new Object[]{"input", 1.0, 0.0}, result);
    }

    @Test
    void pendingRestoreIsNotDescribedAsAPhysicsWriteRejection() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                hopResult = "blocked-pending-restore"
                assert(UIMap:startVehicleTeleport(5, 0))
                for i = 1, 200 do clock = i * 20; tick() end
                return lastReason, hops, rollbacks
                """);
        assertArrayEquals(new Object[]{"UI_Map_VehTeleportReasonPendingRestore", 0.0, 0.0}, result);
    }

    @Test
    void restoreTimeoutAndLostLocalSimulationHaveTheirOwnReasons() throws Exception {
        for (String[] sample : new String[][]{
                {"blocked-restore-timeout", "UI_Map_VehTeleportReasonRestoreTimeout"},
                {"blocked-not-local", "UI_Map_VehTeleportReasonNotLocal"},
                {"blocked-physics-off", "UI_Map_VehTeleportReasonFrozen"},
                {"physics-rejected(rc=-5)", "UI_Map_VehTeleportReasonRejected"}}) {
            LuaTestRuntime lua = runtime();
            Object[] result = lua.execute("""
                    hopResult = "%s"
                    assert(UIMap:startVehicleTeleport(5, 0))
                    for i = 1, 200 do clock = i * 20; tick() end
                    return lastReason
                    """.formatted(sample[0]));
            assertArrayEquals(new Object[]{sample[1]}, result);
        }
    }

    @Test
    void failedLandingDoesNotClaimRollbackCompletedWhenRecoveryIsStillPending() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                mode = "frozen"
                rollbackResult = "pending-restore"
                assert(UIMap:startVehicleTeleport(5, 0))
                for i = 1, 200 do clock = i * 20; tick() end
                return lastReason, rollbacks
                """);
        assertArrayEquals(new Object[]{"UI_Map_VehTeleportReasonFrozen; UI_Map_VehTeleportRestorePending", 1.0}, result);
    }

    @Test
    void towingIsRefusedBeforeTheStartMessage() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                mode = "towing"
                assert(UIMap:startVehicleTeleport(5, 0))
                return lastMessageKey, hops, rollbacks, UIMap.lastTeleportCancel
                """);
        assertArrayEquals(new Object[]{"UI_Map_VehTeleportTowing", 0.0, 0.0, null}, result,
                "a towed vehicle must be refused immediately, not after a hop attempt");
    }

    @Test
    void aStoppedEngineIsRefusedBeforeTheStartMessage() throws Exception {
        LuaTestRuntime lua = runtime();
        Object[] result = lua.execute("""
                mode = "engine-off"
                assert(UIMap:startVehicleTeleport(5, 0))
                return lastMessageKey, hops, rollbacks, UIMap.lastTeleportCancel
                """);
        assertArrayEquals(new Object[]{"UI_Map_VehTeleportEngineOff", 0.0, 0.0, null}, result,
                "a vehicle without local simulation and a stopped engine must ask to start it first");
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                function print() end
                UIMap = {}
                clock = 0
                mode = "falling"
                hops = 0
                rollbacks = 0
                vehicle = { x = 0, y = 0, z = 0 }
                hopClock = nil
                landingClock = nil
                savedX, savedY, savedZ = 0, 0, 0
                function vehicle:getX() return self.x end
                function vehicle:getY() return self.y end
                function vehicle:isDriver(p) return p == player end
                player = {}
                function player:getVehicle() return vehicle end
                function player:isDead() return false end
                function getPlayer() return player end
                function getTimestampMs() return clock end
                function tr(key, params)
                    lastMessageKey = key
                    if key == "UI_Map_VehTeleportAborted" then lastReason = params.reason end
                    return key
                end
                HaloTextHelper = {
                    addText = function() end,
                    getColorRed = function() return {} end,
                    getColorGreen = function() return {} end,
                }
                function isKeyDown() return false end
                function isMouseButtonDown() return false end
                Keyboard = { KEY_ESCAPE = 1 }
                Events = { OnTick = { Add = function(callback) tick = callback end } }
                function vehicleNativeDestReady()
                    if mode == "blocked" then return "chunk-ring-not-loaded" end
                    if mode == "towing" then return "towing-linked" end
                    if mode == "engine-off" then return "engine-off" end
                    return "ok"
                end
                function vehicleNativeHop(v, dx, dy, raise)
                    if hopResult ~= nil then return hopResult end
                    if mode == "reject" then return "physics-rejected" end
                    savedX, savedY, savedZ = v.x, v.y, v.z
                    hops = hops + 1
                    hopClock = clock
                    v.x = v.x + dx
                    v.y = v.y + dy
                    v.z = v.z + raise
                    return "ok"
                end
                function vehicleNativePhysicsZ(v)
                    if hopClock ~= nil then
                        if mode == "falling" then
                            v.z = math.max(0, 3.7 - 5 * (clock - hopClock) / 1000)
                        elseif mode == "slow" then
                            v.z = math.max(0, 3.7 - 1.3 * (clock - hopClock) / 1000)
                        end
                    end
                    return v.z
                end
                function vehicleNativePhysicsActive() return mode ~= "frozen" end
                function vehicleNativeLandingBegin() landingClock = clock; return true end
                function vehicleNativeLandingEnd() landingClock = nil end
                function vehicleNativeLandingPoll(v)
                    if mode == "frozen" then return "physics-off" end
                    if mode == "not-local" then return "not-local" end
                    if mode == "ring-lost" and clock - hopClock >= 100 then return "chunk-ring-not-loaded" end
                    if mode == "off-again" and clock - hopClock >= 100 then return "physics-off" end
                    if v.z > 0 then return "falling" end
                    if clock - landingClock < 1000 then return "settling" end
                    return "ok"
                end
                function vehicleNativeCommit() landingClock = nil; return true end
                function vehicleNativeVerify() return "ok" end
                function vehicleNativeRollback(v)
                    rollbacks = rollbacks + 1
                    v.x, v.y, v.z = savedX, savedY, savedZ
                    hopClock = nil
                    return rollbackResult or "ok"
                end
                function vehicleNativeUpright() return "ok" end
                function vehicleNativeProbeRead() return "probe" end
                """);
        var stream = VehicleTeleportStateTest.class.getClassLoader()
                .getResourceAsStream("modcore/lua/components/ui/UIMap.lua");
        assertNotNull(stream, "UIMap.lua resource");
        String production = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        int start = production.indexOf("--* C3 连人带车传送");
        assertTrue(start > 0, "teleport section marker must exist");
        lua.execute(production.substring(start));
        return lua;
    }
}
