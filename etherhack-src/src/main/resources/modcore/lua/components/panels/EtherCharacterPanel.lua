require "ISUI/ISPanel"

--*********************************************************
--* EtherCharacterPanel: 角色/系统作弊开关面板
--*
--* 2026-08 模块化重构 (用户需求): 从"整页平铺复选框组"改为与战利品/ESP 页
--* 同款的功能模块形态 (addModule: 背景框 + 标题 + 盒内内容):
--*   - 调试权限功能: 上帝/穿墙/隐身/进度条秒走完 (标签带 (SP); ⑩ 回退 — 多人上帝/隐身
--*     不走本页, 由「其他」页「连接期授权: 上帝+隐身 (多人)」组合开关授予),
--*     盒底附说明: 需在「其他」页开启「解锁调试权限(单人)」, 仅单人有效;
--*     多人下上帝/隐身开启「其他」页的「连接期授权」(进服前生效)
--*     (B42 的 Role.isUsingDebugMode 显式排除联网客户端, 多人开关无效);
--*   - 物品与携带: 手中物品无限耐久/自动修理/无限负重(多人经 PlayerDamage
--*     自报包周期上报, 服务端每帧重算由 20/s 重发压制);
--*   - 配方: 学习所有可用的制作配方 (2026-09-04 自本页迁入「玩家」页);
--*   - 特殊模式: 创造/夜视/真-夜视/僵尸不理会
--*     (作弊耕种已于 2026-08-25 移入独立「耕种」选项卡 EtherFarmingPanel;
--*      僵尸不理会 = IsoZombie.setTarget 注入拦截本地玩家, 模拟上传
--*      target=-1 服务端零校验采纳 —— 多人可用, 不再需要调试权限);
--*   - 状态与需求: 无限耐力 + 各类负面状态禁用 + 维持最佳体重/卡路里
--*     + 禁用肌肉拉伤/高速回血 (同走 PlayerDamage 自报通道);
--*   - 战斗强化已于 2026-09-05 迁入独立「战斗」选项卡 (EtherCombatPanel);
--*   - 建号增强已于 2026-08-26 迁入独立「创建角色」选项卡
--*     (EtherCharacterBoostPanel, 含自定义编辑/角色特性点数)。
--* 2026-09-05 更名: 页面改称「生存」(战斗功能拆出后, 剩余内容均为生存向作弊)。
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
-- 二百七十五 (L8): ModuleHint 下沉至 EtherFormPanel 共用 (原为本页私有逐字节拷贝),
-- 本页直接引用全局 ModuleHint, 不再本地定义。

--*********************************************************
--* 复选框网格预排/摆放: 二百七十五 (L8) 下沉 EtherFormPanel.planGrid/placeGrid
--* (原为角色/战斗两页逐行相同拷贝), 本地别名保持调用点不变。
--*********************************************************
local planGrid = EtherFormPanel.planGrid;
local placeGrid = EtherFormPanel.placeGrid;

--*********************************************************
--* 数值输入行 (标签 + 输入框 + 应用/重置), 摆进所属模块盒内。
--* 控件紧贴标签 (作物管理半径行同款): 标签实测宽 + ctrlGap 起排输入框,
--* 应用/重置依次续排, 不再右对齐到盒内右沿; 放不下时标签独占一行, 控件组贴左换行。
--* spec = { title, getInitial, minValue, maxValue, resetText, apply }。
--* 返回实际占用高度, 供模块高度预算先算一遍 (同一套判定, 结果一致)。
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

    -- 紧贴排版: 输入框贴在标签右侧 (作物管理半径行同款), 应用/重置依次续排
    local cx = twoLine and bx or (bx + labelW + gap);
    local entryW = math.min(EtherFormPanel.ENTRY_W, bx + innerW - cx);

    local entry = ISTextEntryBox:new(tostring(spec.getInitial()), cx, ctrlY, entryW, rowH);
    EtherTheme.styleEntry(entry);
    entry:initialise();
    entry:instantiate();

    local function applyEntry()
        local num = tonumber(entry:getText());
        if num ~= nil then
            if num < spec.minValue then num = spec.minValue; end
            if num > spec.maxValue then num = spec.maxValue; end
            spec.apply(num);
        end
    end
    entry.onTextChange = applyEntry;
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
--* 标签 + 右对齐按钮 行 (模块盒内, 一次性动作如学习配方)。
--* placeEntryRow 同族布局: 标签左, 按钮贴盒内右沿; 返回占用高度。
--* 按钮以 onlyInGame 跟踪 — 主菜单构建时 refreshGating 灰置
--* (面板随玩家状态变化重建, 游戏内构建即启用, 与实时 canRun 等价)。
--*********************************************************
local function placeButtonRow(panel, bx, by, innerW, spec)
    local ctrlH = EtherTheme.ctrlH;
    local label = EtherTheme.makeLabel(bx, by, ctrlH, tr(spec.title));
    panel:addChild(label);
    local btnTitle = tr(spec.btnKey);
    local btnW = UIButton.measureWidth(btnTitle);
    local btn = UIButton:new(bx + innerW - btnW - EtherFormPanel.BOX_PAD_X, by, btnW, ctrlH,
        btnTitle, spec.onClick, btnW);
    panel:addWidget(btn, { onlyInGame = true });
    return ctrlH;
