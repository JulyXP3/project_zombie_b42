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
--* 刷新: 事件驱动 + 时间片步进 —— onPlayerUpdate 每 tick:
--*       1. 比较玩家背包物品数, 有变化置"脏", 安静满 1 秒才启动重扫;
--*       2. 比较玩家位置, 走出 5 格且距上次扫描完成 ≥4 秒即重扫 (标记跟随角色);
--*       3. 推进在途扫描游标 (每帧 3ms 预算, 超时让出下帧继续)。
--* 扫描不再是单帧全量 (~3.8 万格走 Kahlua 桥, 密集区 100-500ms 尖峰,
--* 结果在扫描完成后原子替换 —— 进行期间旧标记照常显示, 刷新无感。
--* 注意: 禁止 "obj:getItem ~= nil" 这类索引式方法探测 (Kahlua2 不可靠),
--*       一律直接调用或 pcall 兜底
--*********************************************************
EtherItemSearch = EtherItemSearch or {};

EtherItemSearch.results = nil; -- { {x=.., y=.., z=.., name=.., count=..}, ... } 命中位置列表 (z=楼层, name=首个命中物品显示名, 与世界标记共用)
EtherItemSearch.radius = 56;   -- 扫描半径(格), 仅覆盖玩家周围已加载区域
EtherItemSearch.lastTargets = nil;    -- 上次扫描目标, 供自动刷新复用
EtherItemSearch.debounceMs = 1000;    -- 防抖: 背包变动后安静满 1 秒才重扫
EtherItemSearch.refreshPending = false; -- 待重扫标记
EtherItemSearch.lastChangeAt = 0;     -- 最后一次背包变动时刻
EtherItemSearch.lastScanAt = 0;       -- 上次扫描完成时刻
EtherItemSearch.moveRefreshTiles = 5; -- 玩家走出 N 格触发移动重扫
EtherItemSearch.moveRefreshMs = 4000; -- 移动重扫最小间隔 (连续奔跑也不超过 1 次/4 秒)
EtherItemSearch.stepBudgetMs = 3;     -- 时间片: 每帧扫描步进预算 (ms), 超出即让出下帧继续
EtherItemSearch._scanX = nil;         -- 上次扫描时的玩家位置
EtherItemSearch._scanY = nil;
EtherItemSearch._cursor = nil;        -- 在途扫描游标 (非 nil = 有扫描进行中)

--*********************************************************
--* 启动扫描: targetTypes = { ["Base.Axe"]=true, ... }
--* 立即返回; 扫描由 onPlayerUpdate 里的 stepScan 按时间片推进,
--* 完成时 results 原子替换并回调 onDone(nHits, stats)。
--* onDone 可为 nil (自动刷新用); 新扫描会取代在途扫描 (游标重置,
--* 旧 results 保留到新扫描完成 —— 无闪烁)。
--* silent=true 且无 onDone 时, 零命中不打印诊断 (自动刷新用)。
--*********************************************************
function EtherItemSearch.startScan(targetTypes, silent, onDone)
    local cell = getCell();
    local player = getPlayer();
    if cell == nil or targetTypes == nil or player == nil then
        EtherItemSearch.results = nil;
        if onDone then onDone(0, nil) end
        return;
    end

    EtherItemSearch.lastTargets = targetTypes;

    -- 游标: 累积结果与循环位置全部挂在 cur 上 (闭包与步进器共享)
    local cur = {
        cell = cell;
        targets = targetTypes; silent = silent; onDone = onDone;
        stats = { squares = 0, floor = 0, containers = 0, bags = 0, items = 0 };
        out = {}; key = {}; n = 0;
        px = player:getX(); py = player:getY(); pz = player:getZ();
        R = EtherItemSearch.radius;
    };
    cur.cx0 = math.floor(cur.px);
    cur.cy0 = math.floor(cur.py);
    cur.z = math.floor(cur.pz) - 1;   -- 三重循环游标: z / dx / dy
    cur.zMax = math.floor(cur.pz) + 1;
    cur.dx = -cur.R;
    cur.dy = -cur.R;

    local function addAt(x, y, z, name)
        local k = math.floor(x) .. "," .. math.floor(y) .. "," .. math.floor(z);
        if cur.key[k] ~= nil then
            cur.out[cur.key[k]].count = cur.out[cur.key[k]].count + 1;
        else
            cur.n = cur.n + 1;
            cur.key[k] = cur.n;
            cur.out[cur.n] = { x = math.floor(x), y = math.floor(y), z = math.floor(z), name = name, count = 1 };
        end
    end

    local function itemHits(item)
        if item == nil then return nil end
        cur.stats.items = cur.stats.items + 1;
        local t = item:getFullType();
        -- 命中返回显示名 (真值), 未命中返回 nil —— 名称供世界标记文字使用
        if cur.targets[t] then return item:getDisplayName() end
        if cur.targets[item:getType()] == true then return item:getDisplayName() end
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
                cur.stats.containers = cur.stats.containers + 1;
                scanItems(c:getItems(), obj:getX(), obj:getY(), obj:getZ());
            end
        end
    end

    -- 地面物品 (含放在地上的包: 包本身 + 包内物品)
    local function scanFloor(w)
        cur.stats.floor = cur.stats.floor + 1;
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
                cur.stats.bags = cur.stats.bags + 1;
                scanItems(inv:getItems(), w:getX(), w:getY(), w:getZ());
            end
        end
    end

    local function scanSquare(sq)
        cur.stats.squares = cur.stats.squares + 1;
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
                        cur.stats.containers = cur.stats.containers + 1;
                        scanItems(c:getItems(), body:getX(), body:getY(), body:getZ());
                    end
                end
            end
        end
    end

    cur.scanSquare = scanSquare;
    cur.scanItems = scanItems;
    EtherItemSearch._cursor = cur;
end

--*********************************************************
--* 时间片步进器: 由 onPlayerUpdate 每 tick 调用 (先于一切门控,
--* 在途扫描必跑完 —— 扫描由 startScan 启动才存在, 天然有界)。
--* 预算内尽量多走方格, 超时让出下帧继续; 走完全部方格后收尾
--* 车辆部件容器 (车辆数少, 单步收尾), 原子替换 results, 回调 onDone。
--*********************************************************
local function stepScan()
    local cur = EtherItemSearch._cursor;
    if cur == nil then return end
    local deadline = getTimestampMs() + EtherItemSearch.stepBudgetMs;
    local cell = cur.cell;
    local R = cur.R;

    -- 玩家周围半径内的已加载方格 (getGridSquare 纯查询, 不会触发区块生成)
    while cur.z <= cur.zMax do
        while cur.dx <= R do
            local cx = cur.cx0 + cur.dx;
            while cur.dy <= R do
                local sq = cell:getGridSquare(cx, cur.cy0 + cur.dy, cur.z);
                if sq ~= nil then
                    cur.scanSquare(sq);
                end
                cur.dy = cur.dy + 1;
                if getTimestampMs() >= deadline then return end
            end
            cur.dy = -R;
            cur.dx = cur.dx + 1;
        end
        cur.dx = -R;
        cur.z = cur.z + 1;
    end

    -- 收尾: 车辆部件容器 (后备箱/座位等); getVehicles() 返回 Set,
    -- :toArray() 转 Lua 数组 (官方 UIMap 同款用法)。
    -- 部件遍历必须在车辆对象上调用 getPartCount()/getPartByIndex()
    -- (官方 ISInventoryPage 同款); 车辆.getParts 返回的 VehicleParts
    -- java 对象未暴露给 Lua, 无法对其点方法
    local vehicles = cell:getVehicles();
    if vehicles ~= nil then
        local vlist = vehicles:toArray();
        for vi = 1, #vlist do
            local v = vlist[vi];
            if v ~= nil and math.abs(v:getX() - cur.px) <= R and math.abs(v:getY() - cur.py) <= R then
                local nParts = v:getPartCount();
                for i = 0, nParts - 1 do
                    local part = v:getPartByIndex(i);
                    if part ~= nil then
                        local c = part:getItemContainer();
                        if c ~= nil then
                            cur.stats.containers = cur.stats.containers + 1;
                            cur.scanItems(c:getItems(), part:getX(), part:getY(), v:getZ());
                        end
                    end
                end
            end
        end
    end

    -- 完成: 原子替换结果 (进行期间旧标记照常显示)
    local n = cur.n;
    EtherItemSearch.results = cur.out;
    EtherItemSearch.lastScanAt = getTimestampMs();
    EtherItemSearch._scanX = cur.px;
    EtherItemSearch._scanY = cur.py;
    EtherItemSearch._cursor = nil;
    if cur.onDone ~= nil then
        cur.onDone(n, cur.stats);
    elseif n == 0 and not cur.silent then
        print("[EtherHack] 未找到该物品 (已扫=" .. cur.stats.squares .. "格, 地面物品=" .. cur.stats.floor .. ", 容器=" .. cur.stats.containers .. ", 背包=" .. cur.stats.bags .. ", 检查物品=" .. cur.stats.items .. ")");
    end
end

--*********************************************************
--* 事件驱动刷新: 背包物品数变化 / 库存窗口容器变化 -> 置脏,
--* 安静满 1 秒后由 onPlayerUpdate 触发重扫一次
--* (拾取、丢弃、吃东西等都会改变背包物品数, 标记随之更新;
--*  在途扫描进行中不重复启动, 完成后节流自然兜住下一轮)
--*********************************************************
function EtherItemSearch.refresh()
    if EtherItemSearch.results == nil or EtherItemSearch.lastTargets == nil then return end
    -- 两组开关全关: 显示门控关闭, 暂停重扫 (结果与目标保留, 重开即恢复)
    if not UIMap.drawItems and not UIMap.drawItemEsp then return end
    if not EtherItemSearch.refreshPending then return end
    local now = getTimestampMs();
    if now - EtherItemSearch.lastChangeAt < EtherItemSearch.debounceMs then return end
    EtherItemSearch.refreshPending = false;
    if EtherItemSearch._cursor ~= nil then return end
    EtherItemSearch.startScan(EtherItemSearch.lastTargets, true, nil);
end

local function onPlayerUpdate(player)
    -- 步进器先于一切门控: 在途扫描必跑完 (首扫时 results 尚为 nil)
    stepScan();
    if player == nil then return end
    if EtherItemSearch.results == nil then return end
    -- 两组开关全关: 暂停位置/背包轮询与自动重扫 (结果与目标保留, 重开即恢复)
    if not UIMap.drawItems and not UIMap.drawItemEsp then return end
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

    -- 移动刷新: 走出 5 格且距上次扫描完成 ≥4 秒 -> 静默重扫, 标记跟随角色
    if EtherItemSearch._scanX ~= nil then
        local dx = player:getX() - EtherItemSearch._scanX;
        local dy = player:getY() - EtherItemSearch._scanY;
        local now = getTimestampMs();
        if dx * dx + dy * dy >= (EtherItemSearch.moveRefreshTiles * EtherItemSearch.moveRefreshTiles)
           and now - EtherItemSearch.lastScanAt >= EtherItemSearch.moveRefreshMs then
            if EtherItemSearch._cursor == nil then
                EtherItemSearch.startScan(EtherItemSearch.lastTargets, true, nil);
            end
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
--* 清除搜索结果 (标记不再显示; 中止在途扫描)
--*********************************************************
function EtherItemSearch.clear()
    EtherItemSearch.results = nil;
    EtherItemSearch.lastTargets = nil;
    EtherItemSearch._invSize = nil;
    EtherItemSearch.refreshPending = false;
    EtherItemSearch.lastChangeAt = 0;
    EtherItemSearch._scanX = nil;
    EtherItemSearch._scanY = nil;
    EtherItemSearch._cursor = nil;
end

--*********************************************************
--* 小地图"物品"开关: 关闭时清掉标记并停止事件开销 (保留搜索目标),
--* 重新开启时立即按上次目标静默重扫
--*********************************************************
--*********************************************************
--* 两组独立开关 (雷达页两个勾选 / 小地图「物品」按钮 / 地图页勾选 的落点):
--*   setMinimapEnabled = 小地图标记 (UIMap.drawItems, Java setMapDrawItems 持久化)
--*   setItemEspEnabled = ESP 画线追踪 (UIMap.drawItemEsp, 会话级)
--* 开关只是显示门控: 扫描结果与追踪目标保留不清, 两组全关时暂停自动刷新轮询
--* (onPlayerUpdate/refresh 门控), 重新开启立即恢复上次追踪
--* (修 bug: 小地图物品按钮关→开后追踪消失 —— 旧清理逻辑把目标一并清掉了)
--*********************************************************
function EtherItemSearch.setMinimapEnabled(enabled)
    UIMap.ensureDrawFlags();
    UIMap.drawItems = enabled;   -- 小地图标记渲染 + 快捷按钮灰白 (逐帧读取, 天然同步)
    setMapDrawItems(enabled);    -- Java 持久化
end

function EtherItemSearch.setItemEspEnabled(enabled)
    UIMap.ensureDrawFlags();
    UIMap.drawItemEsp = enabled; -- 世界画面雷达线 (drawWorldMarkers 逐帧读取)
end

--*********************************************************
--* ESP 画线开关的全局读取 (雷达页勾选框初始态/逐帧同步用):
--*   UIMap.drawItemEsp = ESP 画线追踪 (会话级); 小地图标记 = UIMap.drawItems (Java 持久化)
--* 两组自由组合: 只小地图 / 小地图+画线 / 单独画线
--*********************************************************
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
        print("[EtherHack] 世界标记: 投影/绘制原语不可用, 功能自动禁用 (其余功能不受影响)");
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
            local dz = (p.z or 0) - pz;
            local floorTag = "";
            if dz > 0 then
                floorTag = " " .. tr("UI_RadarPanel_FloorUp");
            elseif dz < 0 then
                floorTag = " " .. tr("UI_RadarPanel_FloorDown");
            end
            local text = p.name .. " x" .. p.count .. floorTag;
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
    Events.OnPostUIDraw.Add(drawWorldMarkers);
end
