require "ISUI/ISPanel"

--*********************************************************
--* 战利品重掷 + 刷弹药 + 钓竿生成 面板 (clearContainerExplore POC, multiplayer)
--* 服务端 object.clearContainerExplore 无权限/距离校验:
--* 任意客户端可清空容器「已探索」标记 + 房间程序化生成记录,
--* 之后打开容器由服务端按容器类型表重新 roll 战利品
--* (枪柜/军用包等表含武器弹药)。
--* 复用 EtherContainerPOC (UIItemTables.lua 定义, F10 同入口)。
--* 下半: 钓竿生成任意物品 POC (FishingSpawn, 仅自建服务器):
--*   全物品列表 + 名称/ID 搜索 + 单次生成 (风格对齐物品生成)
--*********************************************************
EtherLootRollPanel = ISPanel:derive("EtherLootRollPanel");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* 布局登记 (createChildren 期间算好, prerender 统一绘制)
--*   _group 分组盒矩形 (三个功能分组各自成框, 建立边界感)
--*   _text  静态文案 (标题/标签/提示; 集中登记便于按宽度折行, 防各语言溢出)
--* 本面板不滚动 (无 setScrollChildren), 故可直接按内容坐标绘制。
--*********************************************************
function EtherLootRollPanel:_group(gx, gy, gw, gh)
    table.insert(self.groups, { x = gx, y = gy, w = gw, h = gh });
end

function EtherLootRollPanel:_text(tx, ty, text, col, font, hint)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        col = col or EtherTheme.text,
        font = font or UIFont.Small,
        header = false,
        -- hint=true: 说明文字, prerender 里走 EtherTheme.drawHintText 缩放绘制。
        -- 不能在 createChildren 里即时画: 此时面板还没挂到父节点, getAbsoluteX
        -- 拿到的是错误坐标, 而且 createChildren 只执行一次, 画完即丢 (实机:
        -- g1 提示消失、render 里逐帧画的钓鱼提示却正常, 即此因)。
        hint = hint or false,
    });
end

-- 分区标题: Small 薄荷居中 (标题在功能分组盒内, 与全局标题语言一致)
function EtherLootRollPanel:_header(tx, ty, text, w)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        header = true,
        w = w,
    });
end

--*********************************************************
--* Обработка prerender
--* 分组盒与静态文案画在子控件之下 (PZ 渲染序: prerender -> 子控件 -> render)
--*********************************************************
function EtherLootRollPanel:prerender()
    self:setStencilRect(0, 10, self:getWidth(), self:getHeight() - 20);
    ISPanel.prerender(self);

    if self.localPlayer == nil then return end
    if self.groups == nil then return end

    for i = 1, #self.groups do
        local g = self.groups[i];
        EtherTheme.drawTileBox(self, g.x, g.y, g.w, g.h, false, 8);
    end
    for i = 1, #self.texts do
        local t = self.texts[i];
        if t.header then
            EtherTheme.drawSectionTitle(self, t.x, t.y, t.w, t.text, nil, "lines");
        elseif t.hint then
            EtherTheme.drawHintText(self, t.text, t.x, t.y, t.col);
        else
            self:drawText(t.text, t.x, t.y, t.col.r, t.col.g, t.col.b, t.col.a or 1, t.font);
        end
    end
end

--*********************************************************
--* Обработка render (仅动态状态文字)
--*********************************************************
function EtherLootRollPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end

    -- 钓竿生成状态 (生成中/已生成/失败; 无消息时显示用法提示)
    -- 长提示按可用宽度折行, 且结果缓存 (render 每帧调用, 不能每帧测量)
    local fishStatus = tostring(EtherFishSpawn.message or "")
    if fishStatus == "" then
        fishStatus = getTranslate("UI_FishSpawn_Hint")
    end
    if self.statusX ~= nil then
        if self.statusCacheText ~= fishStatus or self.statusCacheW ~= self.statusW then
            self.statusLines = EtherTheme.wrapHint(fishStatus, self.statusW);
            self.statusCacheText = fishStatus;
            self.statusCacheW = self.statusW;
        end
        local td = EtherTheme.textDim;
        -- 多行时整体上移, 使文字块相对按钮竖直居中 (说明文字按 hintScale 缩小绘制)
        local fhH = EtherTheme.fontHgtHint;
        local n = #self.statusLines;
        local y0 = self.statusY - math.floor((n - 1) * fhH / 2);
        for i = 1, n do
            EtherTheme.drawHintText(self, self.statusLines[i], self.statusX,
                y0 + (i - 1) * fhH, td, 0.9);
        end
    end
