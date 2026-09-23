--*********************************************************
--* 免费建造 POC (BuildSpawn, multiplayer)
--* 链: 客户端现造合法 ghost (各 Type.new, 纯本地构造) ->
--*   原版 createBuildAction 发包 (BuildActionPacket, 无材料表, Type 无白名单) ->
--*   服务端只验 playerId 即 Accept + 重建 ->
--*   遗留 create() 缺料仅打印不 abort, 无条件放置 = 零料直建。
--* 用法: 建造页列表选物 -> 进摆放 -> 蓝图跟鼠标 (自动朝向 + R) ->
--*   按 "`" 读蓝图当前格/朝向直发 -> 自动下新蓝图连建。
--*   包里与 3x3 地面不能有可扣材料 (有就真扣, 只有缺料才走 fail-open)。
--* 数据驱动 (一百七十三): 条目 = 数据 {nameKey/name, type, sprite,
--*   northSprite, args, nargs}, 加东西改 BuildCatalogCustom.lua 即可。
--*   占位符: $sprite/$northSprite/$player/$playernum/$null, 其余字面量。
--* 条目准入: 遗留 fail-open create + 真实精灵对 + 构造器可调;
--* 新实体/陷阱/特殊分支一律不进表 (条目用占位精灵只验放置)。
--*********************************************************
EtherBuildSpawn = EtherBuildSpawn or {}

EtherBuildSpawn.message = ""
EtherBuildSpawn.ghost = nil
EtherBuildSpawn.lastEntry = nil

-- 已知类直引 (文件加载时求值, 缺失即 nil, 安全):
-- 未知/mod 类走 _G (见 resolveClass)。
local KNOWN = {
    ISWoodenWall = ISWoodenWall,
    ISWoodenStairs = ISWoodenStairs,
    ISSimpleFurniture = ISSimpleFurniture,
    ISWoodenContainer = ISWoodenContainer,
    ISCompost = ISCompost,
    RainCollectorBarrel = RainCollectorBarrel,
    campingCampfire = campingCampfire,
    ISWoodenFloor = ISWoodenFloor,
    ISWoodenDoorFrame = ISWoodenDoorFrame,
    ISWoodenDoor = ISWoodenDoor,
    ISDoubleDoor = ISDoubleDoor,
    ISBarbedWire = ISBarbedWire,
    ISButcheringHook = ISButcheringHook,
    ISEmptyGraves = ISEmptyGraves,
    ISLightSource = ISLightSource,
    ISDoubleTileFurniture = ISDoubleTileFurniture,
}

