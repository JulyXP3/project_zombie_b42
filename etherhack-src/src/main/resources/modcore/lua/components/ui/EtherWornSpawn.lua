--*********************************************************
--* 穿戴生成 POC (WornSpawn, multiplayer / 自建测试服)
--* 链 (b42.22 反编译+字节码核对):
--*   SyncClothingPacket 凭空 CreateItem(任意类型) 并挂到穿戴槽 ->
--*   把背包里一件食物用 ItemStatsPacket 临时压到 ~60 ->
--*   服务端执行 ISWearClothing (complete -> setWornItem(forceDropTooHeavy)) ->
--*   旧穿戴物 (伪造品) 放不进背包 -> performDropItemOnSquare 掉在脚下 ->
--*   捡起, 恢复食物重量。
--* 用法: 背包备一件可穿衣物 (该部位当前空着) + 一件食物 -> 列表选目标 ->
--*   点生成 -> 等待服务端换装和掉落确认 -> 捡起目标物品。
--* 本文件只做入口与状态显示; 网络动作与收尾在 WornSpawnAPI (Java)。
--*********************************************************
EtherWornSpawn = EtherWornSpawn or {}

EtherWornSpawn.message = ""
EtherWornSpawn.active = false

local function setMsg(key)
    EtherWornSpawn.message = getTranslate(key)
end

local function showFailure(status)
    if status == "no-food" or status == "food-weight" then
        setMsg("UI_WornSpawn_NeedFood")
    elseif status == "no-cloth" then
        setMsg("UI_WornSpawn_NeedCloth")
    elseif status == "bad-item" then
        setMsg("UI_WornSpawn_BadItem")
    elseif status == "not-mp" then
        setMsg("UI_FishSpawn_MultiplayerOnly")
    elseif status == "timeout" then
        setMsg("UI_WornSpawn_Timeout")
    elseif status == "rejected" then
        setMsg("UI_WornSpawn_Rejected")
    elseif status == "send-failed" then
        setMsg("UI_WornSpawn_SendFailed")
    else
        setMsg("UI_WornSpawn_Error")
    end
end

function EtherWornSpawn.trigger(target)
    if not isMultiplayer() then
        EtherWornSpawn.message = getTranslate("UI_FishSpawn_MultiplayerOnly")
        return
    end
    if target == nil or target == "" then
        return
    end
    if EtherWornSpawn.active then
        EtherWornSpawn.message = getTranslate("UI_WornSpawn_Waiting")
        return
    end
    local status = wornSpawnStart(target)
    print("[WornSpawn] start -> " .. tostring(status))
    if status == "started" or status == "waiting" then
        EtherWornSpawn.active = true
        setMsg(status == "started" and "UI_WornSpawn_Started" or "UI_WornSpawn_Waiting")
    else
        showFailure(status)
    end
end

function EtherWornSpawn.cancel()
    local status = wornSpawnRestore()
    EtherWornSpawn.active = false
    EtherWornSpawn.message = ""
    if status ~= "restored" then
        showFailure(status)
    end
end

function EtherWornSpawn.tick()
    if not EtherWornSpawn.active then
        return
    end
    local status = wornSpawnPoll()
    if status == "waiting" or status == "waiting-drop" then
        if status == "waiting-drop" then
            setMsg("UI_WornSpawn_Waiting")
        end
        return
    end
    EtherWornSpawn.active = false
    print("[WornSpawn] poll -> " .. tostring(status))
    if status == "done" then
        setMsg("UI_WornSpawn_Done")
    else
        showFailure(status)
    end
end

Events.OnTick.Add(EtherWornSpawn.tick)
Events.OnDisconnect.Add(EtherWornSpawn.cancel)
