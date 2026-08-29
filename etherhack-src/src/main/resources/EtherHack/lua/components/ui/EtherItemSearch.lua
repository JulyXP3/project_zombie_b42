--*********************************************************
--* 物品搜索模块: 以玩家为中心扫描已加载区块方格内的物品
--* 数据源: getCell():getGridSquare(x,y,z) -> square:getObjects()/getWorldObjects()
--*   IsoObject 容器(getContainerCount/getContainerByIndex)
--*   IsoWorldInventoryObject 地面物品(getItem + 背包getInventory)
--*   尸体: square:getDeadBodys() -> body:getContainer() (尸体不在 getObjects 里)
--*   车辆: cell:getVehicles():toArray() -> 部件容器
--* 坐标: 一律使用物件自身 getX()/getY() (比方格中心精确)
--* 原因: 客户端 processItems 注册表恒为空 (IsoCell.addToProcessItems
--*       首行 if (GameClient.client) return), 只能用已加载方格扫描
--* 匹配: 先比 getFullType() ("Base.Axe"), 不比中再比 getType() ("Axe"),
--*       兼容按钮侧 scriptItem:getFullName() 返回短名的可能
--* 刷新: 事件驱动而非定时器 —— OnPlayerUpdate 每 tick:
--*       1. 比较玩家背包物品数 (getItems():size() 一次调用), 有变化置"脏",
--*          安静满 1 秒才真正重扫一次 (防抖: 连续转移物品期间 0 次扫描);
--*       2. 比较玩家位置, 走出 5 格且距上次扫描 ≥2 秒即静默重扫 (标记跟随角色)。
--*       空闲零开销, 拾取/丢弃物品停手后标记一次性刷新到位。
--* 注意: 禁止 "obj:getItem ~= nil" 这类索引式方法探测 (Kahlua2 不可靠),
--*       一律直接调用或 pcall 兜底
--*********************************************************
EtherItemSearch = EtherItemSearch or {};

EtherItemSearch.results = nil; -- { {x=.., y=.., z=.., name=.., count=..}, ... } 命中位置列表 (z=楼层, name=首个命中物品显示名, 与世界标记共用)
EtherItemSearch.radius = 48;   -- 扫描半径(格), 仅覆盖玩家周围已加载区域
EtherItemSearch.lastTargets = nil;    -- 上次扫描目标, 供自动刷新复用
EtherItemSearch.debounceMs = 1000;    -- 防抖: 背包变动后安静满 1 秒才重扫
EtherItemSearch.refreshPending = false; -- 待重扫标记
EtherItemSearch.lastChangeAt = 0;     -- 最后一次背包变动时刻
EtherItemSearch.lastScanAt = 0;       -- 上次扫描时刻
EtherItemSearch.moveRefreshTiles = 5; -- 玩家走出 N 格触发移动重扫
EtherItemSearch.moveRefreshMs = 2000; -- 移动重扫最小间隔 (连续奔跑也不超过 1 次/2 秒)
EtherItemSearch._scanX = nil;         -- 上次扫描时的玩家位置
EtherItemSearch._scanY = nil;

