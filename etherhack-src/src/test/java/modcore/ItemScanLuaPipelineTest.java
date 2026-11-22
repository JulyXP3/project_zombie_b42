package modcore;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Production Lua controller + production Java Bridge/Runtime, not a fake scanner. */
class ItemScanLuaPipelineTest {
    @Test void playerLayerWindowScansOneBelowAndTwoAboveOnly() throws Exception {
        var lua=ItemSearchTestRuntime.create();
        var got=lua.execute("""
                scanLayers={["12,12"]={min=-3,max=6,version=1}}
                EtherItemSearch.layerMode=1
                for _,z in ipairs({-3,-2,-1,0,1,2,3,4}) do square(100,100,z).world={floorObject(100,100,z,item("Base.Axe"))} end
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local counts={};for _,p in ipairs(EtherItemSearch.results or {}) do counts[p.z]=p.count end
                return counts[-2],counts[-1],counts[0],counts[1],counts[2],counts[3],counts[4],#EtherItemSearch.results
                """);
        // Preset "+-2" with the player on z=0 => -2, -1, 0, 1, 2 are scanned, 3/4 are not.
        assertArrayEquals(new Object[]{1.0,1.0,1.0,1.0,1.0,null,null,5.0},got);
    }
    @Test void highLayerDirtyEventReplacesItsContributionWithoutClippingToPlayerNeighbors() throws Exception {
        var got=ItemSearchTestRuntime.create().execute("""
                scanLayers={["12,12"]={min=-1,max=4,version=1}}
                local high=square(100,100,2);high.world={floorObject(100,100,2,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                high.world={floorObject(100,100,2,item("Base.Axe")),floorObject(100,100,2,item("Base.Axe"))}
                Events.OnContainerUpdate.fire(high);now=now+251;tick();finish()
                local count=0;for _,p in ipairs(EtherItemSearch.results or {}) do if p.z==2 then count=p.count end end
                return count,#EtherItemSearch.results
                """);
        assertArrayEquals(new Object[]{2.0,1.0},got);
    }
    @Test void movementCarriesNotYetDebouncedDirtyChangesInsideItsRetainedCenter() throws Exception {
        var got=ItemSearchTestRuntime.create().execute("""
                local center=square(100,100,0);center.world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                now=now+1001;center.world={};Events.OnContainerUpdate.fire(center);player.x=102
                tick();finish()
                return hitCount(100,100)
                """);
        assertArrayEquals(new Object[]{0.0},got);
    }
    @Test void publicRefreshEntryKeepsDeferredDirtyUpdateContract() throws Exception {
        var got=ItemSearchTestRuntime.create().execute("""
                local sq=square(100,100,0);sq.world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                sq.world={};Events.OnContainerUpdate.fire(sq);now=now+251
                local available=type(EtherItemSearch.refresh)=="function"
                if available then EtherItemSearch.refresh();finish() end
                return available,hitCount(100,100)
                """);
        assertArrayEquals(new Object[]{true,0.0},got);
    }
    @Test void relocationThrottleShrinksWithVehicleSpeedAndRecoversWhenStopped() throws Exception {
        var got=ItemSearchTestRuntime.create().execute("""
                if type(EtherItemSearch.moveThrottleMs) ~= "function" then return -1,-1 end
                player.x,player.y=100,100
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local idle=EtherItemSearch.moveThrottleMs()
                for i=1,20 do player.x=player.x+0.9;now=now+16.7;tick() end
                local fast=EtherItemSearch.moveThrottleMs()
                for i=1,60 do now=now+16.7;tick() end
                return idle,fast,EtherItemSearch.moveThrottleMs()
                """);
        assertArrayEquals(new Object[]{1000.0,250.0,1000.0},got);
    }
    @Test void fastTravelStillDeliversFreshCompleteResultsInsteadOfRestartingForever() throws Exception {
        var got=ItemSearchTestRuntime.create().execute("""
                -- Ground plaza so the moving vehicle streams new squares into view.
                for x=96,132 do for y=98,102 do square(x,y,0) end end
                scanLayers={}
                local function had(x,y) scanLayers[x..","..y]={min=0,max=0,version=1} end
                for x=96,132 do for y=98,102 do had(x,y) end end
                square(100,100,0).world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                local firstAt=EtherItemSearch.resultCollectedAt
                local completions=0
                local lastAt=firstAt
                for i=1,600 do
                    player.x=player.x+3;now=now+16.7;tick()
                    local at=EtherItemSearch.resultCollectedAt
                    if at~=nil and at>lastAt then completions=completions+1;lastAt=at end
                end
                -- While still driving a follow-up scan may legitimately be in flight; what must never
                -- happen is "no scan running and no fresh result", i.e. permanently starving.
                local healthy = EtherItemSearch._cursor==nil or (EtherItemSearch.progress~=nil and EtherItemSearch.progress.state=="running")
                return completions>0, healthy, EtherItemSearch.results~=nil
                """);
        assertArrayEquals(new Object[]{true,true,true},got);
    }
    @Test void newlyLoadedFloorIsDiscoveredAndItsUnloadedContributionRetracted() throws Exception {
        var got=ItemSearchTestRuntime.create().execute("""
                local ground=square(100,100,0);ground.world={floorObject(100,100,0,item("Base.Axe"))}
                scanLayers={["12,12"]={min=-1,max=1,version=1}}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil);finish()
                scanLayers["12,12"].max=2;square(100,100,2).world={floorObject(100,100,2,item("Base.Axe"))}
                EtherItemSearch.patrolMs=1000;now=now+1001;tick();finish();local loaded=#EtherItemSearch.results
                scanLayers["12,12"].max=1;squares[key(100,100,2)]=nil;now=now+1001;tick();finish()
                return loaded,#EtherItemSearch.results,hitCount(100,100)
                """);
        assertArrayEquals(new Object[]{2.0,1.0,1.0},got);
    }
}
