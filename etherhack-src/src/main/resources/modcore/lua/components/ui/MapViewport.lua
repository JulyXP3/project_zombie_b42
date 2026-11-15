--*********************************************************
--* MapViewport: 底图前准备相机; 图层按实际四角视口筛选与投影。
--* 只查看当前小地图和当前地图页, 不长期持有世界或旧窗口对象。
--*********************************************************
if MapViewport ~= nil then
    Events.OnPreUIDraw.Remove(MapViewport.onPreUIDraw);
end
MapViewport = {};

local function finite(value)
    return type(value) == "number" and value == value and math.abs(value) < math.huge;
end

function MapViewport.eachVisible(fn)
    local mini = UIMovableMiniMap ~= nil and UIMovableMiniMap.instance or nil;
    local small = mini ~= nil and mini.map or nil;
    if small ~= nil and small.mapAPI ~= nil and not small.removed and small:isReallyVisible() then
        fn(small);
    end
    local main = EtherMain ~= nil and EtherMain.instance or nil;
    local nav = main ~= nil and main.buttonsPanel or nil;
    local panel = nav ~= nil and nav.currentPanel or nil;
    local large = panel ~= nil and panel.map or nil;
    if large ~= nil and large ~= small and large.mapAPI ~= nil and not large.removed
        and large:isReallyVisible() then
        fn(large);
    end
end

function MapViewport.prepare(view, player)
    view.localPlayer = player;
    if player == nil or view.mapAPI == nil or not view.centerByPlayer or view.dragging then return; end
    local target = player:getVehicle() or player;
    local x, y = target:getX(), target:getY();
    if finite(x) and finite(y) then view.mapAPI:centerOn(x, y); end
end

function MapViewport.onPreUIDraw()
    local player = getPlayer();
    MapViewport.eachVisible(function(view) MapViewport.prepare(view, player); end);
    if MapData ~= nil then MapData.beginFrame(); end
end
Events.OnPreUIDraw.Remove(MapViewport.onPreUIDraw);
Events.OnPreUIDraw.Add(MapViewport.onPreUIDraw);

function MapViewport.context(view)
    local api = view.mapAPI;
    if api == nil then return nil; end
    local scale = api:getWorldScale();
    local w, h = view:getWidth(), view:getHeight();
    if not finite(scale) or scale <= 0 or w <= 0 or h <= 0 then return nil; end
    local c = view.mapViewport or {};
    view.mapViewport = c;
    c.api, c.w, c.h, c.pad = api, w, h, 6;
    c.scale, c.size = scale, math.max(2, math.min(5, 125 / scale));
    c.minX, c.minY, c.maxX, c.maxY = math.huge, math.huge, -math.huge, -math.huge;
    for i = 0, 3 do
        local ux = (i == 1 or i == 2) and w + c.pad or -c.pad;
        local uy = i >= 2 and h + c.pad or -c.pad;
        local x, y = api:uiToWorldX(ux, uy), api:uiToWorldY(ux, uy);
        if not finite(x) or not finite(y) then return nil; end
        c.minX, c.maxX = math.min(c.minX, x), math.max(c.maxX, x);
        c.minY, c.maxY = math.min(c.minY, y), math.max(c.maxY, y);
    end
    return c;
end

function MapViewport.project(c, wx, wy)
    if not finite(wx) or not finite(wy) or wx < c.minX or wx > c.maxX
        or wy < c.minY or wy > c.maxY then return nil; end
    local x, y = c.api:worldToUIX(wx, wy), c.api:worldToUIY(wx, wy);
    if not finite(x) or not finite(y) or x < -c.pad or x > c.w + c.pad
        or y < -c.pad or y > c.h + c.pad then return nil; end
    return x, y;
end
