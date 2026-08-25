require "ISUI/ISPanel"

--*********************************************************
--* EtherCharacterPanel: 角色/系统作弊开关面板
--*
--* 2026-08 模块化重构 (用户需求): 从"整页平铺复选框组"改为与战利品/ESP 页
--* 同款的功能模块形态 (addModule: 背景框 + 标题 + 盒内内容):
--*   - 调试权限功能: 上帝/穿墙/隐身/进度条秒走完 (标签带 (SP)),
--*     盒底附说明: 需在「其他」页开启「解锁调试权限(单人)」, 仅单人有效
--*     (B42 的 Role.isUsingDebugMode 显式排除联网客户端, 多人开关无效);
--*   - 战斗强化: 秒杀/暴击Max/枪械只爆头/提高枪械射速/群攻/无限弹药/无卡壳
--*     + 攻速倍率输入行 (应用/重置, 手动摆进盒内);
--*   - 物品与携带: 手中物品无限耐久/自动修理/无限负重(多人经 PlayerDamage
--*     自报包周期上报, 服务端每帧重算由 20/s 重发压制);
--*   - 特殊模式: 创造/夜视/真-夜视/僵尸不理会
--*     (作弊耕种已于 2026-08-25 移入独立「耕种」选项卡 EtherFarmingPanel;
--*      僵尸不理会 = IsoZombie.setTarget 注入拦截本地玩家, 模拟上传
--*      target=-1 服务端零校验采纳 —— 多人可用, 不再需要调试权限);
--*   - 状态与需求: 无限耐力 + 各类负面状态禁用 + 维持最佳体重/卡路里
--*     + 禁用肌肉拉伤/高速回血 (同走 PlayerDamage 自报通道);
--*   - 建号增强: 全特性/技能满级/自带服装 (CreatePlayerPacket.set 注入,
--*     建号包零校验, 下次建号生效)。
--* 模块内复选框按模块内宽自适应 1~3 列 (planGrid 预排一次, 高度预算与
--* 实际摆放共用同一份布局; 长标签折行按行内最大行数增高)。
--*
--* 零功能影响: 下列 toggleX/isX/setX 及 ISBuildMenu.cheat
--*   均为既有全局, 名称/签名/调用顺序原样保留。
--*   (ISFarmingMenu.cheat 自耕种选项卡上线后不再有代码设置, 见 EtherFarmingPanel)
--*********************************************************

EtherCharacterPanel = EtherFormPanel:derive("EtherCharacterPanel");

--*********************************************************
--* 模块内说明行 (hint 缩放文字, 信息页同款): 做成子控件随滚动统一定位,
--* 不吞鼠标事件。折行在构造时算一次, render 零测量。
--*********************************************************
local ModuleHint = ISPanel:derive("EtherModuleHint");

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
--* 模块内复选框网格预排: 返回 rows(逐行单元格)/contentH/colW。
--* 列数按"组内最宽标签"与模块内宽自适应 (1~3 列);
--* 长标签在列宽内折行, 行距取该行最大折行高 (同基类 addCheckboxGroup 规则)。
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

--*********************************************************
--* 按预排结果摆放复选框 (与 planGrid 同一份布局, 不再测量)。
--*********************************************************
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
                cb.width = cell.w;               -- 多行时命中区域 = 整列宽
            end
            panel:addWidget(cb, it);
            it.widget = cb;                      -- 回填 (vehicleCheckbox 对外暴露)
        end
        cy = cy + row.step;
    end
end

