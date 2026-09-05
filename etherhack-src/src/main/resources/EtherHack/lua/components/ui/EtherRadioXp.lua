--*********************************************************
--* VHS教学 (RadioXp) Lua 侧: 锚点设备扫描 + 技能表 + 统一升级入口
--* 锚点必须是注册进 Zomboid.devices 的设备 (DistributeTransmission
--* 只遍历该列表): IsoRadio / IsoTelevision 世界家具物件, 或车载电台
--* (VehiclePart, 部件 id = "Radio")。手放的 Radio 物品不注册, 不能做锚点。
--* Java 侧 radioXpBroadcast(channel, isTv, codes, amount) 伪造 WaveSignalPacket,
--* 服务端经 OnDeviceText 官方 addXp 账本推技能 XP (封顶 LevelForMediaXPCutoff)。
--*********************************************************

EtherRadioXp = EtherRadioXp or {};

--*********************************************************
--* 技能 Perk 名 -> 电台交互 code (ISRadioInteractions.lua:109-147 全表)。
--* 只有此表覆盖的技能可经 VHS 教学推 XP; 服务端每 code 30 秒冷却,
--* 每次生效 XP = 50 × amount, 等级封顶 = LevelForMediaXPCutoff (默认 3)。
--*********************************************************
EtherRadioXp.PERK_CODE = {
    Sprinting = "SPR", Lightfoot = "LFT", Nimble = "NIM", Sneak = "SNE",
    Axe = "BAA", Blunt = "BUA",
    Woodwork = "CRP", Cooking = "COO", Farming = "FRM", Doctor = "DOC",
    Electricity = "ELC", MetalWelding = "MTL", FlintKnapping = "FKN", Carving = "CRV",
    Aiming = "AIM", Reloading = "REL",
    Fishing = "FIS", Trapping = "TRA", PlantScavenging = "FOR",
    Tailoring = "TAI", Mechanics = "MEC",
    Combat = "CMB", Spear = "SPE", SmallBlunt = "SBU", LongBlade = "LBA", SmallBlade = "SBA",
    Masonry = "MAS", Pottery = "POT", Blacksmith = "BLA", Glassmaking = "GLA",
    Husbandry = "HUS", Butchering = "BUT", Tracking = "TRK",
};

-- 可训练技能列表 (仅 PERK_CODE 覆盖的), 供 VHS教学选择列表。
-- API 用 vanilla 同款链 (ISPlayerStatsUI.lua:720): 全局 Perks + PerkFactory.getPerk。
-- 键匹配必须用 getId() (枚举常量名, 稳定) —— getName() 在 initTranslations
-- (PerkFactory.java:112-115) 后返回本地化文本 (中文客户端 "金工"), 匹配不到 code。
-- 返回 { {name=Perk id, label=本地化名, code=三字码}, ... } 按显示名排序。
function EtherRadioXp.trainablePerks()
    local out = {};
    local ok, err = pcall(function()
        local maxIndex = Perks.getMaxIndex();
        for i = 0, maxIndex - 1 do
            local perk = PerkFactory.getPerk(Perks.fromIndex(i));
            if perk ~= nil then
                local name = perk:getId();
                local code = (name ~= nil) and EtherRadioXp.PERK_CODE[name] or nil;
                if code ~= nil then
                    local label = getText("IGUI_perks_" .. name);
                    if label == nil or label == "" then label = name; end
                    table.insert(out, { name = name, label = label, code = code });
                end
            end
        end
    end);
    if not ok then
        print("[EtherHack] VHS trainablePerks error: " .. tostring(err));
    end
    table.sort(out, function(a, b) return string.sort(a.label, b.label); end);
    return out;
end

