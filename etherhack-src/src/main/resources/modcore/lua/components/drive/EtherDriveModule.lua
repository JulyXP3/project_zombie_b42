require "ISUI/ISPanel"

--*********************************************************
--* EtherDriveModule: 导航模式模块盒 (载具页内 — EtherVehiclePanel.build
--* 末尾经全局 EtherDriveModule_addTo(panel) 追加; 用户定夺 2026-09-05:
--* 放「载具」页不设独立驾驶 Tab, 研判 §八 UI 设计同步修正;
--* 2026-09-06 更名「导航模式」— 沿道路网开往终点, 用户拍板)
--*
--* 盒内内容 (研判 §五):
--*   1. 状态行 (动态): 状态机状态 + 目标坐标 / 最近消息 / 服务端限速→硬顶
--*      (消息行与状态行同文时自动隐藏 — 驾驶中不再重复显示);
--*   2. 巡航速度输入行: [0] = 自适应 (默认, 取 min(硬顶, ~55)), 正值 = 指定目标速
--*      (仍被硬顶钳制); 应用/重置 (0 = 系统自己定, 用户对速度的全部控制权);
--*   3. 说明行: M 地图右键锚定出发 + 任意驾驶键当帧接管。
--* 停止按钮已移除 (2026-09-06 用户拍板): 按任意驾驶键即当帧接管 = 停止,
--* 到达/下车/引擎熄火自动结束, 无需独立按钮。
--*
--* 蠕行脱困 (研判 §六 可选扩展, 默认关+需实测) v1 不入库: 冻结态下物理整个停用,
--* 其实现属传送类盲采步进通道, 待实测后单列开关。
--*
--* 依赖: EtherTheme / EtherFormPanel / UICheckbox / UIButton (modcoreMenu
--* 模块清单先于载具页构建加载)。
--* Java 暴露面: autoDrive* 全局函数 (AutoDriveAPI)。
--*********************************************************

--*********************************************************
--* 动态状态行 (每帧渲染拉取, Java 调用量 3 次/帧, 与其他面板 render 惯例一致)
--*********************************************************
local StatusRow = ISPanel:derive("EtherDriveStatus");

local STATE_KEYS = {
    [0] = "UI_DrivePanel_StatusIdle",
    [1] = "UI_DrivePanel_StatusDriving",
    [2] = "UI_DrivePanel_StatusBrakeToBoundary",
    [3] = "UI_DrivePanel_StatusWaitLoad",
    [4] = "UI_DrivePanel_StatusIdle",
};

function StatusRow:render()
    local y = 0;
    -- 行 1: 状态 + 目标坐标
    local stateId = autoDriveGetStateId();
    local line = tr("UI_DrivePanel_Status") .. ": " .. tr(STATE_KEYS[stateId] or "UI_DrivePanel_StatusIdle");
    if autoDriveIsActive() then
        line = line .. string.format("  (%d, %d)", autoDriveGetTargetX(), autoDriveGetTargetY());
    end
    EtherTheme.drawHintText(self, line, 0, y, { r = 1, g = 1, b = 1, a = 1 }, 1);
    y = y + EtherTheme.fontHgtHint;
    -- 行 2: 最近消息 (锚定结果/取消原因/到达提示); 与状态行同文时隐藏
    -- (驾驶中消息恒为状态键, 原样渲染 = 与行 1 重复 — 实测冗余, 用户拍板去除)
    local msgKey = autoDriveGetMessage();
    if msgKey ~= nil and msgKey ~= "" and msgKey ~= (STATE_KEYS[stateId] or "") then
        EtherTheme.drawHintText(self, tr(msgKey), 0, y, EtherTheme.textDim, 1);
        y = y + EtherTheme.fontHgtHint;
    end
    -- 行 3: 速度体系提示 (服务端限速 → 硬顶, 登录同步可读, 研判 §四.3)
    local limitText = string.format("%.0f", autoDriveGetSpeedLimit());
    local capText = string.format("%.0f", autoDriveGetHardCap());
    EtherTheme.drawHintText(self, tr("UI_DrivePanel_HintLimit", { limit = limitText, cap = capText }),
        0, y, EtherTheme.textDim, 1);
end

StatusRow.onMouseDown = UIRowBox.onMouseDown;
StatusRow.onMouseUp = UIRowBox.onMouseUp;
StatusRow.onMouseMove = UIRowBox.onMouseMove;

function StatusRow:new(x, y, w, h)
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
--* 静态说明行 (EtherCharacterPanel 的 ModuleHint 同款, 折行构造时算一次)
--*********************************************************
local HintRow = ISPanel:derive("EtherDriveHint");

function HintRow:render()
    for i = 1, #self.lines do
        EtherTheme.drawHintText(self, self.lines[i], 0, (i - 1) * EtherTheme.fontHgtHint,
            EtherTheme.textDim, 0.9);
    end
end

HintRow.onMouseDown = UIRowBox.onMouseDown;
HintRow.onMouseUp = UIRowBox.onMouseUp;
HintRow.onMouseMove = UIRowBox.onMouseMove;

