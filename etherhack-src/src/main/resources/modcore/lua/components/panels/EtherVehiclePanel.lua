require "ISUI/ISPanel"

--*********************************************************
--* EtherVehiclePanel: 载具操作页
--*
--* b42.22 已移除直发修理、加油和两项远程上车入口。
--* 保留启动、短接与自动修理的现有实现供兼容性研判, 不把本地状态当成服务端成功。
--* startEngine 由服务端查询钥匙; ISRepairLightbar 只接受 lightbar 部件。
--* 自动驾驶及驾驶战斗模块由本页末尾追加。
--*********************************************************

EtherVehiclePanel = EtherFormPanel:derive("EtherVehiclePanel");

--*********************************************************
--* 模块内说明行 (hint 缩放文字, 与 EtherCharacterPanel 的 ModuleHint 同款):
--* 做成子控件随滚动统一定位, 不吞鼠标事件。折行在构造时算一次。
--*********************************************************
-- 二百七十五 (L8): ModuleHint 下沉至 EtherFormPanel 共用 (原为本页私有逐字节拷贝),
-- 本页直接引用全局 ModuleHint, 不再本地定义。

--*********************************************************
--* 当前载具 (玩家所坐的载具, 任意座位)。
--*********************************************************
local function currentVehicle()
    local player = getPlayer();
    if not player then return nil, nil; end
    local vehicle = player:getVehicle();
    return player, vehicle;
end

--*********************************************************
--* 修理: 每件修至 95。回执与物品归属分别确认, 同时只允许一笔动作在途。
--* 等待超时只停止, 不按固定时间重发; 取消期间仍保留目标零件隔离。
--*********************************************************
local ETHER_REPAIR_TARGET = 95;
local ETHER_REPAIR_TICK_MS = 150;
local ETHER_REPAIR_ACTION_MAX_MS = 15000;
local ETHER_REPAIR_SYNC_MAX_MS = 10000;
local ETHER_REPAIR_JOB_MAX_MS = 1200000;
local ETHER_REPAIR_ACTION_TIME = 20;
EtherRepairJob = nil;

local function setRepairChecked(checked)
    local cbs = EtherVehiclePanel.checkboxByKey;
    local cb = cbs ~= nil and cbs["UI_VehiclePanel_RepairAuto"] or nil;
    if cb ~= nil then pcall(function() cb:setCheked(checked); end); end
end

local function endRepairJob()
    EtherRepairJob = nil;
    setRepairChecked(false);
end


--*********************************************************
--* 头顶浮动提示 (2026-09-14 九十一, 用户要求"每修完一件都像自动导航那样在人物头顶给出
--* 提示消耗了什么或者无消耗, 失败也要"): HaloTextHelper = 人物头顶浮字 (与「电台 XP」同款;
--* 自动导航用的是 player:Say 的聊天气泡 —— 修理每件一条会刷聊天栏, 浮字更合适)。
--* 绿 = 成功, 红 = 失败; 文案走 tr() + 三语键。
--*********************************************************
local function partDisplayName(part)
    local id = part and part:getId();
    if id == nil then return "?"; end
    local name = getText("IGUI_VehiclePart" .. id);   -- 原版部件名键 (IG_UI.json)
    if name == nil or name == "" then return id; end
    return name;
end

local function repairFloat(player, key, params, isError)
    if player == nil or HaloTextHelper == nil then return; end
    local color = isError and HaloTextHelper.getColorRed() or HaloTextHelper.getColorGreen();
    HaloTextHelper.addText(player, tr(key, params), "[col=175,175,175], [/]", color);
end

--*********************************************************
--* 两阶段结果: 原版动作完成回执 + 对应的部件/根背包更新。
--* 失败安装会移除旧物品再下发新对象; 不能拿原对象仍在背包当作失败回执。
--*********************************************************
local ETHER_REPAIR_MAX_TRIES = 80;

local function partHasInstall(part)
    local ok, tbl = pcall(function() return part:getTable("install") end);
    return ok and tbl ~= nil;
end

