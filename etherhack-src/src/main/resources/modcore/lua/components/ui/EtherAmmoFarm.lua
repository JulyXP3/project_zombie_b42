-- 嵌套背包弹匣刷取 (B1, 仅多人客户端):
--   源弹匣放在主背包内“未装备的背包/工具箱”里; 插匣时服务端只扫主容器、不递归,
--   源弹匣不被消耗, 枪内仍获得其弹量; 再退匣即产出新弹匣。循环执行即可连续产出。
--   本模块只排原版插匣/退匣动作, 不写弹药、不发新包。

EtherAmmoFarm = {
    message = "",
    bagFarm = nil,
}

local function bagContainerOf(item)
    -- 只有 InventoryContainer 才有 getInventory(); 对普通物品直接调用会抛 Java 异常,
    -- 即使 pcall 兜住也会被 Kahlua 逐条刷日志, 所以先用 instanceof 判类型。
    if item == nil or not instanceof(item, "InventoryContainer") then return nil end
    return item:getInventory()
end

local function findBagMagazine(player, gun)
    local magType = gun:getMagazineType()
    if not magType or magType == "" then return nil end
    local inv = player:getInventory()
    local items = inv:getItems()
    for i = 0, items:size() - 1 do
        local bag = items:get(i)
        local bagInv = bagContainerOf(bag)
        if bagInv ~= nil and bagInv ~= inv then
            local inner = bagInv:getItems()
            for j = 0, inner:size() - 1 do
                local it = inner:get(j)
                if it:getFullType() == magType and it:getCurrentAmmoCount() > 0 then
                    return it
                end
            end
        end
    end
    return nil
end

local function setMessage(key, n)
    if n == nil then
        EtherAmmoFarm.message = getTranslate(key)
    else
        EtherAmmoFarm.message = tr(key, { n = tostring(n) })
    end
end

local function stopFarm(key)
    local s = EtherAmmoFarm.bagFarm
    local n = s and s.count or 0
    EtherAmmoFarm.bagFarm = nil
    setMessage(key, n)
end

function EtherAmmoFarm.startBagFarm()
    if not isClient() then
        setMessage("UI_AmmoFarm_MPOnly")
        return false
    end
    local player = getPlayer()
    if not player then return false end
    local gun = player:getPrimaryHandItem()
    if not gun or gun:getStringItemType() ~= "RangedWeapon" then
        setMessage("UI_AmmoFarm_NoGun")
        return false
    end
    local magType = gun:getMagazineType()
    if not magType or magType == "" then
        setMessage("UI_AmmoFarm_NoMagType")
        return false
    end
    if gun:isContainsClip() or gun:getCurrentAmmoCount() > 0 then
        setMessage("UI_AmmoFarm_NotEmpty")
        return false
    end
    local mag = findBagMagazine(player, gun)
    if mag == nil then
        setMessage("UI_AmmoFarm_NoMag")
        return false
    end
    EtherAmmoFarm.bagFarm = {
        phase = "insert", count = 0,
        gunId = gun:getID(),
        magId = mag:getID(),
        magCount = mag:getCurrentAmmoCount(),
        deadline = getTimestampMs() + 4000,
    }
    setMessage("UI_AmmoFarm_Running", 0)
    ISTimedActionQueue.add(ISInsertMagazine:new(player, gun, mag))
    return true
end

function EtherAmmoFarm.stopBagFarm()
    if EtherAmmoFarm.bagFarm ~= nil then
        stopFarm("UI_AmmoFarm_Stopped")
    end
end

function EtherAmmoFarm.pollBagFarm()
    local s = EtherAmmoFarm.bagFarm
    if s == nil then return end
    local player = getPlayer()
    if not player then stopFarm("UI_AmmoFarm_Stopped"); return end
    local gun = player:getPrimaryHandItem()
    if not gun or gun:getID() ~= s.gunId then
        stopFarm("UI_AmmoFarm_GunChanged")
        return
    end
    local now = getTimestampMs()
    if now > s.deadline then
        stopFarm("UI_AmmoFarm_Timeout")
        return
    end
    if s.phase == "insert" then
        if gun:isContainsClip() and gun:getCurrentAmmoCount() == s.magCount then
            s.phase = "eject"
            s.deadline = now + 4000
            ISTimedActionQueue.add(ISEjectMagazine:new(player, gun))
        end
    elseif s.phase == "eject" then
        if not gun:isContainsClip() and gun:getCurrentAmmoCount() == 0 then
            s.count = s.count + 1
            if player:getInventory():getItemById(s.magId) == nil then
                stopFarm("UI_AmmoFarm_MagGone")
                return
            end
            if player:getInventory():isFull(player) then
                stopFarm("UI_AmmoFarm_Full")
                return
            end
            local mag = findBagMagazine(player, gun)
            if mag == nil then
                stopFarm("UI_AmmoFarm_MagGone")
                return
            end
            s.magId = mag:getID()
            s.magCount = mag:getCurrentAmmoCount()
            s.phase = "insert"
            s.deadline = now + 4000
            setMessage("UI_AmmoFarm_Running", s.count)
            ISTimedActionQueue.add(ISInsertMagazine:new(player, gun, mag))
        end
    end
end

function EtherAmmoFarm.onTick()
    EtherAmmoFarm.pollBagFarm()
end

Events.OnTick.Add(EtherAmmoFarm.onTick)