EtherBuildSpawn.catalog = {
    { nameKey = "UI_Build_Wall_Log", type = "ISWoodenWall",
      sprite = "carpentry_02_80", northSprite = "carpentry_02_81",
      args = {"$sprite", "$northSprite", false}, nargs = 3 },
    { nameKey = "UI_Build_Wall_A", type = "ISWoodenWall",
      sprite = "constructedobjects_01_48", northSprite = "constructedobjects_01_49",
      args = {"$sprite", "$northSprite", false}, nargs = 3 },
    { nameKey = "UI_Build_Wall_B", type = "ISWoodenWall",
      sprite = "constructedobjects_01_64", northSprite = "constructedobjects_01_65",
      args = {"$sprite", "$northSprite", false}, nargs = 3 },
    { nameKey = "UI_Build_Stairs", type = "ISWoodenStairs",
      sprite = "carpentry_02_88", northSprite = "carpentry_02_96",
      args = {"carpentry_02_88", "carpentry_02_89", "carpentry_02_90",
              "carpentry_02_96", "carpentry_02_97", "carpentry_02_98",
              "carpentry_02_94", "carpentry_02_95"}, nargs = 8 },
    { nameKey = "UI_Build_Shelves", type = "ISSimpleFurniture",
      sprite = "furniture_shelving_01_40", northSprite = "furniture_shelving_01_41",
      args = {"Shelves", "$sprite", "$northSprite"}, nargs = 3 },
    { nameKey = "UI_Build_Fence41", type = "ISSimpleFurniture",
      sprite = "carpentry_02_41", northSprite = "carpentry_02_41",
      args = {"Fence", "$sprite", "$northSprite"}, nargs = 3 },
    { nameKey = "UI_Build_Fence40", type = "ISSimpleFurniture",
      sprite = "carpentry_02_40", northSprite = "carpentry_02_40",
      args = {"Fence", "$sprite", "$northSprite"}, nargs = 3 },
    { nameKey = "UI_Build_Crate", type = "ISWoodenContainer",
      sprite = "carpentry_01_19", northSprite = "carpentry_01_19",
      args = {"$sprite", "$northSprite"}, nargs = 2 },
    { nameKey = "UI_Build_Compost", type = "ISCompost",
      sprite = "camping_01_19", northSprite = "camping_01_19",
      args = {"Compost Bin", "$sprite"}, nargs = 2 },
    { nameKey = "UI_Build_Rain", type = "RainCollectorBarrel",
      sprite = "carpentry_02_54", northSprite = "carpentry_02_54",
      args = {"$player", "$sprite", 400}, nargs = 3 },
    { nameKey = "UI_Build_Campfire", type = "campingCampfire",
      sprite = "camping_01_6", northSprite = "camping_01_6",
      args = {"$player"}, nargs = 1 },
    { nameKey = "UI_Build_Floor", type = "ISWoodenFloor",
      sprite = "carpentry_02_56", northSprite = "carpentry_02_56",
      args = {"$sprite", "$northSprite"}, nargs = 2 },
    { nameKey = "UI_Build_DoorFrame_T", type = "ISWoodenDoorFrame",
      sprite = "walls_exterior_wooden_01_50", northSprite = "walls_exterior_wooden_01_51",
      args = {"$sprite", "$northSprite", false}, nargs = 3 },
    { nameKey = "UI_Build_Door_T", type = "ISWoodenDoor",
      sprite = "carpentry_01_52", northSprite = "carpentry_01_53",
      args = {"carpentry_01_52", "carpentry_01_53", "carpentry_01_54", "carpentry_01_55"}, nargs = 4 },
    { nameKey = "UI_Build_DoubleDoor_A", type = "ISDoubleDoor",
      sprite = "fixtures_doors_fences_01_56", northSprite = "fixtures_doors_fences_01_50",
      args = {"fixtures_doors_fences_01_", 56}, nargs = 2 },
    { nameKey = "UI_Build_DoubleDoor_B", type = "ISDoubleDoor",
      sprite = "fixtures_doors_fences_01_104", northSprite = "fixtures_doors_fences_01_98",
      args = {"fixtures_doors_fences_01_", 104}, nargs = 2 },
    { nameKey = "UI_Build_Barbed_T", type = "ISBarbedWire",
      sprite = "fencing_01_20", northSprite = "fencing_01_21",
      args = {"$sprite", "$northSprite"}, nargs = 2 },
    { nameKey = "UI_Build_Hook_T", type = "ISButcheringHook",
      sprite = "crafted_04_120", northSprite = "crafted_04_120",
      args = {"Hook", "$sprite"}, nargs = 2 },
    { nameKey = "UI_Build_Graves_T", type = "ISEmptyGraves",
      sprite = "location_community_cemetary_01_33", northSprite = "location_community_cemetary_01_34",
      args = {"location_community_cemetary_01_33", "location_community_cemetary_01_32",
              "location_community_cemetary_01_34", "location_community_cemetary_01_35", "$null"}, nargs = 5 },
    { nameKey = "UI_Build_Light_T", type = "ISLightSource",
      sprite = "carpentry_02_59", northSprite = "carpentry_02_60",
      args = {"$sprite", "$northSprite", "$player"}, nargs = 3 },
    { nameKey = "UI_Build_DoubleTile_T", type = "ISDoubleTileFurniture",
      sprite = "furniture_storage_01_2", northSprite = "furniture_storage_01_6",
      args = {"Cabinet", "furniture_storage_01_2", "furniture_storage_01_3",
              "furniture_storage_01_6", "furniture_storage_01_7"}, nargs = 5 },
}