local function findDamagedPart(vehicle, skip)
    for i = 0, vehicle:getPartCount() - 1 do
        local part = vehicle:getPartByIndex(i);
        if part ~= nil and part:getCondition() < ETHER_REPAIR_TARGET then
            local id = part:getId();
            if id ~= nil and not skip[id] then
                return part;
            end
        end
    end
    return nil;
end

local function repairItemById(player, itemId)
    if itemId == nil then return nil; end
    local ok, item = pcall(function() return player:getInventory():getItemById(itemId) end);
    if ok and item ~= nil then return item; end
    return nil;
end

--* 安装成功率估算 (与 VehicleUtils.calculateInstallationSuccess 同式: 每项技能差扣 20+(需求-实际)*15)。
--* 载具安装表只写 Mechanics; 读不到需求时返回 nil (提示里就不显示百分比)。
local function installChance(part, player)
    local ok, tbl = pcall(function() return part:getTable("install") end);
    if not ok or tbl == nil then return nil; end
    local skills = tbl.skills;
    if skills == nil or skills == "" then return 100; end
    local _, _, reqText = string.find(tostring(skills), "Mechanics:(%d+)");
    local req = tonumber(reqText);
    if req == nil then return nil; end
    local okLvl, lvl = pcall(function() return player:getPerkLevel(Perks.Mechanics) end);
    if not okLvl or type(lvl) ~= "number" then return nil; end
    if lvl >= req then return 100; end
    local success = 100 - (20 + (req - lvl) * 15);
    if success < 0 then success = 0; end
    return success;
end

local function chanceText(chance)
    if chance == nil then return ""; end
    return "，成功率约 " .. tostring(chance) .. "%";
end

local function repairSkip(job, part, key)
    local id = part and part:getId() or "?";
    job.skip[id] = true;
    job.skipCount = job.skipCount + 1;
    if key ~= nil then repairFloat(job.player, key, { part = partDisplayName(part) }, false); end
end

local function repairAbort(job, key, quarantine)
    if quarantine then
        vehicleRepairQuarantine();
    elseif vehicleRepairRelease() ~= "released" then
        vehicleRepairQuarantine();
    end
    repairFloat(job.player, key, { part = partDisplayName(job.target) }, true);
    endRepairJob();
end

local function requestRepairStop(job, key)
    if job.stopping then return; end
    job.stopping = true;
    setRepairChecked(false);
    repairFloat(job.player, key, nil, true);
    if job.phase == nil then
        if vehicleRepairRelease() ~= "released" then vehicleRepairQuarantine(); end
        endRepairJob();
    end
end

local function repairItemInRoot(job)
    local item = repairItemById(job.player, job.itemId);
    if item == nil then return nil, false; end
    local inventory = job.player:getInventory();
    return item, item:getContainer() == inventory and inventory:getItemWithID(job.itemId) == item;
end

local function sameRepairItem(job, item)
    return item ~= nil and item:getID() == job.itemId and item:getFullType() == job.itemType;
end

local function finishRepairAction(job)
    if vehicleRepairFinishAction() ~= "ready" then
        repairAbort(job, "UI_VehiclePanel_RepairFloatUncertain", true);
        return false;
    end
    job.phase = nil;
    job.receiptAt = nil;
    return true;
end

local function releaseRepairTarget(job)
    if vehicleRepairRelease() ~= "released" then
        repairAbort(job, "UI_VehiclePanel_RepairFloatUncertain", true);
        return false;
    end
    job.target, job.itemId, job.itemType, job.sentItem = nil, nil, nil, nil;
    return true;
end

