package modcore;

/** Small game-world boundary shared by production-service behavior tests. */
final class ItemSearchTestRuntime {
    private ItemSearchTestRuntime() {}

    static LuaTestRuntime create() throws Exception {
        return create("");
    }

    static LuaTestRuntime create(String beforeModuleLoad) throws Exception {
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
                    sq.getStaticMovingObjects=function(self) return javaList(self.bodies) end
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
                    return coords({class="IsoDeadBody",getContainer=function() return {getItems=function() return javaList(items) end} end},x,y,z)
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
        if (!beforeModuleLoad.isEmpty()) lua.execute(beforeModuleLoad);
        ItemScanLuaWorld.install(lua);
        MinimapTestRuntime.loadOptional(lua, "ItemScanIndex");
        lua.resource("modcore/lua/components/ui/EtherItemSearch.lua");
        lua.execute("EtherItemSearch.radius=2; EtherItemSearch.patrolMs=60000; EtherItemSearch._invSize=0");
        return lua;
    }
}