end

--*********************************************************
--* 物品列表过滤 (名称 + ID 双条件)
--*********************************************************
function EtherLootRollPanel:applyFishFilter()
    local nameTxt = string.lower(self.filterName:getInternalText() or "");
    local idTxt = string.lower(self.filterId:getInternalText() or "");
    self.datas:clear();
    self.totalResult = 0;
    for i, item in ipairs(self.fullList) do
        -- 纯子串匹配 (与 UIItemTables 修复同款): 物品名含 ( ) : . 等时
        -- Lua 模式匹配会把括号当捕获组, 完整名称失配
        local okName = nameTxt == "" or string.find(string.lower(item:getDisplayName()), nameTxt, 1, true);
        local okId = idTxt == "" or string.find(string.lower(item:getFullName() or ""), idTxt, 1, true);
        if okName and okId then
            self.datas:addItem(i, item);
            self.totalResult = self.totalResult + 1;
        end
    end
end

--*********************************************************
--* 列表行绘制 (名称 + 物品ID, 风格对齐 UIItemTables)
--*********************************************************
function EtherLootRollPanel:drawFishItem(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    local a = 0.9;
    local th = EtherTheme;

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)
    EtherTheme.drawColumnLines(self, y, self.itemheight)

    local iconSize = fontHeightSmall;
    local clipX = self.columns[1].size
    local clipX2 = self.columns[2].size
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)

    self:setStencilRect(clipX, clipY, clipX2 - clipX, clipY2 - clipY)
    self:drawText(item.item:getDisplayName(), 25, y + 4, th.text.r, th.text.g, th.text.b, a, self.font);
    self:clearStencilRect()

    local itemId = item.item:getFullName()
    if itemId == nil then itemId = item.item:getName() or "?" end
    self:drawText(itemId, self.columns[2].size + 10, y + 4, th.textDim.r, th.textDim.g, th.textDim.b, a, self.font);

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
--* 初始化全物品列表
--*********************************************************
function EtherLootRollPanel:initFishList()
    local items = getAllItems();
    local all = {};
    for i = 0, items:size() - 1 do
        local item = items:get(i);
        if item ~= nil and not item:getObsolete() and not item:isHidden() then
            table.insert(all, item);
        end
    end
    table.sort(all, function(a, b) return not string.sort(a:getDisplayName(), b:getDisplayName()); end);

    self.fullList = all;
    self.totalResult = 0;
    self.datas:clear();
    for i, item in ipairs(all) do
        self.datas:addItem(item:getDisplayName(), item);
        self.totalResult = self.totalResult + 1;
    end
end

