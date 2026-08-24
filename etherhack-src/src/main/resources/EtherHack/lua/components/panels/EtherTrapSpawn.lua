require "ISUI/ISPanel"

--*********************************************************
--* 陷阱刷物品面板 (trap-spawn items, multiplayer)
--* 列出所有陷阱链能生成的物品 (食物, hungerChange < 0),
--* 搜索 + 点击生成; 需站在已放置的陷阱旁。
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
        -- 不能在 createChildren 里即时画: 此时面板还没挂到父节点, getAbsoluteX
        -- 拿到的是错误坐标, 而且 createChildren 只执行一次, 画完即丢 (实机:
        -- 陷阱页提示消失、render 里逐帧画的钓鱼提示却正常, 即此因)。
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
function EtherTrapSpawn:render()
    ISPanel.render(self);
    self:clearStencilRect();

    if self.localPlayer == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2, 1.0, 1.0, 1.0, 1.0, UIFont.Large)
        return
    end

    local status = "idle";
    if EtherTrapPOC.armed then
        status = tostring(EtherTrapPOC.target) .. " x" .. tostring(EtherTrapPOC.count) .. " [" .. tostring(EtherTrapPOC.phase) .. "]";
    end
    if self.statusX ~= nil then
        local td = EtherTheme.textDim;
        -- 直接显示状态文案: 内部代号 "[TrapPOC] " 属于调试前缀, 不应出现在正式 UI
        self:drawText(status, self.statusX, self.statusY, td.r, td.g, td.b, 1, UIFont.Small);
    end
end

--*********************************************************
--* Фильтр по названию
--*********************************************************
function EtherTrapSpawn:applyFilter()
    local filterTxt = string.lower(self.searchBox:getInternalText());
    local full = self.fullList;
    self.datas:clear();
    self.totalResult = 0;
    for i, v in ipairs(full) do
        local name = string.lower(v.item:getDisplayName());
        if filterTxt == "" or (checkStringPattern(filterTxt) and string.match(name, filterTxt)) then
            self.datas:addItem(i, v.item);
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
    self:drawText(item.item:getDisplayName(), 25, y + 4, th.text.r, th.text.g, th.text.b, a, self.font);

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
--* Инициализация списка (только食物: hungerChange < 0)
--*********************************************************
function EtherTrapSpawn:initList()
    local items = getAllItems();
    local foodList = {};
    for i = 0, items:size() - 1 do
        local item = items:get(i);
        if not item:getObsolete() and not item:isHidden() and item:getHungerChange() < 0 then
            table.insert(foodList, { item = item });
        end
    end
    table.sort(foodList, function(a, b) return not string.sort(a.item:getDisplayName(), b.item:getDisplayName()); end);

    self.fullList = foodList;
    self.totalResult = 0;
    self.datas:clear();
    for i, v in ipairs(foodList) do
        self.datas:addItem(v.item:getDisplayName(), v.item);
        self.totalResult = self.totalResult + 1;
    end
end

