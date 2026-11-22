-- Item search controller: Java owns all loaded-layer traversal, source deltas and complete export.
if EtherItemSearch ~= nil and EtherItemSearch._handlers ~= nil then
    for name, fn in pairs(EtherItemSearch._handlers) do
        if Events[name] ~= nil then Events[name].Remove(fn); end
    end
end
if EtherItemSearch ~= nil then
    for _, hook in ipairs(EtherItemSearch._pickupHooks or {}) do
        -- Restore only our own wrapper; preserve later changes by other modules.
        if hook.owner[hook.key] == hook.wrapped then hook.owner[hook.key] = hook.previous; end
    end
end
EtherItemSearch = EtherItemSearch or {};
EtherItemSearch.results, EtherItemSearch.lastTargets, EtherItemSearch.progress = nil, nil, nil;
EtherItemSearch.radius, EtherItemSearch.stepBudgetMs = 56, 3;
EtherItemSearch.debounceMs, EtherItemSearch.maxDebounceMs = 250, 1000;
EtherItemSearch.moveRefreshTiles, EtherItemSearch.moveRefreshMs = 2, 1000;
-- Vehicle-speed adaptation: sample recent travel, then shrink the relocation throttle so a fast
-- vehicle does not wait a full second between window updates.
EtherItemSearch.speedWindowMs, EtherItemSearch.speedFastTiles, EtherItemSearch.speedMidTiles = 500, 8, 4;
EtherItemSearch.moveThrottleFastMs, EtherItemSearch.moveThrottleMidMs = 250, 500;
-- Let an in-flight scan finish while the vehicle is still inside this many scan radii; restarting
-- before completion would starve the request forever and never publish a result.
EtherItemSearch.relocateTolerance = 2;
EtherItemSearch._speedSamples = {};
EtherItemSearch.patrolMs, EtherItemSearch.patrolCells = 1000, 128;
-- Layer preset (persisted): 0 = all loaded floors, 1 = current +-2, 2 = current only.
EtherItemSearch.layerMode = type(getItemScanLayerMode) == "function" and (getItemScanLayerMode() or 0) or 0;
EtherItemSearch.refreshPending, EtherItemSearch.lastChangeAt, EtherItemSearch.lastScanAt = false, 0, 0;
EtherItemSearch._cursor, EtherItemSearch._enableRequestHandle, EtherItemSearch._bounds = nil, nil, nil;
EtherItemSearch._generation = (EtherItemSearch._generation or 0) + 1;
EtherItemSearch._contributions, EtherItemSearch._contributionKeys, EtherItemSearch._patrolOffset = nil, nil, nil;
EtherItemSearch._dirty, EtherItemSearch._firstDirtyAt, EtherItemSearch._failed = {}, nil, false;
local handlers = {};
EtherItemSearch._handlers = handlers;
local sessionPlayer, sessionCell = nil, nil;

local function enabled() return UIMap ~= nil and (UIMap.drawItems or UIMap.drawItemEsp); end
local function finite(v) return type(v) == "number" and v == v and math.abs(v) < math.huge; end
local function tr(key) if type(getTranslate) == "function" then return getTranslate(key); end return key; end
local floorLabels, floorNumberText, basementNumberText = {}, nil, nil;
function EtherItemSearch.refreshFloorLabels()
    local upper, lower = tr("UI_RadarPanel_FloorNumber"), tr("UI_RadarPanel_BasementNumber");
    if upper ~= floorNumberText or lower ~= basementNumberText then
        floorLabels, floorNumberText, basementNumberText = {}, upper, lower;
    end
end
function EtherItemSearch.floorLabel(z)
    if not finite(z) then return ""; end
    z = math.floor(z);if z < -32 or z > 31 then return ""; end
    local text = floorLabels[z];
    if text == nil then
        local number = z >= 0 and z+1 or -z;
        local template = z >= 0 and floorNumberText or basementNumberText;
        text = string.gsub(template,"{n}",tostring(number));floorLabels[z] = text;
    end
    return text;
end
EtherItemSearch.refreshFloorLabels();

