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
--* 修理 (2026-11-09 改): 直发原版网络计时动作, 不走本地动作队列、不走位、不下车。
--*
--* 为什么能整车修: b42.22 的 ISUninstallVehiclePart 成功拆卸只把部件物品摘下来放进背包,
--*   **不扣部件条件**; 拆下的物品在背包里(可解析容器), 用 vehicleRepairSyncItemCondition
--*   把耐久拉满(服务端 SyncItemFields 直接采用客户端给的值); 再 ISInstallVehiclePart 装回,
--*   服务端 complete 会把部件条件写成物品耐久 → 100, 且原零件(刹车力/胎压/悬挂/电瓶电量/
--*   油量/消声)全部保留。
--* 失败代价: 拆卸失败扣部件耐久 5-10、安装失败扣物品耐久 5-10, 都可重试; 装回成功时部件
--*   条件被覆盖成 100, 所以技能只影响尝试次数, 不影响结果。
--* 服务端不调用 isValid(ActionManager.start 不调, NetTimedAction.isValid 恒 false), 所以
--*   不需要下车/走位/把零件放进背包; 客户端只负责发包与轮询部件状态。
--* 引擎/后备箱容器/手套箱/加热器/乘客舱没有 table install, 拆装链覆盖不到, 直接跳过;
--*   灯光条没有安装表, 走原版 ISRepairLightbar(不看技能), 过账件仍取本车容器物品。
--*********************************************************
local ETHER_REPAIR_TICK_MS = 150;        -- 状态机节流
local ETHER_REPAIR_STEP_MAX_MS = 1200;   -- 单个阶段(拆/装/灯光条一发)等待上限
local ETHER_REPAIR_JOB_MAX_MS = 600000;  -- 整单上限
--* 每个网络动作的时长 (maxTime, 20/s): 20 = 0.4 秒/发。服务端只按这个值算 endTime, 不校验。
--* 失败重试只扣物品耐久, 拉满即可, 所以时长只影响总耗时。
local ETHER_REPAIR_ACTION_TIME = 20;
EtherRepairJob = nil;                    -- 当前修理单 (状态机数据)

--* 收尾 (一百七十六): 单一出口清单 + 自动取消勾选。状态机所有终止点
--* (完成/超时/卡住/无件/被踢/玩家车没了) 一律走这里, 复选框与任务状态
--* 永远一致: 勾选 = 有单在跑, 未勾选 = 没单。
local function endRepairJob()
    EtherRepairJob = nil;
    local cbs = EtherVehiclePanel.checkboxByKey;
    local cb = cbs ~= nil and cbs["UI_VehiclePanel_RepairAuto"] or nil;
    if cb ~= nil then pcall(function() cb:setCheked(false); end); end
