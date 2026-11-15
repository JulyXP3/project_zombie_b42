--*********************************************************
--* MapData: 每个实际 UI 绘制周期采集一次, 两个视图共用标量快照。
--* 不加动态坐标时间门限, 隐身停更五秒后隐藏直到再次移动。
--*********************************************************
if MapData ~= nil then
    Events.OnMainMenuEnter.Remove(MapData.reset);
end
MapData = {};

local frame = { zombies = {}, vehicles = {}, players = {}, remote = {} };
local records = {};
local currentIds = {};
local owner = nil;
local available = false;
local collecting = false;
local route = { points = {}, count = 0, kind = "" };
local routeReady = false;
local FREEZE_MS = 5000;

local function clear(data)
    for key in pairs(data) do data[key] = nil; end
end

local function clearFrame()
    clear(frame.zombies);
    clear(frame.vehicles);
    clear(frame.players);
    clear(frame.remote);
    frame.localX, frame.localY, frame.localName = nil, nil, nil;
end

function MapData.reset()
    clearFrame();
    clear(records);
    clear(currentIds);
    owner, available, routeReady = nil, false, false;
    clear(route.points);
    route.count = 0;
end
Events.OnMainMenuEnter.Remove(MapData.reset);
Events.OnMainMenuEnter.Add(MapData.reset);

local function remotePlayers(now)
    local invTag, lastTag = tr("UI_Map_InvisibleTag"), tr("UI_Map_LastKnown");
    local ids = currentIds;
    clear(ids);
    clear(frame.remote);
    local function emit(rec, last)
        local label = rec.name;
        if rec.inv then
            local suffix = invTag .. (last and lastTag or "");
            if rec.suffix ~= suffix or rec.labelName ~= rec.name then
                rec.label, rec.labelName, rec.suffix = rec.name .. " " .. suffix, rec.name, suffix;
            end
            label = rec.label;
        end
        local out = frame.remote;
        out[#out + 1], out[#out + 2], out[#out + 3] = rec.x, rec.y, label;
    end
    for i = 1, #frame.players, 5 do
        local x, y, name, id, inv = frame.players[i], frame.players[i + 1],
            frame.players[i + 2], frame.players[i + 3], frame.players[i + 4];
        ids[id] = true;
        local rec = records[id];
        if rec == nil then
            rec = {};
            records[id] = rec;
        end
        if rec.x ~= x or rec.y ~= y or not inv or not rec.inv then
            rec.x, rec.y, rec.inv, rec.ts = x, y, inv, now;
        end
        rec.name = name;
        rec.hidden = inv and now - rec.ts > FREEZE_MS;
        if not rec.hidden then emit(rec, false); end
    end
    for id, rec in pairs(records) do
        if not ids[id] then
            if not rec.inv or now - rec.ts > FREEZE_MS then
                records[id] = nil;
            else
                emit(rec, true);
            end
        end
    end
end

function MapData.beginFrame()
    if collecting then return; end
    local player = getPlayer();
    if owner ~= player then MapData.reset(); owner = player; end
    available, routeReady = false, false;
    if player == nil then return; end
    local visible = false;
    MapViewport.eachVisible(function() visible = true; end);
    if not visible then clearFrame(); return; end
    UIMap.ensureDrawFlags();
    local flags = (UIMap.drawZombies and 1 or 0) + (UIMap.drawVehicles and 2 or 0)
        + (UIMap.drawAllPlayers and 4 or 0) + (UIMap.drawLocalPlayer and 8 or 0);
    if flags == 0 or type(mapCollectFrame) ~= "function" then clearFrame(); return; end
    collecting = true;
    local ok, collected = pcall(mapCollectFrame, frame, flags);
    collecting = false;
    if not ok or not collected then clearFrame(); return; end
    if UIMap.drawAllPlayers then remotePlayers(getTimestampMs());
    else clear(records); end
    available = true;
end

function MapData.current()
    return frame, available;
end

function MapData.currentRoute()
    if routeReady then return route; end
    routeReady = true;
    clear(route.points);
    route.count, route.kind = 0, "";
    if getPlayer() == nil or type(autoDriveGetRouteCount) ~= "function" then return route; end
    local count = autoDriveGetRouteCount();
    if count < 1 then return route; end
    route.kind = type(autoDriveGetRouteKind) == "function" and autoDriveGetRouteKind() or "";
    for i = 0, count - 1 do
        local p = i * 2 + 1;
        route.points[p], route.points[p + 1] = autoDriveGetRouteX(i), autoDriveGetRouteY(i);
    end
    route.count = count;
    return route;
end