--*********************************************************
--* 自动放置收音机锚点 (升级按钮无锚点时默认触发): 从背包选一台收音机,
--* 尽力预开机+音量 (DeviceData 的 setIsTurnedOn/setDeviceVolume 自带
--* 客户端->服务器状态同步包, DeviceData.transmitDeviceDataState:932-940),
--* 然后打开原版放置光标 (与"把物品拖出背包放到世界"完全同一条链,
--* ISInventoryPane.lua:1942-1949; 放置后 placeMoveableInternal:2129-2143
--* 把物品 DeviceData 原样转给 IsoRadio 并注册进服务端 devices —— MP 安全,
--* 服务端经交易系统自己创建设备)。
--* 收音机需要电池(或所在建筑通电)才能开机。
--* 返回 true = 已打开放置光标; false = 背包没有可放置的收音机。
--*********************************************************
function EtherRadioXp.placeAnchor()
    local player = getPlayer();
    if player == nil then return false; end

    -- 找背包里第一台可放置的收音机 (Radio 类且有世界形态)
    local items = player:getInventory():getItems();
    local radio = nil;
    for i = 0, items:size() - 1 do
        local it = items:get(i);
        if it ~= nil and instanceof(it, "Radio") and it:getWorldSprite() ~= nil then
            radio = it;
            break;
        end
    end
    if radio == nil then
        return false;
    end

    -- 尽力预开机+音量 (电池没电且无外电时 setIsTurnedOn 会拒绝, 只提示不阻断 ——
    -- 放好后仍可经原版"设备选项"手动开机)
    local dd = radio:getDeviceData();
    if dd ~= nil then
        local canPower = (not dd:getIsBatteryPowered()) or dd:getPower() > 0 or dd:canBePoweredHere();
        if canPower then
            dd:setIsTurnedOn(true);
            dd:setDeviceVolume(1.0);   -- DeviceData 内部音量标度 0-1 (setDeviceVolume 钳制), 1.0 = UI 音量条 10/10 满格, 满足"5以上"门槛
            EtherRadioXp.placePending = true;   -- OnObjectAdded 里二次保险 (见下)
        else
            HaloTextHelper.addText(player, getTranslate("UI_RadioXp_NeedBattery"), "[br/]", HaloTextHelper.getColorRed());
        end
    end

    -- 原版放置光标 (与拖拽物品到世界同款调用)
    local mo = ISMoveableCursor:new(player);
    getCell():setDrag(mo, mo.player);
    mo:setMoveableMode("place");
    mo:tryInitialItem(radio);
    HaloTextHelper.addText(player, getTranslate("UI_RadioXp_Placing"), "[br/]", HaloTextHelper.getColorGreen());
    return true;
end

-- 二次保险: placeMoveableInternal 添加对象后触发 OnObjectAdded (Lua 侧 :2382),
-- 若预开机因时序未同步成功, 此处对落地的 IsoRadio 再补一次 (setter 自带上行)。
local function onObjectAdded(obj)
    if not EtherRadioXp.placePending then return; end
    if obj ~= nil and instanceof(obj, "IsoRadio") then
        EtherRadioXp.placePending = nil;
        local dd = obj:getDeviceData();
        if dd ~= nil then
            if not dd:getIsTurnedOn() then dd:setIsTurnedOn(true); end
            if dd:getDeviceVolume() < 0.5 then dd:setDeviceVolume(1.0); end
        end
    end
end
Events.OnObjectAdded.Add(onObjectAdded);

