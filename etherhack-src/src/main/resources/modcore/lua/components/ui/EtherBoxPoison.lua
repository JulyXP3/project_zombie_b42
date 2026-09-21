--*********************************************************
--* 开盒生成 POC (BoxPoison, multiplayer)
--* 链: 给背包里载体的本地 modData 写投毒键 ->
--*   syncItemFieldsNow 整表上行 (服务端 wipe + rawset, 零校验) ->
--*   正常跑原版合成, 服务端重执配方 OnCreate, 按 modData 字符串建物:
--*   B1 Base.BoxOfJars + OpenBoxOfJars -> unpackItemTypeFromBox
--*      (numberOfPackedItems + packedItem<N>, 任意类型 x 数量);
--*   B2 Base.LogStacks2/3/4 / Base.FirewoodBundle + 对应 Unstack 配方
--*      -> splitLogStack (ropeItems 嵌套表, 任意类型)。
--* 用法 (多人自建服): 背包备好载体 -> 列表选目标 -> 点生成 (投毒) ->
--*   去合成界面正常做一次对应配方 -> 目标物品进背包 (真物品)。
--* 本文件只做投毒与上行, 合成走原版 UI (== 留痕与正常合成一致)。
--*********************************************************
EtherBoxPoison = EtherBoxPoison or {}

EtherBoxPoison.message = ""
EtherBoxPoison.MAX_COUNT = 100

-- 载体全毒 (一百五十七): 背包里有的载体全部投毒, 不再二选一 ——
-- 之前 B1 优先导致包里留着盒子时永远毒盒子, B2 木堆测不到。
-- B1 Base.BoxOfJars (平键 numberOfPackedItems/packedItem<N>, 最稳);
-- B2 Base.LogStacks2/3/4 + Base.FirewoodBundle (嵌套表 ropeItems)。
local CARRIERS_B1 = { "Base.BoxOfJars" }
local CARRIERS_B2 = { "Base.LogStacks2", "Base.LogStacks3", "Base.LogStacks4", "Base.FirewoodBundle" }

local function findAllCarriers(inv, types)
    local out = {};
    for _, t in ipairs(types) do
        local ok, list = pcall(function() return inv:getItemsFromType(t) end)
        if ok and list ~= nil then
            for i = 0, list:size() - 1 do
                local item = list:get(i);
                if item ~= nil then table.insert(out, item); end
            end
        end
    end
    return out;
end

--*********************************************************
--* 入口: 目标物品全名 + 数量 (1..100)
--*********************************************************
function EtherBoxPoison.trigger(target, count)
    if not isMultiplayer() then
        print("[BoxPoison] multiplayer only (use your own dedicated server)")
        EtherBoxPoison.message = getTranslate("UI_FishSpawn_MultiplayerOnly")
        return
    end
    if target == nil or target == "" then
        return
    end
    local player = getPlayer()
    if player == nil then
        return
    end
    count = math.floor(tonumber(count) or 1)
    if count < 1 then count = 1 end
    if count > EtherBoxPoison.MAX_COUNT then count = EtherBoxPoison.MAX_COUNT end
    local inv = player:getInventory()
    if inv == nil then
        return
    end

    local boxes = findAllCarriers(inv, CARRIERS_B1)
    local stacks = findAllCarriers(inv, CARRIERS_B2)
    if #boxes == 0 and #stacks == 0 then
        EtherBoxPoison.message = getTranslate("UI_BoxPoison_NeedCarrier")
        print("[BoxPoison] need Base.BoxOfJars or LogStacks/FirewoodBundle in inventory")
        return
    end

    local names = {};
    for _, box in ipairs(boxes) do
        local mod = box:getModData()
        if mod ~= nil then
            -- B1: 只写目标。原版产出 (6 空罐 + 6 罐盖) 在 OnCreate 之前已由
            -- performRecipe 经 addOrDropItem 进包 (ISHandcraftAction.lua:226-230),
            -- luaCallOnCreate (:233) 里 toOutputItems.clear() 清的是已交付的暂存表,
            -- 罐/盖不会消失 (用户实测证实; 曾误读为吞产出并补 12 件, 已回滚)。
            mod["numberOfPackedItems"] = count
            for i = 0, count - 1 do
                mod["packedItem" .. i] = target
            end
            syncItemFieldsNow(box:getID())
            table.insert(names, box:getFullType());
        end
    end
    local keptRopes = 0;
    for _, stack in ipairs(stacks) do
        local mod = stack:getModData()
        if mod ~= nil then
            -- B2: 保留原有绳索条目再追加目标 —— 之前整表替换导致绳子消失
            -- (用户实测)。原条目是合法堆自带的绳 (createLogStack 写入)。
            -- 读法用整数索引直取 (createLogStack 以 0 起逐项 rawset),
            -- 不用 pairs (Kahlua 表迭代语义不可靠); 读不到=该堆已被旧版毒坏
            -- (服务端 wipe 后原绳已丢), 只能用新堆重测。
            local rope = {}
            local idx = 0;
            local ok, old = pcall(function() return mod["ropeItems"] end)
            if ok and old ~= nil then
                for i = 0, 31 do
                    local v = old[i];
                    if v == nil then break; end
                    if type(v) == "string" and v ~= "" then
                        rope[idx] = v;
                        idx = idx + 1;
                        keptRopes = keptRopes + 1;
                    end
                end
            end
            for i = 0, count - 1 do
                rope[idx] = target;
                idx = idx + 1;
            end
            mod["ropeItems"] = rope
            syncItemFieldsNow(stack:getID())
            table.insert(names, stack:getFullType());
        end
    end
    if #names == 0 then
        return
    end
    local res = "ready: " .. table.concat(names, " + ") .. " x" .. tostring(count);
    if #stacks > 0 then
        res = res .. " (kept " .. tostring(keptRopes) .. " ropes)";
    end
    EtherBoxPoison.message = res .. " -> " .. getTranslate("UI_BoxPoison_NextStep")
    print("[BoxPoison] " .. res)
end