-- 用户自定义条目合并 (后加载覆盖重名; BuildCatalogCustom.lua 见文件头格式)
if type(BuildCatalogCustom) == "table" then
    for _, e in ipairs(BuildCatalogCustom) do
        if e ~= nil and e.type ~= nil then
            table.insert(EtherBuildSpawn.catalog, e);
        end
    end
end

--*********************************************************
--* 扫描mod建筑 (放宽标准版): 列出已装 mod 里 ISBuildingObject 派生类
--* 的 Type + 构造器签名, 结果打控制台 + 状态行计数。
--* 注意: 只解决"有什么", 精灵与放行性仍要人工补 (见 BuildCatalogCustom
--* 文件头三铁律); 列出 ≠ 保证能造。
--*********************************************************
function EtherBuildSpawn.scanMods()
    if type(scanModBuildings) ~= "function" then
        EtherBuildSpawn.message = getTranslate("UI_Build_Unavailable")
        return
    end
    local ok, res = pcall(function() return scanModBuildings() end)
    if not ok or res == nil then
        EtherBuildSpawn.message = getTranslate("UI_Build_Unavailable")
        return
    end
    print("[ModScan]\n" .. tostring(res))
    local n = 0;
    for _ in string.gmatch(tostring(res), "[^\n]+") do
        n = n + 1;
    end
    if string.find(tostring(res), "^error") == 1
        or string.find(tostring(res), "^no ") == 1
        or string.find(tostring(res), "^none") == 1 then
        EtherBuildSpawn.message = tostring(res);
    else
        EtherBuildSpawn.message = "mod candidates: " .. tostring(n)
            .. " (detail in console/log)";
    end
end

function EtherBuildSpawn.entryName(e)
    if e == nil then return "?" end
    if e.nameKey ~= nil then
        local ok, s = pcall(function() return getTranslate(e.nameKey) end)
        if ok and s ~= nil then return s end
        return e.nameKey;
    end
    return e.name or "?";
end

local function resolveClass(typeName)
    if typeName == nil then return nil end
    local cls = KNOWN[typeName];
    if cls ~= nil then return cls end
    local ok, g = pcall(function() return _G end)
    if ok and type(g) == "table" then
        return g[typeName];
    end
    return nil;
end

local function resolveArgs(e, player)
    local out = {};
    local n = e.nargs or #(e.args or {});
    for i = 1, n do
        local v = e.args[i];
        if v == "$sprite" then v = e.sprite;
        elseif v == "$northSprite" then v = e.northSprite;
        elseif v == "$player" then v = player;
        elseif v == "$playernum" then v = player:getPlayerNum();
        elseif v == "$null" then v = nil; end
        out[i] = v;
    end
    return out, n;
end

function EtherBuildSpawn.makeGhost(e, player)
    if e == nil or e.type == nil then return nil end
    local cls = resolveClass(e.type);
    if type(cls) ~= "table" or type(cls.new) ~= "function" then
        return nil;
    end
    local args, n = resolveArgs(e, player);
    return cls.new(cls, unpack(args, 1, n));
end

