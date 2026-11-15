package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Executes the eight-tile generator maintenance service without a world or network. */
class GeneratorMaintenanceTest {
    @Test
    void refuelsEveryGeneratorInsideTheSameFloorRadius() throws Exception {
        Object[] result = runtime().execute("""
                local near = addGenerator(101, 100, 0, 2, 45)
                local edge = addGenerator(108, 100, 0, 4, 55)
                local outside = addGenerator(108, 100, 0, 3, 65)
                outside.x = 108.1
                local diagonal = addGenerator(106, 106, 0, 1, 75)
                local upstairs = addGenerator(100, 100, 1, 1, 85)
                local result = GeneratorMaintenance.run("fuel")
                return result.code, result.found, result.submitted, #writes,
                    near.fuel, edge.fuel, outside.fuel, diagonal.fuel, upstairs.fuel,
                    near.condition, near.activated, near.connected
                """);
        assertArrayEquals(new Object[]{"submitted", 2.0, 2.0, 2.0,
                10.0, 10.0, 3.0, 1.0, 1.0, 45.0, true, true}, result);
    }

    @Test
    void repairsWithoutRefuelingOrChangingPowerState() throws Exception {
        Object[] result = runtime().execute("""
                local a = addGenerator(100, 100, 0, 2, 5)
                a.activated = false
                a.connected = false
                local b = addGenerator(103, 104, 0, 6, 90)
                local result = GeneratorMaintenance.run("condition")
                return result.submitted, #writes, a.condition, b.condition,
                    a.fuel, b.fuel, a.activated, a.connected
                """);
        assertArrayEquals(new Object[]{2.0, 2.0, 100.0, 100.0, 2.0, 6.0, false, false}, result);
    }

    @Test
    void fullGeneratorsDoNotSendAnotherUpdate() throws Exception {
        Object[] result = runtime().execute("""
                addGenerator(100, 100, 0, 10, 100)
                local result = GeneratorMaintenance.run("fuel")
                return result.code, result.found, result.skipped, #writes
                """);
        assertArrayEquals(new Object[]{"full", 1.0, 1.0, 0.0}, result);
    }

    @Test
    void missingPlayerAndConnectionFailBeforeScanning() throws Exception {
        Object[] result = runtime().execute("""
                local g = addGenerator(100, 100, 0, 1, 1)
                ready = false
                local disconnected = GeneratorMaintenance.run("fuel")
                player = nil
                local missing = GeneratorMaintenance.run("condition")
                return disconnected.code, missing.code, scans, #writes, g.fuel, g.condition
                """);
        assertArrayEquals(new Object[]{"not-ready", "no-player", 0.0, 0.0, 1.0, 1.0}, result);
    }

    @Test
    void removedOrMisindexedObjectsAreNeverModified() throws Exception {
        Object[] result = runtime().execute("""
                local removed = addGenerator(100, 100, 0, 1, 1)
                removed.index = -1
                local moved = addGenerator(101, 100, 0, 2, 2)
                moved.square = nil
                local wrong = addGenerator(102, 100, 0, 3, 3)
                wrong.index = 1
                local result = GeneratorMaintenance.run("fuel")
                return result.submitted, #writes, removed.fuel, moved.fuel, wrong.fuel
                """);
        assertArrayEquals(new Object[]{0.0, 0.0, 1.0, 2.0, 3.0}, result);
    }

    @Test
    void duplicateReferencesReceiveOnlyOneUpdate() throws Exception {
        Object[] result = runtime().execute("""
                local g = addGenerator(100, 100, 0, 1, 20)
                table.insert(g.square.objects, g)
                local result = GeneratorMaintenance.run("fuel")
                return result.found, result.submitted, #writes
                """);
        assertArrayEquals(new Object[]{1.0, 1.0, 1.0}, result);
    }

    @Test
    void failedSetterIsNotRetriedOrRolledBack() throws Exception {
        Object[] result = runtime().execute("""
                local g = addGenerator(100, 100, 0, 1, 20)
                g.fail = true
                local result = GeneratorMaintenance.run("fuel")
                return result.code, result.failed, result.submitted, #writes, g.fuel
                """);
        assertArrayEquals(new Object[]{"partial", 1.0, 0.0, 1.0, 1.0}, result);
    }

