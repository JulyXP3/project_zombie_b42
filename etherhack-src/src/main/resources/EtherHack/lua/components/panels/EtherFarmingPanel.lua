require "ISUI/ISPanel"

--*********************************************************
--* EtherFarmingPanel: 耕作面板 (原角色页「作弊耕种模式」开关的面板化替代)
--*
--* 通道: 客户端 CFarmingSystem.instance:sendCommand -> 服务端
--*   SFarmingSystem:OnClientCommand -> farmingCommands.lua,
--*   全部指令零校验(无权限/距离/物品检查, 反作弊枚举不含农业),
--*   参数格式与原版调试菜单 ISFarmingMenu.onCheat* 一致。
--* 作物管理: 状态行(每0.5s扫描, 范围跟随下方范围输入) + 生长进入下一阶段/
--*   直接成熟/浇水到满/清空水分/全部治病/感染病害+25/收获/铲除/移除残骸。
--*   提示: 生长类操作最晚在下一个游戏10分钟刻生效 (服务端
--*   SFarmingSystem:EveryTenMinutes -> checkPlant 才评估 nextGrowing,
--*   与原版作弊菜单行为一致)。全部动作按范围(格) N×N 生效, 默认 3 = 3×3。
--*   播种: 种子列表(farming_vegetableconf.props, 客户端加载时同样执行
--*     LoadDirBase("server"), 数据在 SP/MP 均可用) + 翻土/播种。
--*     翻土前客户端按 ISFarmingMenu.canDigHereSquare 过滤 —— 服务端
--*     Commands.plow 对任意格子无条件生成耕地(farmingCommands.lua:67-78),
--*     不滤会把水泥地/地板也翻成田; 有活作物的格子跳过(plow 先 removePlant
--*     再翻土, 错发即铲作物)。播种只对 state=="plow" 的格子发 seed 指令
--*     (服务器同样只查格子状态), 不消耗背包种子。
--*********************************************************

EtherFarmingPanel = EtherFormPanel:derive("EtherFarmingPanel");

--*********************************************************
--* 目标收集: 范围 N×N 方形区域内的全部作物 (与翻土/播种同语义, 半宽 = N/2)
--*********************************************************
-- 范围驱动的目标收集 (N×N 方形, 与翻土/播种循环一致; 3×3 → 9 格)。
-- 走 Java 层判空遍历: getLuaObjectByIndex 是 getObjectByIndex(i-1):getModData()
-- 链式调用, 空洞槽会抛原生异常。
local function collectTargetPlants(radius)
    local p = getPlayer();
    if p == nil then return {}; end
    local sys = CFarmingSystem.instance;
    if sys == nil or sys.system == nil then return {}; end
    local px, py, pz = math.floor(p:getX()), math.floor(p:getY()), p:getZ();
    local out = {};
    for i = 1, sys:getLuaObjectCount() do
        local go = sys.system:getObjectByIndex(i - 1);
        if go ~= nil then
            local plant = go:getModData();
            if plant ~= nil and plant.x ~= nil and plant.y ~= nil and plant.z == pz then
                local dx, dy = plant.x - px, plant.y - py;
                if math.abs(dx) <= radius and math.abs(dy) <= radius then
                    table.insert(out, plant);
                end
            end
        end
    end
    return out;
end

--*********************************************************
--* 统一指令封装 (字段名与 vanilla ISFarmingMenu.onCheat 一致)。
--* cheat 字段不存在时不发(服务端 plant[var] + count 会因 nil 报错),
--* count 为 0 不发(省包)。
--*********************************************************
local function cheatAdjust(plant, var, count)
    if plant[var] == nil or count == 0 then return; end
    CFarmingSystem.instance:sendCommand(getPlayer(), 'cheat',
        { var = var, count = count, x = plant.x, y = plant.y, z = plant.z });
end

local function sendCmd(command, args)
    CFarmingSystem.instance:sendCommand(getPlayer(), command, args);
end