local function sendRepairAction(job, phase, item, nowMs)
    if job.stopping then return; end
    if job.attempts >= ETHER_REPAIR_MAX_TRIES then
        repairAbort(job, "UI_VehiclePanel_RepairFloatSkipped", false);
        return;
    end
    local result;
    if phase == "install" then
        if item == nil or item:getConditionMax() < ETHER_REPAIR_TARGET then
            repairAbort(job, "UI_VehiclePanel_RepairFloatConflict", false);
            return;
        end
        result = vehicleRepairSyncItemCondition(item, ETHER_REPAIR_TARGET);
        if result ~= "sent" then
            repairAbort(job, "UI_VehiclePanel_RepairFloatSyncFailed", false);
            return;
        end
        result = vehicleRepairSendInstall(job.target, item, ETHER_REPAIR_ACTION_TIME);
    else
        result = vehicleRepairSendUninstall(job.target, ETHER_REPAIR_ACTION_TIME);
    end
    if result ~= "sent" then
        repairAbort(job, "UI_VehiclePanel_RepairFloatSyncFailed", false);
        return;
    end
    job.phase = phase;
    job.sentItem = item;
    job.beforeCondition = job.target:getCondition();
    job.actionSentAt = nowMs;
    job.receiptAt = nil;
    job.attempts = job.attempts + 1;
end

local function repairTick()
    local job = EtherRepairJob;
    if job == nil then return; end
    local nowMs = getTimestampMs();
    if job.nextCheckMs ~= nil and nowMs < job.nextCheckMs then return; end
    job.nextCheckMs = nowMs + ETHER_REPAIR_TICK_MS;

    local session = vehicleRepairSessionState();
    if session == "offline" then endRepairJob(); return; end
    if session == "quarantined" then
        repairAbort(job, "UI_VehiclePanel_RepairFloatUncertain", true);
        return;
    end
    local player, vehicle = job.player, job.vehicle;
    if player == nil or player:isDead() or vehicle == nil or vehicle:getScript() == nil then
        requestRepairStop(job, "UI_VehiclePanel_RepairFloatAborted");
    elseif player:getVehicle() ~= vehicle then
        requestRepairStop(job, "UI_VehiclePanel_RepairFloatLeftVehicle");
    elseif nowMs > job.deadline then
        requestRepairStop(job, "UI_VehiclePanel_RepairFloatAborted");
    end
    if EtherRepairJob == nil then return; end

    if job.phase ~= nil then
        local receipt = vehicleRepairActionStatus();
        if receipt == "missing" or receipt == "idle" then
            repairAbort(job, "UI_VehiclePanel_RepairFloatUncertain", true);
            return;
        end
        if receipt == "rejected" then
            if finishRepairAction(job) then repairAbort(job, "UI_VehiclePanel_RepairFloatRejected", false); end
            return;
        end
        if receipt ~= "done" then
            if nowMs - job.actionSentAt > ETHER_REPAIR_ACTION_MAX_MS then
                repairAbort(job, "UI_VehiclePanel_RepairFloatUncertain", true);
            end
            return;
        end
        if job.receiptAt == nil then job.receiptAt = nowMs; end
        local part = job.target;
        local installed = part:getInventoryItem();
        local item, inRoot = repairItemInRoot(job);
        if (installed ~= nil and not sameRepairItem(job, installed))
                or (item ~= nil and (not sameRepairItem(job, item) or not inRoot)) then
            repairAbort(job, "UI_VehiclePanel_RepairFloatConflict", true);
            return;
        end

        if job.phase == "uninstall" then
            if installed == nil and inRoot then
                if not finishRepairAction(job) then return; end
                if job.stopping then
                    if releaseRepairTarget(job) then endRepairJob(); end
                else
                    job.attempts = 0;
                    sendRepairAction(job, "install", item, nowMs);
                end
                return;
            end
            if sameRepairItem(job, installed) and item == nil and part:getCondition() < job.beforeCondition then
                if not finishRepairAction(job) then return; end
                if job.stopping then
                    if releaseRepairTarget(job) then endRepairJob(); end
                else
                    repairFloat(player, "UI_VehiclePanel_RepairFloatUninstallFail",
                        { part = partDisplayName(part), cond = part:getCondition(), n = job.attempts }, true);
                    sendRepairAction(job, "uninstall", installed, nowMs);
                end
                return;
            end
        else
            if sameRepairItem(job, installed) and item == nil then
                if not finishRepairAction(job) then return; end
                if part:getCondition() ~= ETHER_REPAIR_TARGET then
                    repairAbort(job, "UI_VehiclePanel_RepairFloatConflict", false);
                    return;
                end
                job.steps = job.steps + 1;
                repairFloat(player, "UI_VehiclePanel_RepairFloatPartDone",
                    { part = partDisplayName(part), to = part:getCondition() }, false);
                if releaseRepairTarget(job) and job.stopping then endRepairJob(); end
                return;
            end
            -- 安装失败会下发新物品对象; 原引用仍在 = 更新尚未到齐, 不是重试许可。
            if installed == nil and inRoot and item ~= job.sentItem then
                if not finishRepairAction(job) then return; end
                if job.stopping then
                    if releaseRepairTarget(job) then endRepairJob(); end
                else
                    repairFloat(player, "UI_VehiclePanel_RepairFloatInstallFail",
                        { part = partDisplayName(part), cond = item:getCondition(), n = job.attempts }, true);
                    sendRepairAction(job, "install", item, nowMs);
                end
                return;
            end
        end
        if nowMs - job.receiptAt > ETHER_REPAIR_SYNC_MAX_MS then
            repairAbort(job, "UI_VehiclePanel_RepairFloatUncertain", true);
        end
        return;
    end

    if job.stopping then
        if releaseRepairTarget(job) then endRepairJob(); end
        return;
    end
    local part = findDamagedPart(vehicle, job.skip);
    if part == nil then
        repairFloat(player, "UI_VehiclePanel_RepairFloatFinished",
            { done = job.steps, skipped = job.skipCount }, false);
        endRepairJob();
        return;
    end
    local id = part:getId();
    if id == "GasTank" or id == "lightbar" then
        repairSkip(job, part, "UI_VehiclePanel_RepairFloatExcluded");
        return;
    end
    if not partHasInstall(part) then repairSkip(job, part); return; end
    local item = part:getInventoryItem();
    if item == nil or item:getConditionMax() < ETHER_REPAIR_TARGET then repairSkip(job, part); return; end
    local claim = vehicleRepairClaim(part);
    if claim == "unsupported" then repairSkip(job, part); return; end
    if claim ~= "ready" then
        repairAbort(job, "UI_VehiclePanel_RepairFloatUncertain", true);
        return;
    end
    job.target = part;
    job.itemId = item:getID();
    job.itemType = item:getFullType();
    job.attempts = 0;
    repairFloat(player, "UI_VehiclePanel_RepairFloatWorking",
        { part = partDisplayName(part), cond = part:getCondition(), extra = chanceText(installChance(part, player)) }, false);
    sendRepairAction(job, "uninstall", item, nowMs);
