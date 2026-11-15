package modcore;

import java.io.IOException;

/** Native map, world and input boundaries; production Lua owns projection use, menus and actions. */
final class MinimapTestRuntime {
    private MinimapTestRuntime() {}

    static LuaTestRuntime create() throws IOException {
        LuaTestRuntime lua = LuaPanelRuntime.create();
        lua.execute("""
                local function event()
                    local e = { handlers = {} }
                    e.Add = function(fn) table.insert(e.handlers, fn) end
                    e.Remove = function(fn)
                        for i = #e.handlers, 1, -1 do
                            if e.handlers[i] == fn then table.remove(e.handlers, i) end
                        end
                    end
                    e.fire = function(...)
                        for _, fn in ipairs(e.handlers) do fn(...) end
                    end
                    return e
                end
                Events = { OnTick = event(), OnPreUIDraw = event(), OnGameStart = event(),
                    OnMainMenuEnter = event(), EveryTenMinutes = event() }
                now = 10000
                function getTimestampMs() return now end
                Keyboard = { KEY_W=1, KEY_A=2, KEY_S=3, KEY_D=4, KEY_SPACE=5, KEY_ESCAPE=6 }
                keys = {}
                function isKeyDown(key) return keys[key] == true end
                function isMouseButtonDown() return false end
                multiplayer = false
                function isMultiplayer() return multiplayer end
                function javaList(values)
                    return { size = function() return #values end,
                        get = function(self, i) return values[i + 1] end }
                end
                function actor(x, y, name)
                    return { x=x, y=y, name=name or "player", vehicle=nil, id=2,
                        getX=function(self) return self.x end,
                        getY=function(self) return self.y end,
                        getZ=function() return 0 end,
                        setX=function(self, value) self.x=value end,
                        setY=function(self, value) self.y=value end,
                        getVehicle=function(self) return self.vehicle end,
                        getUsername=function(self) return self.name end,
                        getOnlineID=function(self) return self.id or 1 end,
                        isInvisible=function(self) return self.invisible == true end,
                        isDead=function() return false end,
                        Say=function(self, text) self.said=text end }
                end
                player = actor(100, 100)
                player.id=1
                zombies = {}; vehicles = {}; players = {}
                cellReads = 0
                cell = {
                    getZombieList=function() cellReads=cellReads+1; return javaList(zombies) end,
                    getVehicles=function() cellReads=cellReads+1; return {
                        toArray=function() return vehicles end,
                        size=function() return #vehicles end,
                        get=function(self,i) return vehicles[i+1] end,
                    } end,
                    getGridSquare=function(self, x, y, z)
                        if x < 90 or x > 115 or y < 90 or y > 110 then return nil end
                        return { getX=function() return x end, getY=function() return y end,
                            getZ=function() return z end, getPathMatrix=function() return false end,
                            getDoorOrWindow=function() return nil end }
                    end,
                }
                function getCell() return cell end
                function getOnlinePlayers() cellReads=cellReads+1; return javaList(players) end
                validChunk = true
                function getWorld() return { getMetaGrid=function()
                    return { isValidChunk=function() return validChunk end }
                end } end
                function getLanguage() return "EN" end
                function isMapDrawZombies() return false end
                function isMapDrawVehicles() return false end
                function isMapDrawAllPlayers() return false end
                function isMapDrawLocalPlayer() return false end
                function isMapDrawItems() return false end
                EtherItemSearch = { results={} }
                routeCount = 0
                function autoDriveGetRouteCount() return routeCount end
                function autoDriveIsActive() return false end
                function autoDriveTarget(x, y) anchoredX=x; anchoredY=y; return true end
                function autoDriveGetMessage() return "" end
                function autoDriveResume() resumed=true; return true end
                function autoDriveClearRoute() cleared=true; return true end
                function autoDriveGetSpeedLimit() return 70 end
                menu = nil; menuGets=0
                ISContextMenu = { get=function(index, x, y)
                    menuGets=menuGets+1
                    menu={ options={}, requestX=x, requestY=y, isVisible=function() return true end }
                    menu.addOption=function(self, name, target, fn, x, y)
                        local o={ name=name, target=target, fn=fn, x=x, y=y }
                        table.insert(self.options, o)
                        return o
                    end
                    return menu
                end }
                function getPlayerContextMenu() return menu end
                function choose(name)
                    for _, o in ipairs(menu.options) do
                        if o.name == name then return o.fn(o.target, o.x, o.y) end
                    end
                    error("missing option " .. name)
                end
                function ISPanel:derive(name)
                    local cls = setmetatable({ Type=name }, { __index=self })
                    cls.__index=cls
                    return cls
                end
                function ISPanel:new(x, y, w, h)
                    return setmetatable({ x=x, y=y, width=w, height=h,
                        children={}, visible=true, rects={}, labels={}, lines={} }, self)
                end
                function ISPanel:getWidth() return self.width end
                function ISPanel:getHeight() return self.height end
                function ISPanel:getAbsoluteX() return self.x end
                function ISPanel:getAbsoluteY() return self.y end
                function ISPanel:getMouseX() return self.mouseX or 0 end
                function ISPanel:getMouseY() return self.mouseY or 0 end
                function ISPanel:isVisible() return self.visible ~= false end
                function ISPanel:isReallyVisible()
                    if self.visible == false or self.removed then return false end
                    return self.parent == nil or self.parent:isReallyVisible()
                end
                function ISPanel:addChild(child)
                    child.parent=self
                    table.insert(self.children, child)
                end
                function ISPanel:suspendStencil() end
                function ISPanel:resumeStencil() end
                function ISPanel:clampStencilRectToParent() end
                function ISPanel:clearStencilRect() end
                function ISPanel:drawRect(x,y,w,h,a,r,g,b)
                    table.insert(self.rects,{x=x,y=y,w=w,h=h,a=a,r=r,g=g,b=b})
                end
                function ISPanel:drawRectBorder() end
                function ISPanel:drawTextCentre(text,x,y) table.insert(self.labels,{text=text,x=x,y=y}) end
                function ISPanel:drawText() end
                function ISPanel:render() end
                function mapApi(view)
                    local api={ cx=100, cy=100, scale=1, iso=false, centers=0 }
                    function api:centerOn(x,y) self.cx=x; self.cy=y; self.centers=self.centers+1 end
                    function api:getCenterWorldX() return self.cx end
                    function api:getCenterWorldY() return self.cy end
                    function api:getZoomF() return 18 end
                    function api:getWorldScale() return self.scale end
                    function api:worldToUIX(x,y)
                        if self.iso then return view.width/2 + ((x-self.cx)-(y-self.cy))*self.scale end
                        return view.width/2 + (x-self.cx)*self.scale
                    end
                    function api:worldToUIY(x,y)
                        if self.iso then return view.height/2 + ((x-self.cx)+(y-self.cy))*self.scale/2 end
                        return view.height/2 + (y-self.cy)*self.scale
                    end
                    function api:uiToWorldX(x,y)
                        if self.iso then return self.cx+(x-view.width/2)/self.scale/2+(y-view.height/2)/self.scale end
                        return self.cx+(x-view.width/2)/self.scale
                    end
                    function api:uiToWorldY(x,y)
                        if self.iso then return self.cy-(x-view.width/2)/self.scale/2+(y-view.height/2)/self.scale end
                        return self.cy+(y-view.height/2)/self.scale
                    end
                    function api:zoomAt(x,y,d) self.scale=self.scale+d; self.zoomX=x; self.zoomY=y end
                    function api:resetView() end
                    return api
                end
                ISWorldMap = ISPanel:derive("ISWorldMap")
                function ISWorldMap:new(x,y,w,h)
                    local o=ISPanel.new(self,x,y,w,h)
                    o.mapAPI=mapApi(o)
                    return o
                end
                function ISWorldMap:instantiate() self:createChildren() end
                function ISWorldMap:initDataAndStyle() end
                function ISWorldMap:onRightMouseUp(x,y)
                    if originalMenu then
                        local ctx=ISContextMenu.get(0,x+self:getAbsoluteX(),y+self:getAbsoluteY())
                        ctx:addOption("original",nil,function() end)
                        return true
                    end
                    return symbolsConsumed == true
                end
                ISMap = ISPanel:derive("ISMap")
                ISMap.new = ISWorldMap.new
                ISMap.onRightMouseUp = ISWorldMap.onRightMouseUp
                MainScreen = {}
                HaloTextHelper = { getColorRed=function() return {} end,
                    getColorGreen=function() return {} end,
                    addText=function(p,text) p.halo=text end }
                """);
        loadOptional(lua, "MapActions");
        loadOptional(lua, "MapViewport");
        loadOptional(lua, "MapData");
        loadOptional(lua, "MapLayers");
        lua.resource("modcore/lua/components/ui/UIMap.lua");
        lua.resource("modcore/lua/components/drive/AutoDriveMap.lua");
        return lua;
    }

    static void loadOptional(LuaTestRuntime lua, String module) throws IOException {
        String name = "modcore/lua/components/ui/" + module + ".lua";
        if (MinimapTestRuntime.class.getClassLoader().getResource(name) != null) lua.resource(name);
    }
}
