package modcore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ItemSearchIncrementalTest {
    @Test
    void initialScanKeepsFurnitureFloorBagCorpseAndVehicleContributions() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(100,100,0)
                sq.objects={containerObject(100,100,0,{item("Base.Axe")})}
                sq.world={floorObject(101,100,0,item("Base.Bag",{item("Base.Axe")}))}
                sq.bodies={bodyObject(100,101,0,{item("Base.Axe")})}
                vehicles={vehicle(1,101,101,{item("Base.Axe")})}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                return #EtherItemSearch.results,hitCount(100,100),hitCount(101,100),
                    hitCount(100,101),hitCount(101,101)
                """);
        assertArrayEquals(new Object[]{4.0, 1.0, 1.0, 1.0, 1.0}, result);
    }

    @Test
    void twoTileMoveScansOnlyTheNewEdgesAndPrunesTheOldEdge() throws Exception {
        Object[] result = runtime().execute("""
                square(98,100,0).world={floorObject(98,100,0,item("Base.Axe"))}
                square(100,100,0).world={floorObject(100,100,0,item("Base.Axe"))}
                square(104,100,0).world={floorObject(104,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                reads={}; gridCalls=0
                player.x=102
                now=now+1000
                tick()
                finish()
                return hitCount(98,100),hitCount(100,100),hitCount(104,100),
                    reads[key(100,100,0)] or 0,gridCalls
                """);
        assertArrayEquals(new Object[]{0.0, 1.0, 1.0, 0.0, 30.0}, result);
    }

    @Test
    void dirtySquareChangesReplaceTheirOldContributionAfterTheShortDebounce() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(100,100,0)
                sq.world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                reads={}; gridCalls=0
                sq.world={}
                Events.OnContainerUpdate.fire(sq)
                now=now+249
                tick()
                local before=hitCount(100,100)
                now=now+1
                tick()
                finish()
                return before,hitCount(100,100),gridCalls
                """);
        assertArrayEquals(new Object[]{1.0, 0.0, 1.0}, result);
    }

    @Test
    void continuousDirtyEventsCannotPostponeARefreshForever() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(100,100,0)
                sq.world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                sq.world={}
                for i=1,6 do
                    Events.OnContainerUpdate.fire(sq)
                    now=now+200
                    tick()
                end
                finish()
                return hitCount(100,100)
                """);
        assertArrayEquals(new Object[]{0.0}, result);
    }

    @Test
    void changedTargetsInvalidateOldWorkAndNeverInvokeItsCallback() throws Exception {
        Object[] result = runtime().execute("""
                square(100,100,0).world={floorObject(100,100,0,item("Base.Axe")),
                    floorObject(101,100,0,item("Base.Saw"))}
                oldCallback=false
                clockStep=1
                EtherItemSearch.startScan({["Base.Axe"]=true},true,function() oldCallback=true end)
                tick()
                EtherItemSearch.startScan({["Base.Saw"]=true},true,nil)
                finish()
                return oldCallback,#EtherItemSearch.results,hitCount(100,100),hitCount(101,100)
                """);
        assertArrayEquals(new Object[]{false, 1.0, 0.0, 1.0}, result);
    }

    @Test
    void cursorYieldsInsideDenseContainersInsteadOfProcessingAllItemsInOneTick() throws Exception {
        Object[] result = runtime().execute("""
                local items={}
                for i=1,100 do items[i]=item("Base.Axe") end
                square(98,98,-1).objects={containerObject(98,98,-1,items)}
                clockStep=1
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                tick()
                return itemChecks <= 3,EtherItemSearch._cursor ~= nil
                """);
        assertArrayEquals(new Object[]{true, true}, result);
    }

    @Test
    void newSessionBeforeCompletionCannotPublishOldWorldResults() throws Exception {
        Object[] result = runtime().execute("""
                square(98,98,-1).world={floorObject(98,98,-1,item("Base.Axe"))}
                clockStep=1
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                tick()
                player=nil
                clockStep=0
                Events.OnPlayerUpdate.fire(nil)
                return EtherItemSearch.results == nil,EtherItemSearch._cursor == nil
                """);
        assertArrayEquals(new Object[]{true, true}, result);
    }

    @Test
    void unloadedDirtySquareRemovesItsContributionAndLargeMovesUseAFullScan() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(100,100,0)
                sq.world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                squares[key(100,100,0)]=nil
                Events.OnContainerUpdate.fire(sq)
                now=now+250
                tick(); finish()
                local removed=hitCount(100,100)
                player.x=120
                gridCalls=0
                now=now+1000
                tick(); finish()
                return removed,gridCalls,#EtherItemSearch.results
                """);
        assertArrayEquals(new Object[]{0.0, 75.0, 0.0}, result);
    }

    @Test
    void patrolConvergesUnknownRemoteChangesWithoutRepeatedFullScans() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(100,100,0)
                sq.world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                EtherItemSearch.patrolMs=100
                EtherItemSearch.patrolCells=5
                sq.world={}
                local maxReads=0
                for i=1,16 do
                    gridCalls=0
                    now=now+100
                    tick(); finish()
                    maxReads=math.max(maxReads,gridCalls)
                end
                return hitCount(100,100),maxReads <= 5
                """);
        assertArrayEquals(new Object[]{0.0, true}, result);
    }

    @Test
    void reEnablingTrackingRefreshesChangesMissedWhileBothDisplaysWereOff() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(100,100,0)
                sq.world={floorObject(100,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                EtherItemSearch.setMinimapEnabled(false)
                gridCalls=0
                sq.world={}
                now=now+2000
                tick()
                local idle=gridCalls
                EtherItemSearch.setMinimapEnabled(true)
                finish()
                return idle,hitCount(100,100),EtherItemSearch.lastTargets["Base.Axe"]
                """);
        assertArrayEquals(new Object[]{0.0, 0.0, true}, result);
    }

    @Test
    void dirtyContributionsOutsideTheNewBoundsCannotReappear() throws Exception {
        Object[] result = runtime().execute("""
                local old=square(98,100,0)
                old.world={floorObject(98,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                Events.OnContainerUpdate.fire(old)
                player.x=102
                now=now+1000
                tick(); finish()
                return hitCount(98,100),reads[key(98,100,0)] or 0
                """);
        assertArrayEquals(new Object[]{0.0, 1.0}, result);
    }

    @Test
    void failedScanStopsWithoutAnAutomaticRetryLoop() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(98,98,-1)
                local bad=item("Base.Axe")
                bad.getFullType=function() error("unloaded item") end
                sq.objects={containerObject(98,98,-1,{bad})}
                callbacks=0
                EtherItemSearch.startScan({["Base.Axe"]=true},true,function() callbacks=callbacks+1 end)
                tick()
                local attempts=gridCalls
                for i=1,5 do now=now+2000; tick() end
                return callbacks,EtherItemSearch._cursor == nil,gridCalls == attempts
                """);
        assertArrayEquals(new Object[]{1.0, true, true}, result);
    }

    @Test
    void inventoryWindowEndEventMarksTheActualContainerSquare() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(102,100,0)
                sq.world={floorObject(102,100,0,item("Base.Axe"))}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                sq.world={}
                local page={backpacks={{inventory={getSourceGrid=function() return sq end}}}}
                Events.OnRefreshInventoryWindowContainers.fire(page,"begin")
                now=now+250
                tick()
                local before=hitCount(102,100)
                Events.OnRefreshInventoryWindowContainers.fire(page,"end")
                now=now+250
                tick(); finish()
                return before,hitCount(102,100)
                """);
        assertArrayEquals(new Object[]{1.0, 0.0}, result);
    }

    @Test
    void eventDuringAnInFlightScanIsStillProcessedAfterItCompletes() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(98,98,-1)
                local items={}
                for i=1,40 do items[i]=item("Base.Axe") end
                sq.objects={containerObject(98,98,-1,items)}
                clockStep=1
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                tick()
                sq.objects={}
                Events.OnContainerUpdate.fire(sq)
                finish()
                now=now+1000
                tick(); finish()
                return hitCount(98,98),EtherItemSearch.refreshPending
                """);
        assertArrayEquals(new Object[]{0.0, false}, result);
    }

    @Test
    void sameTileContributionsMergeWithoutDoublingAfterADirtyRescan() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(100,100,0)
                sq.objects={containerObject(100,100,0,{item("Base.Axe"),item("Base.Axe")})}
                sq.world={floorObject(100,100,0,item("Base.Axe"))}
                vehicles={vehicle(1,100,100,{item("Base.Axe")})}
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                local before=hitCount(100,100)
                Events.OnContainerUpdate.fire(sq)
                now=now+250
                tick(); finish()
                return before,hitCount(100,100),#EtherItemSearch.results
                """);
        assertArrayEquals(new Object[]{4.0, 4.0, 1.0}, result);
    }

    @Test
    void scanModuleReloadRemovesItsOldEventSubscriptions() throws Exception {
        LuaTestRuntime lua = runtime();
        lua.resource("modcore/lua/components/ui/EtherItemSearch.lua");
        Object[] result = lua.execute("""
                return #Events.OnPlayerUpdate.handlers,#Events.OnContainerUpdate.handlers,
                    #Events.OnRefreshInventoryWindowContainers.handlers,#Events.OnPostUIDraw.handlers
                """);
        assertArrayEquals(new Object[]{1.0, 1.0, 1.0, 1.0}, result);
    }

    @Test
    void patrolKeepsTheOriginalWindowAndMovementReferenceForSmallMoves() throws Exception {
        Object[] result = runtime().execute("""
                EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                finish()
                EtherItemSearch.patrolMs=100
                EtherItemSearch.patrolCells=5
                player.x=101
                gridCalls=0
                now=now+100
                tick(); finish()
                return EtherItemSearch._bounds.cx,EtherItemSearch._scanX,gridCalls
                """);
        assertArrayEquals(new Object[]{100.0, 100.0, 5.0}, result);
    }

    @Test
    void oldEnableHandleCannotReuseAnInFlightRequestAcrossPause() throws Exception {
        Object[] result = runtime().execute("""
                local sq=square(98,98,-1)
                local first=item("Base.Axe")
                first.getFullType=function() readFirst=true; return "Base.Axe" end
                sq.world={floorObject(98,98,-1,first)}
                local many={}
                for i=1,200 do many[#many+1]=item("Base.Plank") end
                square(98,99,-1).objects={containerObject(98,99,-1,many)}
                clockStep=1; readFirst=false
                local oldHandle=EtherItemSearch.startScan({["Base.Axe"]=true},true,nil)
                for i=1,100 do tick(); if readFirst then break end end
                assert(readFirst,"first item was not sampled")
                local inFlight=EtherItemSearch._cursor ~= nil
                EtherItemSearch.setMinimapEnabled(false)
                sq.world={}
                EtherItemSearch.setItemEspEnabled(true,oldHandle)
                finish()
                return inFlight,hitCount(98,98),UIMap.drawItems,UIMap.drawItemEsp
                """);
        assertArrayEquals(new Object[]{true,0.0,false,true}, result);
    }

    private static LuaTestRuntime runtime() throws Exception {
        return ItemSearchTestRuntime.create();
    }
}
