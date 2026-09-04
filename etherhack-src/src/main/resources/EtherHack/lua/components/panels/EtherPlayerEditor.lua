require "ISUI/ISPanel"

--*********************************************************
--* 玩家编辑面板 (已迁移到 EtherFormPanel: 游标布局 + 统一 add* API)
--*
--* 2026-09-04 改版 (用户需求):
--*   - 人物信息精简为「玩家信息」功能模块 (背景框+标题): 只保留
--*     用户名 / 编辑存活时间 / 编辑击杀丧尸 三行, 其余信息行删除;
--*   - 「配方」(学习全部配方) 自角色页迁入;
--*   - 新增「VHS教学」模块: 可训练技能选择列表 (搜索) + 升级所选技能 /
--*     全技能升级两按钮 + 前提条件说明 (伪造 WaveSignalPacket 官方账本,
--*     见 EtherRadioXp.lua 与 研判文档 §七)。
--* 页面顺序: 玩家信息 -> 配方 -> VHS教学 -> 角色特性 -> 技能。
--*
--* 红线 (零功能影响): getHoursAlive/setHoursAlive/getZombieKills/setZombieKills、
--*   UITraitsTable/UISkillTable、ISUI3DModel 的用法与参数一律未改。
--*********************************************************
EtherPlayerEditor = EtherFormPanel:derive("EtherPlayerEditor");

--*********************************************************
--* 标签 + 右对齐按钮 行 (模块盒内, 一次性动作如学习配方)。
--* 按钮右缘 = 信息行编辑按钮右缘 (同扣一个 BOX_PAD_X), 竖直居中同 makeLabel。
--*********************************************************
local function placeButtonRow(panel, bx, by, innerW, spec)
    local ctrlH = EtherTheme.ctrlH;
    local label = EtherTheme.makeLabel(bx, by, ctrlH, tr(spec.title));
    panel:addChild(label);
    local btnTitle = tr(spec.btnKey);
    local btnW = UIButton.measureWidth(btnTitle);
    local btn = UIButton:new(bx + innerW - btnW, by, btnW, ctrlH,
        btnTitle, spec.onClick, btnW);
    panel:addWidget(btn, { onlyInGame = true });
    return ctrlH;
end

--*********************************************************
--* 人物信息行数据 (标签 + 值), 集中构建便于统一排布与折行。
--* 2026-09-04 精简: 只保留用户名 + 两个可编辑行。
--*********************************************************
function EtherPlayerEditor:infoRows()
    local p = self.localPlayer;

    local timeSurvived = "N/A";
    if p.getTimeSurvived then
        timeSurvived = tostring(p:getTimeSurvived() or "N/A");
    end
    local zombieKills = "0";
    if p.getZombieKills then
        zombieKills = tostring(p:getZombieKills() or 0);
    end

    return {
        { label = getText("IGUI_PlayerStats_Username"), value = tostring(p:getUsername() or "") },
        { label = getText("IGUI_char_Survived_For"), value = timeSurvived, edit = "time" },
        { label = getText("IGUI_char_Zombies_Killed"), value = zombieKills, edit = "kills" },
    };
end

