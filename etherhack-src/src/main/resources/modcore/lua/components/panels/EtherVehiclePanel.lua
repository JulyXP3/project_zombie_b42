require "ISUI/ISPanel"

--*********************************************************
--* EtherVehiclePanel: 载具操作页
--*
--* 通道: 「修理车辆」走**原版计时动作** ISRepairLightbar 状态机 (B1 四修, 2026-09-14
--* 八十五, PienZ 同款: 服务端权威执行 + 不进 cmd 日志, 默认不消耗物品 —— 只用**本车**容器里的
--* 物品当"过账件", 优先级 后备箱 → 手套箱 → 座椅 (九十一 用户裁定, 见 §修理); 每步结果
--* (成功/失败) 走头顶浮动提示;
--* 其余通道是 vanilla 服务器车辆指令 (media/lua/server/Vehicles/
--* VehicleCommands.lua, 无权限校验, 已逐行核实):
--*   - startEngine (:14-30): 只查 isDriver + 客户端上传的 haveKey,
--*     "引擎可用"检查被 `if true or` 短路成恒真;
--*   - fixPart (:32-62): 直接设 part/item condition 并三连 transmit,
--*     全程无 checkPermissions —— 对照 setPartCondition (:66) 需要
--*     UseMechanicsCheat 的权限不对称即本页的利用点;
--*   - setContainerContentAmount (:80-93): 任意 part 内容量直设。
--* 引擎能否真启动仍由服务端 tryStartEngine 判定 (油/电/损坏), 与原版一致。
--* "无条件启动引擎(自动重试)": 坐驾驶座且引擎未转时每秒补发 startEngine,
--*   成功即自动取消勾选; 取消勾选立即停止重试。
--* "无条件启动引擎(单次)": 手动单发一次 startEngine。
--* "车辆无条件短接": ServerSyncBlocker 下行保护 (tryHotwire/tryStartEngine
--*   安装期 ASM 注入), 引擎启动成功即自动回弹 (30s 仅作起不来时的兜底)。
--* 全部操作需坐在载具内 (玩家当前载具), 非驾驶座也可 (startEngine 由
--* 服务端再查 isDriver, 无效只产生一条 noise 日志)。
--*********************************************************

EtherVehiclePanel = EtherFormPanel:derive("EtherVehiclePanel");

--*********************************************************
--* 模块内说明行 (hint 缩放文字, 与 EtherCharacterPanel 的 ModuleHint 同款):
--* 做成子控件随滚动统一定位, 不吞鼠标事件。折行在构造时算一次。
--*********************************************************
local ModuleHint = ISPanel:derive("EtherVehicleHint");

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
--* 当前载具 (玩家所坐的载具, 任意座位)。
--*********************************************************
local function currentVehicle()
    local player = getPlayer();
    if not player then return nil, nil; end
    local vehicle = player:getVehicle();
    return player, vehicle;
end

--*********************************************************
--* 修理: 两个按钮语义不同 (2026-09-14 八十五起) ——
--*   「修理车辆」= 原版计时动作状态机 (ISRepairLightbar, **服务端权威执行**,
--*     不进 cmd 日志); **默认免费** (过账件只取**本车**容器里的物品: 后备箱 → 手套箱 →
--*     座椅; 服务端对不在玩家背包里的件做 Remove 是空操作); 每步成功/失败在人物头顶
--*     浮动提示 (消耗了什么/未消耗); 再点一次 = 取消。
--*   「直发修理」= 一次性发一轮 vehicle.fixPart (每损坏部件 1 条, 条件直设 100,
--*     **不消耗物品**); 该命令在默认 ClientCommandFilter 白名单里 = 每件一行服务端
--*     cmd 日志, 按钮文案已标注。
--* 技能差 ≤ 0 时计时路线整体放弃 (原版公式会倒扣 condition), 提示改直发。
--* 注: 排队时那串 "ISRepairLightbar" LOG 是原版构造函数自带的调试打印, 每步一条, 无害。
--*********************************************************
local function repairVehicleDirectNow()
    local player, vehicle = currentVehicle();
    if not vehicle then
        print("[VehiclePanel] sit inside the vehicle first");
        return;
    end
    for i = 0, vehicle:getPartCount() - 1 do
        local part = vehicle:getPartByIndex(i);
        if part and part:getInventoryItem() then
            sendClientCommand(player, "vehicle", "fixPart", {
                vehicle = vehicle:getId(), part = part:getId(),
                condition = 100, haveBeenRepaired = 1 });
        end
    end
end

--*********************************************************
--* 修理 (B1 四修, 2026-09-14 八十五, 用户指示"按 PienZ 的来")
--* = **原版计时动作路线** (PienZ.dll 的 VehicleRepair 同款做法):
--*   逐件排队**原版** ISRepairLightbar —— 原版 complete() 对任意 VehiclePart 做
--*   setCondition(+15+机械学-引擎修理等级) 并调 transmitPartCondition 同步; MP 下该动作
--*   被 LuaTimedActionNew 判为**服务端权威** (它定义了 complete → useCustomRemoteTimedActionSync
--*   = false → start() 里 createNetTimedAction), 由**服务端**跑它自己的 vanilla 副本 →
--*   条件真正写进服务端并被广播 (队友可见、不会被下一次同步打回), 且**不进 cmd 日志**
--*   (对比「直发修理」走 vehicle.fixPart 客户命令: 服务端权威但每件一行 cmd 日志)。
--*
--* 与前几轮失败方案的两点关键区别:
--*   ① **一步只排"走位 + 本件修理"两个动作** (九十四: 走位 = 原版
--*      ISPathFindAction:pathToVehicleArea, 见 repairStep), 不预排整条链 —— 原版
--*      ISBaseTimedAction:stop() 会 resetQueue 清空整个队列, 预排必被一锅端 (七十六教训);
--*      中途被打断只损失当前这一件, 下一 tick 重新挑需要修的部件继续 (自愈);
--*   ② 不自研动作类 —— 自研类没有 complete → MP 下变成纯客户端动作 → 客户端改的
--*      condition **同步不到服务端** (BaseVehicle.transmitPartCondition:7287 开头即
--*      if (!GameServer.server) return; 客户端调用是空操作), 那是"看着修好实际没修"的根因。
--*
--* **消耗 (2026-09-14 九十一 用户裁定)**: 默认**不消耗玩家的东西** —— 服务端 complete() 只对
--*   self.item 做 inventory:Remove(item), 只要这个"过账件"不在玩家背包里, 该调用就是
--*   空操作 (ItemContainer.Remove:1940 遍历不到即返回, 无副作用) → 条件照修、物品不消耗。
--*   本实现只拿**本车容器**的物品当过账件: 后备箱 (TruckBed/TruckBedOpen) → 手套箱
--*   (GloveBox) → 座椅 (Seat*); 不再使用周围车辆/附近箱柜/背包, 见 repairTokenFor 的注释。
--* 技能差 ≤0 / 本车内一件物品都没有 / 中途被打断不生效 → 头顶红字提示并中止本单。
--*********************************************************
local ETHER_REPAIR_TICK_MS = 250;        -- 状态机节流 (每帧检查, 250ms 才动手)
local ETHER_REPAIR_STEP_MAX_MS = 60000;  -- 单件动作卡住上限
local ETHER_REPAIR_JOB_MAX_MS = 600000;  -- 整单上限 (整车重损约上百步)
--* 每件修理的动作时长 (单位 = 原版动作 tick, 20/s; 40 = 2.0 秒/件)。
--* 这个值是 ISRepairLightbar:new(character, part, item, maxTimeInit) 的第 4 参, 取证链:
--*   原版把 maxTimeInit 同时存进 o.maxTimeInit / o.maxTime (ISRepairLightbar.lua:94-95)
--*   → NetTimedAction.set 按 new 的**参数名**把它当动作参数一起发上行 (NetTimedAction.java:42-58)
--*   → 服务端照原样重建动作 (NetTimedAction.parse:145-171) 并拿它当自己的时长
--*     (Action.setTimeData:35-39 → NetTimedAction.getDuration:74-98)。**服务端不校验、不钳制**。
--* 档位取舍 (2026-09-14 九十四, 用户: "放缓读条做安全冗余"):
--*   200 = 原版 (4.0 秒/件; mood/疼痛/体温系数只在 >1 时相乘, ISBaseTimedAction.lua:99-123);
--*   40  = 本档: 原版一半, 单步端到端 ~2.3 秒 ≈ 26 件/分 (原版上限 15 件/分), 工作动画能播完;
--*   1   = 九十二的"秒修": 单步 ~0.3 秒 = 200 件/分, 工作动画一帧即被打断 —— 不要在他人服使用。
--* 依据 (为何回调): 官方反作弊 (zombie/network/anticheats 24 项) 无一读动作时长, 也不会被钳制;
--*   但 maxTime 是 vanilla 类实例上的普通字段, 任何 Lua 侧钩子 (含原版
--*   ISTimedActionQueue.getTimedActionQueue(p).queue 遍历) 一行就能读到, 1 这种
--*   "原版不可能出现"的极端值 + 200 件/分的频率是唯一会自曝的形态, 故留冗余。
local ETHER_REPAIR_ACTION_TIME = 40;
local ETHER_REPAIR_STALL_MS = 12000;     -- 条件长时间不上升判停窗口: 服务端条件经载具周期更新回包,
                                         -- 可能滞后数秒 (实测)。按**时间窗**判定而非步数 (九十一教训),
                                         -- 窗口 = 走位 (1~4s, 九十四 起每件先走到该部件区域) + 单件动作
                                         -- (2s) + 数秒同步滞后余量; 秒修档可调回 6000

EtherRepairJob = nil;                    -- 当前修理单 (状态机数据; 再点一次按钮 = 取消)
--* 取"过账件" (即 PienZ 的"供给件"): 服务端 complete() 对该件只做 inventory:Remove(item) ——
--* 若该件**不在玩家背包**里, Remove 是**空操作** (ItemContainer.Remove:1940 遍历不到即返回,
--* 无任何副作用) → 条件照修、东西不消耗。
--* 能被服务端解析的前提 = 该物品有个能编码的容器 (PZNetKahluaTableImpl.saveInventoryItem
--* → ContainerID)。**可行**: 载具部件容器 (ContainerType.Vehicle —— VehicleParts:304 建的容器
--* parent 就是载具 ✓)、物件容器/箱柜 (ObjectContainer/IsoObject ✓)、背包与背包内袋 ✓。
--* **不可行 (实测取证)**: ① 装在部件上的那个件 —— VehiclePart.setInventoryItem:170 只存
--* this.item, 没有容器; ② **地上的物品** —— IsoWorldInventoryObject 构造里就
--* item.setContainer(null) (:82), 落地即无容器 → 服务端一律解析成 null → complete 直接
--* return false 且不报错 (PienZ 菜单那句 "No packet-resolvable item found outside player
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

--* 部件到玩家的距离 (走位用): 原版 vehicle:getAreaDist(areaId, chr) (BaseVehicle.java:7450);
--* 无 area / 接口异常 → 返回 nil, 由调用方按"很远"处理 (退化成部件索引顺序)。
local function partDistance(vehicle, part, player)
    local area = part:getArea();
    if area == nil or player == nil then return nil; end
    local ok, dist = pcall(function() return vehicle:getAreaDist(area, player); end);
    if ok and type(dist) == "number" then return dist; end
    return nil;
end

--* 选件 (九十四 调整): 每次挑**离玩家最近**的"带 item 且 condition < 100"部件。
--* 每件都要先走位到该部件所在区域 (见 repairStep 的 ISPathFindAction), 就近先修 =
--* 少绕车一圈 (原版玩家也是就近修), 距离取不到时退化回部件索引顺序。
--* 仍然无需步数记账: 被打断/失败下一 tick 重新挑件即自愈。
local FAR_DIST = 9999;
local function repairTargetPart(vehicle, player)
    local best, bestD = nil, nil;
    for i = 0, vehicle:getPartCount() - 1 do
        local part = vehicle:getPartByIndex(i);
        if part and part:getInventoryItem() and part:getCondition() < 100 then
            local d = partDistance(vehicle, part, player) or FAR_DIST;
            if best == nil or d < bestD then
                best, bestD = part, d;
            end
        end
    end
    return best;
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

--* 走位失败回调 (九十四): 原版 ISPathFindAction 失败时 forceStop → ISBaseTimedAction:stop
--* → ISTimedActionQueue.resetQueue() 会把排在它后面的修理动作一起清掉
--* (ISPathFindAction.lua:22-32), 所以不能指望"下一 tick 自己续上" —— 直接终止整单,
--* 避免出现"以为在修其实没修"的悬空单。
local function repairPathFail(player, part)
    repairFloat(player, "UI_VehiclePanel_RepairFloatAborted", nil, true);
    print("[VehiclePanel] repair stopped: cannot walk to "
        .. tostring(part and part:getId()) .. " (no path to that part)");
    EtherRepairJob = nil;
end

--*********************************************************
--* 状态机 (Events.OnTick 驱动, 250ms 节流): **队列空闲时才排下一个原版修理动作** ——
--* 队列里任意时刻只有一个动作, 被 stop 清队也只损失当前这一件, 下一 tick 自动续上。
--*********************************************************
local function repairTick()
    local job = EtherRepairJob;
    if job == nil then return; end
    local nowMs = getTimestampMs();
    if job.nextCheckMs ~= nil and nowMs < job.nextCheckMs then return; end
    job.nextCheckMs = nowMs + ETHER_REPAIR_TICK_MS;

    local player, vehicle = job.player, job.vehicle;
    if player == nil or player:isDead() or vehicle == nil or vehicle:getScript() == nil then
        print("[VehiclePanel] repair job ended (player or vehicle gone)");
        EtherRepairJob = nil;
        return;
    end
    if nowMs > job.deadline then
        repairFloat(player, "UI_VehiclePanel_RepairFloatAborted", nil, true);
        print("[VehiclePanel] repair job timeout - stopped after " .. tostring(job.steps) .. " step(s)");
        EtherRepairJob = nil;
        return;
    end
    -- 先等下车完成 (vanilla 修车必须在车外)
    if player:getVehicle() ~= nil then
        if nowMs > job.exitDeadline then
            repairFloat(player, "UI_VehiclePanel_RepairFloatAborted", nil, true);
            print("[VehiclePanel] repair aborted: could not exit the vehicle");
            EtherRepairJob = nil;
        end
        return;
    end
    -- 当前动作还没结束 (含 MP 下等服务端回执) → 继续等
    local queue = ISTimedActionQueue.getTimedActionQueue(player);
    if queue ~= nil and queue.queue ~= nil and #queue.queue > 0 then
        if nowMs > job.stepDeadline then
            repairFloat(player, "UI_VehiclePanel_RepairFloatAborted", nil, true);
            print("[VehiclePanel] repair step stalled - job aborted");
            EtherRepairJob = nil;
        end
        return;
    end
    -- A2 (八十九, 见 analysis/DLL分析/A-隐蔽性工程件-设计方案.md §A2 的修订): 先问**原版回执** ——
    -- 原版引擎本来就在等 ActionManager.isDone/isRejected (LuaTimedActionNew.java:88-103), 我们把状态读出来:
    -- 服务端"拒绝"就立刻中止 (九十一: 不再退化用背包件), 不必再等容差; 已完成则清零待办。
    -- 回执不可用 (返回 3, 例如桥初始化失败或动作已移出队列) 时完全退回下面的原有判定。
    if job.pendingAction ~= nil and type(timedActionState) == "function" then
        local receipt = timedActionState(job.pendingAction);
        if receipt == 2 then
            job.pendingAction = nil;
            repairFloat(player, "UI_VehiclePanel_RepairFloatRejected",
                { part = partDisplayName(job.pendingPart) }, true);
            print("[VehiclePanel] repair stopped: the server rejected the repair action");
            EtherRepairJob = nil;
            return;
        elseif receipt == 1 then
            job.pendingAction = nil;
        end
    end
    -- 上一件是否真的生效? (条件应上升; 没上升 = 被打断或服务端解析不到过账件)
    if job.pendingPart ~= nil then
        if job.pendingPart:getCondition() > job.pendingCond then
            repairFloat(player, "UI_VehiclePanel_RepairFloatDone", {
                part = partDisplayName(job.pendingPart),
                from = job.pendingCond,
                to = job.pendingPart:getCondition(),
                source = getText("IGUI_VehiclePart" .. tostring(job.pendingSource)),
                item = tostring(job.pendingItemName),
            }, false);
            job.lastRiseMs = nowMs;
            job.pendingPart, job.pendingCond = nil, nil;
        elseif nowMs - job.lastRiseMs > ETHER_REPAIR_STALL_MS then
            repairFloat(player, "UI_VehiclePanel_RepairFloatStalled",
                { part = partDisplayName(job.pendingPart) }, true);
            print("[VehiclePanel] repair stopped: steps are not taking effect"
                .. " (the server may not resolve the token item)");
            EtherRepairJob = nil;
            return;
        end
    end
    -- 挑下一个要修的部件 (全 100 即收工)
    local part = repairTargetPart(vehicle, player);
    if part == nil then
        print("[VehiclePanel] repair finished (" .. tostring(job.steps) .. " step(s) queued)");
        EtherRepairJob = nil;
        return;
    end
    -- 取过账件 (只用本车: 后备箱 → 手套箱 → 座椅; 见 repairTokenFor)
    -- (A2 回执在下方队列空后统一检查)
    local token, source = repairTokenFor(vehicle);
    if token == nil then
        repairFloat(player, "UI_VehiclePanel_RepairFloatNoToken", nil, true);
        print("[VehiclePanel] repair stopped: no item in this vehicle (trunk / glovebox / seats); "
            .. tostring(job.steps) .. " step(s) done");
        EtherRepairJob = nil;
        return;
    end
    job.steps = job.steps + 1;
    job.pendingPart = part;
    job.pendingCond = part:getCondition();
    job.pendingItem = token;                              -- 供结算时显示"消耗了什么/未消耗"
    job.pendingItemName = token:getDisplayName();
    job.pendingSource = source;                           -- 后备箱/手套箱/座椅部件的 ID
    job.stepDeadline = nowMs + ETHER_REPAIR_STEP_MAX_MS;
    job.lastRiseMs = nowMs;   -- 停滞窗口从本步排队时起算 (走位 + 读条 + 条件回包)
    print("[VehiclePanel] repair step " .. tostring(job.steps) .. ": " .. tostring(part:getId())
        .. " cond " .. string.format("%.0f", job.pendingCond) .. " (token: " .. tostring(source) .. ", free)");
    -- 先走位到该部件所在区域 (九十四 用户实测反馈: 之前站在驾驶座旁边原地修, 与原版不符)。
    -- 原版所有载具动作都是先 ISPathFindAction:pathToVehicleArea(player, vehicle, part:getArea())
    -- 再排实际动作 —— 取证: ISVehicleMechanics.lua:428/472 (取引擎件/换件)、
    -- ISVehiclePartMenu.lua:242/264 (加油)。走位动作与本件修理动作一起排队, 队列天然保证顺序。
    if type(ISPathFindAction) == "table" and part:getArea() ~= nil then
        local walk = ISPathFindAction:pathToVehicleArea(player, vehicle, part:getArea());
        walk:setOnFail(repairPathFail, player, part);
        ISTimedActionQueue.add(walk);
    end
    local stepAction = ISRepairLightbar:new(player, part, token, ETHER_REPAIR_ACTION_TIME);
    job.pendingAction = stepAction;   -- A2 (八十九): 留引用供 timedActionState 查原版回执
    ISTimedActionQueue.add(stepAction);
end
Events.OnTick.Add(repairTick);

local function repairVehicle()
    -- 取消优先于一切 (2026-09-14 九十一 修): 状态机会强制下车, 再点按钮时人在车外,
    -- 旧顺序 (先查 currentVehicle) 让取消分支永远走不到 —— 用户实测"开始后取消不了"。
    -- 取消时连当前动作一起清 (与 D2「整理」取消同款), 立即拿回控制权。
    if EtherRepairJob ~= nil then
        if type(ISTimedActionQueue) == "table" then
            ISTimedActionQueue.clear(EtherRepairJob.player);
        end
        print("[VehiclePanel] repair job cancelled");
        EtherRepairJob = nil;
        return;
    end
    local player, vehicle = currentVehicle();
    if not vehicle then
        print("[VehiclePanel] sit inside the vehicle first");
        return;
    end
    -- 乘客在行驶中的车: 原版 onExit 直接放弃 (不下车), 修理步会卡在车内 → 前置拦截
    if not vehicle:isDriver(player) and not vehicle:isStopped() then
        print("[VehiclePanel] wait for the vehicle to stop before repairing");
        return;
    end
    local script = vehicle:getScript();
    local gain = 15 + (player:getPerkLevel(Perks.Mechanics) - (script and script:getEngineRepairLevel() or 0));
    if gain <= 0 then
        print("[VehiclePanel] mechanics skill too low for timed repair (use direct repair)");
        return;
    end
    local nowMs = getTimestampMs();
    EtherRepairJob = {
        player = player,
        vehicle = vehicle,
        deadline = nowMs + ETHER_REPAIR_JOB_MAX_MS,
        exitDeadline = nowMs + 15000,
        stepDeadline = nowMs + ETHER_REPAIR_STEP_MAX_MS,
        nextCheckMs = nowMs,
        lastRiseMs = nowMs,
        steps = 0,
    };
    ISVehicleMenu.onExit(player);   -- 原版下车; 下车完成后状态机逐件排队原版修理动作
    print("[VehiclePanel] repair job started (" .. tostring(gain) .. " per step; each step uses a free token from this vehicle)");
end

--*********************************************************
--* 加油: GasTank 部件容量拉到上限。
--*********************************************************
local function refuelVehicle()
    local player, vehicle = currentVehicle();
    if not vehicle then return; end
    local tank = vehicle:getPartById("GasTank");
    if tank then
        sendClientCommand(player, "vehicle", "setContainerContentAmount", {
            vehicle = vehicle:getId(), part = "GasTank",
            amount = tank:getContainerCapacity() });
    end
end

--*********************************************************
--* 立即启动: 单发一次 startEngine(haveKey=true)。
--*********************************************************
local function startEngineNow()
    local player, vehicle = currentVehicle();
    if not vehicle then return; end
    sendClientCommand(player, "vehicle", "startEngine", { haveKey = true });
end

--*********************************************************
--* C1/C2 (2026-09-14): 载具座位包通道 (VehicleEnterPacket, 服务端权威执行
--* enter + 广播; requiredCapability=LoginOnServer, 不进 cmd 日志)。
--* 仅自建测试环境使用; MP 有效, 单机隐藏。
--*********************************************************
local C2_RADIUS = 20;   -- 米, 与 PienZ "Vehicle beyond 20 m" 同档

local function nearestVehicle(px, py, exclude)
    local best, bestD = nil, C2_RADIUS;
    -- 修订 (2026-09-14): getVehicles() 返回的是并发队列 (无 get(i), 有 toArray),
    -- 直接 :get(i) 会 "tried to call nil" —— 与 UIMap 同款 toArray 后再遍历
    local vehicles = getCell() and getCell():getVehicles():toArray();
    if vehicles == nil then return nil; end
    for i = 1, #vehicles do
        local v = vehicles[i];
        if v ~= nil and v ~= exclude then
            local d = math.sqrt((v:getX() - px) ^ 2 + (v:getY() - py) ^ 2);
            if d < bestD then best, bestD = v, d; end
        end
    end
    return best;
end

-- C1: 自己上车 (20 米内最近载具的司机座)
local function enterNearVehicle()
    local player = getPlayer();
    if not player then return; end
    local vehicle = nearestVehicle(player:getX(), player:getY(), player:getVehicle());
    if not vehicle then
        print("[VehiclePanel] no loaded vehicle within " .. C2_RADIUS .. " m");
        return;
    end
    if vehicleSeatInfo(vehicle, 0) ~= "ok" then
        print("[VehiclePanel] nearest vehicle driver seat not available: " .. tostring(vehicleSeatInfo(vehicle, 0)));
        return;
    end
    vehicleEnterSeat(vehicle, player, 0);
    print("[VehiclePanel] VehicleEnter sent (self -> driver seat)");
end

-- C2: 把最近的其他在线玩家塞进 20 米内最近载具的司机座 (仅自建测试环境!)
local function sendNearPlayerToVehicle()
    local player = getPlayer();
    if not player then return; end
    local target, targetD = nil, math.huge;
    local players = getOnlinePlayers();
    if players ~= nil then
        for i = 0, players:size() - 1 do
            local op = players:get(i);
            if op ~= nil and op ~= player and op:getOnlineID() ~= player:getOnlineID() then
                local d = math.sqrt((op:getX() - player:getX()) ^ 2 + (op:getY() - player:getY()) ^ 2);
                if d < targetD then target, targetD = op, d; end
            end
        end
    end
    if target == nil then
        print("[VehiclePanel] no other online player");
        return;
    end
    local vehicle = nearestVehicle(target:getX(), target:getY(), nil);
    if not vehicle then
        print("[VehiclePanel] no loaded vehicle near target within " .. C2_RADIUS .. " m");
        return;
    end
    if vehicleSeatInfo(vehicle, 0) ~= "ok" then
        print("[VehiclePanel] driver seat not available: " .. tostring(vehicleSeatInfo(vehicle, 0)));
        return;
    end
    vehicleEnterSeat(vehicle, target, 0);
    print("[VehiclePanel] VehicleEnter sent (" .. tostring(target:getUsername()) .. " -> driver seat)");
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
    local gap = EtherTheme.ctrlGap;
    local btnW = UIButton.measureGroupWidth(titles);
    local ctrlH = EtherTheme.ctrlH;
    if btnW * #titles + gap * (#titles - 1) <= innerW then
        return ctrlH;
    end
    return #titles * ctrlH + (#titles - 1) * 4;
end

local function placeButtonRow(panel, bx, by, innerW, titles, callbacks)
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
                -- 车辆无条件短接 (ServerSyncBlocker 下行保护, 引擎启动成功即自动回弹, 30s 兜底;
                -- 仅游戏内可勾, 成功/超时由 ServerSyncBlocker 回调 :setCheked(false))
                { key = "UI_Exploit_VehicleProtection", onlyInGame = true,
                  get = function()
                      return (ServerSyncBlocker and type(ServerSyncBlocker) == "table"
                          and ServerSyncBlocker.vehicleProtection) or false;
                  end,
                  on = function(c)
                      if ServerSyncBlocker and type(ServerSyncBlocker) == "table" then
                          if c then
                              if ServerSyncBlocker.enableVehicle then ServerSyncBlocker.enableVehicle(); end
                          else
                              if ServerSyncBlocker.disableVehicle then ServerSyncBlocker.disableVehicle(); end
                          end
                      else
                          print("[modcore] ERROR: ServerSyncBlocker not loaded!");
                      end
                  end },
            },
            buttons = {
                { key = "UI_VehiclePanel_StartNow", fn = startEngineNow },
            },
            hint = "UI_VehiclePanel_EngineHint",
        },
        {
            title = "UI_VehiclePanel_Group_Teleport",
            buttons = {
                { key = "UI_VehiclePanel_EnterNear", fn = enterNearVehicle, onlyInGame = true, mpOnly = true },
                { key = "UI_VehiclePanel_SendNear", fn = sendNearPlayerToVehicle, onlyInGame = true, mpOnly = true },
            },
            hint = "UI_VehiclePanel_TeleportHint",
        },
        {
            title = "UI_VehiclePanel_Group_Service",
            buttons = {
                { key = "UI_VehiclePanel_Repair", fn = repairVehicle },
                { key = "UI_VehiclePanel_RepairDirect", fn = repairVehicleDirectNow },
                { key = "UI_VehiclePanel_Refuel", fn = refuelVehicle },
            },
            hint = "UI_VehiclePanel_Hint",
        },
    };

    -- mpOnly 按钮: 单机剔除 (C1/C2 走 VehicleEnterPacket, 仅 MP 有意义);
    -- 剔空了的模块整体不显示
    for mi = #modules, 1, -1 do
        local mod = modules[mi];
        if mod.buttons then
            for bi = #mod.buttons, 1, -1 do
                if mod.buttons[bi].mpOnly and not isMultiplayer() then
                    table.remove(mod.buttons, bi);
                end
            end
        end
        if (mod.buttons == nil or #mod.buttons == 0)
            and (mod.checkboxes == nil or #mod.checkboxes == 0) then
            table.remove(modules, mi);
        end
    end

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
