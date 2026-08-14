-- 刷弹药 (ammo farming): 把主/副手武器弹药数拉到 N 并同步服务端, 再触发香草
-- 退匣 / 退弹动作, 把 N 发"兑换"成真实弹药 / 弹匣物品。
-- Mode A (手动): farmSetAmmo() 仅设置弹药数, 玩家自行右键退弹/退匣。
-- Mode B (自动): 本模块的 OnTick 在弹药耗尽后自动补充并触发退弹/退匣。
EtherAmmoFarm = {
    enabled = false,
}

local function triggerAction(player)
    local gun = player:getPrimaryHandItem()
    if not gun or gun:getStringItemType() ~= "RangedWeapon" then
        return
    end
    local magType = gun:getMagazineType()
    if magType and magType ~= "" then
        ISTimedActionQueue.add(ISEjectMagazine:new(player, gun))
    else
        ISTimedActionQueue.add(ISUnloadBulletsFromFirearm:new(player, gun))
    end
end

function EtherAmmoFarm.onTick()
    if not EtherAmmoFarm.enabled then
        return
    end
    local player = getPlayer()
    if not player then
        return
    end
    local gun = player:getPrimaryHandItem()
    if not gun or gun:getStringItemType() ~= "RangedWeapon" then
        return
    end
    -- 弹药耗尽后再补充并触发, 避免在动作执行中重复填充导致动作无法结束。
    if gun:getCurrentAmmoCount() <= 0 then
        farmSetAmmo()
        triggerAction(player)
    end
end

Events.OnTick.Add(EtherAmmoFarm.onTick)