end
Events.OnTick.Add(repairTick);

local function repairStop()
    if EtherRepairJob ~= nil then
        requestRepairStop(EtherRepairJob, "UI_VehiclePanel_RepairFloatStopping");
    else
        setRepairChecked(false);
    end
end

local function repairStart()
    if EtherRepairJob ~= nil then
        setRepairChecked(not EtherRepairJob.stopping);
        return;
    end
    local player = getPlayer();
    if player == nil then endRepairJob(); return; end
    if not isMultiplayer() or type(vehicleRepairSessionState) ~= "function" then
        endRepairJob();
        return;
    end
    if vehicleRepairSessionState() ~= "ready" then
        repairFloat(player, "UI_VehiclePanel_RepairFloatUncertain", nil, true);
        setRepairChecked(false);
        return;
    end
    local vehicle = player:getVehicle();
    if vehicle == nil or not vehicle:isDriver(player) or not vehicle:isStopped() then
        endRepairJob();
        return;
    end
    local nowMs = getTimestampMs();
    EtherRepairJob = {
        player = player, vehicle = vehicle, deadline = nowMs + ETHER_REPAIR_JOB_MAX_MS,
        nextCheckMs = nowMs, steps = 0, skip = {}, skipCount = 0, stopping = false,
    };
    setRepairChecked(true);
end

--*********************************************************
--* 立即启动: 用身上任意一把车钥匙插入目标车点火口, 再由服务端启动。
--*********************************************************
local function startEngineNow()
    local player, vehicle = currentVehicle();
    if not vehicle then return; end
    if type(vehicleIgnitionStart) == "function" then
        local result = vehicleIgnitionStart();
        if result ~= "sent" then
            print("[VehiclePanel] any-key ignition unavailable: " .. tostring(result));
        end
        return;
    end
    sendClientCommand(player, "vehicle", "startEngine", { haveKey = true });
