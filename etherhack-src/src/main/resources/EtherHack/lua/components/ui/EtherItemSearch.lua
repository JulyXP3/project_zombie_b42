--*********************************************************
--* 物品搜索模块: 以玩家为中心扫描已加载区块方格内的物品
--* 数据源: getCell():getGridSquare(x,y,z) -> square:getObjects()/getWorldObjects()
--*   IsoObject 容器(getContainerCount/getContainerByIndex)
--*   IsoWorldInventoryObject 地面物品(getItem + 背包getInventory)
--*   车辆: cell:getVehicles():toArray() -> 部件容器
--* 坐标: 一律使用物件自身 getX()/getY() (比方格中心精确)
--* 原因: 客户端 processItems 注册表恒为空 (IsoCell.addToProcessItems
--*       首行 if (GameClient.client) return), 只能用已加载方格扫描
--* 匹配: 先比 getFullType() ("Base.Axe"), 不比中再比 getType() ("Axe"),
--*       兼容按钮侧 scriptItem:getFullName() 返回短名的可能
--* 注意: 禁止 "obj:getItem ~= nil" 这类索引式方法探测 (Kahlua2 不可靠),
--*       一律直接调用或 pcall 兜底
--*********************************************************
EtherItemSearch = EtherItemSearch or {};

EtherItemSearch.results = nil; -- { {x=.., y=.., count=..}, ... } 命中位置列表
EtherItemSearch.radius = 48;   -- 扫描半径(格), 仅覆盖玩家周围已加载区域
EtherItemSearch.lastTargets = nil;    -- 上次扫描目标, 供自动刷新复用
EtherItemSearch.lastRefreshAt = 0;    -- 上次扫描时刻 (getTimestampMs)
EtherItemSearch.autoRefreshSeconds = 3; -- 标记显示期间每隔 N 秒自动重扫 (拾取后自动消失)

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
    EtherItemSearch.lastRefreshAt = getTimestampMs();

    local out, key, n = {}, {}, 0;
    local stats = { squares = 0, floor = 0, containers = 0, bags = 0, items = 0 };

    local function addAt(x, y)
        local k = math.floor(x) * 100000 + math.floor(y);
        if key[k] ~= nil then
            out[key[k]].count = out[key[k]].count + 1;
        else
            n = n + 1;
            key[k] = n;
            out[n] = { x = math.floor(x), y = math.floor(y), count = 1 };
        end
    end

    local function itemHits(item)
        if item == nil then return false end
        stats.items = stats.items + 1;
        local t = item:getFullType();
        if targetTypes[t] then return true end
        return targetTypes[item:getType()] == true;
    end

    local function scanItems(items, px, py)
        if items == nil then return end
        for i = 1, items:size() do
            local item = items:get(i - 1);
            if itemHits(item) then
                addAt(px, py);
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
                scanItems(c:getItems(), obj:getX(), obj:getY());
            end
        end
    end

    -- 地面物品 (含放在地上的包: 包本身 + 包内物品)
    local function scanFloor(w)
        stats.floor = stats.floor + 1;
        local item = w:getItem();
        if item == nil then return end
        if itemHits(item) then
            addAt(w:getX(), w:getY());
        end
        -- 仅当物品是容器 (包/箱子) 时才取内部物品; IsInventoryContainer 所有物品都有, 不会抛异常
        if item:IsInventoryContainer() then
            local inv = item:getInventory();
            if inv ~= nil then
                stats.bags = stats.bags + 1;
                scanItems(inv:getItems(), w:getX(), w:getY());
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
    local vehicles = cell:getVehicles();
    if vehicles ~= nil then
        local vlist = vehicles:toArray();
        for vi = 1, #vlist do
            local v = vlist[vi];
            if v ~= nil and math.abs(v:getX() - px) <= R and math.abs(v:getY() - py) <= R then
                local parts = v:getParts();
                local nParts = parts:getPartCount();
                for i = 0, nParts - 1 do
                    local part = parts:getPartByIndex(i);
                    if part ~= nil then
                        local c = part:getItemContainer();
                        if c ~= nil then
                            stats.containers = stats.containers + 1;
                            scanItems(c:getItems(), part:getX(), part:getY());
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
--* 自动刷新: 标记显示期间周期性重扫 (拾取/移动物品后标记自动更新)
--* 由 UIMap:render() 每帧调用, 内部限频
--*********************************************************
function EtherItemSearch.tick()
    if EtherItemSearch.results == nil or EtherItemSearch.lastTargets == nil then return end
    local now = getTimestampMs();
    if now - EtherItemSearch.lastRefreshAt < EtherItemSearch.autoRefreshSeconds * 1000 then return end
    EtherItemSearch.scan(EtherItemSearch.lastTargets, true);
end

--*********************************************************
--* 清除搜索结果 (标记不再显示)
--*********************************************************
function EtherItemSearch.clear()
    EtherItemSearch.results = nil;
    EtherItemSearch.lastTargets = nil;
end