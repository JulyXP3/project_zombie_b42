require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
UIItemTables = ISPanel:derive("UIItemTables");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* Обработка prerender
--* 底部控制块的切角框画在子控件之下 (渲染序: prerender -> 子控件 -> render),
--* 与其它面板的"功能成框"风格一致。
--*********************************************************
function UIItemTables:prerender()
    ISPanel.prerender(self);
    if self.ctrlBox ~= nil then
        local b = self.ctrlBox;
        -- 实底 + 切角描边: 控制块成为独立"模块", 与上方列表明确分层不贴边
        self:drawRect(b.x, b.y, b.w, b.h, 0.45, 0.03, 0.05, 0.055);
        EtherTheme.drawTileBox(self, b.x, b.y, b.w, b.h, false, 8);
    end
end

--*********************************************************
--* Обработка render
--*********************************************************
function UIItemTables:render()
    ISPanel.render(self);

    -- 结果计数 (位置随底部控制块, 不再贴死 x=0)
    if self.totalTextX ~= nil then
        local td = EtherTheme.textDim;
        -- 标签与数值的拼接方式交给 UI_Fmt_LabelCount 模板
        self:drawText(tr("UI_Fmt_LabelCount", {
                label = getText("IGUI_DbViewer_TotalResult"),
                value = tostring(self.totalResult),
            }),
            self.totalTextX, self.totalTextY, td.r, td.g, td.b, 1, UIFont.Small)
    end

    -- 过滤行标签 (静态文案集中绘制, 避免 ISLabel 的 Medium 字号压住输入框)
    if self.filterLabels ~= nil then
        local tc = EtherTheme.text;
        for i = 1, #self.filterLabels do
            local l = self.filterLabels[i];
            self:drawText(l.text, l.x, l.y, tc.r, tc.g, tc.b, 1, UIFont.Small);
        end
    end
end

--*********************************************************
--* Создание дочерних элементов
--*********************************************************
EtherContainerPOC = EtherContainerPOC or { radius = 10 }
-- F9 快捷键与「战利品重掷」选项卡共用此入口

