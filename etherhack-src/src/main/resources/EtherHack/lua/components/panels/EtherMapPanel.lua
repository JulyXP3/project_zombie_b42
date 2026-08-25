require "ISUI/ISPanel"

--*********************************************************
--* 地图面板 (已迁移到 EtherFormPanel: 游标布局 + 统一 add* API)
--*
--* 迁移前的问题 (UI重构方案.md P1/P2/P3/P7):
--*   - 自实现 addLabel/addButton/addCheckBox/addButtonWithLabel, 签名与其他面板不一;
--*   - 位置手算 self.map.y + self.map.height + 20 + rows * 40, 且按钮行步长 50、
--*     复选框行步长 40 混用同一个 self.rows;
--*   - uiElements / rows / mapCheckboxes 挂在类表上 (跨实例共享)。
--* 迁移后: 地图作为自定义行 (基类无内建地图行), 其下的按钮/复选框由游标接管;
--*   5 个复选框降为数据表 + 循环; 状态改为实例级。
--*
--* 红线 (零功能影响): toggleMapDrawLocalPlayer / isMapDrawLocalPlayer 等开关契约,
--*   setMapDrawItems / EtherItemSearch.setEnabled / UIMap.* / UIMovableMiniMap.openPanel
--*   的名称与参数一律未改。
--*********************************************************
EtherMapPanel = EtherFormPanel:derive("EtherMapPanel");

--*********************************************************
--* 地图绘制开关: 数据表 + 循环 (取代原先 5 个近乎相同的块)
--*   key 翻译键 / on 开关函数 / get 取值函数 / flag UIMap 上的字段名
--* flag 用于每帧把复选框状态同步回 UIMap (小地图/地图页可能各自改动它)
--*********************************************************
local function drawToggles()
    return {
        {
            key = "UI_Map_DrawLocalPlayer", flag = "drawLocalPlayer",
            get = isMapDrawLocalPlayer,
            on = function(isChecked)
                toggleMapDrawLocalPlayer(isChecked)
                UIMap.drawLocalPlayer = isChecked
            end,
        },
        {
            key = "UI_Map_DrawOtherPlayers", flag = "drawAllPlayers",
            get = isMapDrawAllPlayers,
            on = function(isChecked)
                toggleMapDrawAllPlayers(isChecked)
                UIMap.drawAllPlayers = isChecked
            end,
        },
        {
            key = "UI_Map_DrawVehicles", flag = "drawVehicles",
            get = isMapDrawVehicles,
            on = function(isChecked)
                toggleMapDrawVehicles(isChecked)
                UIMap.drawVehicles = isChecked
            end,
        },
        {
            key = "UI_Map_DrawZombies", flag = "drawZombies",
            get = isMapDrawZombies,
            on = function(isChecked)
                toggleMapDrawZombies(isChecked)
                UIMap.drawZombies = isChecked
            end,
        },
        {
            key = "UI_Map_DrawItems", flag = "drawItems",
            get = function() return UIMap.drawItems; end,
            on = function(isChecked)
                UIMap.drawItems = isChecked;
                EtherItemSearch.setEnabled(isChecked);
                setMapDrawItems(isChecked);
            end,
        },
    };
end

--*********************************************************
--* 面板内容构建 (基类在 createChildren 里回调 build)
--*********************************************************
function EtherMapPanel:build()
    self.mapCheckboxes = {};
    if getPlayer() == nil then return end

    UIMap.ensureDrawFlags();

    -- 地图: 自定义行, 高度吃掉面板上半部分, 下方留给按钮 + 开关行
    local mapH = self.height - 300;
    if mapH < 200 then mapH = 200; end
    self:addCustomRow(mapH, function(bx, by, bw)
        self.map = UIMap:new(bx, by, bw, mapH)
        self.map:initialise()
        self.map:instantiate()
        self.map:initDataAndStyle()
        self.map.mapAPI:resetView()
        self.map:restoreSettings()
        self:_anchor(self.map)
        self:addChild(self.map)
    end);

    self:addLabeledButton("UI_Map_MiniMapOpenLabel",
        getTranslate("UI_Map_MiniMapOpenButton"), function()
            UIMovableMiniMap.openPanel()
        end);

    -- 点亮全图: 把地图全部未知区域标记为已知+已探索 (2026-08-25 用户需求)。
    -- 本地 WorldMapVisited 立即生效 (地图 UI 每帧 WorldMapVisited.update 刷新纹理);
    -- 多人再走 vanilla 的 map.setKnownInSquares 服务器指令 (ClientCommands.lua:1192,
    -- 零校验) 让服务端按玩家记录, 重进不回退 (服务端只存 known 档, 本地会话内为
    -- visited 全亮档)。边界取 mapAPI 的方块坐标 (getMinXInSquares 等), 无单位歧义。
    self:addLabeledButton("UI_Map_RevealAllLabel",
        getTranslate("UI_Map_RevealAllButton"), function()
            local m = self.map;
            if m == nil or m.mapAPI == nil then return; end
            local api = m.mapAPI;
            local visited = WorldMapVisited.getInstance();
            if visited == nil then return; end
            local x1, y1 = api:getMinXInSquares(), api:getMinYInSquares();
            local x2, y2 = api:getMaxXInSquares(), api:getMaxYInSquares();
            visited:setKnownInSquares(x1, y1, x2, y2);
            visited:setVisitedInSquares(x1, y1, x2, y2);
            if isMultiplayer() then
                sendClientCommand(getPlayer(), "map", "setKnownInSquares",
                    { x1 = x1, y1 = y1, x2 = x2, y2 = y2 });
            end
        end);

    local list = drawToggles();
    self:addCheckboxGroup(list);        -- 等宽行盒 (基类按最宽标签定列数)
    for i = 1, #list do
        local cb = list[i].widget;
        if cb ~= nil then
            cb.mapFlag = list[i].flag;
            table.insert(self.mapCheckboxes, cb);
        end
    end
end

--*********************************************************
--* render: 在基类绘制之外, 把复选框状态同步回 UIMap 的开关
--* (小地图等其它入口也会改这些标记, 故每帧对齐), 并在非游戏内给出提示。
--*********************************************************
function EtherMapPanel:render()
    if self.mapCheckboxes ~= nil then
        for _, cb in ipairs(self.mapCheckboxes) do
            cb:setCheked(UIMap[cb.mapFlag] == true)
        end
    end

    EtherFormPanel.render(self);

    if getPlayer() == nil then
        if self.workInGameText == nil then
            self.workInGameText = getTranslate("UI_Map_PanelWorkOnlyInGame");
        end
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2,
            1.0, 1.0, 1.0, 1.0, UIFont.Large)
    end
end

--*********************************************************
--* 滚轮: 悬停在地图上时缩放地图, 否则滚动面板 (与迁移前一致)
--*********************************************************
function EtherMapPanel:onMouseWheel(del)
    if self.map ~= nil then
        local mx, my = self:getMouseX(), self:getMouseY();
        local mapY = self.map.y + self:getYScroll();
        if mx > self.map.x and mx < self.map.x + self.map.width
            and my > mapY and my < mapY + self.map.height then
            self.map:onMouseWheel(del);
            return true;
        end
    end
    return EtherFormPanel.onMouseWheel(self, del);
end

--*********************************************************
--* :new / :createChildren / :prerender
--* 全部继承自 EtherFormPanel, 无需重写。
--*********************************************************
