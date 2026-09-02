--*********************************************************
--* Red team POC: 「交换」(swap, SP & MP)
--*
--* 复用 vanilla 换肤动作 ISClothingExtraAction:
--*   动作参数 extra (换肤目标 fullType) 客户端可控, 服务端
--*   complete() 调 instanceItem(extra) 无白名单校验 →
--*   祭品销毁 + 任意衣物/背包生成 + 全服广播 (NetTimedAction
--*   通道, 与"玩家换戴手表"完全同形, 零伪造包)。
--*
--* 约束 (反编译核证 + 实测修正):
--*   - 祭品/目标都必须带 ClothingItem 脚本 (可穿戴衣物; B42 背包即衣物):
--*     服务端 createItemNew 对 getVisual()==null 的物品在 setTint 处
--*     Lua 报错, 被动作层 pcall 静默吞掉 → 无声失败 (InventoryItem.
--*     getVisual():2070 对 getClothingItem()==null 返回 null)
--*   - 目标是衣物(IsClothing)时祭品必须也是衣物 (complete 的
--*     copyPatchesTo/getWetness 块以祭品为 Clothing 前提)
--*   - 双容器时内容物 takeItemsFrom 整体随迁 (背包→背包升级)
--*   - 衣物产物 condition 拷自祭品 (衣物祭品通常满耐久 → 产物满耐久)
--*
--* 交互 (UI 语义): 用户在上表手选祭品, 一件一换 (count 固定 1)。
--* 重量规则仍由面板的祭品表过滤保证 (面板已按目标类型重建祭品表)。
--*********************************************************
EtherExchange = EtherExchange or {}

EtherExchange.timeoutBaseMs = 30000
EtherExchange.timeoutPerItemMs = 8000

--*********************************************************
--* 单件转换: 指定祭品 → 指定目标 fullType
--* sacrifice 由面板提供 (面板已按目标类型过滤合法性);
--* 这里再验一道 ClothingItem (非衣物祭品在服务端 copyPatchesTo 崩)。
--*********************************************************
function EtherExchange.convert(fullType, sacrifice)
    local player = getPlayer()
    if player == nil then return false, "no player" end
    if fullType == nil or fullType == "" then return false, "no target" end
    if sacrifice == nil then return false, "no sacrifice" end

    -- 客户端预检: 无效 fullType / 非衣物目标即时失败, 不浪费排队
    -- (非衣物目标服务端 getVisual()==null → setTint 崩 → pcall 吞掉 → 无声失败)
    local ok, probe = pcall(instanceItem, fullType)
    if not ok or probe == nil then
        return false, "bad type"
    end
    if probe:getClothingItem() == nil then
        return false, "not clothing"
    end

    -- 祭品合法性: 必须带 ClothingItem 且不在收藏 (服务端 Remove 只查主包直挂)
    if sacrifice:getClothingItem() == nil then
        return false, "bad sacrifice"
    end
    if not player:getInventory():containsID(sacrifice:getID()) then
        return false, "sacrifice moved"
    end

    ISTimedActionQueue.add(ISClothingExtraAction:new(player, sacrifice, fullType))

    EtherExchange.active = {
        fullType = fullType,
        sacrifices = { sacrifice },
        t0 = getTimestampMs(),
    }
    print("[Exchange] queued " .. fullType .. " x1")
    return true, "1"
end

--*********************************************************
--* 完成探测: 祭品从主背包消失即完成 (超时放弃)
--*********************************************************
local function onTickInner()
    local a = EtherExchange.active
    if a == nil then return end
    local player = getPlayer()
    if player == nil then return end
    local now = getTimestampMs()
    if now - a.t0 > EtherExchange.timeoutBaseMs + #a.sacrifices * EtherExchange.timeoutPerItemMs then
        print("[Exchange] timeout: " .. a.fullType)
        EtherExchange.active = nil
        return
    end
    for i = 1, #a.sacrifices do
        if player:getInventory():containsID(a.sacrifices[i]:getID()) then
            return -- 祭品还在包里, 转换未结束
        end
    end
    print("[Exchange] done: " .. a.fullType .. " x" .. #a.sacrifices)
    EtherExchange.doneInfo = { fullType = a.fullType, count = #a.sacrifices, t = now }
    EtherExchange.active = nil
end

local function onTick()
    local ok, err = pcall(onTickInner)
    if not ok then
        print("[Exchange] internal error, disarmed: " .. tostring(err))
        EtherExchange.active = nil
    end
end

if Events ~= nil then
    Events.OnTick.Add(onTick)
end
