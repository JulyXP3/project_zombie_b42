require "ISUI/ISPanel"

--*********************************************************
--* EtherCombatPanel: 战斗页 (2026-09-05 用户立项: 战斗功能自「角色」页拆出,
--* 「角色」页同名更改为「生存」—— 只留生存/状态类作弊)
--*
--*   - 战斗强化 (自角色页整组迁入): 攻速倍率/攻击距离加成 + 秒杀/暴击Max/
--*     枪械只爆头/百发百中/免后座/群攻/无限弹药/不卡壳;
--*     新增「超级群攻」数值行 (研判 超级群攻-研判(未实现).md §五.4):
--*     输入=每次挥击命中上限 (10-20, 即时钳制仅存值); [应用]=生效 (恒全向扇面,
--*     属主僵尸不再逐目标发包, 伤害走本地结算); [重置]=还原原版;
--*     门控为挥击实时读取 — 应用/重置后下一次挥击即切换。
--*   - 临时武器: 客户端本地生成枪械替换手持, 零上行 (EtherTempWeapon.lua)。
--*********************************************************

EtherCombatPanel = EtherFormPanel:derive("EtherCombatPanel");

--*********************************************************
--* 模块内说明行 (角色页 ModuleHint 同款; derive 名不得与角色页重复)
--*********************************************************
local ModuleHint = ISPanel:derive("EtherCombatHint");

function ModuleHint:render()
    for i = 1, #self.lines do
        EtherTheme.drawHintText(self, self.lines[i], 0, (i - 1) * EtherTheme.fontHgtHint,
            EtherTheme.textDim, 0.9);
    end
end

ModuleHint.onMouseDown = UIRowBox.onMouseDown;
ModuleHint.onMouseUp = UIRowBox.onMouseUp;
ModuleHint.onMouseMove = UIRowBox.onMouseMove;

function ModuleHint:new(x, y, w, text)
    local lines = EtherTheme.wrapHint(text, w - 8);
    local o = ISPanel:new(x, y, w, #lines * EtherTheme.fontHgtHint + 2);
    setmetatable(o, self);
    self.__index = self;
    o.background = false;
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 };
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 };
    o.moveWithMouse = false;
    o.lines = lines;
    return o;
end

--*********************************************************
--* 动态状态行 (每帧渲染拉取 EtherTempWeapon 状态)
--*********************************************************
local TempStatusRow = ISPanel:derive("EtherTempWeaponStatus");

function TempStatusRow:render()
    EtherTheme.drawHintText(self, EtherTempWeapon.statusLine(), 0, 0,
        { r = 1, g = 1, b = 1, a = 1 }, 1);
end

TempStatusRow.onMouseDown = UIRowBox.onMouseDown;
TempStatusRow.onMouseUp = UIRowBox.onMouseUp;
TempStatusRow.onMouseMove = UIRowBox.onMouseMove;

function TempStatusRow:new(x, y, w, h)
    local o = ISPanel:new(x, y, w, h);
    setmetatable(o, self);
    self.__index = self;
    o.background = false;
    o.backgroundColor = { r = 0, g = 0, b = 0, a = 0 };
    o.borderColor = { r = 0, g = 0, b = 0, a = 0 };
    o.moveWithMouse = false;
    return o;
end

--*********************************************************
--* 复选框网格预排/摆放 (角色页同款拷贝)
--*********************************************************
local function planGrid(items, innerW)
    local tm = getTextManager();
    local needW = 0;
    for i = 1, #items do
        local w = 18 + 10 + tm:MeasureStringX(UIFont.Small, tr(items[i].key)) + 20;
        if w > needW then needW = w; end
    end
    local cols = math.floor(innerW / math.max(needW, EtherFormPanel.MIN_COL_W));
    if cols < 1 then cols = 1; end
    if cols > EtherFormPanel.MAX_COLS then cols = EtherFormPanel.MAX_COLS; end
    local colW = innerW / cols;
    local cellW = cols > 1 and (colW - EtherFormPanel.BOX_GAP) or innerW;

    local lineH = EtherTheme.fontHgtSmall + 2;
    local rows = {};
    local contentH = 0;
    local row = { cells = {}, step = 0 };
    local col = 0;
    local function flush()
        if #row.cells > 0 then
            table.insert(rows, row);
            contentH = contentH + row.step;
            row = { cells = {}, step = 0 };
        end
    end
    for i = 1, #items do
        local title = tr(items[i].key);
        local availW = cellW - (18 + 10 + 8);
        local lines = { title };
        if tm:MeasureStringX(UIFont.Small, title) > availW then
            lines = EtherTheme.wrapText(title, availW, UIFont.Small);
        end
        local step = EtherFormPanel.ROW_STEP;
        if #lines > 1 then
            step = #lines * lineH + EtherFormPanel.BOX_PAD_Y * 2 + 4;
        end
        if row.step < step then row.step = step; end
        table.insert(row.cells, { it = items[i], col = col, w = cellW, lines = lines });
        col = col + 1;
        if col >= cols then
            col = 0;
            flush();
        end
    end
    flush();
    return rows, contentH, colW;