end

--*********************************************************
--* 构建表单内容 (基类 createChildren 回调): 四个功能模块。
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
                -- 创造模式: 双写 ISBuildMenu.cheat (Lua 全局, 放行菜单/免走位/免锤子/免材料判定)
                -- + player:setBuildCheat (Java 位, 新建造窗口"建造"按钮 canBuild or isBuildCheat()
                --   与服务端 ISBuildIsoEntity:create 免消耗判定读的就是它)。
                -- setBuildCheat 被 Role.hasCapability(UseBuildCheat) 门禁: SP 下本 mod 的 Role 补丁
                -- 无条件放行 → 生效; MP 管理员 → 生效; MP 非管理员 → 静默拒绝 (无害, 不发包不触反作弊)。
                { key = "UI_CharacterPanel_BuildCheat",
                  on = function(c)
                      ISBuildMenu.cheat = c;
                      local p = getPlayer();
                      if p ~= nil then p:setBuildCheat(c); end
                  end,
                  get = function() return ISBuildMenu.cheat; end },
            },
        },
        {
            title = "UI_CharacterPanel_Group_Items",
            items = {
                -- 「手中物品无限耐久」已去除 (2026-09-14 用户指令): UI 入口与配置读取一并移除,
                -- 后端标志永久为 false (无入口可开)
                { key = "UI_CharacterPanel_AutoRepairsItems",   on = toggleAutoRepairItems,      get = isAutoRepairItems },
                { key = "UI_CharacterPanel_RepairClothing",     on = toggleRepairClothing,       get = isRepairClothing },
                -- 填充皮革条: 修复衣物的镜像操作 — 对覆盖部位伪造满级皮革衬垫(原版补丁数据), 防御叠加
                { key = "UI_CharacterPanel_PadClothing",        on = togglePadClothing,          get = isPadClothing },
                { key = "UI_CharacterPanel_UnlimitedCarry",     on = toggleFreeCarryMode, get = isFreeCarryMode },
            },
        },
        {
            title = "UI_CharacterPanel_Group_Special",
            items = {
                { key = "UI_CharacterPanel_NightVision", on = toggleNightVision, get = isEnableNightVision },
                -- 真-夜视 (Fullbright): 与夜视同模块, 渲染级全亮 (见 GamePatcher.patchFullbright)
                { key = "UI_VisualsPanel_Fullbright", on = toggleFullbright, get = isFullbright },
                -- 僵尸不理会 (多人可用): 客户端模拟上传 target=-1, 服务端零校验采纳
                { key = "UI_CharacterPanel_ZombieDontAttack", on = toggleZombieDontAttack, get = isZombieDontAttack },
                -- (一百九十五) 连接期授权已迁「其他」页 (与解锁调试权限同列): 多人上帝/隐身的
                -- 总开关; 本页上帝/隐身/穿墙三行的多人语义见 DebugHint 说明
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
                { key = "UI_CharacterPanel_DisableWetness",     on = toggleNoWetness,     get = isNoWetness },
                { key = "UI_CharacterPanel_DisableInfectionLevel", on = toggleNoZombieInfection, get = isNoZombieInfection },
                { key = "UI_CharacterPanel_DisableFakeInfectionLevel", on = toggleNoFakeInfection, get = isNoFakeInfection },
                { key = "UI_CharacterPanel_OptimalCalories",    on = toggleCalorieLock,    get = isCalorieLock },
                { key = "UI_CharacterPanel_OptimalWeight",      on = toggleWeightLock,      get = isWeightLock },
                { key = "UI_CharacterPanel_NoMuscleStrain",     on = toggleNoMuscleStrain,     get = isNoMuscleStrain },
                -- 「高速回血」已下线 (一百八十四): 服务端同步通道随游戏更新失效, 方案留档待回归
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

        -- 高度预算: 网格 (预排一次, 摆放复用同一份 rows) + 可选说明行/输入行/按钮行
        local rows, gridH, colW = planGrid(mod.items or {}, innerW);
        local contentH = gridH;
        local hintH = 0;
        if mod.hint ~= nil then
            hintH = #EtherTheme.wrapHint(tr(mod.hint), innerW - 8) * EtherTheme.fontHgtHint + 2;
            contentH = contentH + 6 + hintH;
        end
        if mod.entries ~= nil then
            for ei = 1, #mod.entries do
                contentH = contentH + 6 + entryRowHeight(innerW, mod.entries[ei].title);
            end
        end
        if mod.buttons ~= nil then
            for bi = 1, #mod.buttons do
                contentH = contentH + 6 + EtherTheme.ctrlH;
            end
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
            if mod.entries ~= nil then
                local ey = cy + 6;
                for ei = 1, #mod.entries do
                    ey = ey + placeEntryRow(self, ix, ey, iW, mod.entries[ei]) + 6;
                end
            end
            if mod.buttons ~= nil then
                local py = cy + 6;
                for bi = 1, #mod.buttons do
                    py = py + placeButtonRow(self, ix, py, iW, mod.buttons[bi]) + 6;
                end
            end
        end);
    end
end

--*********************************************************
--* :new / :createChildren / :prerender / :render / :onMouseWheel
--* 全部继承自 EtherFormPanel, 无需重写。
--*********************************************************
