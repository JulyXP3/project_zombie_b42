--*********************************************************
--* 红队 POC: 尸体生成任意物品 (multiplayer)
--* 链: 客户端凭空构造 IsoDeadBody (容器 = 伪造物品清单) ->
--*   sq.addCorpse(body, bRemote=false) -> 客户端自动发 AddCorpseToMapPacket
--*   (handlingType=3, 无反作弊) -> 包内 IsoDeadBody.save 全量序列化含 container.save
--*   -> 服务端 parse: createFromBuffer + loadFromRemoteBuffer + addCorpse(body, true)
--*   原样落地, 零校验 -> 搜尸取物。
--* 用法 (仅自建服务器): 选物品 + 数量 -> 生成 -> 脚下立即出现带目标物品的尸体。
--*********************************************************
EtherCorpseSpawn = EtherCorpseSpawn or {}

EtherCorpseSpawn.busy = false
EtherCorpseSpawn.message = ""

--*********************************************************
--* 入口: 面板"生成"按钮调用 (target = 物品全名)
--*********************************************************
function EtherCorpseSpawn.trigger(target, count)
    if not isMultiplayer() then
        print("[CorpseSpawn] multiplayer only (use your own dedicated server)")
        EtherCorpseSpawn.message = getTranslate("UI_CorpseSpawn_MultiplayerOnly")
        return
    end
    if EtherCorpseSpawn.busy then
        return
    end
    if target == nil or target == "" then
        return
    end
    count = tonumber(count) or 1
    if count < 1 then count = 1 end
    if count > 100 then count = 100 end

    EtherCorpseSpawn.busy = true
    EtherCorpseSpawn.message = getTranslate("UI_CorpseSpawn_Preparing")
    print("[CorpseSpawn] target " .. target .. " x" .. count)

    local ok, ret = pcall(corpseSpawnPrepare, target, count)
    if ok and ret then
        EtherCorpseSpawn.message = tr("UI_CorpseSpawn_Done", { item = tostring(target), count = tostring(count) })
        print("[CorpseSpawn] done: corpse spawned with " .. count .. "x " .. target)
    else
        -- Java 侧失败时抛真实原因 (RuntimeException), 不再吞成误导性兜底文案
        local reason = ok and "unknown error" or tostring(ret)
        EtherCorpseSpawn.message = tr("UI_CorpseSpawn_Failed", { reason = reason })
        print("[CorpseSpawn] failed: " .. tostring(ret))
    end
    EtherCorpseSpawn.busy = false
end