local function noteSpeed(x, y, now)
    if not finite(x) or not finite(y) then return; end
    local samples = EtherItemSearch._speedSamples;
    samples[#samples + 1] = {x=x, y=y, at=now};
    local cutoff = now - EtherItemSearch.speedWindowMs;
    while #samples > 2 and samples[1].at < cutoff do table.remove(samples, 1) end
end
function EtherItemSearch.moveThrottleMs()
    local samples = EtherItemSearch._speedSamples;
    if #samples < 2 then return EtherItemSearch.moveRefreshMs; end
    local first, last = samples[1], samples[#samples];
    local dt = last.at - first.at;
    if dt <= 0 then return EtherItemSearch.moveRefreshMs; end
    local dx, dy = last.x - first.x, last.y - first.y;
    local tilesPerSecond = math.sqrt(dx * dx + dy * dy) * 1000 / dt;
    if tilesPerSecond >= EtherItemSearch.speedFastTiles then return EtherItemSearch.moveThrottleFastMs; end
    if tilesPerSecond >= EtherItemSearch.speedMidTiles then return EtherItemSearch.moveThrottleMidMs; end
    return EtherItemSearch.moveRefreshMs;
end
local function copyTargets(source)
    local out = {}; for key, value in pairs(source or {}) do if value == true then out[key] = true; end end
    return out;
end
local function targetsEqual(a, b)
    if a == nil or b == nil then return false; end
    for key, value in pairs(a) do if value == true and b[key] ~= true then return false; end end
    for key, value in pairs(b) do if value == true and a[key] ~= true then return false; end end
    return true;
end
local function boundsAround(player)
    local x, y, r = math.floor(player:getX()), math.floor(player:getY()), EtherItemSearch.radius;
    return {cx=x,cy=y,radius=r,x1=x-r,x2=x+r,y1=y-r,y2=y+r};
end
local function contains(b, x, y) return b ~= nil and x >= b.x1 and x <= b.x2 and y >= b.y1 and y <= b.y2; end
local function noteChange()
    local now = getTimestampMs();
    EtherItemSearch.refreshPending, EtherItemSearch.lastChangeAt = true, now;
    EtherItemSearch._firstDirtyAt = EtherItemSearch._firstDirtyAt or now;
end
local function markSquare(x, y, z)
    if not enabled() or EtherItemSearch.lastTargets == nil or EtherItemSearch._failed then return; end
    if not finite(x) or not finite(y) or not finite(z) then return; end
    x, y, z = math.floor(x), math.floor(y), math.floor(z);
    if z < -32 or z > 31 then return; end
    local cur = EtherItemSearch._cursor;
    local b = cur ~= nil and cur.bounds or EtherItemSearch._bounds;
    if not contains(b, x, y) then return; end
    EtherItemSearch._dirty[x .. ":" .. y .. ":" .. z] = {x=x,y=y,z=z}; noteChange();
end
local function failure(cur, frame)
    EtherItemSearch._cursor, EtherItemSearch._enableRequestHandle = nil, nil;
    EtherItemSearch._failed = true; EtherItemSearch.progress = frame;
    -- A failed refresh must not keep showing the previous snapshot as if it were current.
    EtherItemSearch.results, EtherItemSearch.resultVersion, EtherItemSearch.resultCollectedAt = nil, nil, nil;
    if cur ~= nil and cur.onDone ~= nil then cur.onDone(0, frame.stats); end
end
local function mergeDirty(batch, source)
    for key, value in pairs(source or {}) do batch[key] = value; end
end
local function beginScan(targets, silent, onDone, mode, dirty)
    local player, cell = getPlayer(), getCell();
    if player == nil or cell == nil then EtherItemSearch.clear(); if onDone ~= nil then onDone(0,nil); end return; end
    local copied = copyTargets(targets);
    local sameScope = sessionPlayer == player and sessionCell == cell and targetsEqual(copied, EtherItemSearch.lastTargets);
    local batch = {};
    if sameScope then
        local previous = EtherItemSearch._cursor;
        -- Detached batches are acknowledged only by READY. A replaced request must replay them.
        if previous ~= nil then mergeDirty(batch, previous.dirty); end
        mergeDirty(batch, dirty);
        mergeDirty(batch, EtherItemSearch._dirty);
    end
    if not sameScope then
        EtherItemSearch.results, EtherItemSearch.resultVersion, EtherItemSearch.resultCollectedAt = nil, nil, nil;
    end
    EtherItemSearch._generation = EtherItemSearch._generation + 1;
    local b = (mode == "full" or mode == "move" or EtherItemSearch._bounds == nil) and boundsAround(player) or EtherItemSearch._bounds;
    local cur = {version=EtherItemSearch._generation,player=player,cell=cell,mode=mode,bounds=b,
        startedAt=getTimestampMs(),silent=silent,onDone=onDone,dirty=batch};
    sessionPlayer, sessionCell = player, cell;
    EtherItemSearch.lastTargets, EtherItemSearch._failed = copied, false;
    EtherItemSearch.refreshPending, EtherItemSearch._firstDirtyAt, EtherItemSearch._dirty = false, nil, {};
    EtherItemSearch._enableRequestHandle = nil;
    if type(itemScanBegin) ~= "function" or type(itemScanStep) ~= "function" then
        failure(cur,{state="error",phase="done",error="item scan API unavailable"}); return;
    end
    local layerMode = EtherItemSearch.layerMode or 0;
    local patrolDue = mode ~= "full" and (mode == "patrol"
        or cur.startedAt-(EtherItemSearch._lastPatrolAt or EtherItemSearch.lastScanAt) >= EtherItemSearch.patrolMs);
    cur.patrolCells = patrolDue and EtherItemSearch.patrolCells or 0;
    local ok, ticket = pcall(itemScanBegin, copied, mode, EtherItemSearch.radius, batch, cur.patrolCells, layerMode);
    if not ok or ticket == nil or ticket <= 0 then failure(cur,{state="error",phase="done",error=tostring(ticket)}); return; end
    cur.ticket = ticket; EtherItemSearch._cursor = cur;
    EtherItemSearch.progress = {state="running",phase="coverage",ticket=ticket,stats={}};
    if mode == "full" or mode == "move" then
        EtherItemSearch._scanX, EtherItemSearch._scanY = b.cx, b.cy;
        EtherItemSearch._lastPriorityAt = cur.startedAt;
    end
    if mode == "full" then EtherItemSearch._lastPatrolAt = cur.startedAt; end
    EtherItemSearch._playerZ = math.floor(player:getZ());
    return cur.version;
end
function EtherItemSearch.startScan(targets, silent, onDone)
    local handle = beginScan(targets, silent, onDone, "full", nil);
    EtherItemSearch._enableRequestHandle = handle; return handle;
end
function EtherItemSearch.stepScan()
    local cur = EtherItemSearch._cursor; if cur == nil then return; end
    EtherItemSearch._enableRequestHandle = nil;
    if cur.version ~= EtherItemSearch._generation or cur.player ~= getPlayer() or cur.cell ~= getCell() then EtherItemSearch.clear(); return; end
    local player = cur.player;
    local driftX, driftY = math.abs(math.floor(player:getX())-cur.bounds.cx), math.abs(math.floor(player:getY())-cur.bounds.cy);
    local tolerance = EtherItemSearch.radius * (EtherItemSearch.relocateTolerance or 1);
    if cur.bounds.radius ~= EtherItemSearch.radius
        or driftX > tolerance or driftY > tolerance then
        beginScan(EtherItemSearch.lastTargets,cur.silent,cur.onDone,"move",EtherItemSearch._dirty); return;
    end
    local ok, frame = pcall(itemScanStep, cur.ticket, EtherItemSearch.stepBudgetMs);
    if not ok or type(frame) ~= "table" then failure(cur,{state="error",phase="done",error=tostring(frame)}); return; end
    EtherItemSearch.progress = frame; cur.phase = frame.phase;
    if frame.state == "running" then return; end
    if frame.state == "error" or frame.state == "timeout" then failure(cur,frame); return; end
    if frame.state ~= "ready" or type(frame.results) ~= "table" then
        failure(cur,{state="error",phase=frame.phase,error=frame.error or "incomplete scan delivery"}); return;
    end
    EtherItemSearch.results, EtherItemSearch._bounds = frame.results, frame.window or cur.bounds;
    EtherItemSearch.lastScanAt = getTimestampMs();
    EtherItemSearch.resultVersion, EtherItemSearch.resultCollectedAt = cur.version, EtherItemSearch.lastScanAt;
    if (cur.patrolCells or 0) > 0 then EtherItemSearch._lastPatrolAt = EtherItemSearch.lastScanAt; end
    EtherItemSearch._cursor = nil;
    if cur.onDone ~= nil then cur.onDone(#frame.results, frame.stats); end
end
function EtherItemSearch.requestRefresh()
    if not enabled() or EtherItemSearch.lastTargets == nil or EtherItemSearch._failed then return; end
    local player = getPlayer();if player ~= nil then markSquare(player:getX(),player:getY(),player:getZ()); end
    noteChange();
end
-- Resolve ownership, not contents: no recursive bag scan and no persistent item/container references.
local function sourceMethod(object, name)
    if object == nil then return nil; end
    local ok, value = pcall(function()
        local fn = object[name];
        if fn ~= nil then return fn(object); end
    end);
    if ok then return value; end
end
local function sourcePosition(source)
    local pending, seen, count = {source}, {}, 0;
    local links = {"getSourceGrid","getContainer","getContainingItem","getParent","getVehicle",
        "getOwner","getVehiclePart","getWorldItem","getSquare"};
    while #pending > 0 and count < 16 do
        local object = table.remove(pending);
        if object ~= nil and not seen[object] then
            seen[object], count = true, count + 1;
            local x, y, z = sourceMethod(object,"getX"), sourceMethod(object,"getY"), sourceMethod(object,"getZ");
            if finite(x) and finite(y) and finite(z) then return {x=x,y=y,z=z}; end
            -- Exact square/world item/vehicle ownership takes precedence over the fallback sourceGrid.
            for i = 1, #links do
                local owner = sourceMethod(object,links[i]);
                if owner ~= nil and not seen[owner] then pending[#pending+1] = owner; end
            end
        end
    end
end
local function markPosition(position)
    if position ~= nil then markSquare(position.x,position.y,position.z); end
end
local function markSource(source)
    local position = sourcePosition(source);
    if position ~= nil then markPosition(position);return true; end
    return false;
end
local function observesPickups()
    return enabled() and EtherItemSearch.lastTargets ~= nil and not EtherItemSearch._failed;
end
local pickupHooks = {};
EtherItemSearch._pickupHooks = pickupHooks;
local function installPickupObserver(owner, key, kind)
    if owner == nil or type(owner[key]) ~= "function" then return; end
    for _, hook in ipairs(pickupHooks) do
        if hook.owner == owner and hook.key == key and owner[key] == hook.wrapped then return; end
    end
    local previous = owner[key];
    local wrapped;
    if kind == "transfer" then
        -- The vanilla shared transfer returns one InventoryItem. Keep that return value and every argument.
        wrapped = function(self, character, value, source, target, dropSquare, ...)
            if not observesPickups() then return previous(self,character,value,source,target,dropSquare,...); end
            local beforeSource, beforeTarget = sourcePosition(source), sourcePosition(target);
            local result = previous(self,character,value,source,target,dropSquare,...);
            markPosition(beforeSource);markPosition(beforeTarget);
            markSource(source);markSource(target);markSource(dropSquare);
            -- A vehicle source need not have a static square; even an unresolved owner needs one refresh.
            noteChange();
            return result;
        end;
    elseif kind == "clientTransfer" then
        -- B42 client transactions skip the shared helper in perform(); the action still retains both owners.
        wrapped = function(self, ...)
            if not observesPickups() then return previous(self,...); end
            local source,target = self.srcContainer,self.destContainer;
            local beforeSource,beforeTarget = sourcePosition(source),sourcePosition(target);
            local result = previous(self,...);
            markPosition(beforeSource);markPosition(beforeTarget);
            markSource(source);markSource(target);noteChange();
            return result;
        end;
    else
        wrapped = function(self, ...)
            if not observesPickups() then return previous(self,...); end
            local before = {};
            for _, queued in ipairs(self.queueList or {}) do
                for _, placed in ipairs(queued.items or {}) do before[#before+1] = sourcePosition(placed); end
            end
            local beforeItem = sourcePosition(self.item);if beforeItem ~= nil then before[#before+1] = beforeItem; end
            local beforeTarget = sourcePosition(self.destContainer);
            local result = previous(self,...);
            for _, position in ipairs(before) do markPosition(position); end
            markPosition(beforeTarget);markSource(self.destContainer);noteChange();
            return result;
        end;
    end
    pickupHooks[#pickupHooks+1] = {owner=owner,key=key,previous=previous,wrapped=wrapped};
    owner[key] = wrapped;
end
local function installPickupObservers()
    installPickupObserver(ISTransferAction,"transferItem","transfer");
    installPickupObserver(ISInventoryTransferAction,"perform","clientTransfer");
    installPickupObserver(ISGrabItemAction,"perform","ground");
end
local function onSquareChanged(square)
    if square ~= nil then markSquare(square:getX(),square:getY(),square:getZ()); end
end
local function onObjectChanged(obj) if observesPickups() then markSource(obj); end end
local function onInventoryWindowChanged(page, stage)
    if not enabled() or EtherItemSearch.lastTargets == nil or (stage ~= nil and stage ~= "end") then return; end
    local player = getPlayer();if player == nil then return; end
    markSquare(player:getX(),player:getY(),player:getZ());
    page = page or (type(getPlayerLoot) == "function" and getPlayerLoot(0) or nil);
    if page ~= nil and page.backpacks ~= nil then
        for _, entry in ipairs(page.backpacks) do
            local container = entry.inventory;
            if container ~= nil then markSource(container); end
        end
    end
end
local function onContainerUpdate(source)
    if not observesPickups() then return; end
    if not markSource(source) then onInventoryWindowChanged(); end
end
local function tryDirtyRefresh(now, moveDue)
    if EtherItemSearch.refreshPending and (now-EtherItemSearch.lastChangeAt >= EtherItemSearch.debounceMs
        or now-(EtherItemSearch._firstDirtyAt or now) >= EtherItemSearch.maxDebounceMs) then
        local dirty = EtherItemSearch._dirty;
        beginScan(EtherItemSearch.lastTargets,true,nil,moveDue and "move" or "dirty",dirty);return true;
    end
    return false;
end
function EtherItemSearch.refresh()
    if not enabled() or EtherItemSearch._failed or EtherItemSearch.lastTargets == nil or EtherItemSearch._cursor ~= nil then return; end
    local player = getPlayer();if player == nil then return; end
    local now = getTimestampMs();
    local dx,dy = math.floor(player:getX())-(EtherItemSearch._scanX or math.floor(player:getX())),math.floor(player:getY())-(EtherItemSearch._scanY or math.floor(player:getY()));
    local moveDue = dx*dx+dy*dy >= EtherItemSearch.moveRefreshTiles^2 and now-(EtherItemSearch._lastPriorityAt or 0) >= EtherItemSearch.moveRefreshMs;
    tryDirtyRefresh(now,moveDue);
end
local function onPlayerUpdate(player)
    installPickupObservers(); -- Vanilla action classes may have loaded after this module.
    local current, cell = getPlayer(), getCell();
    if current ~= sessionPlayer or cell ~= sessionCell then
        EtherItemSearch.clear();sessionPlayer,sessionCell = current,cell;
    end
    if player == nil or player ~= current then return; end
    EtherItemSearch.stepScan();
    if not enabled() or EtherItemSearch.lastTargets == nil or EtherItemSearch._failed then return; end
    local size = player:getInventory():getItems():size();
    if EtherItemSearch._invSize ~= nil and size ~= EtherItemSearch._invSize then markSquare(player:getX(),player:getY(),player:getZ()); end
    EtherItemSearch._invSize = size;
    local z = math.floor(player:getZ());
    if EtherItemSearch._playerZ ~= nil and z ~= EtherItemSearch._playerZ then noteChange(); end
    EtherItemSearch._playerZ = z;
    if EtherItemSearch._cursor ~= nil then return; end
    local now = getTimestampMs();
    noteSpeed(player:getX(), player:getY(), now);
    local dx,dy = math.floor(player:getX())-(EtherItemSearch._scanX or math.floor(player:getX())),math.floor(player:getY())-(EtherItemSearch._scanY or math.floor(player:getY()));
    local moveDue = dx*dx+dy*dy >= EtherItemSearch.moveRefreshTiles^2 and now-(EtherItemSearch._lastPriorityAt or 0) >= EtherItemSearch.moveThrottleMs();
    if tryDirtyRefresh(now,moveDue) then return; end
    if moveDue then
        beginScan(EtherItemSearch.lastTargets,true,nil,"move",EtherItemSearch._dirty);return;
    end
    if now-(EtherItemSearch._lastPatrolAt or EtherItemSearch.lastScanAt) >= EtherItemSearch.patrolMs then
        EtherItemSearch._lastPatrolAt = now;beginScan(EtherItemSearch.lastTargets,true,nil,"patrol",nil);
    end
end
function EtherItemSearch.clear()
    if type(itemScanCancel) == "function" then itemScanCancel(); end
    EtherItemSearch.results, EtherItemSearch.lastTargets, EtherItemSearch.progress = nil,nil,nil;
    EtherItemSearch._cursor, EtherItemSearch._bounds, EtherItemSearch._enableRequestHandle = nil,nil,nil;
    EtherItemSearch._generation = EtherItemSearch._generation + 1;
    EtherItemSearch._dirty, EtherItemSearch._failed = {},false;
    EtherItemSearch.refreshPending, EtherItemSearch._firstDirtyAt = false,nil;
    EtherItemSearch._scanX, EtherItemSearch._scanY, EtherItemSearch._invSize, EtherItemSearch._playerZ = nil,nil,nil,nil;
    EtherItemSearch._speedSamples = {};
    EtherItemSearch._lastPriorityAt, EtherItemSearch._lastPatrolAt = nil,nil;
    EtherItemSearch.resultVersion, EtherItemSearch.resultCollectedAt = nil,nil;
    sessionPlayer,sessionCell = nil,nil;
end
-- Preserve a paused selection in this world, while invalidating its old request and visible snapshot.
function EtherItemSearch.setTrackingTargets(targets)
    local copied = copyTargets(targets);
    EtherItemSearch.clear();
    sessionPlayer,sessionCell = getPlayer(),getCell();
    for _, selected in pairs(copied) do
        if selected == true then EtherItemSearch.lastTargets = copied;break; end
    end
end
local function displayTargets()
    if EtherWatchlist ~= nil and EtherWatchlist.count() > 0 then return EtherWatchlist.targets(); end
    return EtherItemSearch.lastTargets;
end
local function resumeTracking(wasEnabled, requestHandle)
    if enabled() and (not wasEnabled or EtherItemSearch._failed) then
        local targets = displayTargets();
        if targets == nil then return; end
        local cur = EtherItemSearch._cursor;
        if requestHandle ~= nil and requestHandle == EtherItemSearch._enableRequestHandle and cur ~= nil
            and cur.version == requestHandle and cur.version == EtherItemSearch._generation and cur.mode == "full"
            and cur.player == getPlayer() and cur.cell == getCell()
            and targetsEqual(targets,EtherItemSearch.lastTargets) then
            EtherItemSearch._enableRequestHandle = nil;return;
        end
        EtherItemSearch.startScan(targets,true,nil);
    end
end
function EtherItemSearch.setMinimapEnabled(value,requestHandle)
    UIMap.ensureDrawFlags();local wasEnabled = enabled();UIMap.drawItems = value;
    if not value then EtherItemSearch._enableRequestHandle = nil; end
    setMapDrawItems(value);if value then resumeTracking(wasEnabled,requestHandle); end
end
function EtherItemSearch.setItemEspEnabled(value,requestHandle)
    UIMap.ensureDrawFlags();local wasEnabled = enabled();UIMap.drawItemEsp = value;
    if not value then EtherItemSearch._enableRequestHandle = nil; end
    if value then resumeTracking(wasEnabled,requestHandle); end
end
installPickupObservers();
handlers.OnPlayerUpdate = onPlayerUpdate;
handlers.OnRefreshInventoryWindowContainers = onInventoryWindowChanged;
handlers.OnContainerUpdate = onContainerUpdate;
handlers.OnObjectAdded, handlers.OnObjectAboutToBeRemoved = onObjectChanged,onObjectChanged;
handlers.LoadGridsquare = onSquareChanged;
handlers.LoadChunk = function() if enabled() and EtherItemSearch.lastTargets ~= nil and not EtherItemSearch._failed then noteChange(); end end;
handlers.OnMainMenuEnter = function() EtherItemSearch.clear(); end;
for name, fn in pairs(handlers) do if Events[name] ~= nil then Events[name].Remove(fn);Events[name].Add(fn); end end
if type(itemScanCancel) == "function" then itemScanCancel(); end

function isMapDrawItemEsp()
    UIMap.ensureDrawFlags();
    return UIMap.drawItemEsp and true or false;
end

--*********************************************************
--* 世界标记 (ESP): 搜索结果存续期间, 把每个命中位置画在世界画面上
--* 开关: UIMap.drawItemEsp (ESP 页「物品雷达」勾选 / 雷达页「ESP 画线追踪」, 会话级默认关)
--* 通道: Events.OnPostUIDraw 逐帧事件 —— 与 Java 版 ESP (EtherAPI.updateVisuals)
--*       同一事件; 原版 LastStand/TutorialSetup 有 Lua 订阅先例, 无需任何面板
--* 投影: IsoUtils.XToScreen/YToScreen + getZoom/相机偏移/瓦片高度修正,
--*       与 Java 侧 ZombieUtils.getScreenPositionX/Y 完全同一套公式
--* 性能: 开关关或无结果时单布尔早退 (空闲零开销); 只画 worldDrawRadius 格内
--*       命中; 命中数超过 worldMaxMarkers 才排序取最近 (常规路径零排序);
--*       暂存数组跨帧复用, 仅结果清空时重建
--* 留痕: 纯客户端渲染, 零网络包零日志
--*********************************************************
EtherItemSearch.worldDrawRadius = 150; -- 仅世界显示半径；扫描结果不按显示条数截断
EtherItemSearch.worldMaxMarkers = 60;  -- 单帧最多绘制条数, 命中洪峰时取最近的
EtherItemSearch._worldProbe = nil;     -- nil=未探测; true=投影可用; false=投影原语不可达(自动禁用, 不影响其它功能)

local scratchD2 = {};  -- 可见命中距离平方暂存 (跨帧复用)
local scratchP = {};   -- 可见命中结果引用暂存

local function worldProbeOk()
    if EtherItemSearch._worldProbe ~= nil then return EtherItemSearch._worldProbe end
    local ok = pcall(function()
        IsoUtils.XToScreen(0.5, 0.5, 0, 0);
        IsoUtils.YToScreen(0.5, 0.5, 0, 0);
        IsoCamera.getOffX();
        IsoCamera.getOffY();
        getCore():getZoom(0);
        Core.getTileScale();
        getCore():getScreenWidth();
        getCore():getScreenHeight();
        local tm = getTextManager();
        tm:MeasureStringX(UIFont.Small, "0");
        -- 8 参无缩放形式 (原版 DebugDemoTime 同款), 不赌 9 参重载的 Kahlua 分派
        tm:DrawString(UIFont.Small, 0, 0, "0", 0, 0, 0, 0);
        etherDrawThinLine(0, 0, 0, 0, 0, 0, 0, 0);  -- Java 暴露的画线原语 (RenderingAPI, 雷达线)
    end);
    EtherItemSearch._worldProbe = ok;
    if not ok then
        print("[modcore] 世界标记: 投影/绘制原语不可用, 功能自动禁用 (其余功能不受影响)");
    end
    return ok;
end

local function worldToScreen(x, y, z, wallOffset)
    -- 玩家索引恒 0: IsoCamera.frameState.playerIndex 是 Java 公开字段, Kahlua 只暴露
    -- 方法不暴露字段 (实测 "attempted index: playerIndex of non-table"); 单人/多人客户端
    -- 该索引均为 0, 仅分屏非 0 (本 mod 不支持分屏)
    -- wallOffset: 竖直锚定偏移 —— 人物站立用半墙高 128/(2/tileScale) (胸口锚定,
    -- 与 Java 侧 ZombieUtils 一致); 物品贴地传 0 (否则线终点/标签整体高出约半格,
    -- 即实机"追踪线指向过高")
    local sx = IsoUtils.XToScreen(x, y, z, 0);
    local sy = IsoUtils.YToScreen(x, y, z, 0);
    local zoom = getCore():getZoom(0);
    sx = (sx - IsoCamera.getOffX()) / zoom;
    sy = (sy - IsoCamera.getOffY() - wallOffset) / zoom;
    return sx, sy;
end

local function drawWorldMarkers()
    if not UIMap.drawItemEsp then return end
    local results = EtherItemSearch.results;
    -- 注意: PZ 的 Kahlua 环境没有裸 next(), 只有 pairs/ipairs —— 用 # 判空
    if results == nil or #results == 0 then return end
    local player = getPlayer();
    if player == nil then return end
    if not worldProbeOk() then return end

    EtherItemSearch.refreshFloorLabels(); -- two template reads per frame, none per marker
    local px, py, pz = player:getX(), player:getY(), player:getZ();
    -- 人物屏幕锚点: 雷达线起点 (半墙高胸口锚定 + 头顶 60, 与 Java 侧载具/僵尸雷达同款)
    local psx, psy = worldToScreen(px, py, pz, 128 / (2 / Core.getTileScale()));
    local r2max = EtherItemSearch.worldDrawRadius * EtherItemSearch.worldDrawRadius;
    local maxN = EtherItemSearch.worldMaxMarkers;

    local cnt = 0;
    for _, p in pairs(results) do
        local dx, dy = p.x + 0.5 - px, p.y + 0.5 - py;
        local d2 = dx * dx + dy * dy;
        if d2 <= r2max then
            cnt = cnt + 1;
            scratchD2[cnt] = d2;
            scratchP[cnt] = p;
        end
    end
    if cnt == 0 then return end

    -- 洪峰才排序取最近; 常规路径按结果顺序画 (顺序无关)
    local order = nil;
    if cnt > maxN then
        order = {};
        for i = 1, cnt do order[i] = i; end
        table.sort(order, function(a, b) return scratchD2[a] < scratchD2[b] end);
    end

    local n = math.min(cnt, maxN);
    local tm = getTextManager();
    local sw, sh = getCore():getScreenWidth(), getCore():getScreenHeight();
    for i = 1, n do
        local idx = (order ~= nil) and order[i] or i;
        local p = scratchP[idx];
        local sx, sy = worldToScreen(p.x + 0.5, p.y + 0.5, p.z or 0, 0);
        -- 雷达线: 物品 → 人物头顶 (+60 垂直偏移), 与载具/僵尸雷达同款 —— 视野外也画
        -- (投影坐标由 GPU 裁剪, 但线的方向可见, 可指引屏幕外物品方位)
        etherDrawThinLine(sx, sy, psx, psy + 60, 1, 0.78, 0.35, 0.8);

        if sx > -160 and sy > -20 and sx < sw and sy < sh then
            -- 物品侧文字: 名称 x数量 + 绝对楼层，换玩家楼层不改变目标层名
            -- 数量分隔用 ASCII x: 游戏 Small 字体缺 × 字形, 实测渲染成空白 (实机截图 "扳手 1")
            -- 文字主体预拼于扫描完成时 (cur.out 侧 label 字段), 此处只拼楼层小尾巴
            local floorTag = " " .. EtherItemSearch.floorLabel(p.z or 0);
            local text = (p.label or (p.name .. " x" .. p.count)) .. floorTag;
            local tx = sx - tm:MeasureStringX(UIFont.Small, text) / 2;
            -- 阴影 + 主字双层, 与 ESP 文字同款画法; 琥珀色区别于僵尸红/载具白
            -- DrawString 用 8 参无缩放形式 (原版先例), 不赌 9 参重载分派
            tm:DrawString(UIFont.Small, tx + 1, sy + 1, text, 0, 0, 0, 0.9);
            tm:DrawString(UIFont.Small, tx, sy, text, 1, 0.78, 0.35, 1);
        end

        -- 距离数字: 画在靠近人物端的线上 (公式与 Java 载具/僵尸雷达逐句一致:
        -- 钳位距离/线长像素 → 沿线比例定位, 数字贴近人物端)
        local dist = math.floor(math.sqrt(scratchD2[idx]));
        local dc = math.max(30, math.min(150, dist));
        local ddx, ddy = sx - psx, sy - (psy + 60);
        local dlen = math.sqrt(ddx * ddx + ddy * ddy);
        if dlen > 1 then
            local ratio = dc / dlen;
            local nx = psx + ratio * ddx;
            local ny = (psy + 60) + ratio * ddy;
            local dtext = tostring(dist);
            local ntx = nx - tm:MeasureStringX(UIFont.Small, dtext) / 2;
            tm:DrawString(UIFont.Small, ntx + 1, ny + 1, dtext, 0, 0, 0, 0.9);
            tm:DrawString(UIFont.Small, ntx, ny, dtext, 1, 0.78, 0.35, 1);
        end
    end
    for i = 1, cnt do scratchD2[i] = nil; scratchP[i] = nil; end
end

if Events ~= nil then
    handlers.OnPostUIDraw = drawWorldMarkers;
    Events.OnPostUIDraw.Remove(drawWorldMarkers);
    Events.OnPostUIDraw.Add(drawWorldMarkers);
end
