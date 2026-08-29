require "ISUI/ISPanel"

--*********************************************************
--* 物品雷达选项卡 (2026-08-30 用户需求): 全物品库列表 (名称/ID 搜索,
--* 耕种-播种同款列表展示: 无列头带, 行内 图标+名称) + 「在地图上显示」勾选。
--* 勾选 = EtherItemSearch 总开关唯一入口: 小地图标记 + 世界标记 (ESP「物品信息」
--* 模块) + 小地图「物品」快捷按钮 三处状态双向实时同步。
--* 追踪目标: 选中项 > 过滤列表 (选中优先, 与物品页旧语义对齐)。
--* 列表: 名称/ID 与排序全部用预取小写串, 零逐键 Java 跨界 (全量填充, 可浏览)。
--*********************************************************
EtherRadarPanel = ISPanel:derive("EtherRadarPanel");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

function EtherRadarPanel:_group(gx, gy, gw, gh)
    table.insert(self.groups, { x = gx, y = gy, w = gw, h = gh });
end

function EtherRadarPanel:_text(tx, ty, text, col, font, hint)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        col = col or EtherTheme.text, font = font or UIFont.Small,
        hint = hint or false,   -- hint=true: 说明文字, 走 EtherTheme.drawHintText 缩放绘制
    });
end

--*********************************************************
--* prerender: 分组盒 + 静态文案 (createChildren 登记, 此处统一绘制)
--*********************************************************
function EtherRadarPanel:prerender()
    self:setStencilRect(0, 2, self:getWidth(), self:getHeight() - 4);
    ISPanel.prerender(self);

    if self.localPlayer == nil then return end
    if self.groups == nil then return end

    for i = 1, #self.groups do
        local g = self.groups[i];
        EtherTheme.drawTileBox(self, g.x, g.y, g.w, g.h, false, 8);
    end
    for i = 1, #self.texts do
        local t = self.texts[i];
        if t.hint then
            EtherTheme.drawHintText(self, t.text, t.x, t.y, t.col);
        else
            self:drawText(t.text, t.x, t.y, t.col.r, t.col.g, t.col.b, t.col.a or 1, t.font);
        end
    end
end

--*********************************************************
--* render: 状态行 (动态) + 勾选框状态同步 (小地图按钮/ESP 模块 → 本页勾选)
--*********************************************************
function EtherRadarPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end

    -- 三处 UI 共用同一状态: 其它两处 (小地图按钮/ESP 模块) 切换后本页勾选框逐帧跟随
    if self.showCb ~= nil then
        self.showCb:setCheked(isMapDrawWorld());
    end
    if self.statusText ~= nil and self.statusX ~= nil then
        local td = EtherTheme.textDim;
        self:drawText(self.statusText, self.statusX, self.statusY, td.r, td.g, td.b, 1, UIFont.Small);
    end
end

--*********************************************************
--* 初始化列表: 全物品库 (名称/ID/全名 预取小写, 过滤与排序全程纯 Lua 零逐键跨界)
--*********************************************************
function EtherRadarPanel:initList()
    local items = getAllItems();
    local list = {};
    for i = 0, items:size() - 1 do
        local item = items:get(i);
        if not item:getObsolete() and not item:isHidden() then
            local name = item:getDisplayName();
            table.insert(list, {
                item = item,
                name = name,
                lname = string.lower(name),
                lfid = string.lower(item:getFullName() or ""),
                -- 注意: ScriptItem 没有 getType() (那是 InventoryItem 的), 短名用 getName()
                lid = string.lower(item:getName() or ""),
            });
        end
    end
    table.sort(list, function(a, b) return not string.sort(a.lname, b.lname); end);

    self.fullList = list;
    self.totalResult = 0;
    self.datas:clear();
    for i, v in ipairs(list) do
        self.datas:addItem(v.name, v.item);
        self.totalResult = self.totalResult + 1;
    end
    self:applyFilter();
end

--*********************************************************
--* 过滤: 名称/ID 双条件 AND, 纯子串 (plain, 物品名含括号冒号不失配), 零 Java 跨界
--*********************************************************
function EtherRadarPanel:applyFilter()
    local nameTxt = string.lower(self.searchName:getInternalText() or "");
    local idTxt = string.lower(self.searchId:getInternalText() or "");
    self.datas:clear();
    self.totalResult = 0;
    for i, v in ipairs(self.fullList) do
        local okName = nameTxt == "" or string.find(v.lname, nameTxt, 1, true);
        local okId = idTxt == "" or string.find(v.lfid, idTxt, 1, true) or string.find(v.lid, idTxt, 1, true);
        if okName and okId then
            self.datas:addItem(v.name, v.item);
            self.totalResult = self.totalResult + 1;
        end
    end
end