--*********************************************************
--* Создание дочерних элементов
--* 排版: 三个功能分组 (重置战利品 / 刷弹药 / 钓竿生成) 各自一个切角盒,
--*   盒内统一 标题 -> 控件行 -> 提示 的节奏; 控件高度/内边距统一走
--*   EtherTheme.ctrlH / ctrlPadX / ctrlGap; 静态文案按内宽折行防各语言溢出。
--*********************************************************
function EtherLootRollPanel:createChildren()
    ISPanel.createChildren(self);

    if self.localPlayer == nil then return end

    local tm = getTextManager();
    local W, H = self.width, self.height;
    local PAD  = 16;                     -- 面板外边距
    local IP   = 12;                     -- 分组盒内边距
    local GAP  = EtherTheme.ctrlGap;     -- 控件/行间距
    local GGAP = 12;                     -- 分组之间间距
    local ctrlH = EtherTheme.ctrlH;
    local fhS  = EtherTheme.fontHgtSmall;
    local fhM  = EtherTheme.fontHgtMedium;
    local boxW = W - PAD * 2;
    local innerX = PAD + IP;
    local innerW = boxW - IP * 2;
    local labelDY = math.floor((ctrlH - fhS) / 2);   -- 标签相对控件行的垂直居中偏移

    self.groups = {};
    self.texts = {};

    -- ================= 分组1: 重置战利品 =================
    local g1y = 12;
    local cy = g1y + IP;
    self:_header(innerX, cy, getTranslate("UI_LootRoll_SectionReset"), innerW);
    cy = cy + fhS + GAP;

    -- 半径: 标签 + 输入框 + 重置按钮 (同排, 输入行高 entryH; 按钮在其中竖直居中)
    local radiusText = getTranslate("UI_LootRoll_RadiusLabel");
    self:_text(innerX, cy + EtherTheme.entryLabelDY, radiusText, EtherTheme.text, UIFont.Small);
    local rbX = innerX + tm:MeasureStringX(UIFont.Small, radiusText) + GAP;
    local rbW = 70;
    self.radiusBox = ISTextEntryBox:new(tostring(EtherContainerPOC.radius), rbX, cy, rbW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.radiusBox);
    self.radiusBox:initialise();
    self.radiusBox:instantiate();
    self.radiusBox:setClearButton(false);
    self:addChild(self.radiusBox);

    local resetTitle = getTranslate("UI_LootRoll_Button");
    local resetW = UIButton.measureWidth(resetTitle);
    local maxResetW = (innerX + innerW) - (rbX + rbW + GAP);
    if resetW > maxResetW then resetW = maxResetW; end
    self.resetBtn = UIButton:new(rbX + rbW + GAP, cy + EtherTheme.entryBtnDY, resetW, ctrlH, resetTitle,
    function()
        if not isMultiplayer() then
            print("[ContainerPOC] multiplayer only (use your own dedicated server)")
            return
        end
        local r = tonumber(self.radiusBox:getInternalText());
        if r ~= nil and r >= 1 and r <= 100 then
            EtherContainerPOC.radius = math.floor(r);
        end
        EtherContainerPOC.reset();
    end, resetW);
    self.resetBtn:initialise();
    self.resetBtn:instantiate();
    self.resetBtn.isOnlyInGame = true;
    self:addChild(self.resetBtn);
    cy = cy + EtherTheme.entryH + GAP;

    -- 提示 (按内宽折行, 防俄语等长文案越过右缘; 说明文字按 hintScale 缩小绘制)
    local hintLines = EtherTheme.wrapHint(getTranslate("UI_LootRoll_Hint"), innerW);
    for i = 1, #hintLines do
        self:_text(innerX, cy, hintLines[i], EtherTheme.textDim, nil, true);
        cy = cy + EtherTheme.fontHgtHint;
    end
    self:_group(PAD, g1y, boxW, (cy - g1y) + IP);

    -- ================= 分组2: 刷弹药 =================
    local g2y = cy + IP + GGAP;
    cy = g2y + IP;
    self:_header(innerX, cy, getTranslate("UI_LootRoll_SectionFarm"), innerW);
    cy = cy + fhS + GAP;

    -- 三行竖排 (用户实测反馈: "自动开"文案带退弹提示较长, 与"自动关"并排时
    -- 被钳到半行宽, 文字溢出按钮框 —— 图1):
    --   行1: [自动刷(开)] 独占整行
    --   行2: [自动刷(关)] 独占整行
    --   行3: [设置弹药数] [弹药数输入框 占余宽]
    -- 各按钮按自身文案取宽、钳到整行内宽; 仍放不下的极端语言/字体档
    -- 由 UIButton:render 的缩字兜底保证不越出按钮框。
    -- 必须传 maxWidth 防 UIButton:new 的自动加宽还原钳制。
    local farmTitles = {
        getTranslate("UI_CharacterPanel_AmmoFarmAuto"),
        getTranslate("UI_CharacterPanel_AmmoFarmStop"),
        getTranslate("UI_CharacterPanel_AmmoFarmSet"),
    };
    local farmActions = {
        function()
            local n = tonumber(self.ammoFarmBox:getInternalText());
            if n and n > 0 then setAmmoFarmCount(n) end
            farmSetAmmo();
            EtherAmmoFarm.enabled = true;
        end,
        function()
            EtherAmmoFarm.enabled = false;
        end,
        function()
            local n = tonumber(self.ammoFarmBox:getInternalText());
            if n and n > 0 then setAmmoFarmCount(n) end
            farmSetAmmo();
        end,
    };

    local fw = {};
    for i = 1, 3 do
        fw[i] = math.min(UIButton.measureWidth(farmTitles[i]), innerW);
    end

    for i = 1, 2 do
        local btn = UIButton:new(innerX, cy + EtherTheme.entryBtnDY, fw[i], ctrlH, farmTitles[i], farmActions[i], fw[i]);
        btn:initialise();
        btn:instantiate();
        self:addChild(btn);
        cy = cy + EtherTheme.entryH + GAP;
    end

    local b3 = UIButton:new(innerX, cy + EtherTheme.entryBtnDY, fw[3], ctrlH,
        farmTitles[3], farmActions[3], fw[3]);
    b3:initialise();
    b3:instantiate();
    self:addChild(b3);

    local ax = innerX + fw[3] + GAP;
    self.ammoFarmBox = ISTextEntryBox:new(tostring(getAmmoFarmCount()),
        ax, cy, (innerX + innerW) - ax, EtherTheme.entryH);
    EtherTheme.styleEntry(self.ammoFarmBox);
    self.ammoFarmBox:initialise();
    self.ammoFarmBox:instantiate();
    self.ammoFarmBox:setClearButton(false);
    self:addChild(self.ammoFarmBox);
    cy = cy + EtherTheme.entryH;
    self:_group(PAD, g2y, boxW, (cy - g2y) + IP);

    -- ================= 分组3: 钓竿生成 (占据剩余高度) =================
    local g3y = cy + IP + GGAP;
    -- 矮屏(窗口被钳制)时必须压缩本组而不是兜底撑高: 强制最小高度会把
    -- 盒底推出面板, 组内列表与搜索行全部越界 (实机多轮 "search food 重叠" 根因)。
    local g3h = H - g3y - PAD;
    if g3h < 150 then g3h = 150; end
    self:_group(PAD, g3y, boxW, g3h);

    cy = g3y + IP;
    self:_header(innerX, cy, getTranslate("UI_FishSpawn_Title"), innerW);
    cy = cy + EtherTheme.fontHgtSmall + GAP;

    -- 过滤行: 名称 + ID (宽度不足时自动拆成两行, 避免标签压住输入框)
    local nameT = getTranslate("UI_ItemCreator_Title_FilterByName");
    local idT   = getTranslate("UI_ItemCreator_Title_FilterById");
    local nlW = tm:MeasureStringX(UIFont.Small, nameT);
    local ilW = tm:MeasureStringX(UIFont.Small, idT);
    -- 标签限宽: 过长(俄语)时截到 40%, 保证输入框至少 60px 且不越分组盒
    local maxLabelW = math.floor(innerW * 0.4);
    if nlW > maxLabelW then nlW = maxLabelW; end
    if ilW > maxLabelW then ilW = maxLabelW; end
    local entW = math.floor((innerW - nlW - ilW - GAP * 3) / 2);
    local twoRows = entW < 90;
    if twoRows then
        entW = innerW - nlW - GAP;
        if entW < 60 then entW = 60; end
    end

    self:_text(innerX, cy + EtherTheme.entryLabelDY, nameT, EtherTheme.text, UIFont.Small);
    self.filterName = ISTextEntryBox:new("", innerX + nlW + GAP, cy, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.filterName);
    self.filterName:initialise();
    self.filterName:instantiate();
    self.filterName:setClearButton(true);
    self.filterName.onTextChange = function() EtherLootRollPanel.applyFishFilter(self) end
    self:addChild(self.filterName);

    local idX, idY;
    if twoRows then
        cy = cy + EtherTheme.entryH + GAP;
        idX, idY = innerX, cy;
        entW = math.max(60, innerW - ilW - GAP);
    else
        idX = innerX + nlW + GAP + entW + GAP;
        idY = cy;
    end
    self:_text(idX, idY + EtherTheme.entryLabelDY, idT, EtherTheme.text, UIFont.Small);
    self.filterId = ISTextEntryBox:new("", idX + ilW + GAP, idY, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.filterId);
    self.filterId:initialise();
    self.filterId:instantiate();
    self.filterId:setClearButton(true);
    self.filterId.onTextChange = function() EtherLootRollPanel.applyFishFilter(self) end
    self:addChild(self.filterId);
    cy = cy + EtherTheme.entryH + GAP;

    -- 底部行: 生成按钮 + 状态文字 (状态由 render 动态绘制)
    local bottomY = g3y + g3h - IP - ctrlH;
    local spawnTitle = getTranslate("UI_FishSpawn_Button");
    local spawnW = UIButton.measureWidth(spawnTitle);
    if spawnW > innerW then spawnW = innerW; end

    -- 列表: 填满过滤行与底部行之间。
    -- 注意: ISScrollingListBox 的列头画在列表 y 之上(自身边界外, 见 UIItemTables 建在 y=25),
    -- 故这里为列头预留一段高度, 否则 "Name/Item ID" 会压到上方过滤行。
    local hdrH = EtherTheme.listHeaderH;   -- 与列表 itemheight 同源, 防表头顶穿上方搜索行
    local listY = cy + hdrH;
    -- 列表压缩适配 + 16px 分离带; 空间不足时收缩列表, 绝不顶穿搜索行
    local listH = bottomY - 16 - listY;
    if listH < 30 then listH = 30; end

    self.datas = ISScrollingListBox:new(innerX, listY, innerW, listH);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = EtherTheme.listItemH
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawFishItem;
    EtherTheme.styleList(self.datas);
    self.datas.drawBorder = false;      -- 分组盒已提供边框, 避免直角边框与切角框重叠
    self.datas:addColumn(getTranslate("UI_ItemCreator_Title_ItemName"), 0);
    self.datas:addColumn(idT, math.floor(innerW * 0.6));
    self:addChild(self.datas);

    self:initFishList();

    self.spawnBtn = UIButton:new(innerX, bottomY, spawnW, ctrlH, spawnTitle,
    function()
        if not isMultiplayer() then
            print("[FishSpawn] multiplayer only (use your own dedicated server)")
            return
        end
        local sel = self.datas.selected;
        if self.datas.items == nil or sel < 1 or sel > #self.datas.items then
            print("[FishSpawn] select an item first")
            return
        end
        local item = self.datas.items[sel].item;
        if item == nil then return end
        EtherFishSpawn.trigger(item:getFullName());
    end, spawnW)
    self.spawnBtn:initialise();
    self.spawnBtn:instantiate();
    self.spawnBtn.isOnlyInGame = true;
    self:addChild(self.spawnBtn);

    -- 状态文字区域 (生成按钮右侧): 记录可用宽度, 由 render 按此折行,
    -- 避免长提示 (如 "Multiplayer: hold a fishing rod, ...") 冲出面板右缘。
    self.statusX = innerX + spawnW + GAP * 2;
    self.statusY = bottomY + labelDY;
    self.statusW = (innerX + innerW) - self.statusX;
    if self.statusW < 60 then self.statusW = 60; end
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherLootRollPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_LootRoll_WorkInGame");
    menuTableData.localPlayer = getPlayer();
    menuTableData.fullList = {};
    menuTableData.totalResult = 0;
    menuTableData.groups = {};        -- 分组盒矩形 (createChildren 填充)
    menuTableData.texts = {};         -- 静态文案 (createChildren 填充)
    self.__index = self;

    return menuTableData;
end