end

--* (2026-09-14 九十七 用户裁定) C3 探针按钮与探针逻辑**已移除** —— 它的历史使命 (验证原生物理体
--* 是否随动) 已完成, C3 正式功能改由地图右键「汽车传送」承担 (UIMap 车辆档状态机)。
--* Java 侧读数原语 vehicleNativeProbeRead 保留 (SelfProbe 的 C3-hop 自检仍在用, 排障也要它)。

--*********************************************************
--* 模块内一行复选框的高度预算 (与 EtherVisualsPanel.rowStep 同一套规则)。
--*********************************************************
local function rowStep(key, w)
    local title = tr(key);
    local availW = w - (18 + 10 + 8);
    if getTextManager():MeasureStringX(UIFont.Small, title) > availW then
        local n = #EtherTheme.wrapText(title, availW, UIFont.Small);
        return n * (EtherTheme.fontHgtSmall + 2) + EtherFormPanel.BOX_PAD_Y * 2 + 4;
    end
    return EtherFormPanel.ROW_STEP;
end

--*********************************************************
--* 模块内摆放一行复选框, 返回行距与控件实例 (保护开关需对外暴露给
--* ServerSyncBlocker.lua 调 :setCheked)。
--*********************************************************
local function placeCheckbox(panel, key, getState, onToggle, x, y, w, onlyInGame)
    local cb = UICheckbox:new(x, y, tr(key), getState() and true or false, onToggle);
    panel:addWidget(cb, { onlyInGame = onlyInGame == true });
    return rowStep(key, w), cb;
end

--*********************************************************
--* 模块内按钮组: 按组宽与内容宽自适应 —— 放得下同排, 放不下竖排。
--* 高度预算与摆放共用同一套判定 (buttonRowHeight)。
--*********************************************************
local function buttonRowHeight(innerW, titles)
    if #titles == 0 then return 0; end
    local gap = EtherTheme.ctrlGap;
    local btnW = UIButton.measureGroupWidth(titles);
    local ctrlH = EtherTheme.ctrlH;
    if btnW * #titles + gap * (#titles - 1) <= innerW then
        return ctrlH;
    end
    return #titles * ctrlH + (#titles - 1) * 4;
end

local function placeButtonRow(panel, bx, by, innerW, titles, callbacks)
    if #titles == 0 then return 0; end
    local gap = EtherTheme.ctrlGap;
    local btnW = UIButton.measureGroupWidth(titles);
    local ctrlH = EtherTheme.ctrlH;
    if btnW * #titles + gap * (#titles - 1) <= innerW then
        local x = bx;
        for i = 1, #titles do
            local btn = UIButton:new(x, by, btnW, ctrlH, titles[i], callbacks[i], btnW);
            panel:addWidget(btn, { onlyInGame = true });
            x = x + btnW + gap;
        end
        return ctrlH;
    end
    local w = math.min(btnW, innerW);
    local y = by;
    for i = 1, #titles do
        local btn = UIButton:new(bx, y, w, ctrlH, titles[i], callbacks[i], w);
        panel:addWidget(btn, { onlyInGame = true });
        y = y + ctrlH + 4;
    end
    return y - by - 4;
end