--*********************************************************
--* 列表行绘制: 图标 + 名称 (耕种-播种同款无列头列表; 名称取条目预存 text 零跨界)
--*********************************************************
function EtherRadarPanel:drawDatas(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)

    local iconSize = fontHeightSmall;
    self:drawText(item.text, 25, y + 4, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 0.9, self.font);

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
--* 「在地图上显示」勾选: 总开关唯一入口 ——
--*   开 = 构建追踪目标 (选中项 > 过滤列表) + 扫描 + 三处 UI 同步生效;
--*   关 = 清标记, 小地图「物品」按钮变灰, ESP「物品信息」模块同步取消。
--* 无可追踪目标时不设开关: render 的逐帧 setCheked 会把勾选框弹回未勾。
--*********************************************************
function EtherRadarPanel:onShowMapToggled(checked)
    if not checked then
        EtherItemSearch.setEnabled(false);
        self.statusText = nil;
        return
    end

    local targetTypes = {};
    local nTargets = 0;
    local sel = self.datas.selected;
    if self.datas.items ~= nil and sel >= 1 and sel <= #self.datas.items and self.datas.items[sel].item ~= nil then
        targetTypes[self.datas.items[sel].item:getFullName()] = true;
        nTargets = 1;
    elseif self.datas.items ~= nil then
        for i = 1, #self.datas.items do
            local scriptItem = self.datas.items[i].item;
            if scriptItem ~= nil then
                targetTypes[scriptItem:getFullName()] = true;
                nTargets = nTargets + 1;
            end
        end
    end
    if nTargets == 0 then
        self.statusText = tr("UI_RadarPanel_SelectFirst");
        return
    end

    EtherItemSearch.setEnabled(true);   -- 先开总开关 (小地图/世界标记/两处 UI 同步)
    local nHits = EtherItemSearch.scan(targetTypes);
    if nHits == nil or nHits == 0 then
        self.statusText = tr("UI_ItemSearch_NoResults");
    else
        self.statusText = tr("UI_RadarPanel_StatusHits", { count = nHits });
    end
end

--*********************************************************
--* Создание дочерних элементов
--* 排版: 顶部搜索组 (名称/ID 两行) + 中部列表 + 底部组 (勾选 + 说明 + 状态)。
--* 先算底部块, 列表吃中间剩余高度 (与陷阱页同一趟式, 单趟算完不回推)。
--*********************************************************
function EtherRadarPanel:createChildren()
    ISPanel.createChildren(self);

    self.groups = {};
    self.texts = {};
    self.statusText = nil;
    if self.localPlayer == nil then return end;

    local tm = getTextManager();
    local W, H = self.width, self.height;
    local PAD = 16;
    local IP = 10;
    local GAP = EtherTheme.ctrlGap;
    local ctrlH = EtherTheme.ctrlH;
    local fhS = EtherTheme.fontHgtSmall;
    local boxW = W - PAD * 2;
    local innerX = PAD + IP;
    local innerW = boxW - IP * 2;

    -- ================= 顶部: 搜索 (名称/ID 各一行, 标签左 输入框右) =================
    local sy = PAD;
    local nameT = getTranslate("UI_RadarPanel_SearchName");
    local idT = getTranslate("UI_RadarPanel_SearchId");
    local lblW = math.max(tm:MeasureStringX(UIFont.Small, nameT), tm:MeasureStringX(UIFont.Small, idT));
    local entW = innerW - lblW - GAP;
    local rowH = EtherTheme.entryH + GAP;
    local searchH = rowH * 2 + IP;

    self.searchName = ISTextEntryBox:new("", innerX + lblW + GAP, sy + IP, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.searchName);
    self.searchName:initialise();
    self.searchName:instantiate();
    self.searchName:setClearButton(true)
    self.searchName.onTextChange = function() EtherRadarPanel.applyFilter(self) end
    self:addChild(self.searchName);

    self.searchId = ISTextEntryBox:new("", innerX + lblW + GAP, sy + IP + rowH, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.searchId);
    self.searchId:initialise();
    self.searchId:instantiate();
    self.searchId:setClearButton(true)
    self.searchId.onTextChange = function() EtherRadarPanel.applyFilter(self) end
    self:addChild(self.searchId);

    self:_text(innerX, sy + IP + EtherTheme.entryLabelDY, nameT, EtherTheme.text, UIFont.Small);
    self:_text(innerX, sy + IP + rowH + EtherTheme.entryLabelDY, idT, EtherTheme.text, UIFont.Small);
    self:_group(PAD, sy, boxW, searchH);

    -- ================= 底部: 勾选 + 说明 + 状态 =================
    local cbTitle = getTranslate("UI_ItemSearch_ShowOnMap");
    local cbY = 0; -- 占位, actY 定出后回填
    local hintLines = EtherTheme.wrapHint(getTranslate("UI_RadarPanel_Hint"), innerW);
    local hintH = #hintLines * EtherTheme.fontHgtHint;
    local actH = ctrlH + IP + hintH + IP + fhS + IP;   -- 勾选行 + 说明块 + 状态行 (各带间距)
    local actY = H - PAD - (actH + IP * 2);

    cbY = actY + IP;
    self.showCb = UICheckbox:new(innerX, cbY, cbTitle, isMapDrawWorld(), function(v)
        EtherRadarPanel.onShowMapToggled(self, v);
    end);
    self.showCb:initialise();
    self.showCb:instantiate();
    self.showCb.width = innerW;   -- 命中区域 = 整行
    self:addChild(self.showCb);

    local hintY0 = cbY + ctrlH + IP;
    for i = 1, #hintLines do
        self:_text(innerX, hintY0 + (i - 1) * EtherTheme.fontHgtHint, hintLines[i], EtherTheme.textDim, nil, true);
    end

    self.statusX = innerX;
    self.statusY = hintY0 + hintH + IP;
    self:_group(PAD, actY, boxW, actH + IP * 2);

    -- ================= 中部: 列表 (填满搜索组与底部组之间) =================
    local listY = sy + searchH + GAP;
    local listH = (actY - GAP) - listY;
    if listH < 80 then listH = 80; end

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

    self:initList();
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherRadarPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_RadarPanel_WorkInGame");
    menuTableData.localPlayer = getPlayer();
    menuTableData.groups = {};        -- 分组盒矩形 (createChildren 填充)
    menuTableData.texts = {};         -- 静态文案 (createChildren 填充)
    menuTableData.statusText = nil;
    self.__index = self;

    return menuTableData;
end