function HintRow:new(x, y, w, text)
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
--* 巡航速度输入行 (EtherCharacterPanel placeEntryRow 同款: 标签 + 输入框 +
--* 应用/重置, 紧贴排版, 放不下时标签独占一行)。返回占用高度。
--*********************************************************
local function cruiseEntryRowHeight(innerW)
    local tm = getTextManager();
    local title = tr("UI_DrivePanel_CruiseSpeed");
    local btnW = UIButton.measureGroupWidth({
        tr("UI_CharacterPanel_ApplyButton"), tr("UI_CharacterPanel_ResetButton") });
    local ctrlW = EtherFormPanel.ENTRY_W + (EtherTheme.ctrlGap + btnW) * 2;
    if tm:MeasureStringX(UIFont.Small, title) + EtherTheme.ctrlGap + ctrlW > innerW then
        return EtherTheme.fontHgtSmall + 4 + EtherTheme.entryH;
    end
    return math.max(EtherTheme.entryH, EtherTheme.ctrlH);
end

local function placeCruiseEntryRow(panel, bx, by, innerW)
    local tm = getTextManager();
    local title = tr("UI_DrivePanel_CruiseSpeed");
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

    local entry = ISTextEntryBox:new(tostring(autoDriveGetCruiseSpeed()), cx, ctrlY, entryW, rowH);
    EtherTheme.styleEntry(entry);
    entry:initialise();
    entry:instantiate();

    local function applyEntry()
        local num = tonumber(entry:getText());
        if num ~= nil then
            if num < 0 then num = 0; end
            if num > 200 then num = 200; end
            autoDriveSetCruiseSpeed(num);
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
            entry:setText("0");
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
--* 追加进宿主页 (EtherVehiclePanel): addModule 同族模块盒。
--* 高度预算与摆放同一套判定 (与载具页既有模块同构)。
--*********************************************************
function EtherDriveModule_addTo(panel)
    local w = panel:_rowContentW();
    local innerW = w - EtherFormPanel.BOX_PAD_X * 2;

    -- 高度预算 (与摆放同一套判定)
    local statusH = 3 * EtherTheme.fontHgtHint + 4;
    local entryH = cruiseEntryRowHeight(innerW);
    local resetBtnW = UIButton.measureGroupWidth({ tr("UI_DrivePanel_ResetVehicle") });
    local resetH = EtherTheme.ctrlH;
    local hintH = #EtherTheme.wrapHint(tr("UI_DrivePanel_Hint"), innerW - 8) * EtherTheme.fontHgtHint + 2;
    local contentH = statusH + 6 + entryH + 6 + resetH + 6 + hintH + 2;

    panel:addModule("UI_DrivePanel_Title", contentH + 2, function(bx, by, bw)
        local ix = bx + EtherFormPanel.BOX_PAD_X;
        local iW = bw - EtherFormPanel.BOX_PAD_X * 2;
        local cy = by;

        -- ① 动态状态行
        local status = StatusRow:new(ix, cy, iW, statusH);
        status:initialise();
        status:instantiate();
        panel:_anchor(status);
        panel:addChild(status);
        cy = cy + statusH + 6;

        -- ② 巡航速度 (唯一速度旋钮, 0 = 自适应)
        cy = cy + placeCruiseEntryRow(panel, ix, cy, iW) + 6;

        -- ②.5 车辆重置 (宽限期兜底: 嵌墙/埋地时恢复高度 + 碰撞豁免续 15s)
        local resetBtn = UIButton:new(ix, cy, resetBtnW, resetH,
            tr("UI_DrivePanel_ResetVehicle"), function()
                autoDriveResetVehicle();
            end, resetBtnW);
        resetBtn:initialise();
        resetBtn:instantiate();
        panel:addChild(resetBtn);
        cy = cy + resetH + 6;

        -- ③ 操作说明 (停止按钮已移除: 任意驾驶键当帧接管 = 停止)
        local hint = HintRow:new(ix, cy + 6, iW, tr("UI_DrivePanel_Hint"));
        hint:initialise();
        hint:instantiate();
        panel:_anchor(hint);
        panel:addChild(hint);
    end);
end

--*********************************************************
--* 战斗攻击模块盒 (追加进载具页, 2026-09-08): 自动导航期间
--* 三项恒定开启 (导航行为不变); 开关只控制手动驾驶时的可用性。
--*********************************************************
function EtherDriveCombatModule_addTo(panel)
    local rowH = math.max(18, EtherTheme.fontHgtSmall + 4);
    local contentH = 3 * (rowH + 6);

    panel:addModule("UI_DriveCombat_Title", contentH + 2, function(bx, by, bw)
        local ix = bx + EtherFormPanel.BOX_PAD_X;
        local cy = by;

        local function placeCheckbox(key, getter, setter)
            local checked = false;
            if type(getter) == "function" and getter() ~= 0 then checked = true; end
            local cb = UICheckbox:new(ix, cy, tr(key), checked, function(checked)
                if type(setter) == "function" then setter(checked and 1 or 0); end
            end);
            panel:addWidget(cb);
            cy = cy + rowH + 6;
        end

        placeCheckbox("UI_DriveCombat_Wiggle", autoDriveGetCombatWiggle, autoDriveSetCombatWiggle);
        placeCheckbox("UI_DriveCombat_ZombieKill", autoDriveGetCombatZombieKill, autoDriveSetCombatZombieKill);
        placeCheckbox("UI_DriveCombat_NoClip", autoDriveGetCombatNoClip, autoDriveSetCombatNoClip);
    end);
end
