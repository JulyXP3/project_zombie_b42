require "ISUI/ISPanel"

--*********************************************************
--* 「交换」面板 (swap)
--* 上列表: 可换取的目标物品 (带 ClothingItem 脚本, 名称/ID 双搜索);
--* 下列表: 背包中可用的祭品 (衣物/背包, 手选);
--* 一件一换 (无数量), 交换按钮 = 选中祭品 → 选中目标。
--*
--* 渲染注意 (实机崩溃教训):
--*   - ScriptItem:getIcon() 返回 string, 但 InventoryItem:getIcon()
--*     返回 Texture 对象 —— "Item_"..icon 对 Texture 拼接直接 __concat
--*     崩, icon 必须 type()=="string" 才可用;
--*   - ISScrollingListBox:addItem(数字, item) 的 text 是 number,
--*     drawText(number) 无实现 —— 一律 addItem(字符串, item)。
--*********************************************************
EtherExchangePanel = ISPanel:derive("EtherExchangePanel");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* 布局登记 (createChildren 期间算好, prerender 统一绘制)
--*********************************************************
function EtherExchangePanel:_group(gx, gy, gw, gh)
    table.insert(self.groups, { x = gx, y = gy, w = gw, h = gh });
end

function EtherExchangePanel:_text(tx, ty, text, col, font, hint)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        col = col or EtherTheme.text, font = font or UIFont.Small,
        -- hint=true: 说明文字, prerender 里走 EtherTheme.drawHintText 缩放绘制
        -- (createChildren 时面板未挂父节点, 坐标错误且画完即丢)。
        hint = hint or false,
    });
end

--*********************************************************
--* prerender: 分组盒与静态文案画在子控件之下
--*********************************************************
function EtherExchangePanel:prerender()
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
--* render: 动态状态行
--*********************************************************
function EtherExchangePanel:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end

    if self.statusX == nil then return end

    local status;
    local td = EtherTheme.textDim;
    if EtherExchange.active ~= nil then
        status = tr("UI_Exchange_Queued", { name = EtherExchange.active.fullType });
    elseif EtherExchange.doneInfo ~= nil then
        status = tr("UI_Exchange_Done", { name = EtherExchange.doneInfo.fullType });
    elseif self.failMsg ~= nil and getTimestampMs() - self.failT < 5000 then
        status = self.failMsg;
        td = EtherTheme.textWarn or EtherTheme.textDim;
    else
        status = "";
    end
    if status ~= "" then
        self:drawText(status, self.statusX, self.statusY, td.r, td.g, td.b, 1, UIFont.Small);
    end
end

--*********************************************************
--* 祭品列表: 手动刷新 (背包快照 → 条目)。
--* 不自动扫描 (性能): 背包变动后由用户点「刷新祭品」按钮重建;
--* 交换完成后旧条目引用失效, 也靠该按钮 (convert 有 containsID
--* 预检, 引用失效只会安静失败不会误换)。
--* wantContainer: 目标是背包类时祭品可含背包 (内容物随迁),
--*   否则仅衣物 (纯容器祭品在 copyPatchesTo 处崩)。
--*********************************************************
function EtherExchangePanel:refreshSacrifices()
    self.sacDatas:clear();
    if self.localPlayer == nil then return end

    local wantContainer = self.wantContainerSacrifice or false;
    local cands = {};
    local items = self.localPlayer:getInventory():getItems();
    for i = 0, items:size() - 1 do
        local it = items:get(i);
        if it ~= nil and it:getClothingItem() ~= nil and not it:isHidden()
            and not it:isFavorite() and not it:isBroken()
            and not self.localPlayer:isEquippedClothing(it) and not self.localPlayer:isHandItem(it) then
            local isClothing = it:IsClothing();
            local isContainer = instanceof(it, "InventoryContainer");
            local hasContents = false;
            if isContainer then
                local sub = it:getInventory();
                hasContents = sub ~= nil and not sub:isEmpty();
            end
            local pass = false;
            if isClothing then
                -- 衣物祭品: 目标非容器时不可带内容物 (Remove 后内容物会丢)
                pass = wantContainer or not hasContents;
            elseif isContainer and wantContainer then
                -- 背包祭品: 仅当目标也是容器 (内容物 takeItemsFrom 随迁)
                pass = true;
            end
            if pass then table.insert(cands, it) end
        end
    end
    table.sort(cands, function(a, b) return a:getActualWeight() < b:getActualWeight() end);
    for i = 1, #cands do
        self.sacDatas:addItem(cands[i]:getDisplayName(), cands[i]);
    end
    self.sacDatas.selected = 0;