--*********************************************************
--* 扫描: targetTypes = { ["Base.Axe"]=true, ... }
--* 返回命中位置数; 结果存到 EtherItemSearch.results
--* silent=true 时不打印诊断 (自动刷新用)
--*********************************************************
function EtherItemSearch.scan(targetTypes, silent)
    local cell = getCell();
    local player = getPlayer();
    if cell == nil or targetTypes == nil or player == nil then
        EtherItemSearch.results = nil;
        return 0;
    end

    EtherItemSearch.lastTargets = targetTypes;
    EtherItemSearch.lastScanAt = getTimestampMs();
    EtherItemSearch._scanX = player:getX();
    EtherItemSearch._scanY = player:getY();

    local out, key, n = {}, {}, 0;
    local stats = { squares = 0, floor = 0, containers = 0, bags = 0, items = 0 };

    local function addAt(x, y, z, name)
        local k = math.floor(x) .. "," .. math.floor(y) .. "," .. math.floor(z);
        if key[k] ~= nil then
            out[key[k]].count = out[key[k]].count + 1;
        else
            n = n + 1;
            key[k] = n;
            out[n] = { x = math.floor(x), y = math.floor(y), z = math.floor(z), name = name, count = 1 };
        end
    end

    local function itemHits(item)
        if item == nil then return nil end
        stats.items = stats.items + 1;
        local t = item:getFullType();
        -- 命中返回显示名 (真值), 未命中返回 nil —— 名称供世界标记文字使用
        if targetTypes[t] then return item:getDisplayName() end
        if targetTypes[item:getType()] == true then return item:getDisplayName() end
        return nil;
    end

    local function scanItems(items, px, py, pz)
        if items == nil then return end
        for i = 1, items:size() do
            local item = items:get(i - 1);
            local name = itemHits(item);
            if name then
                addAt(px, py, pz, name);
            end
        end
    end

    -- 物件容器 (家具/尸体/箱子)
    local function scanContainers(obj)
        local nCont = obj:getContainerCount();
        for j = 0, nCont - 1 do
            local c = obj:getContainerByIndex(j);
            if c ~= nil then
                stats.containers = stats.containers + 1;
                scanItems(c:getItems(), obj:getX(), obj:getY(), obj:getZ());
            end
        end
    end

    -- 地面物品 (含放在地上的包: 包本身 + 包内物品)
    local function scanFloor(w)
        stats.floor = stats.floor + 1;
        local item = w:getItem();
        if item == nil then return end
        local name = itemHits(item);
        if name then
            addAt(w:getX(), w:getY(), w:getZ(), name);
        end
        -- 仅当物品是容器 (包/箱子) 时才取内部物品; IsInventoryContainer 所有物品都有, 不会抛异常
        if item:IsInventoryContainer() then
            local inv = item:getInventory();
            if inv ~= nil then
                stats.bags = stats.bags + 1;
                scanItems(inv:getItems(), w:getX(), w:getY(), w:getZ());
            end
        end
    end

    local function scanSquare(sq)
        stats.squares = stats.squares + 1;
        local objects = sq:getObjects();
        if objects ~= nil then
            for i = 1, objects:size() do
                local obj = objects:get(i - 1);
                if obj ~= nil then
                    scanContainers(obj);
                end
            end
        end
        local wobs = sq:getWorldObjects();
        if wobs ~= nil then
            for i = 1, wobs:size() do
                local w = wobs:get(i - 1);
                if w ~= nil then
                    scanFloor(w);
                end
            end
        end

        -- 尸体 (IsoDeadBody 不在 getObjects 里, 在 getDeadBodys 独立列表;
        -- 物品在其 getContainer() 容器中, 需单独遍历)
        local bodies = sq:getDeadBodys();
        if bodies ~= nil then
            for i = 0, bodies:size() - 1 do
                local body = bodies:get(i);
                if body ~= nil then
                    local c = body:getContainer();
                    if c ~= nil then
                        stats.containers = stats.containers + 1;
                        scanItems(c:getItems(), body:getX(), body:getY(), body:getZ());
                    end
                end
            end
        end
    end

    -- 玩家周围半径内的已加载方格 (getGridSquare 纯查询, 不会触发区块生成)
    local px, py, pz = player:getX(), player:getY(), player:getZ();
    local R = EtherItemSearch.radius;
    for z = pz - 1, pz + 1 do
        for dx = -R, R do
            local cx = math.floor(px) + dx;
            for dy = -R, R do
                local sq = cell:getGridSquare(cx, math.floor(py) + dy, z);
                if sq ~= nil then
                    scanSquare(sq);
                end
            end
        end
    end

    -- 车辆部件容器 (后备箱/座位等); getVehicles() 返回 Set, :toArray() 转 Lua 数组 (官方 UIMap 同款用法)
    -- 注意: 部件遍历必须在车辆对象上调用 getPartCount()/getPartByIndex()
    --       (官方 ISInventoryPage 同款); 车辆.getParts 返回的 VehicleParts
    --       java 对象未暴露给 Lua, 无法对其点方法
    local vehicles = cell:getVehicles();
    if vehicles ~= nil then
        local vlist = vehicles:toArray();
        for vi = 1, #vlist do
            local v = vlist[vi];
            if v ~= nil and math.abs(v:getX() - px) <= R and math.abs(v:getY() - py) <= R then
                local nParts = v:getPartCount();
                for i = 0, nParts - 1 do
                    local part = v:getPartByIndex(i);
                    if part ~= nil then
                        local c = part:getItemContainer();
                        if c ~= nil then
                        stats.containers = stats.containers + 1;
                        scanItems(c:getItems(), part:getX(), part:getY(), v:getZ());
                        end
                    end
                end
            end
        end
    end

    EtherItemSearch.results = out;
    if n == 0 and not silent then
        print("[EtherHack] 未找到该物品 (已扫=" .. stats.squares .. "格, 地面物品=" .. stats.floor .. ", 容器=" .. stats.containers .. ", 背包=" .. stats.bags .. ", 检查物品=" .. stats.items .. ")");
    end
    return n;