--*********************************************************
--* 攻速倍率输入行 (标签 + 输入框 + 应用/重置), 摆进战斗模块盒内。
--* 布局与基类 addTextEntry 同规则: 放得下同排, 放不下标签独占一行;
--* 返回实际占用高度, 供模块高度预算先算一遍 (同一套判定, 结果一致)。
--*********************************************************
local function entryRowHeight(innerW)
    local tm = getTextManager();
    local title = tr("UI_Exploit_CombatSpeedMultiplierTitle");
    local gap = EtherTheme.ctrlGap;
    local btnW = UIButton.measureGroupWidth({
        tr("UI_CharacterPanel_ApplyButton"), tr("UI_CharacterPanel_ResetButton") });
    local ctrlW = EtherFormPanel.ENTRY_W + (gap + btnW) * 2;
    local twoLine = tm:MeasureStringX(UIFont.Small, title) + gap * 2 + ctrlW > innerW;
    if twoLine then
        return EtherTheme.fontHgtSmall + 4 + EtherTheme.entryH;
    end
    return math.max(EtherTheme.entryH, EtherTheme.ctrlH);
end

local function placeEntryRow(panel, bx, by, innerW)
    local tm = getTextManager();
    local title = tr("UI_Exploit_CombatSpeedMultiplierTitle");
    local gap = EtherTheme.ctrlGap;
    local btnW = UIButton.measureGroupWidth({
        tr("UI_CharacterPanel_ApplyButton"), tr("UI_CharacterPanel_ResetButton") });
    local ctrlW = EtherFormPanel.ENTRY_W + (gap + btnW) * 2;
    local twoLine = tm:MeasureStringX(UIFont.Small, title) + gap * 2 + ctrlW > innerW;

    local rowH = EtherTheme.entryH;
    local ctrlY = twoLine and (by + EtherTheme.fontHgtSmall + 4) or by;
    local labelRowH = twoLine and EtherTheme.fontHgtSmall or rowH;
    local label = EtherTheme.makeLabel(bx, by, labelRowH, title);
    panel:addChild(label);

    local cx = bx + innerW - ctrlW;
    if cx < bx then cx = bx; end

    local initial = 1.0;
    if type(getCombatSpeedMultiplier) == "function" then
        initial = getCombatSpeedMultiplier();
    end
    local entry = ISTextEntryBox:new(tostring(initial), cx, ctrlY, EtherFormPanel.ENTRY_W, rowH);
    EtherTheme.styleEntry(entry);
    entry:initialise();
    entry:instantiate();

    local function applyEntry()
        local num = tonumber(entry:getText());
        if num ~= nil then
            if num < 1.0 then num = 1.0; end
            if num > 3.0 then num = 3.0; end
            setCombatSpeedMultiplier(num);
        end
    end
    entry.onTextChange = applyEntry;
    panel:addWidget(entry);

    local bx2 = cx + EtherFormPanel.ENTRY_W + gap;
    local applyBtn = UIButton:new(bx2, ctrlY + EtherTheme.entryBtnDY, btnW,
        EtherTheme.ctrlH, tr("UI_CharacterPanel_ApplyButton"), applyEntry, btnW);
    applyBtn:initialise();
    applyBtn:instantiate();
    panel:addChild(applyBtn);

    local resetBtn = UIButton:new(bx2 + btnW + gap, ctrlY + EtherTheme.entryBtnDY, btnW,
        EtherTheme.ctrlH, tr("UI_CharacterPanel_ResetButton"), function()
            entry:setText("1.0");
            applyEntry();
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
--* 构建表单内容 (基类 createChildren 回调): 五个功能模块。
--* 描述表在运行时构建, 确保引用的全局已暴露。
--*********************************************************
function EtherCharacterPanel:build()
    local modules = {
        {
            title = "UI_CharacterPanel_Group_DebugCheats",
            hint = "UI_CharacterPanel_DebugHint",
            items = {
                { key = "UI_CharacterPanel_GodMode",         on = toggleGodMode,           get = isEnableGodMode },
                { key = "UI_CharacterPanel_NoClip",          on = toggleNoclip,            get = isEnableNoclip },
                { key = "UI_CharacterPanel_Invisible",       on = toggleInvisible,         get = isEnableInvisible },
                { key = "UI_CharacterPanel_TimedActionCheat", on = toggleTimedActionCheat, get = isTimedActionCheat },
            },
        },
        {
            title = "UI_CharacterPanel_Group_Combat",
            entry = true,
            items = {
                -- 特例: 关闭时额外还原武器数据 (与原版一致)
                { key = "UI_CharacterPanel_InstantKill",
                  on = function(c) toggleExtraDamage(c); if not c then resetWeaponsStats(); end end,
                  get = isExtraDamage },
                { key = "UI_CharacterPanel_CritMax",          on = toggleCritMax,        get = isCritMax },
                { key = "UI_CharacterPanel_HeadshotOnly",     on = toggleHeadshotOnly,   get = isHeadshotOnly },
                { key = "UI_CharacterPanel_DisableRecoil",    on = toggleNoRecoil,       get = isNoRecoil },
                { key = "UI_CharacterPanel_MultiHitZombies",  on = toggleMultiHitZombies, get = isMultiHitZombies },
                { key = "UI_CharacterPanel_UnlimitedAmmo",    on = toggleUnlimitedAmmo,  get = isUnlimitedAmmo },
                { key = "UI_CharacterPanel_NoJam",            on = toggleNoJam,          get = isNoJam },
            },
        },
        {
            title = "UI_CharacterPanel_Group_Items",
            items = {
                { key = "UI_CharacterPanel_UnlimitedCondition", on = toggleUnlimitedCondition,   get = isUnlimitedCondition },
                { key = "UI_CharacterPanel_AutoRepairsItems",   on = toggleAutoRepairItems,      get = isAutoRepairItems },
                { key = "UI_CharacterPanel_UnlimitedCarry",     on = toggleEnableUnlimitedCarry, get = isEnableUnlimitedCarry },
            },
        },
        {
            title = "UI_CharacterPanel_Group_Special",
            items = {
                -- 特例: 直接写 vanilla 全局标志 (非 toggleX)
                { key = "UI_CharacterPanel_BuildCheat",
                  on = function(c) ISBuildMenu.cheat = c; end,
                  get = function() return ISBuildMenu.cheat; end },
                { key = "UI_CharacterPanel_NightVision", on = toggleNightVision, get = isEnableNightVision },
                -- 真-夜视 (Fullbright): 与夜视同模块, 渲染级全亮 (见 GamePatcher.patchFullbright)
                { key = "UI_VisualsPanel_Fullbright", on = toggleFullbright, get = isFullbright },
                -- 僵尸不理会 (多人可用): 客户端模拟上传 target=-1, 服务端零校验采纳
                { key = "UI_CharacterPanel_ZombieDontAttack", on = toggleZombieDontAttack, get = isZombieDontAttack },
            },
        },
        {
            title = "UI_CharacterPanel_Group_Needs",
            items = {
                { key = "UI_CharacterPanel_UnlimitedEndurance", on = toggleUnlimitedEndurance, get = isUnlimitedEndurance },
                { key = "UI_CharacterPanel_DisableHunger",      on = toggleDisableHunger,      get = isDisableHunger },
                { key = "UI_CharacterPanel_DisableThirst",      on = toggleDisableThirst,      get = isDisableThirst },
                { key = "UI_CharacterPanel_DisableFatigue",     on = toggleDisableFatigue,     get = isDisableFatigue },
                { key = "UI_CharacterPanel_DisableDrunkenness", on = toggleDisableDrunkenness, get = isDisableDrunkenness },
                { key = "UI_CharacterPanel_DisableAnger",       on = toggleDisableAnger,       get = isDisableAnger },
                { key = "UI_CharacterPanel_DisableFear",        on = toggleDisableFear,        get = isDisableFear },
                { key = "UI_CharacterPanel_DisablePain",        on = toggleDisablePain,        get = isDisablePain },
                { key = "UI_CharacterPanel_DisablePanic",       on = toggleDisablePanic,       get = isDisablePanic },
                { key = "UI_CharacterPanel_DisableMorale",      on = toggleDisableMorale,      get = isDisableMorale },
                { key = "UI_CharacterPanel_DisableStress",      on = toggleDisableStress,      get = isDisableStress },
                { key = "UI_CharacterPanel_DisableSickness",    on = toggleDisableSickness,    get = isDisableSickness },
                { key = "UI_CharacterPanel_DisableStressFromCigarettes", on = toggleDisableStressFromCigarettes, get = isDisableStressFromCigarettes },
                { key = "UI_CharacterPanel_DisableSanity",      on = toggleDisableSanity,      get = isDisableSanity },
                { key = "UI_CharacterPanel_DisableBoredomLevel", on = toggleDisableBoredomLevel, get = isDisableBoredomLevel },
                { key = "UI_CharacterPanel_DisableUnhappynessLevel", on = toggleDisableUnhappynessLevel, get = isDisableUnhappynessLevel },
                { key = "UI_CharacterPanel_DisableWetness",     on = toggleDisableWetness,     get = isDisableWetness },
                { key = "UI_CharacterPanel_DisableInfectionLevel", on = toggleDisableInfectionLevel, get = isDisableInfectionLevel },
                { key = "UI_CharacterPanel_DisableFakeInfectionLevel", on = toggleDisableFakeInfectionLevel, get = isDisableFakeInfectionLevel },
                { key = "UI_CharacterPanel_OptimalCalories",    on = toggleOptimalCalories,    get = isOptimalCalories },
                { key = "UI_CharacterPanel_OptimalWeight",      on = toggleOptimalWeight,      get = isOptimalWeight },
                { key = "UI_CharacterPanel_NoMuscleStrain",     on = toggleNoMuscleStrain,     get = isNoMuscleStrain },
                { key = "UI_CharacterPanel_FullBodyRestore",    on = toggleFullBodyRestore,    get = isFullBodyRestore },
            },
        },
        {
            title = "UI_CharacterPanel_Group_CharCreate",
            hint = "UI_CharacterPanel_CharCreateHint",
            items = {
                { key = "UI_CharacterPanel_CharCreateAllTraits",  on = toggleCharCreateAllTraits,  get = isCharCreateAllTraits },
                { key = "UI_CharacterPanel_CharCreateMaxSkills",  on = toggleCharCreateMaxSkills,  get = isCharCreateMaxSkills },
                { key = "UI_CharacterPanel_CharCreateClothing",   on = toggleCharCreateClothing,   get = isCharCreateClothing },
            },
        },
    };

    for mi = 1, #modules do
        if mi > 1 then
            self:addSpacer(EtherFormPanel.SECTION_GAP);
        end
        local mod = modules[mi];
        local w = self:_rowContentW();
        local innerW = w - EtherFormPanel.BOX_PAD_X * 2;

        -- 高度预算: 网格 (预排一次, 摆放复用同一份 rows) + 可选说明行/输入行
        local rows, gridH, colW = planGrid(mod.items, innerW);
        local contentH = gridH;
        local hintH = 0;
        if mod.hint ~= nil then
            hintH = #EtherTheme.wrapHint(tr(mod.hint), innerW - 8) * EtherTheme.fontHgtHint + 2;
            contentH = contentH + 6 + hintH;
        end
        if mod.entry then
            contentH = contentH + 6 + entryRowHeight(innerW);
        end

        self:addModule(mod.title, contentH + 2, function(bx, by, bw)
            local ix = bx + EtherFormPanel.BOX_PAD_X;
            local iW = bw - EtherFormPanel.BOX_PAD_X * 2;
            local cy = by;
            placeGrid(self, rows, ix, cy, colW);
            cy = cy + gridH;
            if mod.hint ~= nil then
                local hint = ModuleHint:new(ix, cy + 6, iW, tr(mod.hint));
                hint:initialise();
                hint:instantiate();
                self:_anchor(hint);
                self:addChild(hint);
            end
            if mod.entry then
                placeEntryRow(self, ix, cy + 6, iW);
            end
        end);
    end
end

--*********************************************************
--* :new / :createChildren / :prerender / :render / :onMouseWheel
--* 全部继承自 EtherFormPanel, 无需重写。
--*********************************************************