--*********************************************************
--* 面板内容构建 (基类在 createChildren 里回调 build)
--*********************************************************
function EtherPlayerEditor:build()
    self.localPlayer = getPlayer();
    if self.localPlayer == nil then return end
    if self.localPlayer:getDescriptor() == nil then return end

    local tm = getTextManager();
    local ctrlH = EtherTheme.ctrlH;
    local fhS = EtherTheme.fontHgtSmall;
    -- 行距必须容纳 Edit 按钮 (ctrlH), 否则按钮会比行高还高、上下溢出相邻行
    local rowStep = math.max(fhS + 8, ctrlH + 2);
    self.hintRows = nil;          -- VHS教学说明行登记 (重建时清空)

    -- ================= 玩家信息&配方 (合并模块) =================
    -- 三行信息 + 配方按钮行。对齐策略 (2026-09-05 用户实测修正):
    -- 信息标签/值全走 EtherTheme.makeLabel (ISLabel 精确基线居中, 与按钮同一套
    -- 居中数学 —— 此前手绘 drawText 估算 dy 与按钮错位); 值列统一让出最右按钮列
    -- 宽 (按钮 68px 槽位, 有无按钮都预留) → 所有值右缘/按钮竖直对齐成列;
    -- 学习按钮右缘 = 编辑按钮右缘 (同行宽右对齐)。
    local rows = self:infoRows();
    local recipeRowH = EtherTheme.ctrlH;
    local infoH = #rows * rowStep + 8 + recipeRowH;

    self:addModule("UI_PlayerEditor_PlayerInfoRecipes_Title", infoH, function(bx, by, bw)
        local ix = bx + EtherFormPanel.BOX_PAD_X;
        local iw = (bx + bw) - ix - EtherFormPanel.BOX_PAD_X;
        local editTitle = getTranslate("UI_PlayerEditor_EditStats") or "Edit";
        local editW = tm:MeasureStringX(UIFont.Small, editTitle) + EtherTheme.ctrlPadX * 2;
        -- 最右按钮槽位 (所有行共用): 编辑/学习按钮都右对齐到 iw 右缘
        local btnSlot = editW + EtherTheme.ctrlGap;

        -- 信息标签/值: ISLabel 子控件 (基线居中与按钮一致), 值右对齐到按钮槽左缘
        for i = 1, #rows do
            local r = rows[i];
            local y = by + (i - 1) * rowStep;

            local lb = EtherTheme.makeLabel(ix, y, rowStep, r.label);
            self:addChild(lb);

            -- 值右缘 = iw 右缘 - 按钮槽位 (有 Edit 的行按钮占槽, 没有的行留白同宽)
            local valueRight = ix + iw - btnSlot;
            local valueText = r.value;
            local valueW = tm:MeasureStringX(UIFont.Small, valueText);
            local availW = valueRight - ix - tm:MeasureStringX(UIFont.Small, r.label) - EtherTheme.ctrlGap;
            if availW < 40 then availW = 40; end
            if valueW > availW then
                valueText = EtherTheme.wrapText(valueText, availW, UIFont.Small)[1];
                valueW = tm:MeasureStringX(UIFont.Small, valueText);
            end
            local vb = EtherTheme.makeLabel(valueRight - valueW, y, rowStep, valueText, EtherTheme.textDim);
            self:addChild(vb);

            -- Edit 按钮: 与本行竖直居中, 右对齐到 iw 右缘
            if r.edit ~= nil then
                local cb = (r.edit == "time")
                    and function() self:onEditTimeButton(); end
                    or function() self:onEditKillsButton(); end
                local btn = UIButton:new((bx + bw) - EtherFormPanel.BOX_PAD_X - editW,
                    y + math.floor((rowStep - ctrlH) / 2), editW, ctrlH, editTitle, cb);
                self:addWidget(btn);
            end
        end

        -- 配方按钮行 (信息行下方, 标签同款 ISLabel 基线, 按钮右缘 = 编辑按钮右缘)
        placeButtonRow(self, bx + EtherFormPanel.BOX_PAD_X, by + #rows * rowStep + 8,
            bw - EtherFormPanel.BOX_PAD_X * 2, {
                title = "UI_Exploit_LearnAllRecipesTitle",
                btnKey = "UI_Exploit_LearnAllRecipesButton",
                onClick = function()
                    if learnAllRecipesSynced ~= nil then
                        learnAllRecipesSynced();
                    else
                        learnAllRecipes();
                    end
                end,
            });
    end);

    -- ================= VHS教学 (可训练技能 XP) =================
    self:addSpacer(EtherFormPanel.SECTION_GAP);
    self:buildVhsModule();

    -- ================= 特质 =================
    -- 两张表各自占一个自定义行; 表的列头画在表体之上(PZ 行为), 故预留 LIST_HEADER_H
    local hdrH = EtherFormPanel.LIST_HEADER_H;
    local tableH = 220;

    self:addSpacer(EtherFormPanel.SECTION_GAP);
    self:addModule("UI_PlayerEditor_PlayerTraits_Title", tableH + hdrH, function(bx, by, bw)
        local ok, err = pcall(function()
            self.traitsPanel = UITraitsTable:new(bx, by + hdrH, bw, tableH);
            self.traitsPanel:initialise();
            self.traitsPanel.parent = self;
            self:_anchor(self.traitsPanel);
            self:addChild(self.traitsPanel);
        end)
        if not ok then
            print("[EtherHack] Failed to create traits panel: " .. tostring(err))
        end
    end);

    -- ================= 技能 =================
    self:addSpacer(EtherFormPanel.SECTION_GAP);
    self:addModule("UI_PlayerEditor_PlayerSkills_Title", tableH + hdrH, function(bx, by, bw)
        local ok, err = pcall(function()
            self.skillPanel = UISkillTable:new(bx, by + hdrH, bw, tableH);
            self.skillPanel:initialise();
            self.skillPanel.parent = self;
            self:_anchor(self.skillPanel);
            self:addChild(self.skillPanel);
        end)
        if not ok then
            print("[EtherHack] Failed to create skills panel: " .. tostring(err))
        end
    end);

    self.childrenCreated = true;