--*********************************************************
--* 进入/退出摆放模式: spawn 原版拖影 (只做蓝图, 不接管点击)。
--* 拖影跟随鼠标 + 自动朝向 (rotateMouse) + R 手动 cycle (rotateKey),
--* 全部原版逻辑。确认走确认键 (直发, 见 confirmFromGhost)。
--* 退出 (按钮/ESC/右键清掉拖影) 无需还原任何东西 (零 override)。
--*********************************************************
function EtherBuildSpawn.enterPlacement(entry)
    if not isMultiplayer() then
        EtherBuildSpawn.message = getTranslate("UI_FishSpawn_MultiplayerOnly")
        return
    end
    local player = getPlayer()
    if player == nil or entry == nil then
        return
    end
    EtherBuildSpawn.exitPlacement(true);
    local ok, ghost = pcall(function()
        return EtherBuildSpawn.makeGhost(entry, player)
    end)
    if not ok or ghost == nil then
        print("[BuildSpawn] ghost build failed: " .. tostring(entry.type))
        EtherBuildSpawn.message = getTranslate("UI_Build_Unavailable")
        .. ": " .. tostring(entry.type);
        return
    end
    ghost.player = player:getPlayerNum();
    ghost.isBuildGhost = true;
    local ok2 = pcall(function()
        getCell():setDrag(ghost, player:getPlayerNum());
    end)
    if not ok2 then
        EtherBuildSpawn.message = getTranslate("UI_Build_Unavailable")
        return
    end
    EtherBuildSpawn.ghost = ghost;
    EtherBuildSpawn.lastEntry = entry;
    EtherBuildSpawn.message = getTranslate("UI_Build_Placed");
end

function EtherBuildSpawn.exitPlacement(silent)
    local player = getPlayer();
    if player ~= nil and EtherBuildSpawn.ghost ~= nil then
        pcall(function()
            if getCell():getDrag(player:getPlayerNum()) == EtherBuildSpawn.ghost then
                getCell():setDrag(nil, player:getPlayerNum());
            end
        end)
    end
    EtherBuildSpawn.ghost = nil;
    if not silent then
        EtherBuildSpawn.message = "";
    end
end

--*********************************************************
--* 确认键 (默认 "`", 可改绑): 读拖影当前格/朝向/精灵, 直发建造。
--* 位置取鼠标格 (拖影渲染格), 朝向/精灵取 ghost:getSprite() 现算值
--* (该调用会同步刷新 north, 与原版 tryBuild 同源)。
--* 确认后自动下新蓝图连建 (同条目); ESC/按钮退出摆放。
--*********************************************************
function EtherBuildSpawn.confirmFromGhost()
    if not isMultiplayer() then
        return
    end
    local player = getPlayer()
    if player == nil or EtherBuildSpawn.ghost == nil then
        return
    end
    local drag = nil;
    pcall(function()
        drag = getCell():getDrag(player:getPlayerNum());
    end)
    if drag ~= EtherBuildSpawn.ghost then
        EtherBuildSpawn.message = getTranslate("UI_Build_Unavailable")
        return
    end
    local sq, wx, wy, wz = EtherPick.hoverSquare();
    if sq == nil or wx == nil then
        return
    end
    local x, y, z = math.floor(wx), math.floor(wy), math.floor(wz or 0);
    local ok, sprite = pcall(function() return drag:getSprite() end)
    if not ok or sprite == nil then
        return
    end
    local north = drag.north;
    local entry = EtherBuildSpawn.lastEntry;
    local ok2 = pcall(function()
        return createBuildAction(player, x, y, z, north, sprite, drag)
    end)
    if not ok2 then
        EtherBuildSpawn.message = getTranslate("UI_Build_Unavailable")
        return
    end
    local label = entry ~= nil and EtherBuildSpawn.entryName(entry) or "build";
    local res = "sent " .. label .. " @ " .. tostring(x) .. ","
        .. tostring(y) .. "," .. tostring(z);
    print("[BuildSpawn] " .. res)
    -- 自动下新蓝图: 确认后旧拖影换新, 不用反复开关摆放模式。
    EtherBuildSpawn.exitPlacement(true);
    if entry ~= nil then
        EtherBuildSpawn.enterPlacement(entry);
    end
    EtherBuildSpawn.message = res
end
