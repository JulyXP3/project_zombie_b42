package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WornSpawnFeedbackTest {
    @Test
    void waitingForTheDropKeepsPollingInsteadOfShowingAnError() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.execute("EtherWornSpawn.trigger('Base.Target'); pollResult = 'waiting-drop'; tick()");
        assertArrayEquals(new Object[]{true, "UI_WornSpawn_Waiting"}, lua.execute("""
                return EtherWornSpawn.active, EtherWornSpawn.message
                """));
    }

    @Test
    void rejectedActionHasItsOwnMessage() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.execute("EtherWornSpawn.trigger('Base.Target'); pollResult = 'rejected'; tick()");
        assertArrayEquals(new Object[]{false, "UI_WornSpawn_Rejected"}, lua.execute("""
                return EtherWornSpawn.active, EtherWornSpawn.message
                """));
    }

    @Test
    void failedSendDoesNotStartPolling() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.execute("startResult = 'send-failed'; EtherWornSpawn.trigger('Base.Target')");
        assertArrayEquals(new Object[]{false, "UI_WornSpawn_SendFailed"}, lua.execute("""
                return EtherWornSpawn.active, EtherWornSpawn.message
                """));
    }

    @Test
    void cancelAlsoCallsBackendCleanupAfterPollingHasStopped() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.execute("EtherWornSpawn.active = false; EtherWornSpawn.cancel()");
        assertArrayEquals(new Object[]{1.0, false, ""}, lua.execute("""
                return restores, EtherWornSpawn.active, EtherWornSpawn.message
                """));
    }

    @Test
    void disconnectRunsTheSameCleanupPath() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.execute("EtherWornSpawn.trigger('Base.Target'); disconnected()");
        assertArrayEquals(new Object[]{1.0, false}, lua.execute("""
                return restores, EtherWornSpawn.active
                """));
    }

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                function print() end
                function getTranslate(key) return key end
                function isMultiplayer() return true end
                startResult = "started"
                pollResult = "waiting"
                restores = 0
                function wornSpawnStart() return startResult end
                function wornSpawnPoll() return pollResult end
                function wornSpawnRestore() restores = restores + 1; return "restored" end
                Events = {
                    OnTick = { Add = function(callback) tick = callback end },
                    OnDisconnect = { Add = function(callback) disconnected = callback end },
                }
                """);
        lua.resource("modcore/lua/components/ui/EtherWornSpawn.lua");
        return lua;
    }
}
