package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TeleportClimbLifecycleTest {
    @Test
    void failedEnterDoesNotStartTheLinearTask() throws Exception {
        assertArrayEquals(new Object[]{0.0, "sync-failed", false}, run("""
                enterSuccess=false
                MapActions:onTeleportLinear(10,0)
                MapActions.onTeleportTick()
                now=100
                MapActions.onTeleportTick()
                return player.x,UIMap.lastTeleportCancel,suppressed
                """));
    }

    @Test
    void disappearanceClearsSessionUsingItsOwnerRatherThanCurrentPlayer() throws Exception {
        assertArrayEquals(new Object[]{false, 1.0, true}, run("""
                local owner=player
                MapActions:onTeleportLinear(10,0)
                player=nil
                MapActions.onTeleportTick()
                MapActions.reset()
                return suppressed,exits,lastExitPlayer==owner
                """));
    }

    @Test
    void resetWithoutALuaActiveMarkerStillClearsJavaState() throws Exception {
        assertArrayEquals(new Object[]{false, 1.0}, run("""
                suppressed=true
                MapActions.reset()
                return suppressed,exits
                """));
    }

    @Test
    void cancellationClosesBeforeReturningControlInsteadOfWaitingAnotherTick() throws Exception {
        assertArrayEquals(new Object[]{1.0, false, 1.0, "input"}, run("""
                MapActions:onTeleportLinear(10,0)
                MapActions.onTeleportTick()
                now=100
                MapActions.onTeleportTick()
                keys[Keyboard.KEY_W]=true
                now=200
                MapActions.onTeleportTick()
                return player.x,suppressed,exits,UIMap.lastTeleportCancel
                """));
    }

    @Test
    void readinessFailureStopsBeforeAnotherCoordinateWrite() throws Exception {
        assertArrayEquals(new Object[]{0.0, false, "sync-failed"}, run("""
                MapActions:onTeleportLinear(10,0)
                MapActions.onTeleportTick()
                ready=false
                now=100
                MapActions.onTeleportTick()
                return player.x,suppressed,UIMap.lastTeleportCancel
                """));
    }

    @Test
    void singlePlayerStillTeleportsWithoutNetworkState() throws Exception {
        assertArrayEquals(new Object[]{10.0, 0.0}, run("""
                multiplayer=false
                client=false
                MapActions:onTeleportLinear(10,0)
                return player.x,entries
                """));
    }

    @Test
    void completionClosesTheLastMoveAndDoesNotLeakIntoTheNextTask() throws Exception {
        assertArrayEquals(new Object[]{2.0, false, 1.0, "done"}, run("""
                MapActions:onTeleportLinear(2,0)
                MapActions.onTeleportTick()
                now=100
                MapActions.onTeleportTick()
                now=200
                MapActions.onTeleportTick()
                return player.x,suppressed,exits,UIMap.lastTeleportCancel
                """));
    }

    @Test
    void asynchronousCloseFailureReplacesDoneWithFailureAndWarnsTheOwnerOnce() throws Exception {
        assertArrayEquals(new Object[]{"sync-failed", 1.0, true}, run("""
                deferClose=true
                MapActions:onTeleportLinear(2,0)
                MapActions.onTeleportTick()
                now=100
                MapActions.onTeleportTick()
                now=200
                MapActions.onTeleportTick()
                suppressed=false
                closeFailure=true
                MapActions.onTeleportTick()
                MapActions.onTeleportTick()
                return UIMap.lastTeleportCancel,warnings,lastWarningPlayer==player
                """));
    }

    private static Object[] run(String body) throws Exception {
        LuaTestRuntime lua = new LuaTestRuntime();
        lua.execute("""
                now=0
                client=true
                multiplayer=true
                ready=true
                enterSuccess=true
                entries=0
                exits=0
                suppressed=false
                closeFailure=false
                warnings=0
                keys={}
                HaloTextHelper={addBadText=function(p) warnings=warnings+1;lastWarningPlayer=p end}
                function getTranslate(key) return key end
                Keyboard={ KEY_W=1,KEY_A=2,KEY_S=3,KEY_D=4,KEY_SPACE=5,KEY_ESCAPE=6 }
                function isClient() return client end
                function isMultiplayer() return multiplayer end
                function getTimestampMs() return now end
                function isKeyDown(key) return keys[key] or false end
                function isMouseButtonDown() return false end
                function teleportClimbReady(p) return ready and p~=nil end
                function teleportClimbPending(p) return suppressed end
                function teleportClimbCloseFailed(p) return closeFailure end
                function teleportClimbState(p,entering)
                    if entering then
                        entries=entries+1
                        suppressed=enterSuccess
                        return enterSuccess
                    end
                    if suppressed then exits=exits+1; lastExitPlayer=p end
                    if not deferClose then suppressed=false end
                    return true
                end
                local function event() return {Add=function() end,Remove=function() end} end
                Events={OnTick=event(),OnMainMenuEnter=event()}
                UIMap={}
                function newPlayer()
                    return { x=0,y=0,
                        getX=function(self) return self.x end,getY=function(self) return self.y end,getZ=function() return 0 end,
                        setX=function(self,x) self.x=x end,setY=function(self,y) self.y=y end,
                        isDead=function() return false end,getVehicle=function() return nil end }
                end
                player=newPlayer()
                function getPlayer() return player end
                """);
        lua.resource("modcore/lua/components/ui/MapActions.lua");
        return lua.execute(body);
    }
}