end

local function placeGrid(panel, rows, bx, by, colW)
    local cy = by;
    for r = 1, #rows do
        local row = rows[r];
        for c = 1, #row.cells do
            local cell = row.cells[c];
            local it = cell.it;
            local checked = false;
            if type(it.get) == "function" then
                checked = it.get() and true or false;
            elseif it.get then
                checked = true;
            end
            local cb = UICheckbox:new(bx + cell.col * colW, cy, tr(it.key), checked, it.on);
            if #cell.lines > 1 then
                cb.titleLines = cell.lines;
                cb.height = math.max(18, #cell.lines * (EtherTheme.fontHgtSmall + 2));
                cb.width = cell.w;
            end
            panel:addWidget(cb, it);
            it.widget = cb;
        end
        cy = cy + row.step;
    end
end

--*********************************************************
--* 数值输入行 (角色页同款布局) + 生效分离扩展 (研判 §五.4):
--* spec.onTextChanged ~= nil 时为"分离模式" — 输入只即时钳制存值 (不切生效状态),
--* [应用] = spec.apply(num) 生效; [重置] = 回填默认值 + spec.resetFn() 还原。
--* 未提供时保持角色页原行为 (输入即应用)。
--*********************************************************
local function entryRowHeight(innerW, titleKey)
    local tm = getTextManager();
    local title = tr(titleKey);
    local gap = EtherTheme.ctrlGap;
    local btnW = UIButton.measureGroupWidth({
        tr("UI_CharacterPanel_ApplyButton"), tr("UI_CharacterPanel_ResetButton") });
    local ctrlW = EtherFormPanel.ENTRY_W + (gap + btnW) * 2;
    local twoLine = tm:MeasureStringX(UIFont.Small, title) + gap + ctrlW > innerW;
    if twoLine then
        return EtherTheme.fontHgtSmall + 4 + EtherTheme.entryH;
    end
    return math.max(EtherTheme.entryH, EtherTheme.ctrlH);
end

local function placeEntryRow(panel, bx, by, innerW, spec)
    local tm = getTextManager();
    local title = tr(spec.title);
    local gap = EtherTheme.ctrlGap;
    local btnW = UIButton.measureGroupWidth({
        tr("UI_CharacterPanel_ApplyButton"), tr("UI_CharacterPanel_ResetButton") });
    local ctrlW = EtherFormPanel.ENTRY_W + (gap + btnW) * 2;
    local labelW = tm:MeasureStringX(UIFont.Small, title);
    local twoLine = labelW + gap + ctrlW > innerW;

    local rowH = EtherTheme.entryH;
    local ctrlY = twoLine and (by + EtherTheme.fontHgtSmall + 4) or by;
    local labelRowH = twoLine and EtherTheme.fontHgtSmall or rowH;
    local label = EtherTheme.makeLabel(bx, by, labelRowH, title);
    panel:addChild(label);

    local cx = twoLine and bx or (bx + labelW + gap);
    local entryW = math.min(EtherFormPanel.ENTRY_W, bx + innerW - cx);

    local entry = ISTextEntryBox:new(tostring(spec.getInitial()), cx, ctrlY, entryW, rowH);
    EtherTheme.styleEntry(entry);
    entry:initialise();
    entry:instantiate();

    local function readNum()
        local num = tonumber(entry:getText());
        if num ~= nil then
            if num < spec.minValue then num = spec.minValue; end
            if num > spec.maxValue then num = spec.maxValue; end
        end
        return num;
    end
    local function applyEntry()
        local num = readNum();
        if num ~= nil then spec.apply(num); end
    end
    if spec.onTextChanged ~= nil then
        entry.onTextChange = function()
            local num = readNum();
            if num ~= nil then spec.onTextChanged(num); end
        end;
    else
        entry.onTextChange = applyEntry;
    end
    panel:addWidget(entry);

    local bx2 = cx + entryW + gap;
    local applyBtn = UIButton:new(bx2, ctrlY + EtherTheme.entryBtnDY, btnW,
        EtherTheme.ctrlH, tr("UI_CharacterPanel_ApplyButton"), applyEntry, btnW);
    applyBtn:initialise();
    applyBtn:instantiate();
    panel:addChild(applyBtn);

    local resetBtn = UIButton:new(bx2 + btnW + gap, ctrlY + EtherTheme.entryBtnDY, btnW,
        EtherTheme.ctrlH, tr("UI_CharacterPanel_ResetButton"), function()
            entry:setText(spec.resetText);
            if spec.resetFn ~= nil then
                spec.resetFn();
            else
                applyEntry();
            end
        end, btnW);
    resetBtn:initialise();
    resetBtn:instantiate();
    panel:addChild(resetBtn);

    if twoLine then
        return EtherTheme.fontHgtSmall + 4 + rowH;
    end
    return math.max(rowH, EtherTheme.ctrlH);
end

--*********************************************************
--* 构建表单内容 (基类 createChildren 回调): 战斗强化 + 临时武器
--*********************************************************
function EtherCombatPanel:build()
    local w = self:_rowContentW();
    local innerW = w - EtherFormPanel.BOX_PAD_X * 2;
    local tm = getTextManager();

    -- ① 战斗强化 (自角色页整组迁入 + 超级群攻)
    local combatItems = {
        -- 特例: 关闭时额外还原武器数据 (与原版一致)
        { key = "UI_CharacterPanel_InstantKill",
          on = function(c) toggleExtraDamage(c); if not c then resetWeaponsStats(); end end,
          get = isExtraDamage },
        { key = "UI_CharacterPanel_CritMax",          on = toggleCritMax,        get = isCritMax },
        { key = "UI_CharacterPanel_HeadshotOnly",     on = toggleHeadshotOnly,   get = isHeadshotOnly },
        { key = "UI_CharacterPanel_AlwaysHit",        on = toggleAlwaysHit,      get = getAlwaysHit },
        { key = "UI_CharacterPanel_DisableRecoil",    on = toggleNoRecoil,       get = isNoRecoil },
        { key = "UI_CharacterPanel_MultiHitZombies",  on = toggleMultiHitZombies, get = isMultiHitZombies },
        { key = "UI_CharacterPanel_UnlimitedAmmo",    on = toggleUnlimitedAmmo,  get = isUnlimitedAmmo },
        { key = "UI_CharacterPanel_NoJam",            on = toggleNoJam,          get = isNoJam },
    };
    local rows, gridH, colW = planGrid(combatItems, innerW);
    local hintH = #EtherTheme.wrapHint(tr("UI_CharacterPanel_CombatHint"), innerW - 8) * EtherTheme.fontHgtHint + 2;
    local combatEntries = {
        {   -- 攻速倍率: IsoGameCharacter.calculateCombatSpeed 返回值乘数 (原版 clamp 之后再乘)
            title = "UI_Exploit_CombatSpeedMultiplierTitle",
            minValue = 1.0, maxValue = 2.5, resetText = "1.0",
            getInitial = function()
                if type(getCombatSpeedMultiplier) == "function" then return getCombatSpeedMultiplier(); end
                return 1.0;
            end,
            apply = function(num) setCombatSpeedMultiplier(num); end,
        },
        {   -- 攻击距离加成 (格): 近战最远 = 原版maxRange+加成, 服务器复核线+5 之内留 1 格冗余
            title = "UI_Exploit_AttackRangeBonusTitle",
            minValue = 0.0, maxValue = 4.0, resetText = "0.0",
            getInitial = function()
                if type(getAttackRangeBonus) == "function" then return getAttackRangeBonus(); end
                return 0.0;
            end,
            apply = function(num) setAttackRangeBonus(num); end,
        },
        {   -- 超级群攻 (研判 §五.4): 输入=数量 (仅存值), [应用]=生效(恒全向), [重置]=还原
            title = "UI_CharacterPanel_SuperMultiHitTitle",
            minValue = 10, maxValue = 20, resetText = "10",
            getInitial = function()
                if type(getSuperMultiHitCount) == "function" then return getSuperMultiHitCount(); end
                return 10;
            end,
            onTextChanged = function(num)
                if type(setSuperMultiHitCount) == "function" then setSuperMultiHitCount(num); end
            end,
            apply = function(num)
                if type(setSuperMultiHitCount) == "function" then setSuperMultiHitCount(num); end
                if type(toggleSuperMultiHit) == "function" then toggleSuperMultiHit(true); end
            end,
            resetFn = function()
                if type(setSuperMultiHitCount) == "function" then setSuperMultiHitCount(10); end
                if type(toggleSuperMultiHit) == "function" then toggleSuperMultiHit(false); end
            end,
        },
    };
    -- 说明行 (超级群攻专属, 内容以"超级群攻:"开头) 贴在最后一个数值行
    -- (超级群攻) 的正下方, 归属一眼可见
    local combatH = gridH;
    for ei = 1, #combatEntries do
        combatH = combatH + 6 + entryRowHeight(innerW, combatEntries[ei].title);
    end
    combatH = combatH + 6 + hintH;

    self:addModule("UI_CharacterPanel_Group_Combat", combatH + 2, function(bx, by, bw)
        local ix = bx + EtherFormPanel.BOX_PAD_X;
        local iW = bw - EtherFormPanel.BOX_PAD_X * 2;
        local cy = by;
        placeGrid(self, rows, ix, cy, colW);
        cy = cy + gridH;
        local ey = cy + 6;
        for ei = 1, #combatEntries do
            ey = ey + placeEntryRow(self, ix, ey, iW, combatEntries[ei]) + 6;
        end
        local hint = ModuleHint:new(ix, ey, iW, tr("UI_CharacterPanel_CombatHint"));
        hint:initialise();
        hint:instantiate();
        self:_anchor(hint);
        self:addChild(hint);
    end);

    -- ② 临时武器 (客户端本地生成+装备, 零上行; EtherTempWeapon.lua)
    self:addSpacer(EtherFormPanel.SECTION_GAP);
    if #EtherTempWeapon.list == 0 then
        EtherTempWeapon.buildList();
    end
    -- 搜索行 (交换页同款 名称/ID 双框): 预算与摆放同一套判定; 标签限宽 40%
    -- 防长翻译压住输入框 (交换页同款)
    local searchNameT = tr("UI_TempWeapon_SearchName");
    local searchIdT = tr("UI_TempWeapon_SearchId");
    local nameLblW = tm:MeasureStringX(UIFont.Small, searchNameT);
    local idLblW = tm:MeasureStringX(UIFont.Small, searchIdT);
    local searchGap = EtherTheme.ctrlGap;
    local maxLblW = math.floor(innerW * 0.4);
    if nameLblW > maxLblW then nameLblW = maxLblW; end
    if idLblW > maxLblW then idLblW = maxLblW; end
    local searchEntW = math.floor((innerW - nameLblW - idLblW - searchGap * 3) / 2);
    local searchTwoRows = searchEntW < 90;
    local nameEntW, idEntW, filterH;
    if searchTwoRows then
        nameEntW = math.max(60, innerW - nameLblW - searchGap);
        idEntW = math.max(60, innerW - idLblW - searchGap);
        filterH = EtherTheme.entryH * 2 + searchGap;
    else
        nameEntW = searchEntW;
        idEntW = searchEntW;
        filterH = EtherTheme.entryH;
    end
    local listH = EtherTheme.listItemH * 6;
    local btnRowH = EtherTheme.ctrlH;
    local statusH = EtherTheme.fontHgtHint;
    local tempH = filterH + 4 + listH + 6 + btnRowH + 6 + statusH + 2;

    self:addModule("UI_TempWeapon_Title", tempH + 2, function(bx, by, bw)
        local ix = bx + EtherFormPanel.BOX_PAD_X;
        local iW = bw - EtherFormPanel.BOX_PAD_X * 2;
        local cy = by;

        -- 搜索行 (交换页同款: 名称/ID 双框 AND 过滤, setClearButton 一键清空)
        -- 单行时 ID 列必须排在名称框之后 (idX 计入名称框宽) — 排回行首会与
        -- 名称列整列重叠 (实测缺陷)
        self:addChild(EtherTheme.makeLabel(ix, cy, EtherTheme.entryH, searchNameT));
        local nameEntry = ISTextEntryBox:new("", ix + nameLblW + searchGap, cy, nameEntW, EtherTheme.entryH);
        EtherTheme.styleEntry(nameEntry);
        nameEntry:initialise();
        nameEntry:instantiate();
        nameEntry:setClearButton(true);
        self:addWidget(nameEntry);

        local idX, idY;
        if searchTwoRows then
            idX = ix;
            idY = cy + EtherTheme.entryH + searchGap;
        else
            idX = ix + nameLblW + searchGap + nameEntW + searchGap;
            idY = cy;
        end
        self:addChild(EtherTheme.makeLabel(idX, idY, EtherTheme.entryH, searchIdT));
        local idEntry = ISTextEntryBox:new("", idX + idLblW + searchGap, idY, idEntW, EtherTheme.entryH);
        EtherTheme.styleEntry(idEntry);
        idEntry:initialise();
        idEntry:instantiate();
        idEntry:setClearButton(true);
        self:addWidget(idEntry);

        -- 枪械清单 (VHS教学列表同款: 显式 Small 字体, 主题化行渲染)
        local list = ISScrollingListBox:new(ix, cy + filterH + 4, iW, listH);
        list:initialise();
        list:instantiate();
        list.font = UIFont.Small;
        list.fontHgt = getTextManager():getFontFromEnum(UIFont.Small):getLineHeight();
        list.itemheight = EtherTheme.listItemH;
        list.selected = -1;
        list.drawBorder = false;
        EtherTheme.styleList(list);
        self:addWidget(list);

        local function refill()
            local nameQ = string.lower(nameEntry:getInternalText() or "");
            local idQ = string.lower(idEntry:getInternalText() or "");
            list:clear();
            for i = 1, #EtherTempWeapon.list do
                local wItem = EtherTempWeapon.list[i];
                local okName = nameQ == "" or string.find(string.lower(wItem.label), nameQ, 1, true) ~= nil;
                local okId = idQ == "" or string.find(string.lower(wItem.type), idQ, 1, true) ~= nil;
                if okName and okId then
                    list:addItem(wItem.label .. "  [" .. wItem.type .. "]", wItem);
                end
            end
        end
        nameEntry.onTextChange = refill;
        idEntry.onTextChange = refill;
        refill();

        function list:doDrawItem(y, item, alt)
            if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
                return y + self.itemheight;
            end
            EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight);
            self:drawText(item.text, 6, y + 4,
                EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 0.9, UIFont.Small);
            return y + self.itemheight;
        end

        -- 按钮行: [临时替换] (重复点按=换枪) [取消替换] (未替换时灰置)
        -- 闭包教训 (实测三例): 回调里引用的按钮必须已进入作用域 — cancelBtn
        -- 自引用自身 (local cancelBtn = UIButton:new(..., 引用 cancelBtn 的闭包, ...))
        -- 捕获的是全局 nil, 点取消 tableSet 即崩; replaceBtn 引用 cancelBtn 亦同
        -- (靠下面的前置声明)。policyBtn / EtherDriveModule 同族。
        local btnY = cy + filterH + 4 + listH + 6;
        local btnW = math.floor((iW - EtherTheme.ctrlGap) / 2);
        local cancelBtn;
        cancelBtn = UIButton:new(ix + btnW + EtherTheme.ctrlGap, btnY, btnW, EtherTheme.ctrlH,
            tr("UI_TempWeapon_Cancel"), function()
                EtherTempWeapon.cancel();
                cancelBtn.isEnable = false;
            end, btnW);
        cancelBtn:initialise();
        cancelBtn:instantiate();
        self:addWidget(cancelBtn);
        cancelBtn.isEnable = false;   -- 未替换态灰置 (与 LootRoll 双模式按钮同款交互)

        local replaceBtn = UIButton:new(ix, btnY, btnW, EtherTheme.ctrlH,
            tr("UI_TempWeapon_Replace"), function()
                local sel = list.selected;
                if sel == nil or sel < 1 or sel > #list.items then return; end
                local wItem = list.items[sel].item;
                if wItem == nil then return; end
                EtherTempWeapon.apply(wItem.type);
                cancelBtn.isEnable = EtherTempWeapon.isActive();
            end, btnW);
        replaceBtn:initialise();
        replaceBtn:instantiate();
        self:addWidget(replaceBtn);

        -- 状态行 (每帧拉取: 未替换 / 已替换: <武器> [弹药])
        local status = TempStatusRow:new(ix, btnY + btnRowH + 6, iW, statusH);
        status:initialise();
        status:instantiate();
        self:_anchor(status);
        self:addChild(status);
    end);
end

--*********************************************************
--* :new / :createChildren / :prerender / :render / :onMouseWheel
--* 全部继承自 EtherFormPanel, 无需重写。
--*********************************************************