end

--*********************************************************
--* 事件驱动刷新: 背包物品数变化 / 库存窗口容器变化 -> 置脏,
--* 安静满 1 秒后由 onPlayerUpdate 触发的 refresh() 重扫一次
--* (拾取、丢弃、吃东西等都会改变背包物品数, 标记随之更新)
--*********************************************************
function EtherItemSearch.refresh()
    if EtherItemSearch.results == nil or EtherItemSearch.lastTargets == nil then return end
    if not EtherItemSearch.refreshPending then return end
    local now = getTimestampMs();
    if now - EtherItemSearch.lastChangeAt < EtherItemSearch.debounceMs then return end
    EtherItemSearch.refreshPending = false;
    EtherItemSearch.scan(EtherItemSearch.lastTargets, true);
end

local function onPlayerUpdate(player)
    if EtherItemSearch.results == nil or player == nil then return end
    -- 官方写法: getInventory():getItems():size() (ItemContainer 未直接暴露 size)
    local items = player:getInventory():getItems();
    if items ~= nil then
        local s = items:size();
        if s ~= EtherItemSearch._invSize then
            EtherItemSearch._invSize = s;
            EtherItemSearch.refreshPending = true;
            EtherItemSearch.lastChangeAt = getTimestampMs();
        end
    end

    -- 移动刷新: 走出 5 格且距上次扫描 ≥2 秒 -> 静默重扫, 标记跟随角色
    if EtherItemSearch._scanX ~= nil then
        local dx = player:getX() - EtherItemSearch._scanX;
        local dy = player:getY() - EtherItemSearch._scanY;
        local now = getTimestampMs();
        if dx * dx + dy * dy >= (EtherItemSearch.moveRefreshTiles * EtherItemSearch.moveRefreshTiles)
           and now - EtherItemSearch.lastScanAt >= EtherItemSearch.moveRefreshMs then
            EtherItemSearch.scan(EtherItemSearch.lastTargets, true);
            return;
        end
    end

    EtherItemSearch.refresh();
end

local function onInventoryWindowChanged()
    if EtherItemSearch.results == nil then return end
    EtherItemSearch.refreshPending = true;
    EtherItemSearch.lastChangeAt = getTimestampMs();
    EtherItemSearch.refresh();
end

if Events ~= nil then
    Events.OnPlayerUpdate.Add(onPlayerUpdate);
    Events.OnRefreshInventoryWindowContainers.Add(onInventoryWindowChanged);
end

--*********************************************************
--* 清除搜索结果 (标记不再显示)
--*********************************************************
function EtherItemSearch.clear()
    EtherItemSearch.results = nil;
    EtherItemSearch.lastTargets = nil;
    EtherItemSearch._invSize = nil;
    EtherItemSearch.refreshPending = false;
    EtherItemSearch.lastChangeAt = 0;
    EtherItemSearch._scanX = nil;
    EtherItemSearch._scanY = nil;
end

--*********************************************************
--* 小地图"物品"开关: 关闭时清掉标记并停止事件开销 (保留搜索目标),
--* 重新开启时立即按上次目标静默重扫
--*********************************************************
EtherItemSearch.showOnMap = true; -- 小地图物品标记显示开关 (UIMap 渲染时判断)
function EtherItemSearch.setEnabled(enabled)
    EtherItemSearch.showOnMap = enabled;
    if enabled then
        if EtherItemSearch.lastTargets ~= nil then
            EtherItemSearch.scan(EtherItemSearch.lastTargets, true);
        end
    else
        EtherItemSearch.results = nil;
        EtherItemSearch.refreshPending = false;
    end
end

--*********************************************************
--* 世界标记开关的全局封装: ESP 页「物品信息」勾选模块与小地图快捷按钮
--* 共用同一状态 (UIMap.drawWorld), 两处任意切换另一处即时同步 (各自逐帧读取)
--*********************************************************
function isMapDrawWorld()
    UIMap.ensureDrawFlags();
    return UIMap.drawWorld and true or false;
end

function toggleMapDrawWorld(v)
    UIMap.ensureDrawFlags();
    UIMap.drawWorld = v and true or false;
