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

    private static LuaTestRuntime runtime() throws Exception {
        LuaTestRuntime lua = MinimapTestRuntime.create();
        lua.execute("""
                local function event()
                    local e={handlers={}}
                    e.Add=function(fn) table.insert(e.handlers,fn) end
                    e.Remove=function(fn)
                        for i=#e.handlers,1,-1 do if e.handlers[i]==fn then table.remove(e.handlers,i) end end
                    end
                    e.fire=function(... ) for _, fn in ipairs(e.handlers) do fn(... ) end end
                    return e
                end
                for _, name in ipairs({"OnPlayerUpdate","OnRefreshInventoryWindowContainers",
                    "OnPostUIDraw","OnContainerUpdate","OnObjectAdded","OnObjectAboutToBeRemoved","LoadGridsquare"}) do
                    Events[name]=event()
                end
                now=10000; clockStep=0; gridCalls=0; itemChecks=0
                function getTimestampMs() now=now+clockStep; return now end
                reads={}; squares={}
                function key(x,y,z) return x .. "," .. y .. "," .. z end
                function instanceof(obj, name) return obj ~= nil and obj.class == name end
                function item(full,bagItems)
                    return {getFullType=function() itemChecks=itemChecks+1; return full end,
                        getType=function() return string.sub(full,6) end,
                        getDisplayName=function() return full end,
                        IsInventoryContainer=function() return bagItems ~= nil end,
                        getInventory=function() return { getItems=function() return javaList(bagItems) end } end}
                end
                function square(x,y,z)
                    local sq={class="IsoGridSquare",x=x,y=y,z=z,objects={},world={},bodies={}}
                    sq.getX=function(self) return self.x end
                    sq.getY=function(self) return self.y end
                    sq.getZ=function(self) return self.z end
                    sq.getObjects=function(self) return javaList(self.objects) end
                    sq.getWorldObjects=function(self) return javaList(self.world) end
                    sq.getDeadBodys=function(self) return javaList(self.bodies) end
                    squares[key(x,y,z)]=sq
                    return sq
                end
                local function coords(o,x,y,z)
                    o.getX=function() return x end
                    o.getY=function() return y end
                    o.getZ=function() return z end
                    return o
                end
                function containerObject(x,y,z,items)
                    return coords({getContainerCount=function() return 1 end,
                        getContainerByIndex=function() return {getItems=function() return javaList(items) end} end},x,y,z)
                end
                function floorObject(x,y,z,item)
                    return coords({getItem=function() return item end},x,y,z)
                end
                function bodyObject(x,y,z,items)
                    return coords({getContainer=function() return {getItems=function() return javaList(items) end} end},x,y,z)
                end
                function vehicle(id,x,y,items)
                    local part=coords({getItemContainer=function() return {getItems=function() return javaList(items) end} end},x,y,0)
                    return coords({getId=function() return id end,getPartCount=function() return 1 end,
                        getPartByIndex=function() return part end},x,y,0)
                end
                player.getInventory=function() return {getItems=function() return javaList({}) end} end
                cell.getGridSquare=function(self,x,y,z)
                    gridCalls=gridCalls+1
                    local k=key(x,y,z)
                    reads[k]=(reads[k] or 0)+1
                    return squares[k]
                end
                function tick() Events.OnPlayerUpdate.fire(player) end
                function finish()
                    for i=1,3000 do
                        if EtherItemSearch._cursor == nil then return end
                        tick()
                    end
                    error("scan did not finish")
                end
                function hitCount(x,y)
                    for _, p in ipairs(EtherItemSearch.results or {}) do
                        if p.x==x and p.y==y then return p.count end
                    end
                    return 0
                end
                function setMapDrawItems() end
                UIMap.ensureDrawFlags()
                UIMap.drawItems=true
                """);
        MinimapTestRuntime.loadOptional(lua, "ItemScanIndex");
        lua.resource("modcore/lua/components/ui/EtherItemSearch.lua");
        lua.execute("EtherItemSearch.radius=2; EtherItemSearch.patrolMs=60000; EtherItemSearch._invSize=0");
        return lua;
    }
}
