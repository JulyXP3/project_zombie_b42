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

    -- 两组独立开关: 其它入口 (小地图按钮/ESP 页勾选) 切换后本页勾选框逐帧跟随
    if self.showCb ~= nil then
        self.showCb:setCheked(UIMap.drawItems and true or false);
    end
    if self.espCb ~= nil then
        self.espCb:setCheked(isMapDrawItemEsp());
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
                full = item:getFullName() or "",
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
--* 列表行绘制: 勾选框 + 图标 + 名称 (样式与战利品多选列表一致)
--* 勾选态按物品全名存在 EtherWatchlist, 过滤重建列表不丢
--*********************************************************
local CHECK_TEX = {};
local CHECK_W = 16;
local CHECK_HIT_W = 22;
local NAME_X = 44;

local function checkTexture(on)
    local key = on and "on" or "off";
    if CHECK_TEX[key] == nil then
        CHECK_TEX[key] = resolveTexture(on
            and "modcore/media/ui/checkbox_checked.png"
            or "modcore/media/ui/checkbox_unchecked.png") or false;
    end
    return CHECK_TEX[key];
end

function EtherRadarPanel:drawDatas(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)

    local fullName = item.item and item.item:getFullName() or nil
    local tex = checkTexture(fullName ~= nil and self.checked ~= nil and self.checked[fullName] ~= nil)
    if tex then
        self:drawTextureScaled(tex, 3, y + (self.itemheight - CHECK_W) / 2, CHECK_W, CHECK_W, 1, 1, 1, 1)
    end

    local iconSize = fontHeightSmall;
    self:drawText(item.text, NAME_X, y + 4, EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 0.9, self.font);

    -- 图标 (EtherTheme 缓存; 勾选框占位后右移)
    local texture = EtherTheme.itemTexture(item.item)
    if texture then
        self:drawTextureScaledAspect2(texture, 23, y + (self.itemheight - iconSize) / 2,
            iconSize, iconSize, 1, 1, 1, 1)
    end

    return y + self.itemheight;
end

--*********************************************************
--* 两个勾选 (小地图标记 / ESP 画线追踪) 共用的目标构建 + 扫描:
--* 选中项 > 过滤列表; 无可追踪目标时状态行提示并返回 false
--* (render 的逐帧 setCheked 会把勾选框弹回未勾, 自愈)。
--* 扫描为时间片异步: 立即返回 true, 状态行先显示「搜索中…」,
--* 完成回调更新命中数/无结果 (期间无冻结, 详见 EtherItemSearch)。
--*********************************************************
function EtherRadarPanel:ensureScan()
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
        return false;
    end

    self.statusText = tr("UI_ItemSearch_Searching");
    local panel = self;
    EtherItemSearch.startScan(targetTypes, false, function(n, stats)
        if n == 0 then
            panel.statusText = tr("UI_ItemSearch_NoResults");
        else
            panel.statusText = tr("UI_RadarPanel_StatusHits", { count = n });
        end
    end);
    return true;
end

-- 小地图标记: 与小地图「物品」快捷按钮同一 flag (drawItems, Java 持久化)
function EtherRadarPanel:onShowMapToggled(checked)
    if not checked then
        EtherItemSearch.setMinimapEnabled(false);
        self.statusText = nil;
        return
    end
    if self:ensureScan() then
        EtherItemSearch.setMinimapEnabled(true);
    end
end

-- ESP 画线追踪: 与 ESP 页「物品雷达」勾选同一 flag (drawItemEsp, 会话级)
function EtherRadarPanel:onEspToggled(checked)
    if not checked then
        EtherItemSearch.setItemEspEnabled(false);
        self.statusText = nil;
        return
    end
    if self:ensureScan() then
        EtherItemSearch.setItemEspEnabled(true);
    end
end