end

--*********************************************************
--* 目标列表: 名称/ID 双搜索 (AND, 纯子串, 零逐键 Java 跨界)
--* addItem 一律字符串 text (number 会让 drawText 无实现, 实机崩)
--*********************************************************
function EtherExchangePanel:applyFilter()
    local nameTxt = string.lower(self.searchName:getInternalText() or "");
    local idTxt = string.lower(self.searchId:getInternalText() or "");
    local full = self.fullList;
    self.datas:clear();
    self.totalResult = 0;
    for i, v in ipairs(full) do
        local okName = nameTxt == "" or string.find(v.lname, nameTxt, 1, true);
        local okId = idTxt == "" or string.find(v.lfid, idTxt, 1, true) or string.find(v.lid, idTxt, 1, true);
        if okName and okId then
            self.datas:addItem(v.name, v.item);
            self.totalResult = self.totalResult + 1;
        end
    end
end

--*********************************************************
--* 目标列表: 初始化 (仅带 ClothingItem 脚本的衣物/背包)
--* isHidden 过滤: 胡子/发茬/化妆等是尸体外观碎片 (BodyLocation=
--* zeddmg, hidden=true, 玩家不可见), 只是恰好带 ClothingItem 脚本,
--* 不是玩家可穿戴物品 —— 不滤的话列表会被 200 个 M_Beard_* 淹没。
--*********************************************************
function EtherExchangePanel:initList()
    local items = getAllItems();
    local list = {};
    for i = 0, items:size() - 1 do
        local item = items:get(i);
        if not item:getObsolete() and not item:isHidden() then
            local ci = item:getClothingItem();
            if ci ~= nil and ci ~= "" then
                -- 名称/全名/短名预取小写: 排序/过滤全程纯 Lua, 零逐键 Java 跨界
                local name = item:getDisplayName();
                table.insert(list, {
                    item = item, name = name,
                    lname = string.lower(name),
                    lfid = string.lower(item:getFullName() or ""),
                    lid = string.lower(item:getName() or ""),
                });
            end
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
--* 通用行绘制 (图标 + 名称 [+ 重量]; 目标表 ScriptItem 与祭品表
--* InventoryItem 共用): icon 必须 type()=="string" ——
--* InventoryItem:getIcon() 返回 Texture 对象, 直接拼接会 __concat 崩。
--*********************************************************
local function drawRow(self, y, item, alt, showWeight)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    local a = 0.9;
    local th = EtherTheme;

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)

    local iconSize = fontHeightSmall;
    local label = tostring(item.text or "");
    if showWeight then
        label = label .. "  (" .. string.format("%3.1f", item.item:getActualWeight()) .. ")";
    end
    self:drawText(label, 25, y + 4, th.text.r, th.text.g, th.text.b, a, self.font);

    local icon = item.item:getIcon()
    if type(icon) ~= "string" then icon = nil; end
    if not icon and item.item:getIconsForTexture() and not item.item:getIconsForTexture():isEmpty() then
        icon = item.item:getIconsForTexture():get(0)
        if type(icon) ~= "string" then icon = nil; end
    end
    if icon then
        local texture = getTexture("Item_" .. icon)
        if texture then
            self:drawTextureScaledAspect2(texture, 4, y + (self.itemheight - iconSize) / 2, iconSize, iconSize, 1, 1, 1, 1);
        end
    end

    return y + self.itemheight;
end

function EtherExchangePanel:drawDatas(y, item, alt)
    return drawRow(self, y, item, alt, false)
end

function EtherExchangePanel:drawSac(y, item, alt)
    return drawRow(self, y, item, alt, true)
end

--*********************************************************
--* 交换按钮回调: 选中祭品 × 选中目标, 一件一换
--*********************************************************
function EtherExchangePanel:onExchange()
    if self.sacDatas.items == nil or self.sacDatas.selected == nil
        or self.sacDatas.selected < 1 or self.sacDatas.selected > #self.sacDatas.items then
        self.failMsg = tr("UI_Exchange_NoSacrifice", { name = "" });
        self.failT = getTimestampMs();
        return
    end
    if self.datas.items == nil or self.datas.selected == nil
        or self.datas.selected < 1 or self.datas.selected > #self.datas.items then
        return
    end
    local sacrifice = self.sacDatas.items[self.sacDatas.selected].item;
    local scriptItem = self.datas.items[self.datas.selected].item;
    if sacrifice == nil or scriptItem == nil then return end
    local ok, info = EtherExchange.convert(scriptItem:getFullName(), sacrifice);
    if not ok then
        local key = (info == "not clothing") and "UI_Exchange_NotClothing" or "UI_Exchange_Fail";
        self.failMsg = tr(key, { name = scriptItem:getDisplayName() });
        self.failT = getTimestampMs();
        print("[Exchange] convert failed: " .. tostring(info));
    end