    @Test
    void repeatedClicksAreLimitedWithoutBlockingTheOtherButton() throws Exception {
        Object[] result = runtime().execute("""
                local g = addGenerator(100, 100, 0, 1, 20)
                GeneratorMaintenance.run("fuel")
                g.fuel = 2
                local repeated = GeneratorMaintenance.run("fuel")
                local repair = GeneratorMaintenance.run("condition")
                return repeated.code, repair.submitted, #writes, g.fuel, g.condition
                """);
        assertArrayEquals(new Object[]{"busy", 1.0, 2.0, 2.0, 100.0}, result);
    }

    @Test
    void emptyRangeAndInvalidModeNeverWrite() throws Exception {
        Object[] result = runtime().execute("""
                local empty = GeneratorMaintenance.run("fuel")
                local invalid = GeneratorMaintenance.run("start")
                return empty.code, invalid.code, #writes
                """);
        assertArrayEquals(new Object[]{"empty", "bad-mode", 0.0}, result);
    }

    @Test
    void nonFiniteValuesAndAnUnrepresentableObjectIndexAreRejected() throws Exception {
        Object[] result = runtime().execute("""
                local bad = addGenerator(100, 100, 0, 0/0, 10)
                local huge = addGenerator(101, 100, 0, 1, 20)
                huge.index = 128
                local result = GeneratorMaintenance.run("fuel")
                return result.submitted, #writes, bad.condition, huge.fuel
                """);
        assertArrayEquals(new Object[]{0.0, 0.0, 10.0, 1.0}, result);
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                now = 10000
                ready = true
                scans = 0
                writes = {}
                squares = {}
                function getTimestampMs() return now end
                function generatorMaintenanceReady() return ready end
                function instanceof(o, name) return o ~= nil and o.class == name end
                function squareKey(x, y, z) return x .. "," .. y .. "," .. z end
                player = {
                    getX = function() return 100 end,
                    getY = function() return 100 end,
                    getZ = function() return 0 end,
                    isDead = function() return false end,
                }
                function getPlayer() return player end
                cell = { getGridSquare = function(self, x, y, z)
                    scans = scans + 1
                    return squares[squareKey(x, y, z)]
                end }
                function getCell() return cell end
                function javaList(values)
                    return { size = function() return #values end,
                        get = function(self, i) return values[i + 1] end }
                end
                function addGenerator(x, y, z, fuel, condition)
                    local key = squareKey(x, y, z)
                    local sq = squares[key]
                    if sq == nil then
                        sq = { objects = {} }
                        sq.getObjects = function(self) return javaList(self.objects) end
                        squares[key] = sq
                    end
                    local g = { class = "IsoGenerator", square = sq, index = #sq.objects,
                        x = x, y = y, z = z, fuel = fuel, condition = condition,
                        activated = true, connected = true }
                    g.getX = function(self) return self.x end
                    g.getY = function(self) return self.y end
                    g.getZ = function(self) return self.z end
                    g.getSquare = function(self) return self.square end
                    g.getObjectIndex = function(self) return self.index end
                    g.getFuel = function(self) return self.fuel end
                    g.getMaxFuel = function() return 10 end
                    g.getCondition = function(self) return self.condition end
                    g.isActivated = function(self) return self.activated end
                    g.isConnected = function(self) return self.connected end
                    g.setFuel = function(self, value)
                        table.insert(writes, { "fuel", value })
                        if self.fail then error("send failed") end
                        self.fuel = value
                    end
                    g.setCondition = function(self, value)
                        table.insert(writes, { "condition", value })
                        if self.fail then error("send failed") end
                        self.condition = value
                    end
                    table.insert(sq.objects, g)
                    return g
                end
                """);
        lua.resource("modcore/lua/components/ui/GeneratorMaintenance.lua");
        return lua;
    }
}
