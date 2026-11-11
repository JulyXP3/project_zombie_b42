--*********************************************************
--* 研判测试探针 (2026-11-11): 自建服验证两条研判的客户端探针。
--* ① 僵尸挂载: 把自己主背包第一件真实物品经原版附着物同步包挂到最近僵尸
--*    (服务端只校验槽位名在 Human 组内 + 容器可解析, 不校验物品归属);
--*    僵尸死亡时 DoZombieInventory 把附着物并入尸体库存。
--*    「核验」按钮回答关键问题: 物品是"搬移"还是"两边都在的重复引用"。
--* ② 搜寻池扫描: 只读客户端已收到的服务端搜寻结果池 (类型/数量/坐标),
--*    验证"发现图标之前服务端已决定产出"; 零发包。
--* 仅限自建测试服核实结论; 挂载探针每按一次只发一个包。
--*********************************************************
EtherResearchProbe = EtherResearchProbe or {}
EtherResearchProbe.message = ""
EtherResearchProbe.attach = nil    -- { itemId, fullType, zombieId, location }
EtherResearchProbe.lastScan = nil  -- { total, rare, printed }

local HUMAN_ATTACH_LOCATION = "Belt Right"  -- Human 组合法槽位 (AttachedLocations.lua)
local PROBE_RADIUS = 12                     -- 找最近僵尸的半径 (格)
local CORPSE_SCAN_RANGE = 6                 -- 核验时扫描周围格数

local function say(key, params)
    if params == nil then
        EtherResearchProbe.message = getTranslate(key)
    else
        EtherResearchProbe.message = tr(key, params)
    end
end

local function findNearestZombie(player, radius)
    local cell = getCell();
    if cell == nil then return nil; end
    local list = cell:getZombieList();
    if list == nil then return nil; end
    local best = nil;
    local bestD = radius * radius + 1;
    for i = 0, list:size() - 1 do
        local z = list:get(i);
        if z ~= nil and not z:isDead() then
            local dx = z:getX() - player:getX();
            local dy = z:getY() - player:getY();
            local d = dx * dx + dy * dy;
            if d < bestD then
                best = z;
                bestD = d;
            end
        end
    end
    return best;
end

--*********************************************************
--* ① 挂载: 主背包第一件物品 -> 最近僵尸 (单发探针)
--*********************************************************
function EtherResearchProbe.attachNearest()
    if not isClient() or not isMultiplayer() then
        say("UI_ResearchProbe_NeedMP");
        return
    end
    if type(zombieAttachSend) ~= "function" then
        say("UI_ResearchProbe_NoAPI");
        return
    end
    local player = getPlayer();
    if player == nil then return; end
    local items = player:getInventory():getItems();
    if items == nil or items:size() == 0 then
        say("UI_ResearchProbe_EmptyInv");
        return
    end
    local item = items:get(0);
    if item == nil then
        say("UI_ResearchProbe_EmptyInv");
        return
    end
    local group = AttachedLocations ~= nil and AttachedLocations.getGroup("Human") or nil;
    if group == nil or group:getLocation(HUMAN_ATTACH_LOCATION) == nil then
        say("UI_ResearchProbe_NoAPI");
        return
    end
    local zombie = findNearestZombie(player, PROBE_RADIUS);
    if zombie == nil then
        say("UI_ResearchProbe_NoZombie");
        return
    end
    local ok, ret = pcall(zombieAttachSend, zombie, item, HUMAN_ATTACH_LOCATION);
    if ok and ret == "sent" then
        EtherResearchProbe.attach = {
            itemId = item:getID(),
            fullType = item:getFullType(),
            zombieId = zombie:getOnlineID(),
            location = HUMAN_ATTACH_LOCATION,
        };
        say("UI_ResearchProbe_AttachSent",
            { item = tostring(item:getDisplayName()), loc = HUMAN_ATTACH_LOCATION });
        print("[ResearchProbe] attach sent: id=" .. tostring(item:getID()) .. " "
            .. tostring(item:getFullType()) .. " -> zombie " .. tostring(zombie:getOnlineID())
            .. " @ " .. string.format("%.0f,%.0f", zombie:getX(), zombie:getY()));
    else
        say("UI_ResearchProbe_AttachFail", { reason = tostring(ret) });
        print("[ResearchProbe] attach failed: " .. tostring(ret));
    end
end