--*********************************************************
--* 追踪清单 (2026-11-13): 行勾选 = 暂存; 「加入清单」写入持久清单;
--*   「清单」弹出列表逐项移除; 追踪只针对清单内容。
--*   只存物品全名, 不统计数量。
--*********************************************************
function EtherRadarPanel:refreshWatchLabel()
    if self.watchCb ~= nil then
        local text = tr("UI_RadarPanel_WatchToggle", { n = EtherWatchlist.count() })
        self.watchCb.title = text
        if self.watchCb.titleLines ~= nil then self.watchCb.titleLines = { text } end
    end
    if self.listBtn ~= nil then
        self.listBtn.title = tr("UI_RadarPanel_OpenList", { n = EtherWatchlist.count() })
    end
end

function EtherRadarPanel:persistWatchlist()
    if type(setWatchlist) == "function" then
        setWatchlist(EtherWatchlist.encode())
    end
end

-- 载入并剔除当前物品库中不存在的旧条目 (mod 卸载/改名后)
function EtherRadarPanel:loadWatchlist()
    local raw = type(getWatchlist) == "function" and getWatchlist() or ""
    EtherWatchlist.load(raw)
    if self.fullList == nil then return end
    local known = {}
    for i = 1, #self.fullList do known[self.fullList[i].full] = true end
    local changed = false
    local names = {}
    for i = 1, EtherWatchlist.count() do names[i] = EtherWatchlist.at(i) end
    EtherWatchlist.load("")
    for i = 1, #names do
        if known[names[i]] then
            EtherWatchlist.toggle(names[i])
        else
            changed = true
        end
    end
    if changed then self:persistWatchlist() end
    self:refreshWatchLabel()
end

function EtherRadarPanel:startWatchTracking()
    local targets = EtherWatchlist.targets()
    if EtherWatchlist.count() == 0 then return false end
    self.statusText = tr("UI_RadarPanel_WatchTracking")
    EtherItemSearch.startScan(targets, false, function(n)
        if self.watchCb == nil or not self.watchCb.isChecked then return end
        if n == 0 then
            self.statusText = tr("UI_ItemSearch_NoResults")
        else
            self.statusText = tr("UI_RadarPanel_StatusHits", { count = n })
        end
    end)
    EtherItemSearch.setMinimapEnabled(true)
    EtherItemSearch.setItemEspEnabled(true)
    return true
end

function EtherRadarPanel:stopWatchTracking(restore)
    EtherItemSearch.setMinimapEnabled(self.watchPrevMinimap and true or false)
    EtherItemSearch.setItemEspEnabled(self.watchPrevEsp and true or false)
    if restore then self.statusText = nil end
end

-- 清单内容变化 (加入/弹窗移除/清空): 保存 + 刷新按钮 + 追踪跟随
function EtherRadarPanel:onWatchlistChanged()
    self:persistWatchlist()
    self:refreshWatchLabel()
    if self.watchCb == nil or not self.watchCb.isChecked then return end
    if EtherWatchlist.count() == 0 then
        self.watchCb.isChecked = false
        self:stopWatchTracking(true)
        self.statusText = tr("UI_RadarPanel_WatchEmpty")
    else
        self:startWatchTracking()
    end
end

-- 把列表里勾选的行加入清单 (勾选本身不持久化, 与战利品模块一致)
function EtherRadarPanel:addCheckedToWatchlist()
    local checked = self.datas ~= nil and self.datas.checked or nil
    local selected = 0
    local added = 0
    local rejected = false
    if checked ~= nil then
        for full in pairs(checked) do
            selected = selected + 1
            if not EtherWatchlist.contains(full) then
                local result = EtherWatchlist.toggle(full)
                if result == "added" then
                    added = added + 1
                elseif result == "full" then
                    rejected = true
                end
            end
        end
    end
    if self.datas ~= nil then self.datas.checked = {} end
    if selected == 0 then
        self.statusText = tr("UI_RadarPanel_NothingChecked")
        return
    end
    if added == 0 and not rejected then
        self.statusText = tr("UI_RadarPanel_AlreadyInList")
        return
    end
    if rejected then
        self.statusText = tr("UI_RadarPanel_WatchFull")
    end
    self:onWatchlistChanged()
