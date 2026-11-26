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

-- Uses the same Kahlua iterator as vanilla splitLogStack, including sparse/zero-based keys.
local function copyUnpackEntries(original)
    if original == nil then return nil, 0, "missing"; end
    if type(original) ~= "table" then return nil, 0, "invalid"; end
    local ok, entries, size, reason = pcall(function()
        local result = {}; local n = 0;
        for _, value in pairs(original) do
            if type(value) ~= "string" or value == "" then return nil, 0, "invalid"; end
            if n >= 256 then return nil, 0, "oversized"; end
            result[n] = value; n = n + 1;
        end
        if n == 0 then return nil, 0, "empty"; end
        return result, n;
    end);
    if not ok then return nil, 0, "read-failed"; end
    return entries, size, reason;
end

local function prepareUnpackEntries(original, itemType)
    local entries, size, reason = copyUnpackEntries(original);
    if entries then return entries, size, false; end
    if reason ~= "missing" and reason ~= "empty" then return nil, 0, false, reason; end
    if type(defaultBundleRopeItems) ~= "function" then return nil, 0, false, "defaults-unavailable"; end
    local ok, defaults = pcall(defaultBundleRopeItems, itemType);
    if not ok then return nil, 0, false, "defaults-failed"; end
    entries, size = copyUnpackEntries(defaults);
    if not entries then return nil, 0, false, "defaults-invalid"; end
    return entries, size, true;
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
    local keptEntries, skipped, failed, defaulted = 0, 0, 0, 0;
    for _, stack in ipairs(stacks) do
        local mod = stack:getModData();
        local original = nil;
        if mod then original = mod["ropeItems"]; end
        local entries, kept, usedDefaults, reason = prepareUnpackEntries(original, stack:getFullType());
        if mod and entries then
            local index = kept;
            for i = 0, count - 1 do entries[index] = target; index = index + 1; end
            mod["ropeItems"] = entries;
            local ok, status = pcall(syncItemFieldsNow, stack:getID());
            if ok and type(status) == "string" and string.sub(status, 1, 12) == "sent fields:" then
                keptEntries = keptEntries + kept;
                if usedDefaults then defaulted = defaulted + 1; end
                table.insert(names, stack:getFullType());
            else
                mod["ropeItems"] = original;
                failed = failed + 1;
                print("[BoxPoison] preparation failed " .. stack:getFullType() .. " #" .. tostring(stack:getID()) .. ": " .. tostring(status));
            end
        else
            skipped = skipped + 1;
            print("[BoxPoison] skipped " .. stack:getFullType() .. " #" .. tostring(stack:getID()) .. ": " .. tostring(reason));
        end
    end

    if #names == 0 then
        EtherBoxPoison.message = getTranslate(failed > 0 and "UI_BoxPoison_SyncFailed" or "UI_BoxPoison_BundleDataMissing");
        return;
    end
    local res = "ready: " .. table.concat(names, " + ") .. " x" .. tostring(count);
    if #stacks > 0 then res = res .. " (kept " .. tostring(keptEntries) .. " records)"; end
    if defaulted > 0 then res = res .. " (default ropes: " .. tostring(defaulted) .. " bundles)"; end
    EtherBoxPoison.message = res .. " -> " .. getTranslate("UI_BoxPoison_NextStep");
    if defaulted > 0 then EtherBoxPoison.message = EtherBoxPoison.message .. " | " .. getTranslate("UI_BoxPoison_DefaultRopes"); end
    if skipped > 0 then EtherBoxPoison.message = EtherBoxPoison.message .. " | " .. getTranslate("UI_BoxPoison_BundleDataMissing"); end
    if failed > 0 then EtherBoxPoison.message = EtherBoxPoison.message .. " | " .. getTranslate("UI_BoxPoison_SyncFailed"); end
    print("[BoxPoison] " .. res)
end
