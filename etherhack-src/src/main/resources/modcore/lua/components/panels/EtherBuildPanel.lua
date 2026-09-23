require "ISUI/ISPanel"

--*********************************************************
--* 免费建造选项卡 (BuildSpawn 面板, multiplayer)
--* 一百六十九: 可建表 13 项 (墙/楼梯/家具/箱/堆肥/雨水桶/营火/地板;
--* 构造器+精灵对+fail-open 逐条验证, 新实体/陷阱/特殊分支不进表)。
--* 选行即定条目 (存 self.selEntry); 摆放中换物自动换新蓝图。
--* 流程: 列表选物 -> 进摆放 -> 蓝图跟鼠标 (自动朝向 + R) -> 按 "`"
--* 读蓝图当前格/朝向直发 -> 自动下新蓝图连建, ESC/按钮退出。
--*********************************************************
EtherBuildPanel = ISPanel:derive("EtherBuildPanel");

function EtherBuildPanel:_group(gx, gy, gw, gh)
    table.insert(self.groups, { x = gx, y = gy, w = gw, h = gh });
end

function EtherBuildPanel:_text(tx, ty, text, col, font, hint)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        col = col or EtherTheme.text,
        font = font or UIFont.Small,
        header = false,
        hint = hint or false,
    });
end

function EtherBuildPanel:_header(tx, ty, text, w)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        header = true,
        w = w,
    });
end

function EtherBuildPanel:prerender()
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

function EtherBuildPanel:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end

    -- 摆放开关标题与外部退出 (ESC/右键) 同步 + 列表选中即定精灵
    self:updatePlaceBtn();
    self:syncWallSelection();
    local msg = tostring(EtherBuildSpawn.message or "");
    if msg ~= "" and self.statusX ~= nil then
        EtherTheme.drawHintText(self, msg, self.statusX, self.statusY, EtherTheme.textDim);
    end
end

function EtherBuildPanel:updatePlaceBtn()
    if self.placeBtn == nil then return end
    local on = EtherBuildSpawn.ghost ~= nil;
    local title = getTranslate("UI_Build_Place") .. ": "
        .. getTranslate(on and "UI_TakeSpawn_On" or "UI_TakeSpawn_Off");
    if self.placeBtn.title ~= title then
        self.placeBtn.title = title;
    end
end

--*********************************************************
--* 可建列表: 初始化 + 名称/ID 双条件过滤 + 行绘制 + 选中即定精灵
--* (仿战利品物品生成; ID 指精灵对编号; 摆放中换墙自动换新蓝图)
--*********************************************************
function EtherBuildPanel:initWallList()
    self.wallList = {};
    for _, e in ipairs(EtherBuildSpawn.catalog) do
        table.insert(self.wallList, e);
    end
    self.wallSel = 0;
    self.selEntry = EtherBuildSpawn.catalog[1];
    self:applyWallFilter();
end

function EtherBuildPanel:applyWallFilter()
    if self.datas == nil then return end
    local nameTxt = string.lower(self.filterName:getInternalText() or "");
    local idTxt = string.lower(self.filterId:getInternalText() or "");
    self.datas:clear();
    for i, e in ipairs(self.wallList) do
        local hayName = EtherBuildSpawn.entryName(e) .. " " .. (e.type or "");
        local okName = nameTxt == "" or string.find(string.lower(hayName), nameTxt, 1, true);
        local pair = (e.sprite or "") .. "/" .. (e.northSprite or "");
        local okId = idTxt == "" or string.find(string.lower(pair), idTxt, 1, true);
        if okName and okId then
            self.datas:addItem(i, e);
        end
    end
end

function EtherBuildPanel:drawWallItem(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end
    local a = 0.9;
    local th = EtherTheme;
    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)
    EtherTheme.drawColumnLines(self, y, self.itemheight)
    local clipX = self.columns[1].size
    local clipX2 = self.columns[2].size
    local clipY = math.max(0, y + self:getYScroll())
    local clipY2 = math.min(self.height, y + self:getYScroll() + self.itemheight)
    self:setStencilRect(clipX, clipY, clipX2 - clipX, clipY2 - clipY)
    self:drawText(EtherBuildSpawn.entryName(item.item), 25, y + 4, th.text.r, th.text.g, th.text.b, a, self.font);
    self:clearStencilRect()
    local pair = item.item.sprite .. "/" .. item.item.northSprite;
    self:drawText(pair, self.columns[2].size + 10, y + 4, th.textDim.r, th.textDim.g, th.textDim.b, a, self.font);
    return y + self.itemheight;
