require "ISUI/ISPanel"

--*********************************************************
--* 陷阱刷物品面板 (trap-spawn items, multiplayer)
--* 双链: 食物链(动物陷阱 addAnimalDebug, hungerChange<0) 与
--* 武器链(爆炸陷阱 AddExplosiveTrapPacket, ItemType.WEAPON),
--* 模式切换钮二选一; 搜索 + 点击生成, 数量循环。
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

function EtherTrapSpawn:_text(tx, ty, text, col, font, hint, mode)
    table.insert(self.texts, {
        x = tx, y = ty, text = text,
        col = col or EtherTheme.text, font = font or UIFont.Small,
        -- hint=true: 说明文字, prerender 里走 EtherTheme.drawHintText 缩放绘制。
        -- 不能在 createChildren 里即时画: 此时面板还没挂到父节点, getAbsoluteX
        -- 拿到的是错误坐标, 而且 createChildren 只执行一次, 画完即丢 (实机:
        -- 陷阱页提示消失、render 里逐帧画的其它页提示却正常, 即此因)。
        hint = hint or false,
        -- mode: 可选, "food"/"weapon" —— 仅该模式下绘制 (两种模式的说明/标签
        -- 都在同一位置注册, 按当前模式二选一, 免去重建布局)。
        mode = mode,
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
        if t.mode ~= nil and t.mode ~= self.mode then
            -- 另一模式的静态文案: 跳过不画
        elseif t.hint then
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
--* 模式切换按钮标题 (食物链/武器链共用一枚切换钮)
--*********************************************************
local function modeTitle(mode)
    return getTranslate("UI_TrapSpawn_Mode") .. ": " ..
        getTranslate(mode == "weapon" and "UI_TrapSpawn_ModeWeapon" or "UI_TrapSpawn_ModeFood");
end

--*********************************************************
--* Инициализация списка (按模式: 食物 hungerChange<0 / 武器 ItemType.WEAPON)
--*********************************************************
function EtherTrapSpawn:initList()
    local items = getAllItems();
    local list = {};
    local wantWeapon = (self.mode == "weapon");
    for i = 0, items:size() - 1 do
        local item = items:get(i);
        if not item:getObsolete() and not item:isHidden() then
            local pass = false;
            if wantWeapon then
                -- ItemType.WEAPON 含枪械/近战/投掷物; TrapSpawnAPI 会统一伪造
                -- 安全引信, 投掷物也能落成可回收陷阱
                pass = item:isItemType(ItemType.WEAPON);
            else
                pass = item:getHungerChange() < 0;
            end
            if pass then
                table.insert(list, { item = item });
            end
        end
    end
    table.sort(list, function(a, b) return not string.sort(a.item:getDisplayName(), b.item:getDisplayName()); end);

    self.fullList = list;
    self.totalResult = 0;
    self.datas:clear();
    for i, v in ipairs(list) do
        self.datas:addItem(v.item:getDisplayName(), v.item);
        self.totalResult = self.totalResult + 1;
    end
    self:applyFilter();
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
    -- 两种模式各自的搜索标签都注册 (mode 过滤二选一绘制), 布局按较宽者预留
    local searchFoodText = getTranslate("UI_TrapSpawn_SearchLabel");
    local searchWeaponText = getTranslate("UI_TrapSpawn_SearchWeapon");
    local slW = math.max(tm:MeasureStringX(UIFont.Small, searchFoodText), tm:MeasureStringX(UIFont.Small, searchWeaponText));

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
    self:_text(innerX, slY, searchFoodText, EtherTheme.text, UIFont.Small, nil, "food");
    self:_text(innerX, slY, searchWeaponText, EtherTheme.text, UIFont.Small, nil, "weapon");

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

    -- ================= 模式 + 数量行 =================
    -- 单枚切换钮 (食物/武器) + 数量输入框 (右锚固定宽); 两链循环次数通用 (1..50)。
    -- 极端字号/长翻译下: 按钮钳到"输入框以左"的可用宽 (UIButton render 自带
    -- 超宽缩字), 数量标签放不下就整段不画 (输入框默认值 1 已自明)。
    local modeY = sy + searchH + GAP;
    local modeH = ctrlH + IP * 2;
    local countEntryW = 70;
    local countEntryX = innerX + innerW - countEntryW;
    local modeMax = (countEntryX - GAP) - innerX;
    local modeW = math.max(UIButton.measureWidth(modeTitle("food")), UIButton.measureWidth(modeTitle("weapon")));
    if modeW > modeMax then modeW = modeMax; end
    if modeW < 100 then modeW = 100; end
    local countW = tm:MeasureStringX(UIFont.Small, getTranslate("UI_TrapSpawn_Count"));
    local countLabelX = countEntryX - countW - math.floor(GAP / 2);
    self.modeBtn = UIButton:new(innerX, modeY + IP, modeW, ctrlH, modeTitle(self.mode),
    function()
        self.mode = (self.mode == "weapon") and "food" or "weapon";
        self.modeBtn.title = modeTitle(self.mode);
        self:initList();
    end, modeW)
    self.modeBtn:initialise();
    self.modeBtn:instantiate();
    self.modeBtn.isOnlyInGame = true;
    self:addChild(self.modeBtn);

    if countLabelX >= (innerX + modeW + GAP) then
        self:_text(countLabelX, modeY + IP + EtherTheme.entryLabelDY, getTranslate("UI_TrapSpawn_Count"), EtherTheme.text, UIFont.Small);
    end
    self.countEntry = ISTextEntryBox:new("1", countEntryX, modeY + IP, countEntryW, EtherTheme.entryH);
    EtherTheme.styleEntry(self.countEntry);
    self.countEntry:initialise();
    self.countEntry:instantiate();
    self.countEntry.isOnlyInGame = true;
    self:addChild(self.countEntry);
    self:_group(PAD, modeY, boxW, modeH);

    -- ================= 底部: 生成 + 提示 + 状态 =================
    -- 先算底部块, 列表再吃掉中间剩余高度
    local spawnTitle = getTranslate("UI_TrapSpawn_Button");
    local spawnW = UIButton.measureWidth(spawnTitle);
    local maxSpawnW = boxW - IP * 2;
    if spawnW > maxSpawnW then spawnW = maxSpawnW; end
    local hintX = PAD + IP + spawnW + GAP * 2;
    local hintW = (PAD + boxW - IP) - hintX;
    if hintW < 60 then hintW = 60; end
    -- 两种模式的说明都按可用宽度折行注册, 高度取较髙者, 显示按模式二选一
    local foodHintLines = EtherTheme.wrapHint(getTranslate("UI_TrapSpawn_Hint"), hintW);
    local weaponHintLines = EtherTheme.wrapHint(getTranslate("UI_TrapSpawn_HintWeapon"), hintW);
    local hintH = math.max(#foodHintLines, #weaponHintLines) * EtherTheme.fontHgtHint;
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
        local count = tonumber(self.countEntry:getInternalText()) or 1;
        EtherTrapPOC.setTarget(scriptItem:getFullName());
        EtherTrapPOC.count = count;
        if self.mode == "weapon" then
            EtherTrapPOC.triggerWeapon(count);
        else
            EtherTrapPOC.trigger();
        end
    end, spawnW)
    self.spawnBtn:initialise();
    self.spawnBtn:instantiate();
    self.spawnBtn.isOnlyInGame = true;
    self:addChild(self.spawnBtn);

    -- 提示: 按可用宽度折行, 与按钮竖直居中对齐 (说明文字按 hintScale 缩小绘制,
    -- 与其他静态文案一样走 _text 记录 + prerender 每帧绘制; 两套说明按模式二选一)
    local hintY0 = actY + IP + math.floor((ctrlH - hintH) / 2);
    for i = 1, #foodHintLines do
        self:_text(hintX, hintY0 + (i - 1) * EtherTheme.fontHgtHint, foodHintLines[i],
            EtherTheme.textDim, nil, true, "food");
    end
    for i = 1, #weaponHintLines do
        self:_text(hintX, hintY0 + (i - 1) * EtherTheme.fontHgtHint, weaponHintLines[i],
            EtherTheme.textDim, nil, true, "weapon");
    end

    -- 状态行 (动态, 由 render 绘制)
    self.statusX = innerX;
    self.statusY = actY + IP + math.max(ctrlH, hintH) + GAP;
    self:_group(PAD, actY, boxW, actH + IP * 2);

    -- ================= 中部: 列表 =================
    -- 列表紧贴模式行之下 (仅隔 GAP), 不再预留表头带: ISScrollingListBox 的
    -- 表头(连同 alpha=1 的描边)只在 #columns > 0 时绘制, 本页不 addColumn,
    -- 表头彻底不画 —— 之前的空列会让表头画到列表 y 之上压住搜索盒, 而为它
    -- 预留的 listHeaderH 又变成一条纯空白带 (实机两轮反馈的来源)。
    local listY = modeY + modeH + GAP;
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
    menuTableData.mode = "food";       -- 生成链: food(动物陷阱链) / weapon(爆炸陷阱链)
    menuTableData.groups = {};        -- 分组盒矩形 (createChildren 填充)
    menuTableData.texts = {};         -- 静态文案 (createChildren 填充)
    self.__index = self;

    return menuTableData;
end
