require "ISUI/ISPanel"

--*********************************************************
--* 陷阱刷物品面板 (trap-spawn food, multiplayer)
--* 食物链(动物陷阱 addAnimalDebug, hungerChange<0)。
--* 搜索 + 点击生成, 数量循环。
--*********************************************************
EtherTrapSpawn = ISPanel:derive("EtherTrapSpawn");

local fontHeightSmall = getTextManager():getFontHeight(UIFont.Small)

--*********************************************************
--* 布局登记 (createChildren 期间算好, prerender 统一绘制)
--*   _group 分组盒矩形; _text 静态文案 (集中登记便于按宽度折行, 防各语言溢出)
--*********************************************************
function EtherTrapSpawn:_group(gx, gy, gw, gh)
    table.insert(self.groups, { x = gx, y = gy, w = gw, h = gh });
end

function EtherTrapSpawn:_text(tx, ty, text, col, font, hint)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        col = col or EtherTheme.text, font = font or UIFont.Small,
        -- hint=true: 说明文字, prerender 里走 EtherTheme.drawHintText 缩放绘制。
        -- createChildren 时尚未挂到父节点, 不在这里直接绘制。
        hint = hint or false,
    });
end

--*********************************************************
--* Обработка prerender
--* 分组盒与静态文案画在子控件之下 (渲染序: prerender -> 子控件 -> render)
--*********************************************************
function EtherTrapSpawn:prerender()
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
--* Обработка render (仅动态状态)
--*********************************************************
-- 二百七十七 (L9): 相位代号 → 翻译键 (未登记相位回退原始代号)
local TRAP_PHASE_KEYS = {
    find_trap = "UI_TrapSpawn_PhaseFindTrap",
    inject    = "UI_TrapSpawn_PhaseInject",
    done      = "UI_TrapSpawn_PhaseDone",
};

function EtherTrapSpawn:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end

    -- 二百七十七 (L9/L6): 状态行走三语, 并按源值记忆化
    local status;
    if not EtherTrapPOC.armed then
        status = tr("UI_TrapSpawn_StatusIdle");
    else
        local c = self.mcStatusCache;
        if c == nil or c.target ~= EtherTrapPOC.target or c.count ~= EtherTrapPOC.count
            or c.phase ~= EtherTrapPOC.phase then
            local phaseKey = TRAP_PHASE_KEYS[EtherTrapPOC.phase];
            local phaseText = phaseKey ~= nil and tr(phaseKey) or tostring(EtherTrapPOC.phase);
            c = {
                target = EtherTrapPOC.target, count = EtherTrapPOC.count, phase = EtherTrapPOC.phase,
                str = tr("UI_TrapSpawn_StatusFmt", { target = tostring(EtherTrapPOC.target),
                    count = tostring(EtherTrapPOC.count), phase = phaseText }),
            };
            self.mcStatusCache = c;
        end
        status = c.str;
    end
    if self.statusX ~= nil then
        local td = EtherTheme.textDim;
        self:drawText(status, self.statusX, self.statusY, td.r, td.g, td.b, 1, UIFont.Small);
    end
end

--*********************************************************
--* Фильтр по названию
--*********************************************************
function EtherTrapSpawn:applyFilter()
    -- 名称/ID 双条件 AND, 纯子串匹配: 物品名可含 Lua 模式字符。
    local nameTxt = string.lower(self.searchBox:getInternalText() or "");
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
--* Отрисовка данных
--*********************************************************
function EtherTrapSpawn:drawDatas(y, item, alt)
    if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
        return y + self.itemheight
    end

    local a = 0.9;
    local th = EtherTheme;

    EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight)

    local iconSize = fontHeightSmall;
    -- 二百七十五 (L3): 名称改用条目预存 text, 图标走 EtherTheme 缓存。
    self:drawText(item.text, 25, y + 4, th.text.r, th.text.g, th.text.b, a, self.font);
    EtherTheme.drawItemIcon(self, item.item, y, iconSize);

    return y + self.itemheight;
end