--*********************************************************
--* 统一升级入口 (VHS教学两个按钮共用)。
--* code = 三字码 (升级单技能) 或 nil (全技能)。
--* 流程: MP 检查 -> 30s 本地冷却 -> 锚点扫描 -> 发包 -> 头顶反馈。
--*********************************************************
function EtherRadioXp.upgrade(code)
    local player = getPlayer();
    if player == nil or not isMultiplayer() then return; end
    if type(radioXpBroadcast) ~= "function" then return; end
    if EtherRadioXp.cooldownUntil
        and Calendar.getInstance():getTimeInMillis() / 1000 < EtherRadioXp.cooldownUntil then
        HaloTextHelper.addText(player, getTranslate("UI_RadioXp_Cooldown"), "[br/]", HaloTextHelper.getColorRed());
        return;
    end
    -- 锚点: 扫描周围 10 格开着且音量≥5格 (内部 0.5) 的设备 (家具收音机/电视/车载电台)
    local anchor = EtherRadioXp.findAnchor(10);
    if anchor == nil then
        -- 无锚点 → 尝试自动放置收音机 (打开放置光标, 放好后玩家再点一次升级);
        -- 背包也没有收音机 → 明确提示且不发包
        if EtherRadioXp.placeAnchor() then
            return;   -- 光标已打开, 等放置完成后再点升级
        end
        HaloTextHelper.addText(player, getTranslate("UI_RadioXp_NoAnchor"), "[br/]", HaloTextHelper.getColorRed());
        return;
    end
    local codes;
    if code ~= nil then
        codes = code;
    else
        local parts = {};
        for _, c in pairs(EtherRadioXp.PERK_CODE) do
            table.insert(parts, c);
        end
        codes = table.concat(parts, ",");
    end
    if radioXpBroadcast(anchor.channel, anchor.isTv, codes, 99) then
        EtherRadioXp.cooldownUntil = Calendar.getInstance():getTimeInMillis() / 1000 + 30;
        HaloTextHelper.addText(player, getTranslate("UI_RadioXp_Done"), "[br/]", HaloTextHelper.getColorGreen());
    end
end

-- 检查一个设备对象是否可用锚点 (开着 + 音量达标)。
-- 音量标度: DeviceData.deviceVolume 内部是 0-1 浮点 (setDeviceVolume 把 >1 钳回 1.0),
-- 游戏内设备选项的音量条是 10 格 (ISVolumeBar.volumeSteps=10, 内部值 = 显示格数/10)。
-- 门槛"音量5以上" = 内部 0.5。勿写成 5 —— 内部最大 1.0, <5 恒真会永远找不到锚点。
-- 返回 { channel=, isTv= } 或 nil。
local function anchorFromDevice(dev)
    if dev == nil then return nil end
    local dd = dev:getDeviceData();
    if dd == nil then return nil end
    if not dd:getIsTurnedOn() then return nil end
    if dd:getDeviceVolume() < 0.5 then return nil end
    return { channel = dd:getChannel(), isTv = dd:getIsTelevision() };
end

-- 扫描半径 radius 格内的可用锚点:
--   1) 车载电台 (玩家在载具里, part id "Radio");
--   2) 方块物件中的 IsoRadio / IsoTelevision (家具收音机/电视)。
-- 注意: 不要碰 veh:getParts() —— VehicleParts 类未暴露给 Kahlua, 索引即抛
-- "attempted index of non-table"。getPartById/BaseVehicle/VehiclePart 均已暴露。
-- 返回 { channel=, isTv= } 或 nil。
function EtherRadioXp.findAnchor(radius)
    local player = getPlayer();
    if player == nil then return nil end

    local veh = player:getVehicle();
    if veh ~= nil then
        local ok, a = pcall(anchorFromDevice, veh:getPartById("Radio"));
        if ok and a ~= nil then return a; end
    end

    local px = math.floor(player:getX());
    local py = math.floor(player:getY());
    local pz = player:getZ();
    for dy = -radius, radius do
        for dx = -radius, radius do
            local sq = getCell():getGridSquare(px + dx, py + dy, pz);
            if sq ~= nil then
                local objects = sq:getObjects();
                if objects ~= nil then
                    for i = 0, objects:size() - 1 do
                        local obj = objects:get(i);
                        if obj ~= nil and (instanceof(obj, "IsoRadio") or instanceof(obj, "IsoTelevision")) then
                            local ok, a = pcall(anchorFromDevice, obj);
                            if ok and a ~= nil then return a; end
                        end
                    end
                end
            end
        end
    end
    return nil;
end