end
--* 取"过账件": 服务端 complete() 对该件只做 inventory:Remove(item) ——
--* 若该件**不在玩家背包**里, Remove 是**空操作** (ItemContainer.Remove:1940 遍历不到即返回,
--* 无任何副作用) → 条件照修、东西不消耗。
--* 能被服务端解析的前提 = 该物品有个能编码的容器 (PZNetKahluaTableImpl.saveInventoryItem
--* → ContainerID)。**可行**: 载具部件容器 (ContainerType.Vehicle —— VehicleParts:304 建的容器
--* parent 就是载具 ✓)、物件容器/箱柜 (ObjectContainer/IsoObject ✓)、背包与背包内袋 ✓。
--* **不可行 (实测取证)**: ① 装在部件上的那个件 —— VehiclePart.setInventoryItem:170 只存
--* this.item, 没有容器; ② **地上的物品** —— IsoWorldInventoryObject 构造里就
--* item.setContainer(null) (:82), 落地即无容器 → 服务端一律解析成 null → complete 直接
--* return false 且不报错 (菜单提示 "No packet-resolvable item found outside player
--* root inventories" 就是踩的这类坑)。
--* 优先级 (2026-09-14 九十一 用户裁定): ① 本车后备箱 (TruckBed/TruckBedOpen/TrailerTrunk) →
--*   ② 本车手套箱 (GloveBox) → ③ 本车座椅容器 (Seat*) —— 全部**免费**。
--*   不再使用周围车辆容器/附近箱柜/背包; 本车内一件都没有 = 停止并头顶红字提示。
--* (部件 ID 取自游戏 vehicle 模板: template_trunk.txt / template_glovebox.txt / template_seat.txt)
local TRUNK_TOKEN_PARTS = { TruckBed = true, TruckBedOpen = true, TrailerTrunk = true };

local function tokenInVehicleParts(vehicle, match)
    for i = 0, vehicle:getPartCount() - 1 do
        local part = vehicle:getPartByIndex(i);
        local partId = part and part:getId();
        if partId ~= nil and match(partId) then
            local container = part:getItemContainer();
            if container ~= nil then
                local items = container:getItems();
                if items ~= nil and items:size() > 0 then
                    return items:get(0), partId;
                end
            end
        end
    end
    return nil, nil;
end

local function repairTokenFor(vehicle)
    local item, partId = tokenInVehicleParts(vehicle,
        function(id) return TRUNK_TOKEN_PARTS[id] == true; end);
    if item ~= nil then return item, partId; end
    item, partId = tokenInVehicleParts(vehicle, function(id) return id == "GloveBox"; end);
    if item ~= nil then return item, partId; end
    return tokenInVehicleParts(vehicle, function(id) return string.sub(id, 1, 4) == "Seat"; end);
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
--* 状态机 (2026-11-09 改): 直发原版网络计时动作, 不走本地动作队列、不走位、不下车。
--* 每件三步: 拆(ISUninstallVehiclePart) → 把拆下的零件耐久拉满(vehicleRepairSyncItemCondition)
--* → 装回(ISInstallVehiclePart)。安装/拆卸失败只扣物品耐久, 拉满再试; 装回成功时部件条件
--* 被写成物品耐久, 且原零件(刹车力/胎压/悬挂/电瓶电量/油量/消声)全部保留。
--* 灯光条没有安装表, 走 ISRepairLightbar(原版动作不看技能)。
--* 服务端不调用 isValid, 所以不需要下车、走位或把零件放进背包。
--*********************************************************
local ETHER_REPAIR_MAX_TRIES = 80;        -- 单件最多重试 (车门 20%/次, 80 次失败概率可忽略)

local function partHasInstall(part)
    local ok, tbl = pcall(function() return part:getTable("install") end);
    return ok and tbl ~= nil;
end

local function findDamagedPart(vehicle, skip)
    for i = 0, vehicle:getPartCount() - 1 do
        local part = vehicle:getPartByIndex(i);
        if part ~= nil and part:getCondition() < 100 then
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
    job.target, job.phase = nil, nil;
    if key ~= nil then
        repairFloat(job.player, key, { part = partDisplayName(part) }, true);
    end
    print("[VehiclePanel] skip " .. tostring(id) .. " (" .. tostring(key or "unsupported") .. ")");
end

local function repairTick()
    local job = EtherRepairJob;
    if job == nil then return; end
    local nowMs = getTimestampMs();
    if job.nextCheckMs ~= nil and nowMs < job.nextCheckMs then return; end
    job.nextCheckMs = nowMs + ETHER_REPAIR_TICK_MS;

    local player, vehicle = job.player, job.vehicle;
    job.tries = job.tries or 0;
    job.sends = job.sends or 0;
    if player == nil or player:isDead() or vehicle == nil or vehicle:getScript() == nil then
        print("[VehiclePanel] repair job ended (player or vehicle gone)");
        endRepairJob();
        return;
    end
    if nowMs > job.deadline then
        repairFloat(player, "UI_VehiclePanel_RepairFloatAborted", nil, true);
        print("[VehiclePanel] repair job timeout - " .. tostring(job.steps) .. " step(s) done");
        endRepairJob();
        return;
    end

    -- 拆卸阶段: 等部件物品变 nil
    if job.phase == "uninstall" then
        local part = job.target;
        if part == nil then job.phase = nil; return; end
        if part:getInventoryItem() == nil then
            job.phase = "install";
            job.tries = 0;
            job.sends = 0;
            -- 给服务端的 AddItem 回包留一点时间, 但不空等整个重试窗口
            job.phaseDeadline = nowMs + 400;
            return;
        end
        if nowMs < job.phaseDeadline then return; end
        if job.tries >= ETHER_REPAIR_MAX_TRIES then
            repairSkip(job, part);
            return;
        end
        if job.sends > 0 then
            repairFloat(player, "UI_VehiclePanel_RepairFloatUninstallFail",
                { part = partDisplayName(part), cond = part:getCondition(), n = job.sends }, true);
            print("[VehiclePanel] uninstall failed: " .. tostring(part:getId())
                .. " cond " .. string.format("%.0f", part:getCondition()) .. " (try " .. tostring(job.sends) .. ")");
        end
        job.tries = job.tries + 1;
        job.sends = job.sends + 1;
        vehicleRepairSendUninstall(part, ETHER_REPAIR_ACTION_TIME);
        job.phaseDeadline = nowMs + ETHER_REPAIR_STEP_MAX_MS;
        return;
    end

    -- 安装阶段: 等部件条件变 100 且物品回来
    if job.phase == "install" then
        local part = job.target;
        if part == nil then job.phase = nil; return; end
        if part:getInventoryItem() ~= nil and part:getCondition() >= 100 then
            repairFloat(player, "UI_VehiclePanel_RepairFloatPartDone",
                { part = partDisplayName(part), to = part:getCondition() }, false);
            print("[VehiclePanel] repaired " .. tostring(part:getId()) .. " -> 100");
            job.steps = job.steps + 1;
            job.target, job.phase = nil, nil;
            return;
        end
        if nowMs < job.phaseDeadline then return; end
        local item = repairItemById(player, job.itemId);
        if item == nil then
            -- 背包满时拆卸会把零件丢到地上, 找不到就不再碰这件
            repairSkip(job, part, "UI_VehiclePanel_RepairFloatNoPartItem");
            return;
        end
        if job.tries >= ETHER_REPAIR_MAX_TRIES then
            repairSkip(job, part);
            return;
        end
        if job.sends > 0 then
            repairFloat(player, "UI_VehiclePanel_RepairFloatInstallFail",
                { part = partDisplayName(part), cond = item:getCondition(), n = job.sends }, true);
            print("[VehiclePanel] install failed: " .. tostring(part:getId())
                .. " item cond " .. string.format("%.0f", item:getCondition()) .. " (try " .. tostring(job.sends) .. ")");
        end
        job.tries = job.tries + 1;
        job.sends = job.sends + 1;
        -- 先把耐久拉满再装 (失败时服务端只扣物品耐久, 下一轮再拉满)
        -- conditionMax 为 0 的异常物品不写, 否则会把部件装成 0 耐久
        local maxCond = item:getConditionMax();
        if type(vehicleRepairSyncItemCondition) == "function" and maxCond ~= nil and maxCond > 0 then
            vehicleRepairSyncItemCondition(item, maxCond);
        end
        vehicleRepairSendInstall(part, item, ETHER_REPAIR_ACTION_TIME);
        job.phaseDeadline = nowMs + ETHER_REPAIR_STEP_MAX_MS;
        return;
    end

    -- 灯光条: 原版动作逐发加耐久 (15 + 机械学 - 引擎修理等级), 过账件取自本车容器
    if job.phase == "lightbar" then
        local part = job.target;
        if part == nil then job.phase = nil; return; end
        if part:getCondition() >= 100 then
            repairFloat(player, "UI_VehiclePanel_RepairFloatPartDone",
                { part = partDisplayName(part), to = part:getCondition() }, false);
            job.steps = job.steps + 1;
            job.target, job.phase = nil, nil;
            return;
        end
        if nowMs < job.phaseDeadline then return; end
        if job.tries >= ETHER_REPAIR_MAX_TRIES then
            repairSkip(job, part);
            return;
        end
        local token = repairTokenFor(vehicle);
        if token == nil then
            repairSkip(job, part, "UI_VehiclePanel_RepairFloatNoToken");
            return;
        end
        job.tries = job.tries + 1;
        vehicleRepairSendLightbar(part, token, ETHER_REPAIR_ACTION_TIME);
        job.phaseDeadline = nowMs + ETHER_REPAIR_STEP_MAX_MS;
        return;
    end

    -- 挑下一件
    local part = findDamagedPart(vehicle, job.skip);
    if part == nil then
        repairFloat(player, "UI_VehiclePanel_RepairFloatFinished",
            { done = job.steps, skipped = job.skipCount }, false);
        print("[VehiclePanel] repair finished (" .. tostring(job.steps) .. " repaired, "
            .. tostring(job.skipCount) .. " skipped)");
        endRepairJob();
        return;
    end

    local id = part:getId();
    if id == "lightbar" then
        job.target, job.phase, job.tries, job.sends = part, "lightbar", 0, 0;
        job.phaseDeadline = nowMs + ETHER_REPAIR_STEP_MAX_MS;
        repairFloat(player, "UI_VehiclePanel_RepairFloatWorking",
            { part = partDisplayName(part), cond = part:getCondition(), extra = "" }, false);
        return;
    end
    if not partHasInstall(part) then
        -- 引擎/后备箱容器/手套箱/加热器/乘客舱: 没有安装表, 拆装链覆盖不到
        repairSkip(job, part);
        return;
    end
    local item = part:getInventoryItem();
    if item == nil then
        -- 部件已经空着(例如被打碎的车窗): 没有可拆的零件
        repairSkip(job, part);
        return;
    end

    job.target = part;
    job.itemId = item:getID();
    job.tries = 1;
    job.sends = 1;
    job.phase = "uninstall";
    job.phaseDeadline = nowMs + ETHER_REPAIR_STEP_MAX_MS;
    local chance = installChance(part, player);
    repairFloat(player, "UI_VehiclePanel_RepairFloatWorking",
        { part = partDisplayName(part), cond = part:getCondition(), extra = chanceText(chance) }, false);
    print("[VehiclePanel] repair " .. tostring(id) .. " cond "
        .. string.format("%.0f", part:getCondition()) .. " (uninstall, chance "
        .. tostring(chance == nil and "?" or chance) .. "%)");
    vehicleRepairSendUninstall(part, ETHER_REPAIR_ACTION_TIME);
end
Events.OnTick.Add(repairTick);

--* 修理启停 (一百七十六: 按钮改复选框; 2026-11-09 改直发动作) ——
--*   勾选 = 必须坐在驾驶位、车停稳才开工 (否则拒绝并回弹未勾选); 开工后**不下车**,
--*   逐件直发拆/装动作并轮询部件状态;
--*   取消勾选 = 立刻停 (清队列 + 收尾); 修完/超时/被踢一律自动取消勾选。
local function repairStop()
    if EtherRepairJob ~= nil then
        if type(ISTimedActionQueue) == "table" then
            ISTimedActionQueue.clear(EtherRepairJob.player);
        end
        print("[VehiclePanel] repair job cancelled");
    end
    endRepairJob();
end

local function repairStart()
    local player = getPlayer();
    if player == nil then endRepairJob(); return; end
    -- 拆装链依赖服务端跑 complete; 单机没有网络动作, 直接拒绝而不是空转
    if not isMultiplayer() then
        print("[VehiclePanel] repair chain requires multiplayer (server-side actions)");
        endRepairJob();
        return;
    end
    local vehicle = player:getVehicle();
    if vehicle == nil or not vehicle:isDriver(player) then
        print("[VehiclePanel] sit in the driver seat first");
        endRepairJob();
        return;
    end
    if not vehicle:isStopped() then
        print("[VehiclePanel] wait for the vehicle to stop before repairing");
        endRepairJob();
        return;
    end
    local nowMs = getTimestampMs();
    EtherRepairJob = {
        player = player,
        vehicle = vehicle,
        deadline = nowMs + ETHER_REPAIR_JOB_MAX_MS,
        nextCheckMs = nowMs,
        steps = 0,
        skip = {},
        skipCount = 0,
    };
    print("[VehiclePanel] repair job started (uninstall -> reset item condition -> reinstall)");
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