--*********************************************************
--* Инициализация списка (食物 hungerChange<0)
--*********************************************************
function EtherTrapSpawn:initList()
    local items = getAllItems();
    local list = {};
    for i = 0, items:size() - 1 do
        local item = items:get(i);
        if not item:getObsolete() and not item:isHidden() and item:getHungerChange() < 0 then
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
--* Создание дочерних элементов
--* 排版: 顶部 搜索(名称/ID)+数量 同一行, 中部列表, 底部生成 + 提示 + 状态。
--* 控件高度/内边距统一走 EtherTheme.ctrlH / ctrlPadX / ctrlGap。
--*********************************************************
function EtherTrapSpawn:createChildren()
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

    -- ================= 顶部: 搜索(名称/ID) + 数量 同一行 =================
    -- 用户要求: "搜索食物[ ] ID[ ] 数量[ ]" 同排。搜索框可收缩至 60px;
    -- 再窄才换行, 避免正常的小窗口仍把数量单独占一行。
    local sy = PAD;
    local searchText = getTranslate("UI_TrapSpawn_SearchLabel");
    local slW = tm:MeasureStringX(UIFont.Small, searchText);
    local idLabelT = getTranslate("UI_Exchange_SearchId");
    local ilW = tm:MeasureStringX(UIFont.Small, idLabelT);
    local countLabelT = getTranslate("UI_TrapSpawn_Count");
    local clW = tm:MeasureStringX(UIFont.Small, countLabelT);
    local countEntryW = 70;
    -- 一行预算 = 三标签 + 名称/ID 两框 + 数量框 + 5 个间距
    local entW = math.floor((innerW - slW - ilW - clW - countEntryW - GAP * 5) / 2);
    local countSameRow = entW >= 60;
    local searchTwoRows = false;
    if not countSameRow then
        entW = math.floor((innerW - slW - ilW - GAP * 3) / 2);
        searchTwoRows = entW < 90;
        if searchTwoRows then
            entW = innerW - slW - GAP;
            if entW < 60 then entW = 60; end
        end
    end

    local sbX, sbY, slY = innerX + slW + GAP, sy + IP, sy + IP + EtherTheme.entryLabelDY;
    self:_text(innerX, slY, searchText, EtherTheme.text, UIFont.Small);
    self.searchBox = ISTextEntryBox:new("", sbX, sbY, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.searchBox);
    self.searchBox:initialise();
    self.searchBox:instantiate();
    self.searchBox:setClearButton(true)
    self.searchBox.onTextChange = function()
        EtherTrapSpawn.applyFilter(self)
    end
    self:addChild(self.searchBox);

    local idX, idY;
    if searchTwoRows then
        idX = innerX;
        idY = sy + IP + EtherTheme.entryH + GAP;
        entW = math.max(60, innerW - ilW - GAP);
    else
        idX = innerX + slW + GAP + entW + GAP;
        idY = sbY;
    end
    self:_text(idX, idY + EtherTheme.entryLabelDY, idLabelT, EtherTheme.text, UIFont.Small);
    self.searchId = ISTextEntryBox:new("", idX + ilW + GAP, idY, entW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.searchId);
    self.searchId:initialise();
    self.searchId:instantiate();
    self.searchId:setClearButton(true)
    self.searchId.onTextChange = function()
        EtherTrapSpawn.applyFilter(self)
    end
    self:addChild(self.searchId);

    -- 数量: 能同排就紧接 ID 输入框; 否则另起一行 (右对齐) 并单独一个分组盒。
    local topBottom;
    if countSameRow then
        local cx = idX + ilW + GAP + entW + GAP;
        self:_text(cx, idY + EtherTheme.entryLabelDY, countLabelT, EtherTheme.text, UIFont.Small);
        self.countEntry = ISTextEntryBox:new("1", cx + clW + GAP, idY, countEntryW, EtherTheme.entryH);
        local searchH = EtherTheme.entryH + IP * 2;
        if searchTwoRows then searchH = EtherTheme.entryH * 2 + GAP + IP * 2; end
        self:_group(PAD, sy, boxW, searchH);
        topBottom = sy + searchH;
    else
        local searchH = EtherTheme.entryH + IP * 2;
        if searchTwoRows then searchH = EtherTheme.entryH * 2 + GAP + IP * 2; end
        self:_group(PAD, sy, boxW, searchH);
        local countGroupY = sy + searchH + GAP;
        local countY = countGroupY + IP;
        local countEntryX = innerX + innerW - countEntryW;
        local countLabelX = countEntryX - clW - math.floor(GAP / 2);
        if countLabelX >= innerX then
            self:_text(countLabelX, countY + EtherTheme.entryLabelDY, countLabelT,
                EtherTheme.text, UIFont.Small);
        end
        self.countEntry = ISTextEntryBox:new("1", countEntryX, countY, countEntryW, EtherTheme.entryH);
        self:_group(PAD, countGroupY, boxW, EtherTheme.entryH + IP * 2);
        topBottom = countGroupY + EtherTheme.entryH + IP * 2;
    end
    EtherTheme.styleEntry(self.countEntry);
    self.countEntry:initialise();
    self.countEntry:instantiate();
    self.countEntry.isOnlyInGame = true;
    self:addChild(self.countEntry);

    -- ================= 底部: 生成 + 提示 + 状态 =================
    local spawnTitle = getTranslate("UI_TrapSpawn_Button");
    local spawnW = UIButton.measureWidth(spawnTitle);
    local maxSpawnW = boxW - IP * 2;
    if spawnW > maxSpawnW then spawnW = maxSpawnW; end
    local hintX = PAD + IP + spawnW + GAP * 2;
    local hintW = (PAD + boxW - IP) - hintX;
    if hintW < 60 then hintW = 60; end
    -- 说明最多 6 行, 不挤占整块列表。
    local maxHintLines = 6;
    local hintLines = EtherTheme.wrapHint(getTranslate("UI_TrapSpawn_Hint"), hintW);
    while #hintLines > maxHintLines do table.remove(hintLines); end
    local hintH = #hintLines * EtherTheme.fontHgtHint;
    local actH = math.max(ctrlH, hintH) + fhS + GAP;
    local actY = H - PAD - (actH + IP * 2);

    self.spawnBtn = UIButton:new(innerX, actY + IP, spawnW, ctrlH, spawnTitle,
    function()
        local sel = self.datas.selected;
        if self.datas.items == nil or sel < 1 or sel > #self.datas.items then
            print("[TrapPOC] select an item first")
            return
        end
        local scriptItem = self.datas.items[sel].item;
        if scriptItem == nil then return end
        local count = tonumber(self.countEntry:getInternalText()) or 1;
        EtherTrapPOC.setTarget(scriptItem:getFullName());
        EtherTrapPOC.count = count;
        EtherTrapPOC.trigger();
    end, spawnW)
    self.spawnBtn:initialise();
    self.spawnBtn:instantiate();
    self.spawnBtn.isOnlyInGame = true;
    self:addChild(self.spawnBtn);

    local hintY0 = actY + IP + math.floor((math.max(ctrlH, hintH) - hintH) / 2);
    for i = 1, #hintLines do
        self:_text(hintX, hintY0 + (i - 1) * EtherTheme.fontHgtHint, hintLines[i],
            EtherTheme.textDim, nil, true);
    end
    self.statusX = innerX;
    self.statusY = actY + IP + math.max(ctrlH, hintH) + GAP;
    self:_group(PAD, actY, boxW, actH + IP * 2);

    -- ================= 中部: 列表 =================
    -- 紧贴搜索/数量行; 无表头; 高度不足时不覆盖底部操作区。
    local listY = topBottom + GAP;
    local listH = (actY - GAP) - listY;
    if listH < 0 then listH = 0; end
    if listH < 80 and listH > 0 then listH = 80; end
    if listY + listH > actY - GAP then listH = math.max(0, (actY - GAP) - listY); end

    self.datas = EtherFastList:new(PAD, listY, boxW, listH);
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
function EtherTrapSpawn:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
    menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.workInGameText = getTranslate("UI_TrapSpawn_WorkInGame");
    menuTableData.localPlayer = getPlayer();
    menuTableData.groups = {};
    menuTableData.texts = {};
    self.__index = self;

    return menuTableData;
end