--*********************************************************
--* 构建表单内容 (基类 createChildren 回调): 两个功能模块。
--*********************************************************
function EtherVehiclePanel:build()
    -- 复选框实例表 (key -> widget), 供 ServerSyncBlocker.lua 回调 :setCheked
    EtherVehiclePanel.checkboxByKey = {};
    local modules = {
        {
            title = "UI_VehiclePanel_Group_Engine",
            checkboxes = {
                -- 自动重试: 每秒补发 startEngine 直到引擎转起来;
                -- 成功后由 ServerSyncBlocker 自动取消勾选 (toggleVehicleInstantStart(false))
                { key = "UI_VehiclePanel_InstantStart",
                  get = function() return isVehicleInstantStart and isVehicleInstantStart() or false; end,
                  on = function(c) toggleVehicleInstantStart(c); end },
            },
            buttons = {
                { key = "UI_VehiclePanel_StartNow", fn = startEngineNow },
            },
            hint = "UI_VehiclePanel_EngineHint",
        },
        {
            title = "UI_VehiclePanel_Group_Service",
            checkboxes = {
                -- 自动修理 (一百七十六): 驾驶位勾选即开工 (自动下车逐件修),
                -- 取消勾选立刻停; 修完/超时/卡住自动取消勾选
                { key = "UI_VehiclePanel_RepairAuto", onlyInGame = true,
                  get = function() return EtherRepairJob ~= nil; end,
                  on = function(c)
                      if c then repairStart() else repairStop() end
                  end },
            },
            buttons = {},
            hint = "UI_VehiclePanel_Hint",
        },
    };

    for mi = 1, #modules do
        if mi > 1 then
            self:addSpacer(EtherFormPanel.SECTION_GAP);
        end
        local mod = modules[mi];
        local w = self:_rowContentW();
        local innerW = w - EtherFormPanel.BOX_PAD_X * 2;

        -- 高度预算 (与摆放共用同一套判定)
        local contentH = 0;
        if mod.checkboxes then
            for i = 1, #mod.checkboxes do
                contentH = contentH + rowStep(mod.checkboxes[i].key, innerW) + 2;
            end
            contentH = contentH + 4;
        end
        local titles = {};
        for i = 1, #mod.buttons do titles[i] = tr(mod.buttons[i].key); end
        contentH = contentH + buttonRowHeight(innerW, titles);
        if mod.hint ~= nil then
            contentH = contentH + 6
                + #EtherTheme.wrapHint(tr(mod.hint), innerW - 8) * EtherTheme.fontHgtHint + 2;
        end

        self:addModule(mod.title, contentH + 2, function(bx, by, bw)
            local ix = bx + EtherFormPanel.BOX_PAD_X;
            local iW = bw - EtherFormPanel.BOX_PAD_X * 2;
            local cy = by;
            if mod.checkboxes then
                for i = 1, #mod.checkboxes do
                    local step, cb = placeCheckbox(self, mod.checkboxes[i].key, mod.checkboxes[i].get,
                        mod.checkboxes[i].on, ix, cy, iW, mod.checkboxes[i].onlyInGame);
                    -- 复选框实例对外暴露: ServerSyncBlocker.lua 成功/授权后会调 :setCheked(false)
                    EtherVehiclePanel.checkboxByKey[mod.checkboxes[i].key] = cb;
                    cy = cy + step + 2;
                end
                cy = cy + 4;
            end
            cy = cy + placeButtonRow(self, ix, cy, iW, titles, (function()
                local fns = {};
                for i = 1, #mod.buttons do fns[i] = mod.buttons[i].fn; end
                return fns;
            end)());
            if mod.hint ~= nil then
                local hint = ModuleHint:new(ix, cy + 6, iW, tr(mod.hint));
                hint:initialise();
                hint:instantiate();
                self:_anchor(hint);
                self:addChild(hint);
            end
        end);
    end

    -- 自动驾驶模块 (EtherDrive 域, 独立文件 EtherDriveModule.lua 单一源;
    -- 用户定夺 2026-09-05: 放载具页不设独立驾驶 Tab, 研判 §八 UI 设计同步修正)
    if type(EtherDriveModule_addTo) == "function" then
        self:addSpacer(EtherFormPanel.SECTION_GAP);
        EtherDriveModule_addTo(self);
    end

    -- 战斗攻击模块 (2026-09-08): 导航期间三项恒开, 开关只控制手动驾驶
    if type(EtherDriveCombatModule_addTo) == "function" then
        self:addSpacer(EtherFormPanel.SECTION_GAP);
        EtherDriveCombatModule_addTo(self);
    end
end

--*********************************************************
--* :new / :createChildren / :prerender / :render / :onMouseWheel
--* 全部继承自 EtherFormPanel, 无需重写。
--*********************************************************
