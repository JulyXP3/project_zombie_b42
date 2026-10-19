--*********************************************************
--* 「趣味-钓鱼」B: 自动收网 (4 号候选: 渔网秒收循环)
--*
--* 机制: 「检查渔网」动作 ISCheckFishingNetAction 经通用动作同步在
--* **服务端**执行 (客户端 complete 被禁, LuaTimedActionNew:161-163;
--* 服务端不跑 isValid —— 那是纯客户端 2 小时窗口检查)。服务端
--* fishingNet.checkTrap 按 hours 掷产出: 普通鱼 8%/时 (hours=15 ->
--* 每轮期望 1.9 条), 饵概率 4+(200-饵量)/25 (%)/时, 鱼获直接进背包
--* + sendAddItemToContainer; hours=15 恰好不触发 hours>15 的 20% 折网;
--* 有获时服务端 addXp +1, complete 再 +1 (~2/轮)。
--*
--* 循环节拍 (每轮 ~8s): ①②方块 moddata 写 fishingNetTS = 15 小时前 +
--* fishingNetBait = 200 (顶格, 原版 setBait 上限), transmitModdata 上行
--* (MP 下服务端 ReceiveModData wipe+覆盖, 服务端实例无 TS 校验直接用);
--* ③排队 ISCheckFishingNetAction:new(player, net, 15); ④队列排空即计一轮,
--* 冷却后重写再排 —— checkTrap 结算完会重置 TS 并把饵衰减到 50,
--* 每轮重写即永远按满饵 + 15 小时产量结算。人不需要走到网边
--* (无任何距离校验), 半径 10 格内自动找最近渔网。
--*
--* 边界: 玩家自己在钓鱼 (Fishing.ManagerInstances 有实例) 时不插队,
--* 避免两套动作相互打断; 动作卡在队列超过 RETRY_MS 视为失效重排;
--* 其余异常 pcall 兜底并自动关闭 (TrapPOC 同款纪律)。
--* 详见 analysis/钓鱼作弊功能研判(1-2-4已实施-已实测-5已撤下留档).md。
--*********************************************************
EtherAutoFishingNet = EtherAutoFishingNet or {}

EtherAutoFishingNet.enabled = false
EtherAutoFishingNet.SCAN_RADIUS = 10
EtherAutoFishingNet.CHECK_HOURS = 15
EtherAutoFishingNet.COOLDOWN_MS = 800
EtherAutoFishingNet.NONET_RESCAN_MS = 3000
EtherAutoFishingNet.RETRY_MS = 30000
EtherAutoFishingNet.cycles = 0
EtherAutoFishingNet.noNet = false
EtherAutoFishingNet.pendingAction = nil
EtherAutoFishingNet.pendingDeadline = 0
EtherAutoFishingNet.nextRunMs = 0

pcall(require, "TimedActions/Fishing/TimedActions/ISCheckFishingNetAction")

local function nowMs()
    return getGameTime():getCalender():getTimeInMillis()
end

--*********************************************************
--* 半径内找最近的渔网 (方块对象名 FishingNet; 服务端无距离校验,
--*  远程可用, 但扫描范围限制在玩家附近避免全图遍历)
--*********************************************************
local function findNearestNet(p)
    local px, py, pz = p:getX(), p:getY(), p:getZ()
    local R = EtherAutoFishingNet.SCAN_RADIUS
    local cell = getCell()
    local best, bestD = nil, nil
    for dx = -R, R do
        for dy = -R, R do
            local sq = cell:getGridSquare(px + dx, py + dy, pz)
            if sq ~= nil then
                local objs = sq:getObjects()
                for i = 0, objs:size() - 1 do
                    local o = objs:get(i)
                    if o ~= nil and o:getName() == "FishingNet" then
                        local d = dx * dx + dy * dy
                        if bestD == nil or d < bestD then
                            best = o
                            bestD = d
                        end
                    end
                end
            end
        end
    end
    return best
end

--*********************************************************
--* 把方块 moddata 改写为「满饵 + 15 小时前」并同步给服务端
--*********************************************************
local function armNet(net)
    local sq = net:getSquare()
    if sq == nil then return false end
    local md = sq:getModData()
    md["fishingNetTS"] = nowMs() - EtherAutoFishingNet.CHECK_HOURS * 3600 * 1000
    md["fishingNetBait"] = 200
    sq:transmitModdata()
    return true
end

local function playerIsFishing(p)
    if Fishing == nil or Fishing.ManagerInstances == nil then return false end
    local idx = isMultiplayer() and p:getUsername() or p:getPlayerNum()
    return Fishing.ManagerInstances[idx] ~= nil
end

local function stillQueued(q, action)
    if q == nil or q.queue == nil then return false end
    for i = 1, #q.queue do
        if q.queue[i] == action then return true end
    end
    return false
end

local function mainTick(p, now)
    if EtherAutoFishingNet.pendingAction ~= nil then
        local q = ISTimedActionQueue.getTimedActionQueue(p)
        if stillQueued(q, EtherAutoFishingNet.pendingAction) then
            if now > EtherAutoFishingNet.pendingDeadline then
                -- 动作长时间没走完 (被别的动作占住队列): 放弃跟踪, 冷却后重排
                EtherAutoFishingNet.pendingAction = nil
                EtherAutoFishingNet.nextRunMs = now + EtherAutoFishingNet.COOLDOWN_MS
            end
        else
            -- 动作走完 (或被打断): 计一轮, 冷却
            EtherAutoFishingNet.pendingAction = nil
            EtherAutoFishingNet.cycles = EtherAutoFishingNet.cycles + 1
            EtherAutoFishingNet.nextRunMs = now + EtherAutoFishingNet.COOLDOWN_MS
        end
        return
    end

    if now < EtherAutoFishingNet.nextRunMs then return end

    local net = findNearestNet(p)
    if net == nil then
        EtherAutoFishingNet.noNet = true
        EtherAutoFishingNet.nextRunMs = now + EtherAutoFishingNet.NONET_RESCAN_MS
        return
    end
    EtherAutoFishingNet.noNet = false

    if not armNet(net) then return end
    local action = ISCheckFishingNetAction:new(p, net, EtherAutoFishingNet.CHECK_HOURS)
    ISTimedActionQueue.add(action)
    EtherAutoFishingNet.pendingAction = action
    EtherAutoFishingNet.pendingDeadline = now + EtherAutoFishingNet.RETRY_MS
end

local function onTick()
    if not EtherAutoFishingNet.enabled then return end
    local p = getPlayer()
    if p == nil or p:isDead() then return end
    local ok, err = pcall(mainTick, p, getTimeInMillis())
    if not ok then
        print("[AutoNet] internal error, disabled: " .. tostring(err))
        EtherAutoFishingNet.enabled = false
    end
end

if Events ~= nil then
    Events.OnTick.Add(onTick)
end

function EtherAutoFishingNet.setEnabled(v)
    EtherAutoFishingNet.enabled = v and true or false
    EtherAutoFishingNet.pendingAction = nil
    EtherAutoFishingNet.nextRunMs = 0
    EtherAutoFishingNet.pendingDeadline = 0
    EtherAutoFishingNet.cycles = 0
    EtherAutoFishingNet.noNet = false
    print("[AutoNet] " .. (EtherAutoFishingNet.enabled and "ON" or "OFF"))
    return EtherAutoFishingNet.enabled
end
