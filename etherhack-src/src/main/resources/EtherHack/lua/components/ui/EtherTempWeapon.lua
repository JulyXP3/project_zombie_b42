--*********************************************************
--* EtherTempWeapon: 临时武器 (虚空武器) — 客户端本地生成+装备, 零上行
--* (分析文档: 临时武器-分析与设计(未实施).md; B42 stable 客户端本地枪可开火,
--*  他端看不到持物 — 服务端态没有这件武器, 我们不发起装备同步)
--*
--* 实现纪律 (留痕研判 §1.6 反推):
--*   1. 只用直接 setter (setPrimaryHandItem/setSecondaryHandItem), 禁走背包
--*      交易流 / Transaction → 零装备包;
--*   2. 不入背包容器 (手持引用即可) → 零容器包;
--*   3. 武器数值一律真实脚本 (命中包合法性的根基: AntiCheatHitWeapon 按
--*      包内武器判距离/弹药/射速, 真实数值天然全过);
--*   4. 唯一玩法层暴露 = 枪声全服可闻 (正常噪声事件)。
--*********************************************************

EtherTempWeapon = {
    list = {};      -- 枪械清单 { { type = "Base.M16", label = 显示名, ord = 常用度 } }
    state = {
        active = false;
        itemId = nil;
        gun = nil;                -- 本地生成的手持引用 (不进背包容器)
        savedPrimary = nil;       -- 原真实主手物品 (仅客户端引用, 服务端背包未动)
        savedSecondary = nil;
    };
};

-- 枚举全库脚本枪械, 常用度靠前 (M16/AK/霰弹/泵动/手枪类)。
-- 枪械判定必须走方法: Item:hasTag(ItemTag.FIREARM) (B42 原版枚举全局,
-- ISInventoryBuildMenu 同款)。不能用 item.isAimedFirearm —— 那是脚本 Item 的
-- 公共字段, Kahlua 属性直读恒 nil (静默不报错), 会把整个列表过滤成空 (实测缺陷)。
function EtherTempWeapon.buildList()
    EtherTempWeapon.list = {};
    local items = getAllItems();
    if items == nil then return; end
    local preferred = { "m16", "ak", "shotgun", "pump", "pistol", "rifle" };
    local function ord(fullType)
        local low = string.lower(fullType);
        for i = 1, #preferred do
            if string.find(low, preferred[i], 1, true) ~= nil then return i; end
        end
        return #preferred + 1;
    end
    local function isFirearm(item)
        if ItemTag ~= nil then
            return item:hasTag(ItemTag.FIREARM);
        end
        return item:isRanged();   -- 脚本 Item 公开方法, 无 ItemTag 环境的兜底
    end
    local ok, err = pcall(function()
        for i = 0, items:size() - 1 do
            local item = items:get(i);
            if not item:getObsolete() and not item:isHidden() and isFirearm(item) then
                table.insert(EtherTempWeapon.list, {
                    type = item:getFullName(),
                    label = item:getDisplayName(),
                    ord = ord(item:getFullName()),
                });
            end
        end
    end);
    if not ok then
        print("[EtherHack] TempWeapon buildList error: " .. tostring(err));
        return;
    end
    table.sort(EtherTempWeapon.list, function(a, b)
        if a.ord ~= b.ord then return a.ord < b.ord; end
        return a.label < b.label;
    end);
end

--*********************************************************
--* 临时替换: 记住当前手部引用 → 本地造枪 (真实脚本数值/满条件/满弹/上膛)
--* → 直接 setter 装手 (长枪同引用装副手)。重复点按 = 换枪 (先静默恢复)。
--*********************************************************
function EtherTempWeapon.apply(itemId)
    local player = getPlayer();
    if player == nil then return false; end
    local st = EtherTempWeapon.state;
    if st.active then
        EtherTempWeapon.cancel();
    end
    st.savedPrimary = player:getPrimaryHandItem();
    st.savedSecondary = player:getSecondaryHandItem();

    -- instanceItem = B42 官方全局函数 (LuaManager.GlobalObject @LuaMethod, 内部即
    -- InventoryItemFactory.CreateItem)。B42 不再把 InventoryItemFactory 类注册成
    -- Lua 全局, 直呼类名会报 "attempted index: CreateItem of non-table: null" (实测)。
    local gun = instanceItem(itemId);
    if gun == nil then return false; end
    gun:setCondition(100);
    -- 本地枪不进背包 (零上行设计) — 联机下原版"射后上膛"动作会按 ID 从背包重取枪
    -- (ISRackFirearm:start:11 getInventory():getItemById), 本地枪必取 nil →
    -- canRack 索引 nil 崩 (实测每枪一报)。关掉 RackAfterShoot 跳过该链;
    -- 弹膛由 OnTick 自愈常满, 不上膛无损失。
    if gun.setRackAfterShoot ~= nil then
        gun:setRackAfterShoot(false);
    end
    -- randomizeBullets = 原版"带弹生成"公共路径: 满弹 + 有弹匣型则 setContainsClip(true)
    -- + 有膛室则上膛 (HandWeapon.randomizeBullets, 真实脚本数值)
    if gun.randomizeBullets ~= nil then
        gun:randomizeBullets();
    end
    if gun.getMaxAmmo ~= nil and gun.getCurrentAmmoCount ~= nil and gun.setCurrentAmmoCount ~= nil
            and gun:getCurrentAmmoCount() <= 0 then
        gun:setCurrentAmmoCount(gun:getMaxAmmo());
    end
    if gun.haveChamber ~= nil and gun:haveChamber() and gun.setRoundChambered ~= nil then
        gun:setRoundChambered(true);
    end
    player:setPrimaryHandItem(gun);
    if gun.isTwoHandWeapon ~= nil and gun:isTwoHandWeapon() then
        player:setSecondaryHandItem(gun);
    end
    st.active = true;
    st.itemId = itemId;
    st.gun = gun;
    return true;
