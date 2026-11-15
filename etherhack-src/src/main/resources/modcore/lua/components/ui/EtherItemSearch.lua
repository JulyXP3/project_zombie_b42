--*********************************************************
--* 物品搜索: 方格贡献索引 + 新边缘/已知脏贡献 + 低预算巡回。
--* 显式分步状态机细化到物品, 不用 Kahlua 协程; 版本校验后原子公布结果。
--*********************************************************
if EtherItemSearch ~= nil and EtherItemSearch._handlers ~= nil then
    for name, fn in pairs(EtherItemSearch._handlers) do
        if Events[name] ~= nil then Events[name].Remove(fn); end
    end
end
EtherItemSearch = EtherItemSearch or {};

EtherItemSearch.results = nil;
EtherItemSearch.radius = 56;
EtherItemSearch.lastTargets = nil;
EtherItemSearch.debounceMs = 250;
EtherItemSearch.maxDebounceMs = 1000;
EtherItemSearch.moveRefreshTiles = 2;
EtherItemSearch.moveRefreshMs = 1000;
EtherItemSearch.stepBudgetMs = 3;
EtherItemSearch.patrolMs = 1000;
EtherItemSearch.patrolCells = 128;
EtherItemSearch.refreshPending = false;
EtherItemSearch.lastChangeAt = 0;
EtherItemSearch.lastScanAt = 0;
EtherItemSearch._cursor = nil;
EtherItemSearch._generation = 0;
EtherItemSearch._contributions = {};
EtherItemSearch._contributionKeys = {};
EtherItemSearch._bounds = nil;
EtherItemSearch._dirty = {};
EtherItemSearch._patrolOffset = 0;
EtherItemSearch._failed = false;

local handlers = {};
EtherItemSearch._handlers = handlers;
local sessionPlayer, sessionCell = nil, nil;
local SCAN_TIMEOUT_MS = 120000;

local function enabled()
    return UIMap ~= nil and (UIMap.drawItems or UIMap.drawItemEsp);
end

local function finite(value)
    return type(value) == "number" and value == value and math.abs(value) < math.huge;
end

local function targetsEqual(a, b)
    if a == nil or b == nil then return false; end
    for key, value in pairs(a) do if value == true and b[key] ~= true then return false; end end
    for key, value in pairs(b) do if value == true and a[key] ~= true then return false; end end
    return true;
end

local function markSquare(x, y, z)
    if not enabled() or EtherItemSearch.lastTargets == nil then return; end
    if not finite(x) or not finite(y) or not finite(z) then return; end
    x, y, z = math.floor(x), math.floor(y), math.floor(z);
    local b = EtherItemSearch._cursor ~= nil and EtherItemSearch._cursor.bounds or EtherItemSearch._bounds;
    if not ItemScanIndex.contains(b, x, y, z) then return; end
    local key = ItemScanIndex.key(x, y, z);
    EtherItemSearch._dirty[key] = { x = x, y = y, z = z };
    local now = getTimestampMs();
    if not EtherItemSearch.refreshPending then EtherItemSearch._firstDirtyAt = now; end
    EtherItemSearch.refreshPending = true;
    EtherItemSearch.lastChangeAt = now;
end

local function scanItem(cur, job, item, x, y, z)
    if item == nil then return; end
    cur.stats.items = cur.stats.items + 1;
    if cur.targets[item:getFullType()] == true or cur.targets[item:getType()] == true then
        ItemScanIndex.add(job.out, job.keys, x, y, z, item:getDisplayName());
    end
end