end

--*********************************************************
--* VHS教学模块: 技能选择列表 (搜索) + 升级所选 / 全技能两按钮 + 说明。
--* 数据源 EtherRadioXp.trainablePerks() (仅 PERK_CODE 覆盖的技能)。
--* 说明文字登记到 self.hintRows (renderContent 统一绘制, boost 页同款)。
--*********************************************************
function EtherPlayerEditor:buildVhsModule()
    local innerW = self:_rowContentW() - EtherFormPanel.BOX_PAD_X * 2;
    local searchH = EtherTheme.entryH;
    local listH = 160;
    local btnRowH = EtherTheme.ctrlH + 6;

    local hintLines = EtherTheme.wrapHint(tr("UI_RadioXp_Hint"), innerW);
    local moduleH = searchH + 4 + listH + 6 + btnRowH + 8
        + #hintLines * EtherTheme.fontHgtHint + 6;

    self:addModule("UI_RadioXp_ModuleTitle", moduleH, function(bx, by, bw)
        local ix = bx + EtherFormPanel.BOX_PAD_X;
        local iW = bw - EtherFormPanel.BOX_PAD_X * 2;
        self.hintRows = self.hintRows or {};

        -- 搜索行 (标签 + 输入框, boost 页 makeSection 同款)
        local searchLabel = tr("UI_RadioXp_Search");
        local lb = EtherTheme.makeLabel(ix, by, searchH, searchLabel);
        self:addChild(lb);
        local entryX = ix + getTextManager():MeasureStringX(UIFont.Small, searchLabel) + EtherTheme.ctrlGap;
        local search = ISTextEntryBox:new("", entryX, by,
            math.max(80, ix + iW - entryX), searchH);
        EtherTheme.styleEntry(search);
        self:addWidget(search);

        -- 技能列表 (可训练技能: PERK_CODE 覆盖)。
        -- 字体必须显式降到 Small: 游戏 ISScrollingListBox:new 默认 UIFont.Large
        -- (行高 ≈40px, 用户实测"行太大"); boost 页列表也有此隐患 (只压 itemheight
        -- 不换 font, Large 字在 Small 行高里被挤裁)。
        local list = ISScrollingListBox:new(ix, by + searchH + 4, iW, listH);
        list:initialise();
        list:instantiate();
        list.font = UIFont.Small;
        list.fontHgt = getTextManager():getFontFromEnum(UIFont.Small):getLineHeight();
        list.itemheight = EtherTheme.listItemH;
        list.selected = -1;
        list.drawBorder = false;
        EtherTheme.styleList(list);
        self:addWidget(list);

        local entries = {};
        if EtherRadioXp.trainablePerks ~= nil then
            entries = EtherRadioXp.trainablePerks();
        end
        local function refill()
            local query = string.lower(search:getInternalText() or "");
            list:clear();
            for i = 1, #entries do
                local e = entries[i];
                if query == "" or string.find(string.lower(e.label), query, 1, true) ~= nil then
                    list:addItem(e.label .. " (" .. e.name .. ")", e);
                end
            end
        end
        search.onTextChange = refill;
        refill();

        -- 行渲染 (boost 页列表同款紧凑风格): 主题化行底 + Small 字左对齐
        function list:doDrawItem(y, item, alt)
            if y + self:getYScroll() + self.itemheight < 0 or y + self:getYScroll() >= self.height then
                return y + self.itemheight;
            end
            EtherTheme.drawRowUnderlay(self, y, self.selected == item.index, alt, self.itemheight);
            self:drawText(item.text, 6, y + 4,
                EtherTheme.text.r, EtherTheme.text.g, EtherTheme.text.b, 0.9, UIFont.Small);
            return y + self.itemheight;
        end

        -- 升级按钮行: 升级所选技能 | 全技能升级 (无锚点时自动尝试放置背包收音机)
        local btnY = by + searchH + 4 + listH + 6;
        local btnW = math.floor((iW - EtherTheme.ctrlGap) / 2);
        local oneBtn = UIButton:new(ix, btnY, btnW, EtherTheme.ctrlH,
            tr("UI_RadioXp_ButtonOne"), function()
                local sel = list.selected;
                if sel == nil or sel < 1 or sel > #list.items then return; end
                local it = list.items[sel].item;
                if it == nil or it.code == nil then return; end
                EtherRadioXp.upgrade(it.code);
            end, btnW);
        oneBtn:initialise(); oneBtn:instantiate(); self:addChild(oneBtn);
        local allBtn = UIButton:new(ix + btnW + EtherTheme.ctrlGap, btnY, btnW, EtherTheme.ctrlH,
            tr("UI_RadioXp_ButtonAll"), function()
                EtherRadioXp.upgrade(nil);
            end, btnW);
        allBtn:initialise(); allBtn:instantiate(); self:addChild(allBtn);

        -- 前提条件说明 (登记, renderContent 绘制)
        local hintY = btnY + EtherTheme.ctrlH + 8;
        for hi = 1, #hintLines do
            table.insert(self.hintRows, { text = hintLines[hi], x = ix,
                y = hintY + (hi - 1) * EtherTheme.fontHgtHint });
        end
    end);