--*********************************************************
--* 按钮流式布局: 逐个取宽塞行, 放不下换行。
--* flowRows 返回 rows, 高度预算与摆放共用同一份结果。
--*********************************************************
local function flowRows(defs, innerW)
    local rows, row = {}, { items = {}, w = 0 };
    for i = 1, #defs do
        local w = math.min(UIButton.measureWidth(defs[i].title), innerW);
        local needGap = (#row.items > 0);
        if needGap and row.w + EtherTheme.ctrlGap + w > innerW then
            table.insert(rows, row);
            row = { items = {}, w = 0 };
            needGap = false;
        end
        row.w = row.w + (needGap and EtherTheme.ctrlGap or 0) + w;
        table.insert(row.items, { def = defs[i], w = w });
    end
    if #row.items > 0 then table.insert(rows, row); end
    return rows;
end

local function flowHeight(rows)
    return #rows * EtherTheme.ctrlH + math.max(#rows - 1, 0) * EtherTheme.ctrlGap;
end

local function placeButtonFlow(panel, rows, bx, by)
    local y = by;
    for r = 1, #rows do
        local x = bx;
        for i = 1, #rows[r].items do
            local cell = rows[r].items[i];
            local btn = UIButton:new(x, y, cell.w, EtherTheme.ctrlH, cell.def.title, cell.def.on, cell.w);
            panel:addWidget(btn, { onlyInGame = true });
            x = x + cell.w + EtherTheme.ctrlGap;
        end
        y = y + EtherTheme.ctrlH + EtherTheme.ctrlGap;
    end
end

--*********************************************************
--* 动作: 目标作物模块 (对收集到的每株各发一条指令, 无本地乐观修改)
--*********************************************************
function EtherFarmingPanel:eachTarget(fn)
    self.targetPlants = collectTargetPlants(math.floor(self:readRadius() / 2));
    for i = 1, #self.targetPlants do
        fn(self.targetPlants[i]);
    end
end

--*********************************************************
--* 生长进入下一阶段: 与原版作弊菜单 onCheatGrow 同款 (nextGrowing 拉到
--* 现在, 下一个游戏10分钟刻 checkPlant 评估生长, 服务端 grow() 自行推进
--* 一档)。实测单独有效 (2026-08-25 用户确认)。
--*********************************************************
-- nextGrowing 重置: 客户端数据已同步 → 差量拉到 min(now,100) (服务端钳 0..100);
-- 客户端数据未同步(刚播种, state 仍是 plow) → 服务端值未知, 直发巨大负值
-- (服务端 plant[var]+count 后钳 0 = 立即到期, 下一游戏10分钟刻即生长/成熟)。
-- 仅在面板记录过该格已播种时使用 — 裸土格服务端无 nextGrowing 字段, 发了会 nil 运算报错。
function EtherFarmingPanel:resetNextGrowing(p)
    local sys = CFarmingSystem.instance;
    if sys == nil then return; end
    if p.nextGrowing ~= nil and sys.hoursElapsed ~= nil then
        cheatAdjust(p, 'nextGrowing', math.min(sys.hoursElapsed, 100) - p.nextGrowing);
    elseif p.state == "plow" and self.sownTiles ~= nil
        and self.sownTiles[p.x .. "," .. p.y .. "," .. p.z] ~= nil then
        sendCmd('cheat', { var = 'nextGrowing', count = -100000,
            x = p.x, y = p.y, z = p.z });
    end
end

function EtherFarmingPanel:actGrow()
    if CFarmingSystem.instance == nil then return; end
    self:eachTarget(function(p)
        self:resetNextGrowing(p);
    end);
end

function EtherFarmingPanel:actWaterMax()
    self:eachTarget(function(p)
        if p.waterLvl ~= nil then
            cheatAdjust(p, 'waterLvl', (p.waterNeededMax or 100) - p.waterLvl);
        end
    end);
end

function EtherFarmingPanel:actWaterZero()
    self:eachTarget(function(p)
        if p.waterLvl ~= nil then
            cheatAdjust(p, 'waterLvl', -p.waterLvl);
        end
    end);
end

--*********************************************************
--* 直接成熟: nbOfGrow 直拉到 fullGrown (成熟结籽档, 服务器 grow() 的
--* 最高安全档, 超过会 rotten; 实测 harvestLevel ≤ fullGrown, grow() 末尾
--* :724 的 nbOfGrow>=harvestLevel 检查直接置 hasVegetable), 同时水分拉满
--* + 四虫害清零 + nextGrowing 拉到现在 (缺这步 grow() 要等原生长表到点,
--* 可能数个游戏小时后才可收获 — 2026-08-25 实测反馈)。下一游戏10分钟刻
--* 生效后即可「收获」。farming_vegetableconf.grow :665-729。
--*********************************************************
function EtherFarmingPanel:actFullGrow()
    if CFarmingSystem.instance == nil then return; end
    self:eachTarget(function(p)
        local seedType = p.typeOfSeed;
        local stale = (seedType == nil or p.nextGrowing == nil or p.nbOfGrow == nil);
        if stale then
            -- 未同步(刚播种): 种子类型取面板记录; 服务端 nbOfGrow 播种后确定为 1
            local key = p.x .. "," .. p.y .. "," .. p.z;
            if p.state == "plow" and self.sownTiles ~= nil and self.sownTiles[key] ~= nil then
                seedType = self.sownTiles[key];
            else
                return;   -- 真残骸/未知: 跳过 (状态行已区分)
            end
        end
        if seedType == nil then return; end
        local prop = farming_vegetableconf ~= nil and farming_vegetableconf.props[seedType] or nil;
        if prop == nil then return; end
        local target = prop.fullGrown or prop.mature;
        if target == nil then return; end
        if stale then
            -- 服务端: nbOfGrow 刚播种=1, 水=0, 虫=0 → 直发目标差量
            sendCmd('cheat', { var = 'nbOfGrow', count = target - 1,
                x = p.x, y = p.y, z = p.z });
            sendCmd('cheat', { var = 'waterLvl', count = 100,
                x = p.x, y = p.y, z = p.z });
        else
            if p.nbOfGrow < target then
                cheatAdjust(p, 'nbOfGrow', target - p.nbOfGrow);
            end
            if p.waterLvl ~= nil then
                cheatAdjust(p, 'waterLvl', (p.waterNeededMax or 100) - p.waterLvl);
            end
            cheatAdjust(p, 'aphidLvl', -(p.aphidLvl or 0));
            cheatAdjust(p, 'mildewLvl', -(p.mildewLvl or 0));
            cheatAdjust(p, 'fliesLvl', -(p.fliesLvl or 0));
            cheatAdjust(p, 'slugsLvl', -(p.slugsLvl or 0));
        end
        self:resetNextGrowing(p);
    end);
end

-- 全部治病: 四种虫害等级清零
function EtherFarmingPanel:actCure()
    self:eachTarget(function(p)
        cheatAdjust(p, 'aphidLvl', -(p.aphidLvl or 0));
        cheatAdjust(p, 'mildewLvl', -(p.mildewLvl or 0));
        cheatAdjust(p, 'fliesLvl', -(p.fliesLvl or 0));
        cheatAdjust(p, 'slugsLvl', -(p.slugsLvl or 0));
    end);
end

-- 感染病害 (测试「全部治病」用): 蚜虫/霉病各 +25
function EtherFarmingPanel:actInfect()
    self:eachTarget(function(p)
        cheatAdjust(p, 'aphidLvl', 25);
        cheatAdjust(p, 'mildewLvl', 25);
    end);
end

-- harvest/destroy/removePlant: 服务端自行校验, 无效时静默忽略。
-- removePlant (farmingCommands.lua:80) 整株移除 — 铲除(destroy)后的残骸
-- 也用它清掉, 清完才能重新翻土播种。
function EtherFarmingPanel:actSimple(command)
    self:eachTarget(function(p)
        sendCmd(command, { x = p.x, y = p.y, z = p.z });
    end);
end

--*********************************************************
--* 动作: 翻土 / 播种 (范围 N×N, 默认 3×3)
--*********************************************************
function EtherFarmingPanel:actPlow()
    local p = getPlayer();
    if p == nil or ISFarmingMenu == nil or ISFarmingMenu.canDigHereSquare == nil then return; end
    local px, py, pz = math.floor(p:getX()), math.floor(p:getY()), p:getZ();
    local half = math.floor(self:readRadius() / 2);   -- 范围 N×N → 半宽
    local n = 0;
    for dx = -half, half do
        for dy = -half, half do
            local sq = getCell():getGridSquare(px + dx, py + dy, pz);
            -- canDigHereSquare: 排除坟墓/有活作物/非泥土地面/z>0(除非沙盒允许),
            -- 与原版右键「翻土」的判定完全一致
            if sq ~= nil and ISFarmingMenu.canDigHereSquare(sq) then
                sendCmd('plow', { x = sq:getX(), y = sq:getY(), z = sq:getZ() });
                n = n + 1;
            end
        end
    end
    if n > 0 then
        self:flash(tr("UI_FarmingPanel_Plowed", { count = n }));
    else
        self:flash(tr("UI_FarmingPanel_NoDig"));
    end
end

function EtherFarmingPanel:selectedSeed()
    if self.seedList == nil or self.seedList.items == nil then return nil; end
    local sel = self.seedList.selected;
    if sel < 1 or sel > #self.seedList.items then return nil; end
    return self.seedList.items[sel].item.key;
end

function EtherFarmingPanel:actSow()
    local typeOfSeed = self:selectedSeed();
    if typeOfSeed == nil then
        self:flash(tr("UI_FarmingPanel_NeedSeed"));
        return;
    end
    local p = getPlayer();
    if p == nil then return; end
    local sys = CFarmingSystem.instance;
    if sys == nil then return; end
    local px, py, pz = math.floor(p:getX()), math.floor(p:getY()), p:getZ();
    local half = math.floor(self:readRadius() / 2);
    local sown = {};
    local n = 0;
    for dx = -half, half do
        for dy = -half, half do
            local plant = sys:getLuaObjectAt(px + dx, py + dy, pz);
            -- 只种已翻土格; 非 plow 状态服务器同样跳过(farmingCommands.lua:89-98)
            if plant ~= nil and plant.state == "plow" then
                sendCmd('seed', { x = plant.x, y = plant.y, z = plant.z,
                    typeOfSeed = typeOfSeed, skill = 0 });
                table.insert(sown, { x = plant.x, y = plant.y, z = plant.z });
                -- 记录播种格: 服务器同步回客户端前 state 仍显示 plow,
                -- 生长类按钮靠此记录识别"刚播种未同步"的作物
                self.sownTiles[plant.x .. "," .. plant.y .. "," .. plant.z] = typeOfSeed;
                n = n + 1;
            end
        end
    end
    -- 播种后自动浇水: 新植物 waterLvl=0 处于干枯, 走原版 Commands.water
    -- (+10/次, 10 次 = 满; 顺带记录 lastWaterHour, 与浇水壶语义一致)。
    -- 指令顺序无关紧要: seed 不重置 waterLvl, water 也允许在 plow 状态执行。
    for i = 1, #sown do
        sendCmd('water', { x = sown[i].x, y = sown[i].y, z = sown[i].z, uses = 10 });
    end
    if n > 0 then
        self:flash(tr("UI_FarmingPanel_Sown", { count = n }));
    else
        self:flash(tr("UI_FarmingPanel_NeedPlow"));
    end
end

--*********************************************************
--* 动作: 批量操作 (半径内全部作物)
--*********************************************************
-- 范围语义 = N×N 格 (默认 3 = 3×3); 返回边长 N
function EtherFarmingPanel:readRadius()
    if self.radiusEntry == nil then return 3; end
    local n = tonumber(self.radiusEntry:getInternalText());
    if n == nil then return 3; end
    if n < 3 then n = 3; end
    if n > 101 then n = 101; end
    if n % 2 == 0 then n = n + 1; end   -- 偶数输入 → 下一个奇数 (中心对称 N×N)
    return math.floor(n);
end

--*********************************************************
--* 状态行 / 提示行 (基类 renderContent 钩子, 裁剪区内绘制;
--* drawHintText 自带滚动偏移)。flash 消息优先显示 3 秒。
--*********************************************************
function EtherFarmingPanel:flash(text)
    self.flashMsg = text;
    self.flashUntil = getTimeInMillis() + 3000;
end

function EtherFarmingPanel:update()
    ISPanel.update(self);
    if self.statusX == nil then return; end
    local now = getTimeInMillis();
    if self.nextScan == nil or now >= self.nextScan then
        self.nextScan = now + 500;
        self.targetPlants = collectTargetPlants(math.floor(self:readRadius() / 2));
        -- 作物/残骸拆分: 作物按钮只对 state=="seeded" 的作物真正生效,
        -- 残骸(plow/destroyed/harvested)格被计数却不会被生长类按钮影响
        local crops, bare = 0, 0;
        for i = 1, #self.targetPlants do
            local st = self.targetPlants[i].state;
            local p = self.targetPlants[i];
            local key = p.x .. "," .. p.y .. "," .. p.z;
            if st == "plow" and self.sownTiles ~= nil and self.sownTiles[key] ~= nil then
                crops = crops + 1;          -- 刚播种未同步: 服务端实为作物
            elseif st == "plow" or st == "destroyed" or st == "harvested" then
                bare = bare + 1;
            else
                crops = crops + 1;
                if self.sownTiles ~= nil then self.sownTiles[key] = nil; end  -- 已同步, 记录作废
            end
        end
        self.crops, self.bare = crops, bare;
    end
end

function EtherFarmingPanel:renderContent()
    if self.statusX == nil then return; end
    local msg, col;
    if self.flashUntil ~= nil and getTimeInMillis() < self.flashUntil then
        msg, col = self.flashMsg, EtherTheme.sky;
    elseif self.crops ~= nil and self.crops > 0 then
        msg = tr("UI_FarmingPanel_PlantCount", { count = self.crops });
        if self.bare > 0 then
            msg = msg .. tr("UI_FarmingPanel_BareSuffix", { count = self.bare });
        end
        col = EtherTheme.textDim;
    elseif self.bare ~= nil and self.bare > 0 then
        msg, col = tr("UI_FarmingPanel_OnlyBare", { count = self.bare }), EtherTheme.textDim;
    else
        msg, col = tr("UI_FarmingPanel_NoPlant"), EtherTheme.textDim;
    end
    -- 结果缓存: render 每帧调用, 不能每帧测量折行
    if msg ~= self.statusCacheText or self.statusCacheW ~= self.statusW then
        self.statusLines = EtherTheme.wrapHint(msg, self.statusW);
        self.statusCacheText = msg;
        self.statusCacheW = self.statusW;
    end
    for i = 1, #self.statusLines do
        EtherTheme.drawHintText(self, self.statusLines[i], self.statusX,
            self.statusY + (i - 1) * EtherTheme.fontHgtHint, col, 0.9);
    end
    for i = 1, #self.hintLines do
        local h = self.hintLines[i];
        EtherTheme.drawHintText(self, h.text, h.x, h.y, EtherTheme.textDim, 0.9);
    end
end

--*********************************************************
--* 种子列表行绘制 (ISScrollingListBox 以冒号回调, self=列表框)
--*********************************************************
function EtherFarmingPanel:drawSeedItem(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight;
    end
    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight);
    self:drawText(item.item.name, 6, y + 4,
        EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 0.9, UIFont.Small);
    return y + self.itemheight;
end

--*********************************************************
--* 构建表单内容 (基类 createChildren 回调): 三个功能模块。
--* 说明行不设独立模块, 按 2026-08-25 用户要求拆进对应功能模块:
--*   目标作物 -> 生长延迟说明; 播种 -> 免种子免农具说明。
--*********************************************************
function EtherFarmingPanel:build()
    if getPlayer() == nil then return; end
    if CFarmingSystem == nil or farming_vegetableconf == nil then return; end

    self.targetPlants = {};
    self.sownTiles = {};    -- 播种格记录: key=x,y,z → 种子类型 (未同步识别用)
    self.hintLines = {};
    local fhH = EtherTheme.fontHgtHint;

    -- 静态提示行登记 (renderContent 绘制), 返回占用高度
    local function addHint(bx, by, bw, key)
        local lines = EtherTheme.wrapHint(tr(key), bw);
        for i = 1, #lines do
            table.insert(self.hintLines,
                { x = bx, y = by + (i - 1) * fhH, text = lines[i] });
        end
        return #lines * fhH;
    end

    local rowW = self:_rowContentW();
    local innerW = rowW - EtherFormPanel.BOX_PAD_X * 2;

    -- ================= 模块一: 作物管理 (原 目标作物 + 批量操作 合并) =================
    -- 2026-08-25 三轮(用户需求): 两模块合一; 四轮: 去健康/虫害±按钮,
    -- 保留「全部治病」「感染病害+25」, 范围语义改 N×N 默认 3。
    local tDefs = {
        { key = "UI_FarmingPanel_Grow",        on = function() self:actGrow(); end },
        { key = "UI_FarmingPanel_FullGrow",    on = function() self:actFullGrow(); end },
        { key = "UI_FarmingPanel_WaterMax",    on = function() self:actWaterMax(); end },
        { key = "UI_FarmingPanel_WaterZero",   on = function() self:actWaterZero(); end },
        { key = "UI_FarmingPanel_BatchCure",   on = function() self:actCure(); end },
        { key = "UI_FarmingPanel_Harvest",     on = function() self:actSimple('harvest'); end },
        { key = "UI_FarmingPanel_Destroy",     on = function() self:actSimple('destroy'); end },
        { key = "UI_FarmingPanel_Remove",      on = function() self:actSimple('removePlant'); end },
        { key = "UI_FarmingPanel_Infect",      on = function() self:actInfect(); end },
    };
    for _, d in ipairs(tDefs) do d.title = tr(d.key); end
    local tRows = flowRows(tDefs, innerW);
    local statusH = fhH + 6;
    local tHintH = #EtherTheme.wrapHint(tr("UI_FarmingPanel_Hint_Grow"), innerW) * fhH;

    self:addModule("UI_FarmingPanel_Group_Target",
        statusH + EtherTheme.entryH + 6 + flowHeight(tRows) + 8 + tHintH + 4,
        function(bx, by, bw)
            local ix = bx + EtherFormPanel.BOX_PAD_X;
            local iW = bw - EtherFormPanel.BOX_PAD_X * 2;
            self.statusX = ix;             -- 状态行位置 (renderContent 动态绘制)
            self.statusY = by + 2;
            self.statusW = iW;
            -- 半径行: 默认 3 = 自身周围 3×3 格
            local label = EtherTheme.makeLabel(ix, by + statusH, EtherTheme.entryH, tr("UI_FarmingPanel_Radius"));
            self:addChild(label);
            self:_track(label, { onlyInGame = true });
            local ex = ix + getTextManager():MeasureStringX(UIFont.Small, tr("UI_FarmingPanel_Radius"))
                + EtherTheme.ctrlGap;
            local entry = ISTextEntryBox:new("3", ex, by + statusH,
                math.min(EtherFormPanel.ENTRY_W, ix + iW - ex), EtherTheme.entryH);
            EtherTheme.styleEntry(entry);
            self.radiusEntry = entry;
            self:addWidget(entry, { onlyInGame = true });
            entry:setClearButton(false);
            placeButtonFlow(self, tRows, ix, by + statusH + EtherTheme.entryH + 6);
            addHint(ix, by + statusH + EtherTheme.entryH + 6 + flowHeight(tRows) + 8,
                iW, "UI_FarmingPanel_Hint_Grow");
        end);


    -- ================= 模块三: 播种 =================
    local seeds = {};
    for typeOfSeed, _ in pairs(farming_vegetableconf.props) do
        -- 显示名与原版一致 (getText 缺翻译时返回原键串, 回退内部键名)
        local label = getText("Farming_" .. typeOfSeed);
        if label == nil or label == ("Farming_" .. typeOfSeed) then label = typeOfSeed; end
        table.insert(seeds, { key = typeOfSeed, name = label });
    end
    table.sort(seeds, function(a, b) return not string.sort(a.name, b.name); end);

    local sDefs = {
        { key = "UI_FarmingPanel_Plow", on = function() self:actPlow(); end },
        { key = "UI_FarmingPanel_Sow",  on = function() self:actSow(); end },
    };
    for _, d in ipairs(sDefs) do d.title = tr(d.key); end
    local sRows = flowRows(sDefs, innerW);
    local sHintH = #EtherTheme.wrapHint(tr("UI_FarmingPanel_Hint_Sow"), innerW) * fhH;
    -- 列表高度触达面板底部 (2026-08-25 用户要求): 用游标当前位置反推剩余空间,
    -- 下限 190 (矮窗口回落滚动)。chrome = 模块标题 + 搜索行 + 按钮行 + 提示 + 盒边距
    local titleH = EtherTheme.fontHgtSmall + 12;
    local chrome = titleH + EtherTheme.entryH + 6 + 8 + flowHeight(sRows) + 8
        + sHintH + 4 + EtherFormPanel.BOX_PAD_Y * 2 + 6;
    local LIST_H = math.max(190, self.height - self.cursorY - chrome
        - EtherFormPanel.BOTTOM_PAD);
    -- 搜索行: 名称 / ID 两个独立输入框 (2026-08-25 用户要求拆分), 纯文本包含匹配
    local nameT = getTranslate("UI_ItemCreator_Title_FilterByName");
    local idT = getTranslate("UI_ItemCreator_Title_FilterById");
    local searchRowH = EtherTheme.entryH;

    self:addModule("UI_FarmingPanel_Group_Sow",
        searchRowH + 6 + LIST_H + 8 + flowHeight(sRows) + 8 + sHintH + 4,
        function(bx, by, bw)
            local ix = bx + EtherFormPanel.BOX_PAD_X;
            local iW = bw - EtherFormPanel.BOX_PAD_X * 2;
            local list = ISScrollingListBox:new(ix, by + searchRowH + 6, iW, LIST_H);
            list:initialise();
            list:instantiate();
            list.itemheight = EtherTheme.listItemH;
            list.selected = 0;
            list.joypadParent = self;
            list.font = UIFont.NewSmall;
            list.doDrawItem = self.drawSeedItem;
            EtherTheme.styleList(list);
            list.drawBorder = false;       -- 模块盒已提供边框
            self.seedList = list;
            self:addWidget(list);
            -- 双搜索框 (列表上方同排): 名称条件 AND ID 条件, 空串 = 该条件通过
            local nameW = getTextManager():MeasureStringX(UIFont.Small, nameT);
            local idW = getTextManager():MeasureStringX(UIFont.Small, idT);
            local entW = math.floor((iW - nameW - idW - EtherTheme.ctrlGap * 3) / 2);
            if entW < 60 then entW = 60; end
            local function makeSearch(x, label, labelW)
                local lb = EtherTheme.makeLabel(x, by, EtherTheme.entryH, label);
                self:addChild(lb);
                self:_track(lb, { onlyInGame = true });
                local entry = ISTextEntryBox:new("", x + labelW + EtherTheme.ctrlGap, by,
                    entW, EtherTheme.entryH);
                EtherTheme.styleEntry(entry);
                self:addWidget(entry, { onlyInGame = true });
                entry:setClearButton(true);
                return entry;
            end
            local nameEntry = makeSearch(ix, nameT, nameW);
            local idEntry = makeSearch(ix + nameW + EtherTheme.ctrlGap + entW + EtherTheme.ctrlGap,
                idT, idW);
            local function applySeedFilter()
                local nameTxt = string.lower(nameEntry:getInternalText() or "");
                local idTxt = string.lower(idEntry:getInternalText() or "");
                list:clear();
                list.selected = 0;
                for i = 1, #seeds do
                    local okName = nameTxt == ""
                        or string.find(string.lower(seeds[i].name), nameTxt, 1, true) ~= nil;
                    local okId = idTxt == ""
                        or string.find(string.lower(seeds[i].key), idTxt, 1, true) ~= nil;
                    if okName and okId then
                        list:addItem(seeds[i].name, seeds[i]);
                    end
                end
            end
            nameEntry.onTextChange = function() applySeedFilter(); end
            idEntry.onTextChange = function() applySeedFilter(); end
            applySeedFilter();             -- 初始填充 (此前漏掉导致列表空白)
            placeButtonFlow(self, sRows, ix, by + searchRowH + 6 + LIST_H + 8);
            addHint(ix, by + searchRowH + 6 + LIST_H + 8 + flowHeight(sRows) + 8,
                iW, "UI_FarmingPanel_Hint_Sow");
        end);
end

--*********************************************************
--* render: 非游戏内提示沿用各页惯例 (居中 workInGameText)
--*********************************************************
function EtherFarmingPanel:render()
    EtherFormPanel.render(self);
    if getPlayer() == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2,
            1.0, 1.0, 1.0, 1.0, UIFont.Large);
    end
end

--*********************************************************
--* :new / :createChildren / :prerender / :onMouseWheel 继承自 EtherFormPanel,
--* 仅补 workInGameText。注意必须以 .new(self, ...) 透传派生类:
--* 写成 EtherFormPanel:new(...) 会把实例元表设成基类,
--* EtherFarmingPanel 的全部方法(build/render/act*)对实例不可见 → 空白面板。
--*********************************************************
function EtherFarmingPanel:new(posX, posY, width, height)
    local o = EtherFormPanel.new(self, posX, posY, width, height);
    o.workInGameText = getTranslate("UI_FarmingPanel_WorkInGame");
    return o;
end