end

function EtherRadarPanel:openWatchlist()
    EtherWatchlistPopup.open(function()
        self:onWatchlistChanged()
    end)
end

function EtherRadarPanel:onWatchToggled(checked)
    if checked then
        if EtherWatchlist.count() == 0 then
            self.watchCb.isChecked = false
            self.statusText = tr("UI_RadarPanel_WatchEmpty")
            return
        end
        self.watchPrevMinimap = UIMap.drawItems and true or false
        self.watchPrevEsp = UIMap.drawItemEsp and true or false
        self:startWatchTracking()
    else
        self:stopWatchTracking(true)
    end
    self:refreshWatchLabel()
end

-- 列表鼠标: 行首勾选区域切换暂存勾选, 其余交给原版整行选中 (与战利品同款)
function EtherRadarPanel.onListMouseDown(panel, list, x, y)
    if x <= CHECK_HIT_W then
        local row = list:rowAt(x, y);
        if row >= 1 and row <= #list.items then
            local it = list.items[row];
            local full = it.item and it.item:getFullName() or nil;
            if full ~= nil then
                if list.checked[full] ~= nil then
                    list.checked[full] = nil;
                else
                    list.checked[full] = it.text or full;
                    list.selected = row;
                end
                return true;
            end
        end
    end
    ISScrollingListBox.onMouseDown(list, x, y);
    return true;
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

    -- ================= 顶部: 搜索 (名称/ID 同一行, 标签左 输入框右) =================
    -- 宽度不足时自动拆两行 (鱼竿页同款); 标签限宽 40% 防长翻译压住输入框
    local sy = PAD;
    local nameT = getTranslate("UI_RadarPanel_SearchName");
    local idT = getTranslate("UI_RadarPanel_SearchId");
    local nlW = tm:MeasureStringX(UIFont.Small, nameT);
    local ilW = tm:MeasureStringX(UIFont.Small, idT);
    local maxLabelW = math.floor(innerW * 0.4);
    if nlW > maxLabelW then nlW = maxLabelW; end
    if ilW > maxLabelW then ilW = maxLabelW; end
    local entW = math.floor((innerW - nlW - ilW - GAP * 3) / 2);
    local twoRows = entW < 90;
    if twoRows then
        entW = innerW - nlW - GAP;
        if entW < 60 then entW = 60; end
    end
    local rowH = EtherTheme.entryH + GAP;
    -- 盒高: 上下各留 IP 内边距 (与物品页一致, 下方留冗余), 修正"搜索框偏下贴底"
    local searchH = twoRows and (rowH + EtherTheme.entryH + IP * 2) or (EtherTheme.entryH + IP * 2);

    self.searchName = ISTextEntryBox:new("", innerX + nlW + GAP, sy + IP, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.searchName);
    self.searchName:initialise();
    self.searchName:instantiate();
    self.searchName:setClearButton(true)
    self.searchName.onTextChange = function() EtherRadarPanel.applyFilter(self) end
    self:addChild(self.searchName);

    local idX, idY;
    if twoRows then
        idX = innerX;
        idY = sy + IP + rowH;
        entW = math.max(60, innerW - ilW - GAP);
    else
        idX = innerX + nlW + GAP + entW + GAP;
        idY = sy + IP;
    end
    self.searchId = ISTextEntryBox:new("", idX + ilW + GAP, idY, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.searchId);
    self.searchId:initialise();
    self.searchId:instantiate();
    self.searchId:setClearButton(true)
    self.searchId.onTextChange = function() EtherRadarPanel.applyFilter(self) end
    self:addChild(self.searchId);

    self:_text(innerX, sy + IP + EtherTheme.entryLabelDY, nameT, EtherTheme.text, UIFont.Small);
    self:_text(idX, idY + EtherTheme.entryLabelDY, idT, EtherTheme.text, UIFont.Small);
    self:_group(PAD, sy, boxW, searchH);

    -- ================= 底部: 清单按钮 + 三个勾选 + 说明 + 状态 =================
    -- 勾选 = 暂存; 「加入清单」写入持久清单; 「清单」打开弹窗查看/移除
    local cbTitle = getTranslate("UI_ItemSearch_ShowOnMap");
    local espTitle = getTranslate("UI_RadarPanel_EspLines");
    local hintLines = EtherTheme.wrapHint(getTranslate("UI_RadarPanel_Hint"), innerW);
    local hintH = #hintLines * EtherTheme.fontHgtHint;
    local actH = (ctrlH + GAP) * 4 + IP + hintH + IP + fhS + IP;   -- 按钮行 + 勾选×3 + 说明 + 状态
    local actY = H - PAD - (actH + IP * 2);

    local btnY = actY + IP;
    local addW = UIButton.measureWidth(getTranslate("UI_RadarPanel_AddToList"));
    local listW = UIButton.measureWidth(tr("UI_RadarPanel_OpenList", { n = 0 }));
    local btnW = math.min(math.max(addW, listW), math.floor((innerW - GAP) / 2));
    self.addBtn = UIButton:new(innerX, btnY, btnW, ctrlH,
        getTranslate("UI_RadarPanel_AddToList"), function() EtherRadarPanel.addCheckedToWatchlist(self); end, btnW);
    self.listBtn = UIButton:new(innerX + btnW + GAP, btnY, btnW, ctrlH,
        tr("UI_RadarPanel_OpenList", { n = 0 }), function() EtherRadarPanel.openWatchlist(self); end, btnW);
    self.addBtn:initialise();
    self.addBtn:instantiate();
    self.listBtn:initialise();
    self.listBtn:instantiate();
    self:addChild(self.addBtn);
    self:addChild(self.listBtn);

    local cbY = btnY + ctrlH + GAP;
    self.showCb = UICheckbox:new(innerX, cbY, cbTitle, UIMap.drawItems and true or false, function(v)
        EtherRadarPanel.onShowMapToggled(self, v);
    end);
    self.showCb:initialise();
    self.showCb:instantiate();
    self.showCb.width = innerW;   -- 命中区域 = 整行
    self:addChild(self.showCb);

    local espY = cbY + ctrlH + GAP;
    self.espCb = UICheckbox:new(innerX, espY, espTitle, isMapDrawItemEsp(), function(v)
        EtherRadarPanel.onEspToggled(self, v);
    end);
    self.espCb:initialise();
    self.espCb:instantiate();
    self.espCb.width = innerW;
    self:addChild(self.espCb);

    local watchY = espY + ctrlH + GAP;
    self.watchCb = UICheckbox:new(innerX, watchY, tr("UI_RadarPanel_WatchToggle", { n = 0 }), false,
        function(v) EtherRadarPanel.onWatchToggled(self, v); end);
    self.watchCb:initialise();
    self.watchCb:instantiate();
    self.watchCb.width = innerW;
    self:addChild(self.watchCb);

    local hintY0 = watchY + ctrlH + IP;
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

    self.datas = EtherFastList:new(PAD, listY, boxW, listH);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = EtherTheme.listItemH
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    -- 勾选 = 暂存 (按物品全名, 过滤重建列表不丢), 点「加入清单」才写入持久清单
    self.datas.checked = {};
    self.datas.onMouseDown = function(list, x, y)
        return EtherRadarPanel.onListMouseDown(self, list, x, y);
    end;
    EtherTheme.styleList(self.datas);
    self:addChild(self.datas);

    self:initList();
    self:loadWatchlist();
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