local function pushItems(stack, cur, container, x, y, z)
    if container == nil then return; end
    cur.stats.containers = cur.stats.containers + 1;
    stack[#stack + 1] = { kind = "items", list = container:getItems(), index = 0, x = x, y = y, z = z };
end

-- 每次只取一个对象/容器/物品, 调用者在各步之间检查预算。
local function stepJob(cur, job)
    local stack = job.stack;
    local task = stack[#stack];
    if task == nil then return true; end
    local kind = task.kind;
    if kind == "item" then
        table.remove(stack);
        scanItem(cur, job, task.item, task.x, task.y, task.z);
    elseif kind == "bag" then
        table.remove(stack);
        if task.item:IsInventoryContainer() then
            local inventory = task.item:getInventory();
            if inventory ~= nil then
                cur.stats.bags = cur.stats.bags + 1;
                stack[#stack + 1] = { kind = "items", list = inventory:getItems(), index = 0,
                    x = task.x, y = task.y, z = task.z };
            end
        end
    elseif kind == "containers" then
        if task.index >= task.obj:getContainerCount() then table.remove(stack); return false; end
        local container = task.obj:getContainerByIndex(task.index);
        task.index = task.index + 1;
        pushItems(stack, cur, container, task.obj:getX(), task.obj:getY(), task.obj:getZ());
    elseif kind == "parts" then
        if task.index >= task.vehicle:getPartCount() then table.remove(stack); return false; end
        local part = task.vehicle:getPartByIndex(task.index);
        task.index = task.index + 1;
        if part ~= nil then
            pushItems(stack, cur, part:getItemContainer(), part:getX(), part:getY(), task.vehicle:getZ());
        end
    elseif kind == "vehicles" then
        if task.index > #task.list then table.remove(stack); return false; end
        local vehicle = task.list[task.index];
        task.index = task.index + 1;
        if vehicle ~= nil then
            local x, y, z = vehicle:getX(), vehicle:getY(), vehicle:getZ();
            if finite(x) and finite(y) and finite(z)
                and ItemScanIndex.contains(cur.bounds, math.floor(x), math.floor(y), math.floor(z)) then
                stack[#stack + 1] = { kind = "parts", vehicle = vehicle, index = 0 };
            end
        end
    else
        if task.list == nil or task.index >= task.list:size() then table.remove(stack); return false; end
        local obj = task.list:get(task.index);
        task.index = task.index + 1;
        if obj ~= nil then
            if kind == "items" then
                scanItem(cur, job, obj, task.x, task.y, task.z);
            elseif kind == "objects" then
                stack[#stack + 1] = { kind = "containers", obj = obj, index = 0 };
            elseif kind == "floor" then
                cur.stats.floor = cur.stats.floor + 1;
                local item = obj:getItem();
                if item ~= nil then
                    local x, y, z = obj:getX(), obj:getY(), obj:getZ();
                    stack[#stack + 1] = { kind = "bag", item = item, x = x, y = y, z = z };
                    stack[#stack + 1] = { kind = "item", item = item, x = x, y = y, z = z };
                end
            elseif kind == "bodies" then
                pushItems(stack, cur, obj:getContainer(), obj:getX(), obj:getY(), obj:getZ());
            end
        end
    end
    return false;
end

local function squareJob(cur, square, x, y, z, key)
    local job = { stack = {}, out = {}, keys = {}, x = x, y = y, z = z, key = key };
    if square ~= nil then
        cur.stats.squares = cur.stats.squares + 1;
        job.stack = {
            { kind = "bodies", list = square:getDeadBodys(), index = 0 },
            { kind = "floor", list = square:getWorldObjects(), index = 0 },
            { kind = "objects", list = square:getObjects(), index = 0 },
        };
    end
    return job;
end

local function retain(cur, key, contribution)
    if contribution ~= nil and contribution ~= false
        and ItemScanIndex.contains(cur.bounds, contribution.x, contribution.y, contribution.z) then
        cur.nextContributions[key] = contribution;
        cur.nextKeys[#cur.nextKeys + 1] = key;
    end
end

local function mergeHit(cur, hit)
    if ItemScanIndex.contains(cur.bounds, hit.x, hit.y, hit.z) then
        ItemScanIndex.add(cur.out, cur.resultKeys, hit.x, hit.y, hit.z, hit.name, hit.count);
    end
end

local function stepCursor(cur)
    if cur.phase == "squares" then
        if cur.job ~= nil then
            if stepJob(cur, cur.job) then
                local job = cur.job;
                cur.replacements[job.key] = #job.out > 0 and { x = job.x, y = job.y, z = job.z,
                    hits = job.out, version = cur.version, sampledAt = getTimestampMs() } or false;
                cur.replacementKeys[#cur.replacementKeys + 1] = job.key;
                cur.job = nil;
            end
        else
            local x, y, z, key = ItemScanIndex.take(cur.plan);
            if x == nil then cur.phase = "vehicles"; return; end
            cur.job = squareJob(cur, cur.cell:getGridSquare(x, y, z), x, y, z, key);
        end
    elseif cur.phase == "vehicles" then
        if cur.job == nil then
            local vehicles = cur.cell:getVehicles();
            local list = vehicles ~= nil and vehicles:toArray() or {};
            cur.job = { stack = { { kind = "vehicles", list = list, index = 1 } }, out = cur.vehicles, keys = {} };
        elseif stepJob(cur, cur.job) then
            cur.job, cur.phase, cur.index = nil, "retain", 1;
        end
    elseif cur.phase == "retain" then
        local key = cur.baseKeys[cur.index];
        if key == nil then cur.phase, cur.index = "replace", 1; return; end
        cur.index = cur.index + 1;
        if cur.replacements[key] == nil then retain(cur, key, cur.base[key]); end
    elseif cur.phase == "replace" then
        local key = cur.replacementKeys[cur.index];
        if key == nil then cur.phase, cur.index, cur.hitIndex = "merge", 1, 1; return; end
        cur.index = cur.index + 1;
        retain(cur, key, cur.replacements[key]);
    elseif cur.phase == "merge" then
        local key = cur.nextKeys[cur.index];
        if key == nil then cur.phase, cur.index = "merge-vehicles", 1; return; end
        local contribution = cur.nextContributions[key];
        local hit = contribution.hits[cur.hitIndex];
        if hit == nil then cur.index, cur.hitIndex = cur.index + 1, 1; return; end
        cur.hitIndex = cur.hitIndex + 1;
        mergeHit(cur, hit);
    elseif cur.phase == "merge-vehicles" then
        local hit = cur.vehicles[cur.index];
        if hit == nil then cur.phase = "done"; return; end
        cur.index = cur.index + 1;
        mergeHit(cur, hit);
    end
end

local function beginScan(targetTypes, silent, onDone, mode, dirty)
    local player, cell = getPlayer(), getCell();
    if player == nil or cell == nil or targetTypes == nil then
        EtherItemSearch.clear();
        if onDone ~= nil then onDone(0, nil); end
        return;
    end
    local px, py, pz = player:getX(), player:getY(), player:getZ();
    if not finite(px) or not finite(py) or not finite(pz) then EtherItemSearch.clear(); return; end
    local changed = not targetsEqual(EtherItemSearch.lastTargets, targetTypes);
    if sessionPlayer ~= player or sessionCell ~= cell then
        EtherItemSearch.clear();
        sessionPlayer, sessionCell = player, cell;
        changed = true;
    end
    local targets = {};
    for key, value in pairs(targetTypes) do if value == true then targets[key] = true; end end
    EtherItemSearch.lastTargets = targets;
    EtherItemSearch._failed = false;
    EtherItemSearch._generation = EtherItemSearch._generation + 1;
    local old = EtherItemSearch._bounds;
    local bounds = mode == "patrol" and old or ItemScanIndex.bounds(px, py, pz, EtherItemSearch.radius);
    bounds = bounds or ItemScanIndex.bounds(px, py, pz, EtherItemSearch.radius);
    local full = mode == "full" or changed or old == nil or old.radius ~= bounds.radius
        or old.cz ~= bounds.cz or math.abs(old.cx - bounds.cx) > bounds.radius
        or math.abs(old.cy - bounds.cy) > bounds.radius;
    if full then
        EtherItemSearch._dirty = {};
        EtherItemSearch.refreshPending, EtherItemSearch._firstDirtyAt = false, nil;
    end
    if changed then EtherItemSearch.results = nil; end
    EtherItemSearch._cursor = { cell = cell, player = player, targets = targets,
        version = EtherItemSearch._generation, bounds = bounds, plan = ItemScanIndex.plan(old, bounds, dirty, full),
        silent = silent, onDone = onDone, mode = mode, px = px, py = py, phase = "squares", startedAt = getTimestampMs(),
        base = full and {} or EtherItemSearch._contributions,
        baseKeys = full and {} or EtherItemSearch._contributionKeys,
        replacements = {}, replacementKeys = {}, nextContributions = {}, nextKeys = {},
        vehicles = {}, out = {}, resultKeys = {}, stats = { squares = 0, floor = 0, containers = 0, bags = 0, items = 0 } };
end

function EtherItemSearch.startScan(targetTypes, silent, onDone)
    beginScan(targetTypes, silent, onDone, "full", nil);
end

local function advance(cur, deadline)
    repeat
        stepCursor(cur);
        if cur.phase == "done" then return; end
    until getTimestampMs() >= deadline;
end

local function stopScan(cur, reason)
    EtherItemSearch._cursor = nil;
    EtherItemSearch._failed = true;
    if cur.onDone ~= nil then cur.onDone(0, nil); end
    print("[modcore] item scan stopped: " .. tostring(reason));
end

local function stepScan()
    local cur = EtherItemSearch._cursor;
    if cur == nil then return; end
    if cur.player ~= getPlayer() or cur.cell ~= getCell() or cur.version ~= EtherItemSearch._generation then
        EtherItemSearch.clear();
        return;
    end
    local now = getTimestampMs();
    if now - cur.startedAt > SCAN_TIMEOUT_MS then stopScan(cur, "timeout"); return; end
    local ok, err = pcall(advance, cur, now + EtherItemSearch.stepBudgetMs);
    if not ok then stopScan(cur, err); return; end
    if cur.phase == "done" then
        EtherItemSearch.results = cur.out;
        EtherItemSearch._contributions, EtherItemSearch._contributionKeys = cur.nextContributions, cur.nextKeys;
        EtherItemSearch._bounds = cur.bounds;
        EtherItemSearch.lastScanAt = getTimestampMs();
        EtherItemSearch.resultVersion, EtherItemSearch.resultCollectedAt = cur.version, EtherItemSearch.lastScanAt;
        if cur.mode ~= "patrol" then
            EtherItemSearch._scanX, EtherItemSearch._scanY = cur.px, cur.py;
            EtherItemSearch._lastPriorityAt = EtherItemSearch.lastScanAt;
        end
        EtherItemSearch._cursor = nil;
        if cur.onDone ~= nil then cur.onDone(#cur.out, cur.stats); end
    end
end

function EtherItemSearch.refresh()
    if not enabled() or EtherItemSearch._failed or EtherItemSearch.lastTargets == nil
        or not EtherItemSearch.refreshPending or EtherItemSearch._cursor ~= nil then return; end
    local now = getTimestampMs();
    if now - EtherItemSearch.lastChangeAt < EtherItemSearch.debounceMs
        and now - (EtherItemSearch._firstDirtyAt or now) < EtherItemSearch.maxDebounceMs then return; end
    local dirty = {};
    for _, p in pairs(EtherItemSearch._dirty) do dirty[#dirty + 1] = p; end
    EtherItemSearch._dirty = {};
    EtherItemSearch.refreshPending, EtherItemSearch._firstDirtyAt = false, nil;
    beginScan(EtherItemSearch.lastTargets, true, nil, "dirty", dirty);
end

local function onPlayerUpdate(player)
    if player ~= nil and player ~= getPlayer() then return; end
    if sessionPlayer ~= getPlayer() or sessionCell ~= getCell() then
        EtherItemSearch.clear();
        sessionPlayer, sessionCell = getPlayer(), getCell();
        return;
    end
    stepScan();
    if player == nil or not enabled() or EtherItemSearch._failed or EtherItemSearch.lastTargets == nil then return; end
    local items = player:getInventory():getItems();
    if items ~= nil then
        local count = items:size();
        if EtherItemSearch._invSize ~= nil and count ~= EtherItemSearch._invSize then
            markSquare(player:getX(), player:getY(), player:getZ());
        end
        EtherItemSearch._invSize = count;
    end
    if EtherItemSearch._cursor ~= nil then return; end
    EtherItemSearch.refresh();
    if EtherItemSearch._cursor ~= nil then return; end
    local b = EtherItemSearch._bounds;
    if b == nil then beginScan(EtherItemSearch.lastTargets, true, nil, "full", nil); return; end
    local now = getTimestampMs();
    local dx, dy = player:getX() - (EtherItemSearch._scanX or player:getX()),
        player:getY() - (EtherItemSearch._scanY or player:getY());
    if math.floor(player:getZ()) ~= b.cz or (dx * dx + dy * dy >= EtherItemSearch.moveRefreshTiles ^ 2
        and now - (EtherItemSearch._lastPriorityAt or 0) >= EtherItemSearch.moveRefreshMs) then
        beginScan(EtherItemSearch.lastTargets, true, nil, "move", nil);
        return;
    end
    if now - (EtherItemSearch._lastPatrolAt or EtherItemSearch.lastScanAt) >= EtherItemSearch.patrolMs then
        local dirty, offset = ItemScanIndex.patrol(b, EtherItemSearch._patrolOffset, EtherItemSearch.patrolCells);
        EtherItemSearch._patrolOffset, EtherItemSearch._lastPatrolAt = offset, now;
        beginScan(EtherItemSearch.lastTargets, true, nil, "patrol", dirty);
    end
end

local function onSquareChanged(square)
    if square ~= nil then markSquare(square:getX(), square:getY(), square:getZ()); end
end

local function onObjectChanged(obj)
    if obj ~= nil then onSquareChanged(obj:getSquare()); end
end

local function onInventoryWindowChanged(page, stage)
    if not enabled() or EtherItemSearch.lastTargets == nil or (stage ~= nil and stage ~= "end") then return; end
    local player = getPlayer();
    if player == nil then return; end
    markSquare(player:getX(), player:getY(), player:getZ());
    page = page or (type(getPlayerLoot) == "function" and getPlayerLoot(0) or nil);
    if page ~= nil and page.backpacks ~= nil then
        for _, entry in ipairs(page.backpacks) do
            local container = entry.inventory;
            if container ~= nil then
                local square = container:getSourceGrid();
                if square ~= nil then onSquareChanged(square); end
            end
        end
    end
end

local function onContainerUpdate(square)
    if not enabled() or EtherItemSearch.lastTargets == nil then return; end
    if square ~= nil and instanceof(square, "IsoGridSquare") then onSquareChanged(square);
    else onInventoryWindowChanged(); end
end

handlers.OnPlayerUpdate = onPlayerUpdate;
handlers.OnRefreshInventoryWindowContainers = onInventoryWindowChanged;
handlers.OnContainerUpdate = onContainerUpdate;
handlers.OnObjectAdded = onObjectChanged;
handlers.OnObjectAboutToBeRemoved = onObjectChanged;
handlers.LoadGridsquare = onSquareChanged;
handlers.OnMainMenuEnter = function() EtherItemSearch.clear(); end;
for name, fn in pairs(handlers) do
    if Events[name] ~= nil then Events[name].Remove(fn); Events[name].Add(fn); end
end

function EtherItemSearch.clear()
    EtherItemSearch.results, EtherItemSearch.lastTargets = nil, nil;
    EtherItemSearch._cursor, EtherItemSearch._bounds = nil, nil;
    EtherItemSearch._generation = EtherItemSearch._generation + 1;
    EtherItemSearch._contributions, EtherItemSearch._contributionKeys, EtherItemSearch._dirty = {}, {}, {};
    EtherItemSearch._patrolOffset = 0;
    EtherItemSearch._failed = false;
    EtherItemSearch.refreshPending, EtherItemSearch._firstDirtyAt = false, nil;
    EtherItemSearch._scanX, EtherItemSearch._scanY, EtherItemSearch._invSize = nil, nil, nil;
    EtherItemSearch._lastPriorityAt, EtherItemSearch._lastPatrolAt = nil, nil;
    EtherItemSearch.resultVersion, EtherItemSearch.resultCollectedAt = nil, nil;
end

local function resumeTracking(wasEnabled)
    if not wasEnabled and enabled() and EtherItemSearch.lastTargets ~= nil then
        EtherItemSearch.startScan(EtherItemSearch.lastTargets, true, nil);
    end
end

function EtherItemSearch.setMinimapEnabled(value)
    UIMap.ensureDrawFlags();
    local wasEnabled = enabled();
    UIMap.drawItems = value;
    setMapDrawItems(value);
    resumeTracking(wasEnabled);
end

function EtherItemSearch.setItemEspEnabled(value)
    UIMap.ensureDrawFlags();
    local wasEnabled = enabled();
    UIMap.drawItemEsp = value;
    resumeTracking(wasEnabled);
end

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
EtherItemSearch.worldDrawRadius = 150; -- 雷达线半径(格), 与僵尸/载具雷达同款 150; 结果本身来自 56 格扫描, 实际不超过 ~61
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
            -- 物品侧文字: 名称 x数量 + 楼层 (z 相对玩家: 楼上/楼下, 同层不标 —— 修"分不清哪层楼")
            -- 数量分隔用 ASCII x: 游戏 Small 字体缺 × 字形, 实测渲染成空白 (实机截图 "扳手 1")
            -- 文字主体预拼于扫描完成时 (cur.out 侧 label 字段), 此处只拼楼层小尾巴
            local dz = (p.z or 0) - pz;
            local floorTag = "";
            if dz > 0 then
                floorTag = " " .. tr("UI_RadarPanel_FloorUp");
            elseif dz < 0 then
                floorTag = " " .. tr("UI_RadarPanel_FloorDown");
            end
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