end

--*********************************************************
--* 重建面板内容 (编辑存活时长/击杀数后刷新显示)
--*********************************************************
function EtherPlayerEditor:updateLabels()
    -- 先快照再移除: 直接在遍历 Java 子元素列表时移除元素并不安全
    local kids = {};
    for _, child in pairs(self:getChildren()) do
        table.insert(kids, child);
    end
    for i = 1, #kids do
        self:removeChild(kids[i]);
    end

    self.traitsPanel = nil;
    self.skillPanel = nil;
    self.childrenCreated = false;
    self:createChildren();
end

function EtherPlayerEditor:onEditTimeButton()
    local modal = ISTextBox:new(0, 0, 560, 360, getTranslate("UI_PlayerEditor_EditHoursTitle") or "Edit Hours",
        tostring(getHoursAlive and getHoursAlive() or 0),
        self,
        function(target, button)
            if button.internal == "OK" then
                local value = tonumber(button.parent.entry:getText())
                if value and setHoursAlive then
                    setHoursAlive(value)
                    self:updateLabels()
                end
            end
        end)
    modal:initialise()
    modal:addToUIManager()
end

function EtherPlayerEditor:onEditKillsButton()
    local modal = ISTextBox:new(0, 0, 560, 360, getTranslate("UI_PlayerEditor_EditKillsTitle") or "Edit Kills",
        tostring(getZombieKills and getZombieKills() or 0),
        self,
        function(target, button)
            if button.internal == "OK" then
                local value = tonumber(button.parent.entry:getText())
                if value and setZombieKills then
                    setZombieKills(value)
                    self:updateLabels()
                end
            end
        end)
    modal:initialise()
    modal:addToUIManager()
end

--*********************************************************
--* 手绘内容 (基类 renderContent 钩子): VHS教学登记的说明行。
--* 信息行已改 ISLabel 子控件 (build 时创建, 与按钮同一基线居中), 不在此绘制。
--*********************************************************
function EtherPlayerEditor:renderContent()
    -- VHS教学/其他模块登记的说明行 (boost 页同款)
    if self.hintRows ~= nil then
        for i = 1, #self.hintRows do
            local h = self.hintRows[i];
            EtherTheme.drawHintText(self, h.text, h.x, h.y, EtherTheme.textDim, 0.9);
        end
    end
end

function EtherPlayerEditor:render()
    EtherFormPanel.render(self);

    if getPlayer() == nil then
        self:drawTextCentre(self.workInGameText, self.width / 2, self.height / 2,
            1.0, 1.0, 1.0, 1.0, UIFont.Large)
    end
end

--*********************************************************
--* update: 进入游戏后补建 UI
--*********************************************************
function EtherPlayerEditor:update()
    ISPanel.update(self);

    if not self.childrenCreated then
        local p = getPlayer();
        if p ~= nil then
            self.localPlayer = p;
            self:createChildren();
        end
        return;
    end
end

--*********************************************************
--* 构造 (其余继承 EtherFormPanel)
--*********************************************************
function EtherPlayerEditor:new(posX, posY, width, height)
    local o = EtherFormPanel.new(self, posX, posY, width, height);
    o.workInGameText = getTranslate("UI_PlayerEditor_PanelWorkOnlyInGame") or "This panel only works in-game";
    o.localPlayer = getPlayer();
    o.childrenCreated = false;
    EtherPlayerEditor.instance = o;      -- 原实现误把类表赋给 instance
    return o;
end