end

function EtherBuildPanel:syncWallSelection()
    if self.datas == nil or self.datas.items == nil then return end
    local sel = self.datas.selected;
    if sel == self.wallSel then return end
    self.wallSel = sel;
    if sel < 1 or sel > #self.datas.items then return end
    local e = self.datas.items[sel].item;
    if e == nil then return end
    self.selEntry = e;
    -- 摆放中换物: 直接换新蓝图
    if EtherBuildSpawn.ghost ~= nil then
        EtherBuildSpawn.exitPlacement(true);
        EtherBuildSpawn.enterPlacement(e);
    end
end

function EtherBuildPanel:createChildren()
    ISPanel.createChildren(self);

    if self.localPlayer == nil then return end

    local tm = getTextManager();
    local W, H = self.width, self.height;
    local PAD  = 16;
    local IP   = 12;
    local GAP  = EtherTheme.ctrlGap;
    local GGAP = 12;
    local ctrlH = EtherTheme.ctrlH;
    local fhS  = EtherTheme.fontHgtSmall;
    local boxW = W - PAD * 2;
    local innerX = PAD + IP;
    local innerW = boxW - IP * 2;

    self.groups = {};
    self.texts = {};
    EtherBuildSpawn.panel = self;

    -- ================= 分组1: 可建列表 (占满剩余高度) =================
    local g1y = 12;
    local cy = g1y + IP;
    self:_header(innerX, cy, getTranslate("UI_Build_SectionList"), innerW);
    cy = cy + fhS + GAP;

    -- 搜索行: 名称 + ID 同排 (仿尸体生成; 实在挤不下才拆两行)
    local nameT = getTranslate("UI_ItemCreator_Title_FilterByName");
    local idT   = getTranslate("UI_ItemCreator_Title_FilterById");
    local nlW = tm:MeasureStringX(UIFont.Small, nameT);
    local ilW = tm:MeasureStringX(UIFont.Small, idT);
    local entW = math.floor((innerW - nlW - ilW - GAP * 3) / 2);
    local twoRows = entW < 90;
    local nameEntW, idEntW = entW, entW;
    local idX, idY = innerX + nlW + GAP + entW + GAP, cy;
    if twoRows then
        nameEntW = innerW - nlW - GAP;
        idX, idY = innerX, cy + EtherTheme.entryH + GAP;
        idEntW = innerW - ilW - GAP;
    end
    if nameEntW < 60 then nameEntW = 60; end
    if idEntW < 60 then idEntW = 60; end
    self:_text(innerX, cy + EtherTheme.entryLabelDY, nameT, EtherTheme.text, UIFont.Small);
    self.filterName = ISTextEntryBox:new("", innerX + nlW + GAP, cy,
        nameEntW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.filterName);
    self.filterName:initialise();
    self.filterName:instantiate();
    self.filterName:setClearButton(true);
    self.filterName.onTextChange = function() EtherBuildPanel.applyWallFilter(self) end
    self:addChild(self.filterName);
    self:_text(idX, idY + EtherTheme.entryLabelDY, idT, EtherTheme.text, UIFont.Small);
    self.filterId = ISTextEntryBox:new("", idX + ilW + GAP, idY,
        idEntW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.filterId);
    self.filterId:initialise();
    self.filterId:instantiate();
    self.filterId:setClearButton(true);
    self.filterId.onTextChange = function() EtherBuildPanel.applyWallFilter(self) end
    self:addChild(self.filterId);
    cy = cy + EtherTheme.entryH + GAP;
    if twoRows then
        cy = cy + EtherTheme.entryH + GAP;
    end

    -- 底部状态组先定高 (开关 + 状态 + 提示), 列表吃中间全部
    local hintLines = EtherTheme.wrapHint(getTranslate("UI_Build_Hint"), innerW);
    local warnLines = EtherTheme.wrapHint(getTranslate("UI_Build_TraceWarn"), innerW);
    local placeH = (ctrlH + GAP) * 2;
    local statusH = EtherTheme.fontHgtHint + GAP;
    local notesH = (#hintLines + #warnLines) * EtherTheme.fontHgtHint + GAP * 2;
    local g2h = IP + fhS + GAP + placeH + statusH + notesH + IP;
    local g2y = H - PAD - g2h;
    self:_group(PAD, g2y, boxW, g2h);

    -- 列表: 填满过滤行与状态组之间 (列头预留 + 16px 分离带)
    local hdrH = EtherTheme.listHeaderH;
    local listY = cy + hdrH;
    local listH = g2y - 16 - listY;
    if listH < 30 then listH = 30; end

    self.datas = ISScrollingListBox:new(innerX, listY, innerW, listH);
    self.datas:initialise();
    self.datas:instantiate();
    self.datas.itemheight = EtherTheme.listItemH
    self.datas.selected = 0;
    self.datas.joypadParent = self;
    self.datas.font = UIFont.NewSmall;
    self.datas.doDrawItem = self.drawWallItem;
    EtherTheme.styleList(self.datas);
    self.datas.drawBorder = false;
    self.datas:addColumn(getTranslate("UI_ItemCreator_Title_ItemName"), 0);
    self.datas:addColumn("ID", math.floor(innerW * 0.55));
    self:addChild(self.datas);
    self:initWallList();
    self:_group(PAD, g1y, boxW, (g2y - GGAP - g1y));

    -- ================= 分组2: 状态 (沉底) =================
    cy = g2y + IP;
    self:_header(innerX, cy, getTranslate("UI_Build_SectionGo"), innerW);
    cy = cy + fhS + GAP;

    -- [进入摆放模式]/[退出摆放] 开关 (整行宽)
    local placeT = getTranslate("UI_Build_Place") .. ": "
        .. getTranslate("UI_TakeSpawn_Off");
    local placeW = UIButton.measureWidth(placeT);
    if placeW > innerW then placeW = innerW; end
    self.placeBtn = UIButton:new(innerX, cy + EtherTheme.entryBtnDY, placeW, ctrlH, "",
    function()
        if EtherBuildSpawn.ghost ~= nil then
            EtherBuildSpawn.exitPlacement();
        else
            if not isMultiplayer() then
                print("[BuildSpawn] multiplayer only (use your own dedicated server)")
                EtherBuildSpawn.message = getTranslate("UI_FishSpawn_MultiplayerOnly");
                return
            end
            EtherBuildSpawn.enterPlacement(self.selEntry or EtherBuildSpawn.catalog[1]);
        end
        self:updatePlaceBtn();
    end, placeW);
    self.placeBtn:initialise();
    self.placeBtn:instantiate();
    self.placeBtn.isOnlyInGame = true;
    self:addChild(self.placeBtn);
    self:updatePlaceBtn();
    cy = cy + ctrlH + GAP;

    -- [扫描mod建筑]: 列出已装 mod 的建筑类候选 (Type+构造器, 见控制台)
    local scanT = getTranslate("UI_Build_Scan");
    local scanW = UIButton.measureWidth(scanT);
    if scanW > innerW then scanW = innerW; end
    local scanBtn = UIButton:new(innerX, cy + EtherTheme.entryBtnDY, scanW, ctrlH, scanT,
    function()
        EtherBuildSpawn.scanMods();
    end, scanW);
    scanBtn:initialise();
    scanBtn:instantiate();
    scanBtn.isOnlyInGame = true;
    self:addChild(scanBtn);
    cy = cy + ctrlH + GAP;

    -- [强制重扫]: 清缓存后重新扫描 (装/卸 mod 后使用)
    local forceT = getTranslate("UI_Build_ScanForce");
    local forceW = UIButton.measureWidth(forceT);
    if forceW > innerW then forceW = innerW; end
    local forceBtn = UIButton:new(innerX, cy + EtherTheme.entryBtnDY, forceW, ctrlH, forceT,
    function()
        if type(clearModScanCache) == "function" then
            clearModScanCache();
        end
        EtherBuildSpawn.scanMods();
    end, forceW);
    forceBtn:initialise();
    forceBtn:instantiate();
    forceBtn.isOnlyInGame = true;
    self:addChild(forceBtn);
    cy = cy + ctrlH + GAP;

    -- 状态行 (render 动态绘制, 这里只登记坐标)
    self.statusX, self.statusY = innerX, cy;
    cy = cy + EtherTheme.fontHgtHint + GAP;

    for i = 1, #hintLines do
        self:_text(innerX, cy, hintLines[i], EtherTheme.textDim, nil, true);
        cy = cy + EtherTheme.fontHgtHint;
    end
    for i = 1, #warnLines do
        self:_text(innerX, cy, warnLines[i], EtherTheme.statusRed, nil, true);
        cy = cy + EtherTheme.fontHgtHint;
    end
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherBuildPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_Build_Title");
    menuTableData.localPlayer = getPlayer();
    menuTableData.groups = {};
    menuTableData.texts = {};
    menuTableData.selEntry = nil;
    self.__index = self;

    return menuTableData;
end