end

--*********************************************************
--* 取消替换: 丢弃本地枪引用 (GC 自然回收), 还原真实手部物品 —
--* 原物品全程未离开服务端背包, 恢复零成本零副作用 (零上行)。
--*********************************************************
function EtherTempWeapon.cancel()
    local player = getPlayer();
    local st = EtherTempWeapon.state;
    if player ~= nil then
        player:setPrimaryHandItem(st.savedPrimary);
        player:setSecondaryHandItem(st.savedSecondary);
    end
    st.active = false;
    st.itemId = nil;
    st.gun = nil;
    st.savedPrimary = nil;
    st.savedSecondary = nil;
end

function EtherTempWeapon.isActive()
    return EtherTempWeapon.state.active;
end

-- 临时枪判定 (原版动作包装用): 当前动作的枪 == 本地生成枪引用
function EtherTempWeapon.isTempGun(gun)
    local st = EtherTempWeapon.state;
    return st.active and gun ~= nil and gun == st.gun;
end

-- 状态行文本: 未替换 / 已替换: <武器名> (弹药 <n>/<max>)
function EtherTempWeapon.statusLine()
    local st = EtherTempWeapon.state;
    if not st.active or st.gun == nil then
        return tr("UI_TempWeapon_StatusIdle");
    end
    local gun = st.gun;
    local ammo = "";
    if gun.getCurrentAmmoCount ~= nil and gun.getMaxAmmo ~= nil then
        ammo = string.format(" [%d/%d]", gun:getCurrentAmmoCount(), gun:getMaxAmmo());
    end
    return tr("UI_TempWeapon_StatusActiveFmt", { weapon = gun:getDisplayName() }) .. ammo;
end

--*********************************************************
--* OnTick: 弹药回填 (空仓前本地回满 — 服务端每次看到的都是包内满弹)
--* + 卡壳/下膛自愈 + 自动失效 (死亡/掉线/切图 localPlayer 失效, 或用户手动换持)
--*********************************************************
local tickCounter = 0;

local function resetState()
    local st = EtherTempWeapon.state;
    st.active = false;
    st.itemId = nil;
    st.gun = nil;
    st.savedPrimary = nil;
    st.savedSecondary = nil;
end

local function onTick()
    local st = EtherTempWeapon.state;
    if not st.active then return; end
    local player = getPlayer();
    if player == nil or player:isDead() then
        resetState();
        return;
    end
    if player:getPrimaryHandItem() ~= st.gun then
        resetState();
        return;
    end
    local gun = st.gun;
    if gun == nil then
        resetState();
        return;
    end
    tickCounter = tickCounter + 1;
    if tickCounter % 10 ~= 0 then return; end    -- ~0.6s 节流足够
    if gun.getMaxAmmo ~= nil and gun.getCurrentAmmoCount ~= nil and gun.setCurrentAmmoCount ~= nil
            and gun:getCurrentAmmoCount() < gun:getMaxAmmo() then
        gun:setCurrentAmmoCount(gun:getMaxAmmo());
    end
    if gun.setRoundChambered ~= nil and gun.haveChamber ~= nil and gun.isRoundChambered ~= nil
            and gun:haveChamber() and not gun:isRoundChambered() then
        gun:setRoundChambered(true);
    end
end

Events.OnTick.Add(onTick);

--*********************************************************
--* 手动上膛键 (R) 包装: 临时枪直接跳过 — OnPressRackButton 没有 RackAfterShoot
--* 门禁, 联机下 ISRackFirearm:start 照样按 ID 从背包重取本地枪 → nil 崩 (同源)。
--* 弹膛自愈常满, 手动上膛对临时枪本无意义。
--*********************************************************
if ISReloadWeaponAction ~= nil and ISReloadWeaponAction.OnPressRackButton ~= nil then
    local _origOnPressRack = ISReloadWeaponAction.OnPressRackButton;
    ISReloadWeaponAction.OnPressRackButton = function(player, gun, shift)
        if EtherTempWeapon.isTempGun(gun) then return; end
        return _origOnPressRack(player, gun, shift);
    end;
end