end

--*********************************************************
--* 世界标记 (ESP): 搜索结果存续期间, 把每个命中位置画在世界画面上
--* 开关: UIMap.drawWorld (小地图快捷按钮「世界」, 会话级默认关)
--* 通道: Events.OnPostUIDraw 逐帧事件 —— 与 Java 版 ESP (EtherAPI.updateVisuals)
--*       同一事件; 原版 LastStand/TutorialSetup 有 Lua 订阅先例, 无需任何面板
--* 投影: IsoUtils.XToScreen/YToScreen + getZoom/相机偏移/瓦片高度修正,
--*       与 Java 侧 ZombieUtils.getScreenPositionX/Y 完全同一套公式
--* 性能: 开关关或无结果时单布尔早退 (空闲零开销); 只画 worldDrawRadius 格内
--*       命中; 命中数超过 worldMaxMarkers 才排序取最近 (常规路径零排序);
--*       暂存数组跨帧复用, 仅结果清空时重建
--* 留痕: 纯客户端渲染, 零网络包零日志
--*********************************************************
EtherItemSearch.worldDrawRadius = 32;  -- 世界标记绘制半径(格), 太远的在屏幕外画了也看不见
EtherItemSearch.worldMaxMarkers = 60;  -- 单帧最多绘制条数, 命中洪峰时取最近的
EtherItemSearch._worldProbe = nil;     -- nil=未探测; true=投影可用; false=投影原语不可达(自动禁用, 不影响其它功能)
EtherItemSearch._playerIndex = nil;    -- 渲染玩家索引 (启动后探测一次, 失败回退 0, 单人/多人客户端均正确)

local scratchD2 = {};  -- 可见命中距离平方暂存 (跨帧复用)
local scratchP = {};   -- 可见命中结果引用暂存

local function worldProbeOk()
    if EtherItemSearch._worldProbe ~= nil then return EtherItemSearch._worldProbe end
    local ok = pcall(function()
        local v = IsoCamera.frameState.playerIndex;  -- 类/字段不可达会在此抛错
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
        EtherItemSearch._playerIndex = (type(v) == "number") and v or 0;
    end);
    EtherItemSearch._worldProbe = ok;
    if not ok then
        print("[EtherHack] 世界标记: 投影/绘制原语不可用, 功能自动禁用 (其余功能不受影响)");
    end
    return ok;
end

local function worldToScreen(x, y, z, pi)
    local sx = IsoUtils.XToScreen(x, y, z, pi);
    local sy = IsoUtils.YToScreen(x, y, z, pi);
    local zoom = getCore():getZoom(pi);
    sx = (sx - IsoCamera.getOffX()) / zoom;
    sy = (sy - IsoCamera.getOffY() - 128 / (2 / Core.getTileScale())) / zoom;
    return sx, sy;
end

local function drawWorldMarkers()
    if not UIMap.drawWorld then return end
    local results = EtherItemSearch.results;
    -- 注意: PZ 的 Kahlua 环境没有裸 next(), 只有 pairs/ipairs —— 用 # 判空
    if results == nil or #results == 0 then return end
    local player = getPlayer();
    if player == nil then return end
    if not worldProbeOk() then return end

    local pi = EtherItemSearch._playerIndex or 0;
    local px, py = player:getX(), player:getY();
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
        local sx, sy = worldToScreen(p.x + 0.5, p.y + 0.5, (p.z or 0) + 0.4, pi);
        if sx > -160 and sy > -20 and sx < sw and sy < sh then
            local dist = math.floor(math.sqrt(scratchD2[idx]));
            local text = p.name .. " ×" .. p.count .. " (" .. dist .. "m)";
            local tx = sx - tm:MeasureStringX(UIFont.Small, text) / 2;
            -- 阴影 + 主字双层, 与 ESP 文字同款画法; 琥珀色区别于僵尸红/载具白
            -- DrawString 用 8 参无缩放形式 (原版先例), 不赌 9 参重载分派
            tm:DrawString(UIFont.Small, tx + 1, sy + 1, text, 0, 0, 0, 0.9);
            tm:DrawString(UIFont.Small, tx, sy, text, 1, 0.78, 0.35, 1);
        end
    end
    for i = 1, cnt do scratchD2[i] = nil; scratchP[i] = nil; end
end

if Events ~= nil then
    Events.OnPostUIDraw.Add(drawWorldMarkers);
end