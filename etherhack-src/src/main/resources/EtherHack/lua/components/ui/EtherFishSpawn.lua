--*********************************************************
--* 红队 POC: 钓竿生成任意物品 (multiplayer)
--* 链: 手持鱼竿抛竿 -> 服务端建立浮标 -> 伪造 FishingAction
--*   flagUpdateFish 包注入任意物品(服务端零校验) ->
--*   OnFishingActionMPUpdate 覆盖服务端 bobber.fish.fishItem ->
--*   服务端 FishingAction.update 写入 fishForPickUp ->
--*   ISPickupFishAction(服务端) getPickedUpFish() AddItem 入包
--* 用法 (仅自建服务器): 主手装备鱼竿 -> 面板选物品 -> 生成,
--*   约 5~8 秒物品进入背包 (单次生成一件)。
--*********************************************************
EtherFishSpawn = EtherFishSpawn or {}

EtherFishSpawn.target = nil
EtherFishSpawn.busy = false
EtherFishSpawn.phase = "idle"     -- cast|wait_bobber|spoof|wait_reg|pickup|wait_done
EtherFishSpawn.timer = 0
EtherFishSpawn.armedAt = 0
EtherFishSpawn.timeoutMs = 30000
EtherFishSpawn.countBefore = 0
EtherFishSpawn.message = ""

--*********************************************************
--* 入口: 面板"生成"按钮调用 (target = 物品全名, 如 Base.Sledgehammer)
--*********************************************************
function EtherFishSpawn.trigger(target)
    if not isMultiplayer() then
        print("[FishSpawn] multiplayer only (use your own dedicated server)")
        EtherFishSpawn.message = getTranslate("UI_FishSpawn_MultiplayerOnly")
        return
    end
    if EtherFishSpawn.busy then
        print("[FishSpawn] already busy: " .. tostring(EtherFishSpawn.target))
        return
    end
    if target == nil or target == "" then
        return
    end
    local player = getPlayer()
    if player == nil then
        return
    end
    local rod = player:getPrimaryHandItem()
    if rod == nil or not rod:getScriptItem() or not rod:getScriptItem():getWeaponSprite() then
        print("[FishSpawn] equip a fishing rod in primary hand first")
        EtherFishSpawn.message = getTranslate("UI_FishSpawn_NeedRod")
        return
    end

    -- 移除客户端 FishingManager: 我们的 Java cast 不走客户端钓鱼状态机,
    -- 若保留 manager (其 fishingRod 为 nil), 服务端回传 flagUpdateFish 时
    -- 原版 Bobber.onFishingActionMPUpdate 会因索引 nil 崩溃
    pcall(function()
        if Fishing and Fishing.ManagerInstances then
            local m = Fishing.ManagerInstances[player:getUsername()]
            if m ~= nil then
                m:destroy()
                Fishing.ManagerInstances[player:getUsername()] = nil
            end
        end
    end)

    EtherFishSpawn.target = target
    EtherFishSpawn.busy = true
    EtherFishSpawn.phase = "cast"
    EtherFishSpawn.armedAt = getTimestampMs()
    local ok = pcall(function()
        EtherFishSpawn.countBefore = player:getInventory():getItemCount(target)
    end)
    if not ok then
        EtherFishSpawn.countBefore = 0
    end
    EtherFishSpawn.message = getTranslate("UI_FishSpawn_Casting")
    print("[FishSpawn] target " .. target)
end

--*********************************************************
--* 结算
--*********************************************************
function EtherFishSpawn.finish(reason)
    local ok, _ = pcall(fishSpawnReset)
    if not ok then end
    if reason == "ok" then
        print("[FishSpawn] done: " .. tostring(EtherFishSpawn.target) .. " is in inventory")
        -- 语序/标点交给翻译串的 {count} 占位符, 不在代码里拼接
        EtherFishSpawn.message = tr("UI_FishSpawn_Done", { count = tostring(EtherFishSpawn.target) })
    else
        print("[FishSpawn] " .. tostring(reason))
        EtherFishSpawn.message = tr("UI_FishSpawn_Failed", { reason = tostring(reason) })
    end
    -- 恢复客户端 FishingManager (若仍手持鱼竿), 让正常钓鱼可用
    pcall(function()
        local player = getPlayer()
        if player ~= nil and Fishing and Fishing.Handler and Fishing.Handler.handleFishing then
            Fishing.Handler.handleFishing(player, player:getPrimaryHandItem())
        end
    end)

    EtherFishSpawn.busy = false
    EtherFishSpawn.phase = "idle"
    EtherFishSpawn.target = nil
end

--*********************************************************
--* 每帧状态机
--*********************************************************
local function onTickInner()
    if not EtherFishSpawn.busy then return end
    local player = getPlayer()
    local now = getTimestampMs()

    if player == nil or now - EtherFishSpawn.armedAt > EtherFishSpawn.timeoutMs then
        EtherFishSpawn.finish("timeout")
        return
    end

    if EtherFishSpawn.phase == "cast" then
        local ok, err = pcall(function()
            if not fishSpawnCast() then
                error("cast failed")
            end
        end)
        if not ok then
            EtherFishSpawn.finish(err and tostring(err) or "cast failed (need rod in primary hand?)")
            return
        end
        EtherFishSpawn.phase = "wait_bobber"
        EtherFishSpawn.timer = now
        EtherFishSpawn.message = getTranslate("UI_FishSpawn_WaitingBobber")
    elseif EtherFishSpawn.phase == "wait_bobber" then
        -- 服务端约 85 tick 后建立浮标; 固定等 4 秒留余量
        if now - EtherFishSpawn.timer > 4000 then
            EtherFishSpawn.phase = "spoof"
        end
    elseif EtherFishSpawn.phase == "spoof" then
        local ok = false
        pcall(function() ok = fishSpawnSpoof(EtherFishSpawn.target) end)
        if ok then
            EtherFishSpawn.phase = "wait_reg"
            EtherFishSpawn.timer = now
            EtherFishSpawn.message = getTranslate("UI_FishSpawn_Injected")
        else
            EtherFishSpawn.finish("spoof failed")
        end
    elseif EtherFishSpawn.phase == "wait_reg" then
        -- 等服务端 FishingAction.update 把物品写入 fishForPickUp (一个 tick 足够)
        if now - EtherFishSpawn.timer > 1000 then
            EtherFishSpawn.phase = "pickup"
        end
    elseif EtherFishSpawn.phase == "pickup" then
        local ok = false
        pcall(function() ok = fishSpawnPickup(EtherFishSpawn.target) end)
        if ok then
            EtherFishSpawn.phase = "wait_done"
            EtherFishSpawn.timer = now
            EtherFishSpawn.message = getTranslate("UI_FishSpawn_PickingUp")
        else
            EtherFishSpawn.finish("pickup failed")
        end
    elseif EtherFishSpawn.phase == "wait_done" then
        local count = EtherFishSpawn.countBefore
        pcall(function() count = player:getInventory():getItemCount(EtherFishSpawn.target) end)
        if count > EtherFishSpawn.countBefore then
            EtherFishSpawn.finish("ok")
            return
        end
        if now - EtherFishSpawn.timer > 9000 then
            EtherFishSpawn.finish("pickup timeout (item not in inventory)")
        end
    end
end

local function onTick()
    local ok, err = pcall(onTickInner)
    if not ok then
        print("[FishSpawn] internal error, reset: " .. tostring(err))
        EtherFishSpawn.finish("internal error")
    end
end

if Events ~= nil then
    Events.OnTick.Add(onTick)
end