-- 重置附近容器战利品: 服务端 clearContainerExplore 无校验地
-- 清空 explored 标记 + 房间程序化生成记录 -> 再次搜索重新 roll 战利品
-- 半径可调: 选项卡输入框 / ` 键 Lua 控制台(需 -debug 启动) EtherContainerPOC.radius = 2
function EtherContainerPOC.reset()
    local player = getPlayer();
    if player == nil then return end
    local R = EtherContainerPOC.radius or 10;
    local ok, err = pcall(function()
        local px, py, pz = math.floor(player:getX()), math.floor(player:getY()), math.floor(player:getZ());
        local count = 0;
        for dy = -R, R do
            for dx = -R, R do
                local sq = getCell():getGridSquare(px + dx, py + dy, pz);
                if sq ~= nil then
                    local objects = sq:getObjects();
                    for i = 0, objects:size() - 1 do
                        local obj = objects:get(i);
                        if obj ~= nil then
                            local nCont = obj:getContainerCount();
                            for ci = 0, nCont - 1 do
                                local c = obj:getContainerByIndex(ci);
                                if c ~= nil and c:getSourceGrid() ~= nil and c:getSourceGrid():getRoom() ~= nil then
                                    c:setExplored(false); -- 客户端本地标记也要清, 否则打开时跳过向服务端请求, 只显示旧缓存
                                    sendClientCommand(player, "object", "clearContainerExplore", {
                                        x = sq:getX(), y = sq:getY(), z = sq:getZ(),
                                        index = i, containerIndex = ci,
                                    });
                                    -- 即时刷新 (容器战利品重掷链-分析.md §四 方式B, 已按反编译验证):
                                    -- requestServerItemsForContainer 就是发 RequestItemsForContainer 包,
                                    -- 与同事 C++ 直发同款 —— 服务端清完标记后见容器未探索即重新 roll 并推送,
                                    -- 无需手动重开箱子。注意: 每容器发 2 包, 大半径密集区注意反作弊阈值(~100包)。
                                    c:requestServerItemsForContainer();
                                    count = count + 1;
                                end
                            end
                        end
                    end
                end
            end
        end
        print("[ContainerPOC] reset+refresh " .. tostring(count) .. " containers nearby, loot re-rolled instantly");
    end)
    if not ok then
        print("[ContainerPOC] failed: " .. tostring(err));
    end
end

function UIItemTables:createChildren()
    ISPanel.createChildren(self);

    -- 统一排版参数 (与其它面板同一套): 外边距 / 盒内边距 / 控件高 / 间距
    local tm = getTextManager();
    local PAD = 12;
    local IP = 10;
    local GAP = EtherTheme.ctrlGap;
    local ctrlH = EtherTheme.ctrlH;
    local fhS = EtherTheme.fontHgtSmall;
    local boxW = self.width - PAD * 2;
    local innerX = PAD + IP;
    local innerW = boxW - IP * 2;

    -- ================= 底部控制块 (结果计数行 + 按钮行 + 过滤行) =================
    -- 必须单趟算完: 先把所有会影响块高的分支(地图按钮是否换行 / 过滤行是否拆两行)
    -- 全部测量出来, 再推导 blockY、各行 Y 和列表高度。
    --
    -- 反例(曾经的写法, 导致过滤行掉到面板背景外面):
    --   先按最小高度定下 blockY 和 rowTotalY/rowBtnY/rowFilterY 以及列表高度,
    --   之后在分支里 "blockH = blockH + ctrlH + GAP; blockY = H - PAD - blockH"。
    --   盒子顶边往上长了, 但里面各行仍在旧坐标 —— 内容整体溢出盒底 ctrlH + GAP。
    --   同一个分支还只重置了 bx 而没有推进按钮行 Y, 地图按钮直接叠在 Give x1 下面。

    -- 1) 四个"给物品"按钮等宽(取最宽文案), 且四个按钮 + 三个间隔必须装进 innerW
    local specs = {
        { key = "UI_ItemCreator_Button_AddItemX1", n = 1 },
        { key = "UI_ItemCreator_Button_AddItemX2", n = 2 },
        { key = "UI_ItemCreator_Button_AddItemX5", n = 5 },
        { key = "UI_ItemCreator_Button_AddItemX10", n = 10 },
    };
    local giveTitles = {};
    for i = 1, #specs do
        table.insert(giveTitles, getTranslate(specs[i].key));
    end
    local giveW = UIButton.measureGroupWidth(giveTitles);
    local maxGiveW = math.floor((innerW - GAP * (#specs - 1)) / #specs);
    if giveW > maxGiveW then giveW = maxGiveW; end
    local giveRowW = giveW * #specs + GAP * (#specs - 1);

    -- 2) 过滤区排布。三种形态, 必须在算块高之前定下来:
    --      同排     : 名称 + ID 挤在一行
    --      各占一行 : 每个过滤器一行, 标签在左输入框在右
    --      标签上置 : 标签太宽(长翻译)时标签独占一行, 输入框在下一行占满宽度
    --    关键: 任何情况下都不能用"最小宽度"去硬撑输入框 —— 标签已经把起点推到右边,
    --    再给个 60 的下限就会直接冲出盒子右缘 (test_overflow.py 会抓到)。
    local nameT = getTranslate("UI_ItemCreator_Title_FilterByName");
    local idT = getTranslate("UI_ItemCreator_Title_FilterById");
    local nlW = tm:MeasureStringX(UIFont.Small, nameT);
    local ilW = tm:MeasureStringX(UIFont.Small, idT);

    local pairEntW = math.floor((innerW - nlW - ilW - GAP * 3) / 2);
    local sideBySide = pairEntW >= 90;
    local nameStacked, idStacked = false, false;
    local nameEntW, idEntW = pairEntW, pairEntW;
    local filterCtrlRows = 1;      -- 过滤区占用的输入框行数
    local filterLabelRows = 0;     -- 额外的纯标签行数
    if not sideBySide then
        filterCtrlRows = 2;
        nameEntW = innerW - nlW - GAP;
        if nameEntW < 90 then
            nameStacked = true; nameEntW = innerW;
            filterLabelRows = filterLabelRows + 1;
        end
        idEntW = innerW - ilW - GAP;
        if idEntW < 90 then
            idStacked = true; idEntW = innerW;
            filterLabelRows = filterLabelRows + 1;
        end
    end

    -- 3) 分支全部确定后才能定块高与块顶
    -- 过滤输入框行按 entryH 留位 (UITextBox2 最小渲染高, 见 EtherTheme;
    -- 按 ctrlH 留位时游戏会把框撑高 10px, 实机表现即"输入框冲出控制块")
    local blockH = IP + fhS + GAP + ctrlH;                   -- 计数行 + 给物品按钮行
    for _ = 1, filterCtrlRows do
        blockH = blockH + GAP + EtherTheme.entryH;           -- 过滤输入框行
    end
    blockH = blockH + filterLabelRows * (fhS + GAP);         -- 上置的标签行
    blockH = blockH + IP;
    local blockY = self.height - PAD - blockH;
    self.ctrlBox = { x = PAD, y = blockY, w = boxW, h = blockH };

    local rowTotalY  = blockY + IP;
    local rowBtnY    = rowTotalY + fhS + GAP;
    local rowFilterY = rowBtnY + ctrlH + GAP;
    self.totalTextX = innerX;
    self.totalTextY = rowTotalY;

    -- ================= 列表 (填满顶部到控制块之间) =================
    -- 耕种-播种同款列表: 无列头带 (不 addColumn), 行内 图标+名称;
    -- 底部与控制块保持 44px 分离带, 矮窗口压缩列表绝不反撑
    local listY = PAD + 4;
    -- 与控制块 44px 分离带 (实机仍显贴边, 加大); 矮窗口压缩列表绝不反撑
    local listH = blockY - 44 - listY;
    if listH < 40 then listH = 40; end

    self.datas = ISScrollingListBox:new(PAD, listY, boxW, listH);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = EtherTheme.listItemH
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    EtherTheme.styleList(self.datas);
    self:addChild(self.datas);

    -- ================= 按钮行 (给物品 x1/x2/x5/x10 + 地图标记) =================
    local bx = innerX;
    local function spawnSelected(count)
        local sel = self.datas.selected;
        if self.datas.items == nil or sel < 1 or sel > #self.datas.items then return end
        local item = self.datas.items[sel].item;
        if item == nil then return end
        spawnItem(item:getFullName(), count);
    end

    for i = 1, #specs do
        local n = specs[i].n;
        local btn = UIButton:new(bx, rowBtnY, giveW, ctrlH, getTranslate(specs[i].key),
            function() spawnSelected(n); end, giveW);
        btn:initialise();
        btn:instantiate();
        btn:setAnchorLeft(true);
        btn:setAnchorRight(false);
        btn:setAnchorTop(false);
        btn:setAnchorBottom(true);
        btn.isOnlyInGame = true;
        self:addChild(btn);
        table.insert(self.buttonList, btn);
        bx = bx + giveW + GAP;
    end

    -- ================= 过滤区 =================
    -- 形态(同排 / 各占一行 / 标签上置)与宽度已在块高计算处定好, 这里只摆放。
    -- cy 逐行推进, 与块高的累加顺序严格一致, 保证最后一行正好落在 IP 内边距之上。
    self.filterLabels = {};
    local cy = rowFilterY;
    local eDY = EtherTheme.entryLabelDY;   -- 输入行标签在 entryH 行内居中

    local nameLabelY, nameEntX, nameEntY;
    if nameStacked then
        nameLabelY = cy;
        nameEntX, nameEntY = innerX, cy + fhS + GAP;
        cy = nameEntY + EtherTheme.entryH + GAP;
    else
        nameEntX, nameEntY = innerX + nlW + GAP, cy;
        nameLabelY = cy + eDY;
        cy = cy + EtherTheme.entryH + GAP;
    end
    table.insert(self.filterLabels, { x = innerX, y = nameLabelY, text = nameT });

    self.filterByName = ISTextEntryBox:new("", nameEntX, nameEntY, nameEntW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.filterByName);
    self.filterByName:initialise();
    self.filterByName:instantiate();
    self.filterByName.target = self;
    self.filterByName.itemsListFilter = self.filterName;
    self.filterByName.onTextChange = UIItemTables.onFilterChange;
    self.filterByName:setClearButton(true)
    self:addChild(self.filterByName);
    table.insert(self.filterWidgets, self.filterByName);

    local idLabelX, idLabelY, idEntX, idEntY;
    if sideBySide then
        -- 与名称同排: 接在名称输入框右侧
        idLabelX = nameEntX + nameEntW + GAP;
        idLabelY = nameEntY + eDY;
        idEntX, idEntY = idLabelX + ilW + GAP, nameEntY;
    elseif idStacked then
        idLabelX, idLabelY = innerX, cy;
        idEntX, idEntY = innerX, cy + fhS + GAP;
    else
        idLabelX, idLabelY = innerX, cy + eDY;
        idEntX, idEntY = innerX + ilW + GAP, cy;
    end
    table.insert(self.filterLabels, { x = idLabelX, y = idLabelY, text = idT });

    self.filterById = ISTextEntryBox:new("", idEntX, idEntY, idEntW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.filterById);
    self.filterById:initialise();
    self.filterById:instantiate();
    self.filterById:setClearButton(true)
    self.filterById.target = self;
    self.filterById.itemsListFilter = self.filterType;
    self.filterById.onTextChange = UIItemTables.onFilterChange;
    self:addChild(self.filterById);
    table.insert(self.filterWidgets, self.filterById);

    self:updatePanel();
end

--*********************************************************
--* Обновление панели
--*********************************************************
function UIItemTables:updatePanel()
    for i=1, #self.buttonList do
        local item = self.buttonList[i];
        if item.isOnlyInGame and getPlayer() == nil or getPlayer():isDead() then
            item:setEnable(false);
        end
    end
end


--*********************************************************
--* Инициализация списков
--*********************************************************
function UIItemTables:initList(module)
    self.totalResult = 0;
    for _, v in ipairs(module) do
        self.datas:addItem(v:getDisplayName(), v);
        self.totalResult = self.totalResult + 1;
    end
    -- 排序必须用 addItem 已存的 text (纯 Lua 字符串): 原比较器每次比较调两次
    -- getDisplayName(), N log N 次 Java 跨界 (全物品库数千条 ≈ 数万次) 是
    -- 物品页点开卡顿的主因; 原 displayCategoryNames/Map 收集后无人使用, 一并删除
    table.sort(self.datas.items, function(a,b) return not string.sort(a.text, b.text); end);
end

--*********************************************************
--* Обновление таблицы
--*********************************************************
function UIItemTables:update()
    self.datas.doDrawItem = self.drawDatas;
end

--*********************************************************
--* Фильтр по названию
--*********************************************************
function UIItemTables:filterName(widget, scriptItem)
    local txtToCheck = string.lower(scriptItem:getDisplayName())
    local filterTxt = string.lower(widget:getInternalText())
    -- 纯子串匹配 (plain): 物品名普遍含 ( ) : . 等字符, 走 Lua 模式会把括号当捕获组,
    -- 完整名称过滤匹配不到自身 -> 只能用短词过滤 -> 追踪按钮连带追踪同系列其它物品
    -- (原版 ISItemsListTable 同款缺陷, 此处有意偏离: 物品搜索要的是字面量不是模式)
    return string.find(txtToCheck, filterTxt, 1, true) ~= nil
end

--*********************************************************
--* Фильтр по ID
--*********************************************************
function UIItemTables:filterType(widget, scriptItem)
    local txtToCheck = string.lower(scriptItem:getName())
    local filterTxt = string.lower(widget:getInternalText())
    return string.find(txtToCheck, filterTxt, 1, true) ~= nil
end

--*********************************************************
--* Применение фильтра при написании текста
--*********************************************************
function UIItemTables.onFilterChange(widget)
    local datas = widget.parent.datas;
    if not datas.fullList then datas.fullList = datas.items; end
    widget.parent.totalResult = 0;
    datas:clear();
    for i,v in ipairs(datas.fullList) do -- check every items
        local add = true;
        for j,widget in ipairs(widget.parent.filterWidgets) do -- check every filters
            if not widget.itemsListFilter(self, widget, v.item) then
                add = false
                break
            end
        end
        if add then
            -- 用条目预存的 text (显示名): 旧代码传数字 i, 过滤后条目 text 会变成序号
            datas:addItem(v.text, v.item);
            widget.parent.totalResult = widget.parent.totalResult + 1;
        end
    end
    -- 物品标记状态已迁「雷达」页统一管理, 物品页过滤不再触碰 EtherItemSearch
end

--*********************************************************
--* Отрисовка данных
--*********************************************************
function UIItemTables:drawDatas(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    local a = 0.9;
    local th = EtherTheme;

    -- 耕种-播种同款列表行: 无列头/无分栏, 图标 + 名称 (名称取条目预存 text, 零逐帧跨界)
    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)

    local iconSize = fontHeightSmall;
    self:drawText(item.text, 25, y + 4, th.text.r, th.text.g, th.text.b, a, self.font);

    local icon = item.item:getIcon()
    if item.item:getIconsForTexture() and not item.item:getIconsForTexture():isEmpty() then
        icon = item.item:getIconsForTexture():get(0)
    end
    if icon then
        local texture = getTexture("Item_" .. icon)
        if texture then
            self:drawTextureScaledAspect2(texture, 4, y + (self.itemheight - iconSize) / 2, iconSize, iconSize, 1, 1, 1, 1);
        end
    end

    return y + self.itemheight;
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function UIItemTables:new (x, y, width, height)
    local menuTableData = ISPanel:new(x, y, width, height);
    setmetatable(menuTableData, self);
    menuTableData.listHeaderColor = {r=0.12, g=0.05, b=0.05, a=1.0};
    menuTableData.borderColor = EtherTheme.bloodDim;
    menuTableData.backgroundColor = {r=0, g=0, b=0, a=0};
    menuTableData.buttonBorderColor = {r=0.7, g=0.7, b=0.7, a=0.0};
    menuTableData.totalResult = 0;
    menuTableData.filterWidgets = {};
    menuTableData.buttonList = {};
    UIItemTables.instance = menuTableData;
    return menuTableData;
end