end

--*********************************************************
--* 目标选中变化 → 记录目标类型 (仅影响下次「刷新祭品」的过滤模式;
--* 不自动重建祭品表, 节省性能, 由用户手动刷新)
--*********************************************************
function EtherExchangePanel:onTargetSelected()
    if self.datas.items == nil or self.datas.selected == nil
        or self.datas.selected < 1 or self.datas.selected > #self.datas.items then
        return
    end
    local scriptItem = self.datas.items[self.datas.selected].item;
    if scriptItem == nil then return end
    local wantContainer = false;
    local ok, probe = pcall(instanceItem, scriptItem:getFullName());
    if ok and probe ~= nil then
        wantContainer = instanceof(probe, "InventoryContainer");
    end
    self.wantContainerSacrifice = wantContainer;
end

--*********************************************************
--* 创建子元素
--* 排版 (上=目标 下=祭品):
--*   搜索行 (名称/ID 同行, 无分组盒 → 无分割线)
--*   目标列表组 (标题+列表, 有分割线)
--*   [交换 ↑ 按钮]
--*   祭品列表组 (标题+列表, 有分割线)
--*   底部: 提示 + 状态行
--*********************************************************
function EtherExchangePanel:createChildren()
    ISPanel.createChildren(self);

    self.groups = {};
    self.texts = {};
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

    -- ================= 底部: 提示 + 状态 =================
    -- 先算底部块, 中部双列表吃剩余高度
    local hintLines = EtherTheme.wrapHint(getTranslate("UI_Exchange_Hint"), innerW);
    local hintH = #hintLines * EtherTheme.fontHgtHint;
    local actH = hintH + fhS + GAP;
    local actY = H - PAD - (actH + IP * 2);

    local hintY0 = actY + IP;
    for i = 1, #hintLines do
        self:_text(innerX, hintY0 + (i - 1) * EtherTheme.fontHgtHint, hintLines[i],
            EtherTheme.textDim, nil, true);
    end

    -- 状态行 (动态, 由 render 绘制)
    self.statusX = innerX;
    self.statusY = hintY0 + hintH + IP;
    self:_group(PAD, actY, boxW, actH + IP * 2);

    -- ================= 搜索行: 名称/ID 同一行 (有分组盒, 鱼竿页同款) =================
    -- 宽度不足时自动拆两行; 标签限宽 40% 防长翻译压住输入框
    local sy = PAD;
    local nameT = getTranslate("UI_Exchange_SearchName");
    local idT = getTranslate("UI_Exchange_SearchId");
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

    self:_text(innerX, sy + IP + EtherTheme.entryLabelDY, nameT, EtherTheme.text, UIFont.Small);
    self.searchName = ISTextEntryBox:new("", innerX + nlW + GAP, sy + IP, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.searchName);
    self.searchName:initialise();
    self.searchName:instantiate();
    self.searchName:setClearButton(true);
    self.searchName.onTextChange = function() EtherExchangePanel.applyFilter(self) end
    self:addChild(self.searchName);

    local idX, idY;
    if twoRows then
        idX = innerX;
        idY = sy + IP + EtherTheme.entryH + GAP;
        entW = math.max(60, innerW - ilW - GAP);
    else
        idX = innerX + nlW + GAP + entW + GAP;
        idY = sy + IP;
    end
    self:_text(idX, idY + EtherTheme.entryLabelDY, idT, EtherTheme.text, UIFont.Small);
    self.searchId = ISTextEntryBox:new("", idX + ilW + GAP, idY, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.searchId);
    self.searchId:initialise();
    self.searchId:instantiate();
    self.searchId:setClearButton(true);
    self.searchId.onTextChange = function() EtherExchangePanel.applyFilter(self) end
    self:addChild(self.searchId);

    local searchH = EtherTheme.entryH + IP * 2;
    if twoRows then searchH = EtherTheme.entryH * 2 + GAP + IP * 2; end
    self:_group(PAD, sy, boxW, searchH);
    local searchEndY = sy + searchH;

    -- ================= 中部: 上目标列表 + 交换按钮 + 下祭品列表 =================
    local titleH = fhS + 2;
    local midBtnW = 130;
    local midH = ctrlH + GAP * 2;

    local topY = searchEndY + GAP;
    local bottomBoundary = actY - GAP;
    local totalListArea = bottomBoundary - topY - midH;
    -- 上列表 (目标, 全库) 占大头; 下列表 (祭品) 至少 3 行
    local minSacH = EtherTheme.listItemH * 3 + titleH;
    local sacH = math.floor(totalListArea * 0.33);
    if sacH < minSacH then sacH = math.min(minSacH, totalListArea); end
    local tgtH = totalListArea - sacH;
    local minTgtH = EtherTheme.listItemH * 3 + titleH;
    if tgtH < minTgtH then
        tgtH = minTgtH;
        sacH = math.max(EtherTheme.listItemH * 2 + titleH, totalListArea - tgtH);
    end

    -- 上列表: 目标物品 (标题 + 列表, 分组盒)
    local tgtTitle = getTranslate("UI_Exchange_TargetList");
    self:_text(innerX, topY + 2, tgtTitle, EtherTheme.text, UIFont.Small);
    self.datas = ISScrollingListBox:new(PAD, topY + titleH, boxW, tgtH - titleH);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = EtherTheme.listItemH
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawDatas;
    EtherTheme.styleList(self.datas);
    self.datas.drawBorder = false;
    -- 选中变化 → 祭品表按目标类型重建 (背包目标放行背包祭品)
    self.datas.onSelected = function()
        self:onTargetSelected();
    end
    self:addChild(self.datas);
    self:_group(PAD, topY, boxW, tgtH);

    -- 中缝: 交换按钮 + 刷新祭品按钮 (交换右侧)
    -- 祭品表为手动快照 (不自动扫描背包, 省开销), 背包变动后点刷新重建
    local midBtnY = topY + tgtH + GAP;
    local refreshTitle = getTranslate("UI_Exchange_Refresh");
    local rfW = UIButton.measureWidth(refreshTitle);
    local rfMax = innerW - midBtnW - GAP;
    if rfW > rfMax then rfW = rfMax; end
    local midBtnX = innerX + math.floor((innerW - (midBtnW + GAP + rfW)) / 2);
    local midBtn = UIButton:new(midBtnX, midBtnY, midBtnW, ctrlH,
        getTranslate("UI_Exchange_Button") .. " ↑",
    function()
        self:onExchange()
    end, midBtnW)
    midBtn:initialise();
    midBtn:instantiate();
    midBtn.isOnlyInGame = true;
    self:addChild(midBtn);

    self.refreshBtn = UIButton:new(midBtnX + midBtnW + GAP, midBtnY, rfW, ctrlH,
        refreshTitle,
    function()
        self:refreshSacrifices();
    end, rfW)
    self.refreshBtn:initialise();
    self.refreshBtn:instantiate();
    self.refreshBtn.isOnlyInGame = true;
    self:addChild(self.refreshBtn);

    -- 下列表: 背包祭品 (标题 + 列表, 分组盒)
    local sacY = midBtnY + ctrlH + GAP;
    local sacTitle = getTranslate("UI_Exchange_SacrificeList");
    self:_text(innerX, sacY + 2, sacTitle, EtherTheme.text, UIFont.Small);
    self.sacDatas = ISScrollingListBox:new(PAD, sacY + titleH, boxW, sacH - titleH);
    self.sacDatas:initialise();
    self.sacDatas:instantiate();
    self.sacDatas.itemheight = EtherTheme.listItemH
    self.sacDatas.selected = 0;
    self.sacDatas.joypadParent = self;
    self.sacDatas.font = UIFont.NewSmall;
    self.sacDatas.doDrawItem = self.drawSac;
    EtherTheme.styleList(self.sacDatas);
    self.sacDatas.drawBorder = false;
    self:addChild(self.sacDatas);
    self:_group(PAD, sacY, boxW, sacH);

    self:refreshSacrifices();
    self:initList();
end

--*********************************************************
--* 新实例
--*********************************************************
function EtherExchangePanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
    menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_Exchange_WorkInGame");
    menuTableData.localPlayer = getPlayer();
    menuTableData.groups = {};        -- 分组盒矩形 (createChildren 填充)
    menuTableData.texts = {};         -- 静态文案 (createChildren 填充)
    menuTableData.failMsg = nil;      -- 最近一次失败提示
    menuTableData.failT = 0;
    menuTableData.wantContainerSacrifice = false;  -- 目标类型 → 祭品表过滤模式
    self.__index = self;

    return menuTableData;
end