--*********************************************************
--* Создание дочерних элементов
--* 排版: 顶部搜索行成框, 中部列表填满, 底部操作行成框 (按钮 + 折行提示 + 状态)。
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

    -- ================= 顶部: 搜索 =================
    -- sy 取 PAD, 与底部块的 (H - PAD - ...) 对称。原先写死 10, 比同一函数里
    -- 左右/底部用的 PAD=16 更小, 搜索盒明显比面板其它内容贴近上边缘, 观感上
    -- 就是"冲出了背景框"。
    local sy = PAD;
    local searchText = getTranslate("UI_TrapSpawn_SearchLabel");
    local slW = tm:MeasureStringX(UIFont.Small, searchText);

    -- 标签 + 输入框同排的可用宽度。放不下(长翻译)就把标签移到输入框上方另起一行。
    -- 绝不能像原先那样把 sbW 硬撑到最小 80 —— sbX 已经被长标签推到右边,
    -- sbX + 80 会直接越出分组盒右缘。
    local sbAvail = innerW - slW - GAP;
    local searchTwoRows = sbAvail < 120;
    -- 输入框按 entryH (UITextBox2 最小渲染高, 见 EtherTheme 注释),
    -- 搜索盒随之加高, 杜绝框被游戏自动撑高后顶穿盒边
    local searchH, sbX, sbY, sbW, slY;
    if searchTwoRows then
        slY = sy + IP;
        sbX = innerX;
        sbY = slY + fhS + GAP;
        sbW = innerW;
        searchH = fhS + GAP + EtherTheme.entryH + IP * 2;
    else
        slY = sy + IP + EtherTheme.entryLabelDY;
        sbX = innerX + slW + GAP;
        sbY = sy + IP;
        sbW = sbAvail;
        searchH = EtherTheme.entryH + IP * 2;
    end
    self:_text(innerX, slY, searchText, EtherTheme.text, UIFont.Small);

    self.searchBox = ISTextEntryBox:new("", sbX, sbY, sbW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.searchBox);
    self.searchBox:initialise();
    self.searchBox:instantiate();
    self.searchBox:setClearButton(true)
    self.searchBox.onTextChange = function()
        EtherTrapSpawn.applyFilter(self)
    end
    self:addChild(self.searchBox);
    self:_group(PAD, sy, boxW, searchH);

    -- ================= 底部: 生成 + 提示 + 状态 =================
    -- 先算底部块, 列表再吃掉中间剩余高度
    local spawnTitle = getTranslate("UI_TrapSpawn_Button");
    local spawnW = UIButton.measureWidth(spawnTitle);
    local maxSpawnW = boxW - IP * 2;
    if spawnW > maxSpawnW then spawnW = maxSpawnW; end
    local hintX = PAD + IP + spawnW + GAP * 2;
    local hintW = (PAD + boxW - IP) - hintX;
    if hintW < 60 then hintW = 60; end
    local hintLines = EtherTheme.wrapHint(getTranslate("UI_TrapSpawn_Hint"), hintW);
    local hintH = #hintLines * EtherTheme.fontHgtHint;
    local actH = math.max(ctrlH, hintH) + fhS + GAP;      -- 操作行 + 状态行
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
        EtherTrapPOC.setTarget(scriptItem:getFullName());
        EtherTrapPOC.count = 1;
        EtherTrapPOC.trigger();
    end, spawnW)
    self.spawnBtn:initialise();
    self.spawnBtn:instantiate();
    self.spawnBtn.isOnlyInGame = true;
    self:addChild(self.spawnBtn);

    -- 提示: 按可用宽度折行, 与按钮竖直居中对齐 (说明文字按 hintScale 缩小绘制,
    -- 与其他静态文案一样走 _text 记录 + prerender 每帧绘制)
    local hintY0 = actY + IP + math.floor((ctrlH - hintH) / 2);
    for i = 1, #hintLines do
        self:_text(hintX, hintY0 + (i - 1) * EtherTheme.fontHgtHint, hintLines[i],
            EtherTheme.textDim, nil, true);
    end

    -- 状态行 (动态, 由 render 绘制)
    self.statusX = innerX;
    self.statusY = actY + IP + math.max(ctrlH, hintH) + GAP;
    self:_group(PAD, actY, boxW, actH + IP * 2);

    -- ================= 中部: 列表 =================
    -- 列表紧贴搜索盒之下 (仅隔 GAP), 不再预留表头带: ISScrollingListBox 的
    -- 表头(连同 alpha=1 的描边)只在 #columns > 0 时绘制, 本页不 addColumn,
    -- 表头彻底不画 —— 之前的空列会让表头画到列表 y 之上压住搜索盒, 而为它
    -- 预留的 listHeaderH 又变成一条纯空白带 (实机两轮反馈的来源)。
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
    menuTableData.groups = {};        -- 分组盒矩形 (createChildren 填充)
    menuTableData.texts = {};         -- 静态文案 (createChildren 填充)
    self.__index = self;

    return menuTableData;
end