--*********************************************************
--* ② 核验: 我背包 vs 周围尸体 → 判定"搬移 / 重复引用"
--*********************************************************
function EtherResearchProbe.checkAttached()
    local rec = EtherResearchProbe.attach;
    if rec == nil then
        say("UI_ResearchProbe_NoAttach");
        return
    end
    local player = getPlayer();
    if player == nil then return; end

    -- 我方背包: 按 ID 精确找 (递归含子容器), 并统计同类件数
    local invHas = player:getInventory():getItemById(rec.itemId) ~= nil;
    local invSameType = 0;
    local inv = player:getInventory():getItems();
    for i = 0, inv:size() - 1 do
        local it = inv:get(i);
        if it ~= nil and it.getFullType ~= nil and it:getFullType() == rec.fullType then
            invSameType = invSameType + 1;
        end
    end

    -- 周围尸体: 按 ID 与类型双向核对
    local px = math.floor(player:getX());
    local py = math.floor(player:getY());
    local corpses = 0;
    local corpseHits = 0;
    for dx = -CORPSE_SCAN_RANGE, CORPSE_SCAN_RANGE do
        for dy = -CORPSE_SCAN_RANGE, CORPSE_SCAN_RANGE do
            local sq = getCell():getGridSquare(px + dx, py + dy, player:getZ());
            if sq ~= nil then
                local bodies = sq:getDeadBodys();
                if bodies ~= nil then
                    for b = 0, bodies:size() - 1 do
                        local body = bodies:get(b);
                        local cont = body ~= nil and body:getItemContainer() or nil;
                        if cont ~= nil then
                            corpses = corpses + 1;
                            local hitId = false;
                            local hitType = false;
                            local ok, byId = pcall(cont.getItemWithID, cont, rec.itemId);
                            if ok and byId ~= nil then hitId = true; end
                            local items = cont:getItems();
                            if items ~= nil then
                                for k = 0, items:size() - 1 do
                                    local it = items:get(k);
                                    if it ~= nil and it.getFullType ~= nil
                                        and it:getFullType() == rec.fullType then
                                        hitType = true;
                                        print("[ResearchProbe] corpse item: id="
                                            .. tostring(it.getID and it:getID() or "?") .. " "
                                            .. tostring(it:getFullType()));
                                    end
                                end
                            end
                            if hitId or hitType then
                                corpseHits = corpseHits + 1;
                                print("[ResearchProbe] corpse match @ "
                                    .. (px + dx) .. "," .. (py + dy)
                                    .. " byId=" .. tostring(hitId) .. " byType=" .. tostring(hitType));
                            end
                        end
                    end
                end
            end
        end
    end

    lastCheck = {
        invHas = invHas,
        invSameType = invSameType,
        corpses = corpses,
        corpseHits = corpseHits,
    };
    print("[ResearchProbe] check: invHas=" .. tostring(invHas)
        .. " invSameType=" .. tostring(invSameType)
        .. " corpses=" .. tostring(corpses) .. " hits=" .. tostring(corpseHits));

    if invHas and corpseHits > 0 then
        say("UI_ResearchProbe_VerdictDup");
    elseif (not invHas) and corpseHits > 0 then
        say("UI_ResearchProbe_VerdictMove");
    elseif invHas and corpseHits == 0 then
        say("UI_ResearchProbe_VerdictGone");
    else
        say("UI_ResearchProbe_VerdictNeither");
    end
end

--*********************************************************
--* ③ 搜寻池扫描: 只读客户端收到的服务端结果池, 零发包
--*********************************************************
function EtherResearchProbe.scanPool()
    local player = getPlayer();
    if player == nil then return; end
    if ISSearchManager == nil or ISSearchManager.getManager == nil then
        say("UI_ResearchProbe_NoPool");
        return
    end
    local manager = ISSearchManager.getManager(player);
    if manager == nil or manager.forageIcons == nil then
        say("UI_ResearchProbe_NoPool");
        return
    end
    local total = 0;
    local rare = 0;
    local nearestD = nil;
    for id, icon in pairs(manager.forageIcons) do
        if type(icon) == "table" or type(icon) == "userdata" then
            local itemType = icon.itemType;
            if itemType ~= nil then
                total = total + 1;
                local ix = icon.xCoord or icon.x or 0;
                local iy = icon.yCoord or icon.y or 0;
                local dx = ix - player:getX();
                local dy = iy - player:getY();
                local dist = math.sqrt(dx * dx + dy * dy);
                if nearestD == nil or dist < nearestD then nearestD = dist; end
                local catName = icon.catName;
                if catName == "Ammunition" or catName == "JunkWeapons" then
                    rare = rare + 1;
                end
                print("[ResearchProbe] pool: " .. tostring(itemType)
                    .. " x" .. tostring(icon.count or 1)
                    .. " cat=" .. tostring(catName)
                    .. " dist=" .. string.format("%.0f", dist));
            end
        end
    end
    if total == 0 then
        say("UI_ResearchProbe_NoPool");
        return
    end
    EtherResearchProbe.lastScan = {
        total = total,
        rare = rare,
        printed = total,
    };
    say("UI_ResearchProbe_PoolResult", {
        n = tostring(total),
        rare = tostring(rare),
        near = nearestD ~= nil and string.format("%.0f", nearestD) or "?",
    });
end
