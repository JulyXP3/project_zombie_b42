UIMap = ISWorldMap:derive("UIMap")

--*********************************************************
--* 地图组件只负责视口交互与图层绘制。
--* MapViewport 底图前准备相机, MapData 同帧供数, MapActions 保留原传送。
--*********************************************************
function UIMap:createChildren() end

--*********************************************************
--* Восстановление настроек
--*********************************************************
function UIMap:restoreSettings()
    if not MainScreen.instance or not MainScreen.instance.inGame then return end
    local settings = WorldMapSettings.getInstance()
    if settings:getFileVersion() ~= 1 then return end
    local centerX = settings:getDouble("WorldMap.CenterX", 0.0)
    local centerY = settings:getDouble("WorldMap.CenterY", 0.0)
    local zoom = settings:getDouble("WorldMap.Zoom", 0.0)
    if zoom == 0.0 then return end
    local isometric = settings:getBoolean("WorldMap.Isometric")
    if self.localPlayer ~= nil then
        centerX = self.localPlayer:getX()
        centerY = self.localPlayer:getY()
        zoom = 18
    end
    self.mapAPI:centerOn(centerX, centerY)
    self.mapAPI:setZoom(zoom)
    self.mapAPI:setBoolean("Isometric", isometric)
end

function UIMap:onToggleSymbols() end

function UIMap:prerender()
    -- 晚于原版底图, 不在这里修改相机中心。
end

--*********************************************************
--* 图层显示开关 (五个快捷按钮与地图页共用)
--*********************************************************
function UIMap.ensureDrawFlags()
    if UIMap.drawZombies ~= nil then return end
    UIMap.drawZombies = isMapDrawZombies()
    UIMap.drawVehicles = isMapDrawVehicles()
    UIMap.drawAllPlayers = isMapDrawAllPlayers()
    UIMap.drawLocalPlayer = isMapDrawLocalPlayer()
    UIMap.drawItems = isMapDrawItems()
    UIMap.drawItemEsp = false
end

function UIMap:render()
    self.localPlayer = getPlayer()
    if self.localPlayer == nil then return end
    local viewport = MapViewport.context(self)
    if viewport == nil then return end
    UIMap.ensureDrawFlags()
    local frame, available = MapData.current()
    self:suspendStencil()
    self:clampStencilRectToParent(0, 0, self:getWidth(), self:getHeight())
    local ok, err = pcall(MapLayers.render, self, viewport, frame, available)
    self:clearStencilRect()
    self:resumeStencil()
    if not ok then error(err) end
end

function UIMap:onJoypadDown() end

--*********************************************************
--* 拖动/缩放沿用原语义: 拖动中停跟随, 松手后下次绘制回中。
--*********************************************************
function UIMap:onMouseDown(x, y)
    self.dragging = true
    self.dragMoved = false
    self.dragStartX = x
    self.dragStartY = y
    self.dragStartCX = self.mapAPI:getCenterWorldX()
    self.dragStartCY = self.mapAPI:getCenterWorldY()
    self.dragStartZoomF = self.mapAPI:getZoomF()
    self.dragStartWorldX = self.mapAPI:uiToWorldX(x, y)
    self.dragStartWorldY = self.mapAPI:uiToWorldY(x, y)
    return true
end

function UIMap:onMouseMove(dx, dy)
    if self.dragging then
        local mouseX = self:getMouseX()
        local mouseY = self:getMouseY()
        if not self.dragMoved and math.abs(mouseX - self.dragStartX) <= 4
            and math.abs(mouseY - self.dragStartY) <= 4 then return end
        self.dragMoved = true
        local worldX = self.mapAPI:uiToWorldX(mouseX, mouseY, self.dragStartZoomF, self.dragStartCX, self.dragStartCY)
        local worldY = self.mapAPI:uiToWorldY(mouseX, mouseY, self.dragStartZoomF, self.dragStartCX, self.dragStartCY)
        self.mapAPI:centerOn(self.dragStartCX + self.dragStartWorldX - worldX, self.dragStartCY + self.dragStartWorldY - worldY)
    end
    return true
end

function UIMap:onMouseMoveOutside(dx, dy)
    return self:onMouseMove(dx, dy)
end

function UIMap:onMouseUp(x, y)
    self.dragging = false
    return true
end

function UIMap:onMouseUpOutside(x, y)
    self.dragging = false
    return true
end

function UIMap:onMouseWheel(del)
    self.mapAPI:zoomAt(self:getMouseX(), self:getMouseY(), del)
    return true
end

function UIMap:onRightMouseDown(x, y)
    return false
end

function UIMap:onRightMouseUp(x, y)
    local context = ISContextMenu.get(0, x + self:getAbsoluteX(), y + self:getAbsoluteY())
    MapActions.addTeleportOptions(context, self.mapAPI:uiToWorldX(x, y), self.mapAPI:uiToWorldY(x, y))
    return true
end

-- 同名入口保留, 小地图/面板/原版地图菜单都委托一份动作实现。
function UIMap:onTeleport(x, y)
    return MapActions.onTeleport(self, x, y)
end

function UIMap:onTeleportLinear(x, y)
    return MapActions.onTeleportLinear(self, x, y)
end

function UIMap:startVehicleTeleport(x, y)
    return MapActions.startVehicleTeleport(self, x, y)
end

function UIMap:new(x, y, width, height)
    local o = ISWorldMap:new(x, y, width, height)
    setmetatable(o, self)
    self.__index = self
    o.localPlayer = getPlayer()
    o.localPlayerColor = { r = 0.5, g = 1.0, b = 0.5, a = 1.0 }
    o.playerColor = { r = 1.0, g = 0.2, b = 0.2, a = 1.0 }
    o.vehicleColor = { r = 0.2, g = 0.2, b = 1.0, a = 1.0 }
    o.zombieColor = { r = 1.0, g = 0.5, b = 0.3, a = 1.0 }
    o.centerByPlayer = false
    return o
end
